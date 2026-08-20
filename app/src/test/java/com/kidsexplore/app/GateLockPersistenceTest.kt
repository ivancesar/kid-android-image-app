package com.kidsexplore.app

import androidx.lifecycle.SavedStateHandle
import com.kidsexplore.app.data.GateLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Regression cover for the gate's one job: staying shut.
 *
 * The gate's defence is a question that rotates on every wrong answer plus a
 * lockout once there have been too many. Both used to live in a
 * `SavedStateHandle`, which Android discards when the Activity finishes — and
 * Back from Home finishes it. So the sequence "guess wrong three times, Back,
 * Back, relaunch" reset the failure count *and* the lockout, and a child could
 * keep taking free 1-in-4 guesses until one landed.
 *
 * A relaunch is modelled the way the app actually behaves: the same store (it
 * is on disk) and a brand-new [SavedStateHandle] (it is not).
 */
class GateLockPersistenceTest {

    private val store = FakeThemeStore()
    private var now = 1_000_000L

    private fun relaunch() = AppViewModel(
        store = store,
        savedState = SavedStateHandle(),
        random = Random(7),
        wallClock = { now },
    )

    private val AppViewModel.gate: UiState.Gate get() = uiState as UiState.Gate

    private fun AppViewModel.answerWrong() {
        val q = gate.question
        pickGateAnswer(q.values.first { it != q.correct })
    }

    private fun AppViewModel.answerCorrectly() = pickGateAnswer(gate.question.correct)

    // ------------------------------------------------- surviving a relaunch

    @Test
    fun theLockoutSurvivesARelaunch() {
        val first = relaunch()
        first.openGate()
        repeat(MAX_GATE_FAILURES) { first.answerWrong() }
        assertTrue("precondition: locked before relaunching", first.gate.lockedUntilWallMs > now)

        val second = relaunch()
        second.openGate()

        assertTrue("still locked after relaunch", second.gate.lockedUntilWallMs > now)
        second.answerCorrectly()
        assertTrue("the lockout outranks a correct answer", second.uiState is UiState.Gate)
    }

    /**
     * The subtler half. Even if the deadline were restored, forgetting the
     * count would mean the lockout re-arms from zero on every relaunch — so
     * three wrong answers per launch would stay free forever.
     */
    @Test
    fun theFailureCountSurvivesARelaunch() {
        val first = relaunch()
        first.openGate()
        repeat(MAX_GATE_FAILURES - 1) { first.answerWrong() }
        assertEquals("precondition: not locked yet", 0L, first.gate.lockedUntilWallMs)

        val second = relaunch()
        second.openGate()
        second.answerWrong()

        assertTrue(
            "the earlier failures must still count toward the lockout",
            second.gate.lockedUntilWallMs > now,
        )
    }

    @Test
    fun theLockoutStillExpiresOnItsOwnAcrossARelaunch() {
        val first = relaunch()
        first.openGate()
        repeat(MAX_GATE_FAILURES) { first.answerWrong() }

        now += GATE_LOCKOUT_MS + 1

        val second = relaunch()
        second.openGate()
        assertEquals("expired, so not locked", 0L, second.gate.lockedUntilWallMs)
        second.answerCorrectly()
        assertEquals(UiState.Settings, second.uiState)
    }

    @Test
    fun aCorrectAnswerClearsThePersistedLock() {
        val first = relaunch()
        first.openGate()
        repeat(MAX_GATE_FAILURES - 1) { first.answerWrong() }
        first.answerCorrectly()

        assertEquals(GateLock(failures = 0, lockedUntilWallMs = 0L), store.savedLock)

        // and a fresh launch starts the child back at a full three attempts
        val second = relaunch()
        second.openGate()
        second.answerWrong()
        assertEquals("not immediately re-locked", 0L, second.gate.lockedUntilWallMs)
    }

    @Test
    fun theLockIsWrittenThroughTheStoreNotJustHeldInMemory() {
        val vm = relaunch()
        vm.openGate()
        repeat(MAX_GATE_FAILURES) { vm.answerWrong() }

        assertTrue("a deadline reached the store", store.savedLock.lockedUntilWallMs > now)
    }

    @Test
    fun aLockPersistedByAnEarlierSessionIsHonouredOnFirstOpen() {
        val preLocked = FakeThemeStore(initialLock = GateLock(0, now + GATE_LOCKOUT_MS))
        val vm = AppViewModel(preLocked, SavedStateHandle(), Random(7)) { now }

        vm.openGate()

        assertTrue(vm.gate.lockedUntilWallMs > now)
        vm.answerCorrectly()
        assertTrue("refused while locked", vm.uiState is UiState.Gate)
    }

    // --------------------------------------------------------- clock changes

    /**
     * The deadline is wall-clock so it can outlive the process, which means a
     * device clock moved backwards would otherwise leave a parent locked out
     * for however far it moved. Capped at one lockout from now.
     */
    @Test
    fun aBackwardsClockCannotStrandAParentInAPermanentLockout() {
        val yearAway = FakeThemeStore(
            initialLock = GateLock(failures = 0, lockedUntilWallMs = now + 365L * 24 * 60 * 60 * 1000),
        )
        val vm = AppViewModel(yearAway, SavedStateHandle(), Random(7)) { now }

        vm.openGate()

        assertTrue("still locked right now", vm.gate.lockedUntilWallMs > now)
        assertTrue(
            "but never for longer than one lockout",
            vm.gate.lockedUntilWallMs <= now + GATE_LOCKOUT_MS,
        )

        now += GATE_LOCKOUT_MS + 1
        val later = AppViewModel(yearAway, SavedStateHandle(), Random(7)) { now }
        later.openGate()
        later.answerCorrectly()
        assertEquals("the cap actually lets them back in", UiState.Settings, later.uiState)
    }

    // ------------------------------------------------------ question rotation

    /** Every wrong answer must replace the question, not just the first. */
    @Test
    fun theQuestionRotatesOnEveryWrongAnswer() {
        val vm = relaunch()
        vm.openGate()

        val seen = mutableListOf(vm.gate.question)
        repeat(MAX_GATE_FAILURES - 1) {
            vm.answerWrong()
            val next = vm.gate.question
            assertNotEquals("question repeated after a wrong answer", seen.last(), next)
            seen += next
        }
        assertEquals(MAX_GATE_FAILURES, seen.size)
    }

    /**
     * A relaunch resets how the gate *presents* — fresh question, no "try
     * again" left hanging from last time — while the enforcement state above
     * survives it. Getting the two the wrong way round either strands a stale
     * message on screen or hands back a gate with the failures forgotten.
     *
     * (Deliberately no assertion that the question itself differs: these
     * sessions share a seeded Random so the sequence is reproducible, and
     * rotation is covered by theQuestionRotatesOnEveryWrongAnswer.)
     */
    @Test
    fun aRelaunchResetsThePresentationButNotTheEnforcement() {
        val first = relaunch()
        first.openGate()
        first.answerWrong()
        assertTrue("precondition: the mistake is on screen", first.gate.wrong)

        val second = relaunch()
        assertEquals("a half-answered gate is not restored", UiState.Home, second.uiState)

        second.openGate()
        assertFalse("the stale mistake must not come back with it", second.gate.wrong)
        assertEquals("but the failure still counts", 1, store.savedLock.failures)
    }
}
