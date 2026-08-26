package com.kidsexplore.app

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kidsexplore.app.data.GateLock
import com.kidsexplore.app.data.SharedPreferencesThemeStore
import com.kidsexplore.app.data.ThemeStore
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.model.ThemeDef
import kotlin.random.Random

data class GateQuestion(val a: Int, val b: Int, val correct: Int, val values: List<Int>)

/**
 * The whole app is one of these at a time.
 *
 * Modelled as a sealed type rather than a screen enum alongside separate
 * `themeId` / `imageIndex` / `gate` fields: those could disagree, and the only
 * thing the UI could do about an impossible combination was render nothing —
 * a blank screen with no way out. Here a Viewer cannot exist without a theme
 * and a Gate cannot exist without a question.
 */
sealed interface UiState {
    data object Home : UiState

    data class Viewer(val themeId: String, val imageIndex: Int) : UiState

    data class Gate(
        val question: GateQuestion,
        val wrong: Boolean,
        /** Wall-clock deadline; 0 when the answers are not locked out. */
        val lockedUntilWallMs: Long,
    ) : UiState

    data object Settings : UiState
}

/** Wrong answers allowed before the gate stops accepting taps for a while. */
internal const val MAX_GATE_FAILURES = 3
internal const val GATE_LOCKOUT_MS = 30_000L

/**
 * Distractor distances from the correct sum. Deliberately never ±1: keeping
 * every wrong answer adjacent to the right one, as this used to, rewards a
 * child who can nearly add.
 */
private val DISTRACTOR_OFFSETS =
    listOf(-9, -8, -7, -6, -5, -4, -3, -2, 2, 3, 4, 5, 6, 7, 8, 9)

// internal rather than private so the unit tests can seed a SavedStateHandle
// with a deliberately out-of-range index without duplicating these literals.
internal const val KEY_SCREEN = "screen"
internal const val KEY_VIEWER_THEME = "viewer_theme_id"
internal const val KEY_VIEWER_INDEX = "viewer_index"

private const val SCREEN_HOME = "home"
private const val SCREEN_VIEWER = "viewer"

