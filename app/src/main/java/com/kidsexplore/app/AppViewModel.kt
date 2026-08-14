package com.kidsexplore.app

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
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
        /** Elapsed-realtime deadline; 0 when the answers are not locked out. */
        val lockedUntilElapsedMs: Long,
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
private const val KEY_GATE_FAILURES = "gate_failures"
private const val KEY_GATE_LOCKED_UNTIL = "gate_locked_until"

private const val SCREEN_HOME = "home"
private const val SCREEN_VIEWER = "viewer"

class AppViewModel(
    private val store: ThemeStore,
    private val savedState: SavedStateHandle,
    private val random: Random = Random.Default,
    private val elapsedRealtime: () -> Long = SystemClock::elapsedRealtime,
) : ViewModel() {

    var uiState: UiState by mutableStateOf(restoreState())
        private set

    var disabledThemeIds: Set<String> by mutableStateOf(store.loadDisabled())
        private set

    // Failure count and lockout deliberately live outside UiState: a lockout
    // a child can clear by tapping Cancel and reopening the gate is no lockout
    // at all, so these survive leaving the gate. They reset only on success.
    private var gateFailures: Int = savedState[KEY_GATE_FAILURES] ?: 0
    private var gateLockedUntil: Long = savedState[KEY_GATE_LOCKED_UNTIL] ?: 0L

    val visibleThemes: List<ThemeDef>
        get() = THEME_DEFS.filter { it.id !in disabledThemeIds }

    val activeTheme: ThemeDef?
        get() = (uiState as? UiState.Viewer)?.let { state ->
            THEME_DEFS.find { it.id == state.themeId }
        }

    val currentLabel: String?
        get() = (uiState as? UiState.Viewer)?.let { state ->
            activeTheme?.labels?.getOrNull(state.imageIndex)
        }

    fun isThemeEnabled(id: String): Boolean = id !in disabledThemeIds

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
        val count = THEME_DEFS.find { it.id == state.themeId }?.labels?.size ?: return
        val index = ((state.imageIndex + delta) % count + count) % count
        transitionTo(state.copy(imageIndex = index))
    }

    fun openGate() {
        transitionTo(
            UiState.Gate(
                question = buildGateQuestion(),
                wrong = false,
                lockedUntilElapsedMs = activeLockDeadline(),
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
            gateLockedUntil = elapsedRealtime() + GATE_LOCKOUT_MS
            gateFailures = 0
        }
        persistGateLock()

        // A fresh question every time. Leaving the same one up let a child
        // exhaust all four buttons and reach Settings in at most four taps.
        transitionTo(
            state.copy(
                question = buildGateQuestion(),
                wrong = true,
                lockedUntilElapsedMs = activeLockDeadline(),
            )
        )
    }

    fun toggleThemeEnabled(id: String) {
        val updated = disabledThemeIds.toMutableSet()
        if (!updated.remove(id)) updated.add(id)
        disabledThemeIds = updated
        store.saveDisabled(updated)
    }

    /** The lockout deadline if one is still in the future, otherwise 0. */
    private fun activeLockDeadline(): Long =
        gateLockedUntil.takeIf { it > elapsedRealtime() } ?: 0L

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
        savedState[KEY_GATE_FAILURES] = gateFailures
        savedState[KEY_GATE_LOCKED_UNTIL] = gateLockedUntil
    }

    private fun restoreState(): UiState {
        if (savedState.get<String>(KEY_SCREEN) != SCREEN_VIEWER) return UiState.Home
        val themeId = savedState.get<String>(KEY_VIEWER_THEME) ?: return UiState.Home
        val theme = THEME_DEFS.find { it.id == themeId } ?: return UiState.Home
        val index = (savedState.get<Int>(KEY_VIEWER_INDEX) ?: 0).coerceIn(theme.labels.indices)
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
