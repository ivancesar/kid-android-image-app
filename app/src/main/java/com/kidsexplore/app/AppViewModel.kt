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

    /**
     * The privacy policy, reached from Settings and returning to it.
     *
     * Its own state rather than a flag on [Settings]: Back has to mean "back to
     * Settings" here and "back to Home" there, and a boolean inside Settings
     * would make that a property of a screen rather than a screen of its own.
     */
    data object Policy : UiState
}

/** Wrong answers per round, after which the gate stops accepting taps for a while. */
internal const val MAX_GATE_FAILURES = 3

/** How long the first lockout lasts. Every one after it is twice the last. */
internal const val GATE_LOCKOUT_MS = 30_000L

/**
 * Where the doubling stops: 30s, 1m, 2m, 4m, 8m, 8m, 8m…
 *
 * A ceiling rather than unbounded growth, because the person a very long
 * lockout actually punishes is the parent who mistyped — and eight minutes is
 * already long enough that grinding the gate stops being a game. Four buttons
 * means a round of three wrong answers clears by luck about 58% of the time,
 * so what has to be expensive is *repeating* rounds, not any one of them.
 */
internal const val MAX_GATE_LOCKOUT_MS = 8 * 60 * 1000L

/**
 * How long the lockout lasts for a gate that has accumulated [failures].
 *
 * Derived from the failure count rather than stored beside it, so the persisted
 * [GateLock] schema is unchanged and an install that already has failures on
 * disk carries them straight over.
 *
 * A count below one full round only happens on an install written by the
 * version that reset the count when the lockout armed; it is read as the first
 * level, which is exactly what that version's stored deadline meant.
 */
internal fun gateLockoutMs(failures: Int): Long {
    val level = (failures / MAX_GATE_FAILURES).coerceAtLeast(1)
    var duration = GATE_LOCKOUT_MS
    // Doubled in a loop rather than shifted by (level - 1): nothing bounds
    // `failures`, and a wide enough shift turns 30 seconds into a *negative*
    // duration, which activeLockDeadline() would read as "not locked" — the
    // very bypass this escalation exists to close. The loop cannot run past
    // the cap, so its length is bounded by the cap rather than by the count.
    repeat(level - 1) {
        duration *= 2
        if (duration >= MAX_GATE_LOCKOUT_MS) return MAX_GATE_LOCKOUT_MS
    }
    return duration
}

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
        // would recompute "now + one lockout" forever and never let the parent
        // in.
        //
        // The ceiling is *this level's* lockout, not GATE_LOCKOUT_MS. Using the
        // constant would clamp an escalated eight-minute lockout back to thirty
        // seconds on the next launch, and force-stopping the app is a two-tap
        // gesture — which would hand back the bypass the escalation removes.
        val now = wallClock()
        val ceiling = now + gateLockoutMs(gateFailures)
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

    /**
     * The photograph on screen, or null when the child is not in the Viewer.
     *
     * Here rather than in `MainActivity` so it is reachable from a JVM test:
     * it is what the photograph cache is told to keep when the app stops, and
     * getting it wrong costs a decode on the way back in — a mistake nothing
     * would fail on.
     */
    fun currentPhotographOrNull(): Int? {
        val state = uiState as? UiState.Viewer ?: return null
        val theme = THEME_DEFS.find { it.id == state.themeId } ?: return null
        return theme.imageRes.getOrNull(state.imageIndex.coerceIn(theme.imageRes.indices))
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
        // The count is not reset when the lockout arms, which it used to be.
        // That made every round of three wrong guesses cost the same thirty
        // seconds — and with four buttons a round clears by luck about 58% of
        // the time, so a child reached Parent Settings inside a minute and
        // could keep doing it. Letting the count climb is what makes the next
        // round twice as expensive as the last; only a correct answer clears
        // it. So the lockout arms on every whole round rather than on "at
        // least three", which after the first round is always true.
        if (gateFailures % MAX_GATE_FAILURES == 0) {
            gateLockedUntil = wallClock() + gateLockoutMs(gateFailures)
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

    fun openPolicy() {
        transitionTo(UiState.Policy)
    }

    /** Back to Settings, not Home: the parent came from there and is not done. */
    fun closePolicy() {
        transitionTo(UiState.Settings)
    }

    private fun transitionTo(next: UiState) {
        uiState = next
        persistState(next)
    }

    private fun persistState(state: UiState) {
        // Only Home and Viewer are restorable. Coming back from process death
        // into Settings — or into the policy, which is only reachable through
        // it — would hand a child the parent side without the gate, and a
        // half-answered gate is not worth preserving.
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