class AppViewModel(
    private val store: ThemeStore,
    private val savedState: SavedStateHandle,
    private val random: Random = Random.Default,
    private val wallClock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    var uiState: UiState by mutableStateOf(restoreState())
        private set

    var disabledThemeIds: Set<String> by mutableStateOf(store.loadDisabled())
        private set

    // Failure count and lockout live in the store, not in savedState: saved
    // instance state is dropped when the Activity finishes, and Back from Home
    // finishes it, so a child could clear a lockout by leaving and relaunching.
    // They reset only on a correct answer.
    private var gateFailures: Int
    private var gateLockedUntil: Long

    init {
        val lock = store.loadGateLock()
        gateFailures = lock.failures.coerceAtLeast(0)

        // The deadline is wall-clock so it can outlive the process, which means
        // a device clock moved backwards can leave one stored arbitrarily far
        // in the future. Pull it back to one lockout from now — once, here, on
        // load, and write the correction back. Clamping on every read instead
        // would recompute "now + 30s" forever and never let the parent in.
        val now = wallClock()
        val ceiling = now + GATE_LOCKOUT_MS
        gateLockedUntil = lock.lockedUntilWallMs
        if (gateLockedUntil > ceiling) {
            gateLockedUntil = ceiling
            persistGateLock()
        }
    }

    // derivedStateOf, not a plain getter: a getter re-filtered on every read and
    // handed HomeScreen a new List each time. `List<ThemeDef>` is unstable to
    // the Compose compiler whatever ThemeDef is annotated with, so strong
    // skipping compares it by reference — and a fresh instance never matched,
    // which meant HomeScreen could never skip. This recomputes only when
    // disabledThemeIds actually changes.
    val visibleThemes: List<ThemeDef> by derivedStateOf {
        THEME_DEFS.filter { it.id !in disabledThemeIds }
    }

    fun goHome() {
        transitionTo(UiState.Home)
    }

    fun openTheme(id: String) {
        // Ignore ids that don't resolve rather than entering a Viewer that
        // cannot render — the sealed state can't express "Viewer, no theme".
        if (THEME_DEFS.none { it.id == id }) return
        transitionTo(UiState.Viewer(themeId = id, imageIndex = 0))
    }

    fun next() = stepImage(+1)

    fun prev() = stepImage(-1)

    private fun stepImage(delta: Int) {
        val state = uiState as? UiState.Viewer ?: return
        val count = THEME_DEFS.find { it.id == state.themeId }?.imageRes?.size ?: return
        // Modulo twice: Kotlin's % takes the sign of the dividend, so stepping
        // back from 0 gives -1 rather than the last index. Adding count before
        // the second % is what wraps it around.
        val index = ((state.imageIndex + delta) % count + count) % count
        transitionTo(state.copy(imageIndex = index))
    }

    fun openGate() {
        transitionTo(
            UiState.Gate(
                question = buildGateQuestion(),
                wrong = false,
                lockedUntilWallMs = activeLockDeadline(),
            )
        )
    }

    fun pickGateAnswer(value: Int) {
        val state = uiState as? UiState.Gate ?: return
        // Authoritative check: the screen also disables the buttons while
        // locked, but it must not be the only thing enforcing this.
        if (activeLockDeadline() > 0L) return

        if (value == state.question.correct) {
            gateFailures = 0
            gateLockedUntil = 0L
            persistGateLock()
            transitionTo(UiState.Settings)
            return
        }

        gateFailures++
        if (gateFailures >= MAX_GATE_FAILURES) {
            gateLockedUntil = wallClock() + GATE_LOCKOUT_MS
            gateFailures = 0
        }
        persistGateLock()

        // A fresh question every time. Leaving the same one up let a child
        // exhaust all four buttons and reach Settings in at most four taps —
        // and rotating it is also what stops a relaunch from being a free
        // retry at a question they have already seen.
        transitionTo(
            state.copy(
                question = buildGateQuestion(),
                wrong = true,
                lockedUntilWallMs = activeLockDeadline(),
            )
        )
    }

    fun toggleThemeEnabled(id: String) {
        val updated = disabledThemeIds.toMutableSet()
        if (!updated.remove(id)) updated.add(id)
        disabledThemeIds = updated.toSet()
        store.saveDisabled(disabledThemeIds)
    }

    /**
     * The lockout deadline if one is still in the future, otherwise 0.
     *
     * A plain comparison: `init` has already capped anything a clock change
     * could have left out of range, so there is nothing to re-clamp here.
     */
    private fun activeLockDeadline(): Long =
        gateLockedUntil.takeIf { it > wallClock() } ?: 0L

    private fun buildGateQuestion(): GateQuestion {
        val a = 2 + random.nextInt(6)
        val b = 2 + random.nextInt(6)
        val correct = a + b
        // Drawn from a fixed pool rather than rejection-sampled in a loop that
        // only terminates by accident of the operand range.
        val wrongs = DISTRACTOR_OFFSETS
            .map { correct + it }
            .filter { it > 0 }
            .distinct()
            .shuffled(random)
            .take(3)
        return GateQuestion(a, b, correct, (wrongs + correct).shuffled(random))
    }

    private fun transitionTo(next: UiState) {
        uiState = next
        persistState(next)
    }

    private fun persistState(state: UiState) {
        // Only Home and Viewer are restorable. Coming back from process death
        // into Settings would hand a child the parent screen without the gate,
        // and a half-answered gate is not worth preserving.
        when (state) {
            is UiState.Viewer -> {
                savedState[KEY_SCREEN] = SCREEN_VIEWER
                savedState[KEY_VIEWER_THEME] = state.themeId
                savedState[KEY_VIEWER_INDEX] = state.imageIndex
            }

            else -> {
                savedState[KEY_SCREEN] = SCREEN_HOME
                savedState[KEY_VIEWER_THEME] = null
                savedState[KEY_VIEWER_INDEX] = null
            }
        }
    }

    private fun persistGateLock() {
        store.saveGateLock(GateLock(failures = gateFailures, lockedUntilWallMs = gateLockedUntil))
    }

    private fun restoreState(): UiState {
        if (savedState.get<String>(KEY_SCREEN) != SCREEN_VIEWER) return UiState.Home
        val themeId = savedState.get<String>(KEY_VIEWER_THEME) ?: return UiState.Home
        val theme = THEME_DEFS.find { it.id == themeId } ?: return UiState.Home
        val index = (savedState.get<Int>(KEY_VIEWER_INDEX) ?: 0).coerceIn(theme.imageRes.indices)
        return UiState.Viewer(themeId = themeId, imageIndex = index)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]!!
                AppViewModel(
                    store = SharedPreferencesThemeStore(application),
                    savedState = createSavedStateHandle(),
                )
            }
        }
    }
}
