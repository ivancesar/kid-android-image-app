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

    /** A whole round of wrong answers — the last of which arms the next lockout. */
    private fun AppViewModel.failARound() = repeat(MAX_GATE_FAILURES) { answerWrong() }

    /** How long the lockout now in force has left to run, from [now]. */
    private val AppViewModel.lockoutLength: Long get() = gate.lockedUntilWallMs - now

    /** Move the clock past the current lockout, the way a parent waiting it out does. */
    private fun AppViewModel.waitOutTheLockout() {
        now = gate.lockedUntilWallMs + 1
    }

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

    // ------------------------------------------------------------ escalation

    /**
     * The gap the escalation closes, and the shape of test that missed it: the
     * suite drove the lockout exactly once, which cannot tell a gate that gets
     * progressively more expensive from one that hands out the same thirty
     * seconds forever. At four buttons a round of three guesses clears about
     * 58% of the time, so "the same thirty seconds forever" is a child reaching
     * Parent Settings inside a minute, repeatably.
     */
    @Test
    fun theGateLocksAgainAfterAnEarlierLockoutExpired() {
        val vm = relaunch()
        vm.openGate()
        vm.failARound()
        assertTrue("precondition: locked once", vm.lockoutLength > 0)

        vm.waitOutTheLockout()
        vm.failARound()

        assertTrue("a second round must lock the gate again", vm.lockoutLength > 0)
    }

    @Test
    fun eachLockoutLastsLongerThanTheOneBeforeIt() {
        val vm = relaunch()
        vm.openGate()

        var previous = 0L
        // Four rounds rather than two: doubling once could be a special case
        // for the second lockout, and this is the property, not one example.
        repeat(4) { round ->
            vm.failARound()
            val length = vm.lockoutLength
            assertTrue(
                "lockout ${round + 1} lasted ${length}ms, no longer than the ${previous}ms before it",
                length > previous,
            )
            previous = length
            vm.waitOutTheLockout()
        }
    }

    /**
     * The escalation is derived from the failure count, so a correct answer
     * clearing that count is also what puts the next lockout back to thirty
     * seconds. A parent who gets in must not be paying for their own typos on
     * the way back out.
     */
    @Test
    fun aCorrectAnswerResetsTheEscalation() {
        val vm = relaunch()
        vm.openGate()
        vm.failARound()
        vm.waitOutTheLockout()
        vm.answerCorrectly()
        assertEquals("precondition: through the gate", UiState.Settings, vm.uiState)

        vm.openGate()
        vm.failARound()

        assertEquals("the next lockout starts over at the first one's length", GATE_LOCKOUT_MS, vm.lockoutLength)
    }

    /**
     * Unbounded doubling would eventually lock a parent out for days — and the
     * person a very long lockout punishes is the one who mistyped, not the
     * child grinding the gate.
     */
    @Test
    fun theEscalationStopsAtTheCap() {
        val vm = relaunch()
        vm.openGate()

        var length = 0L
        // Well past the fifth round, which is where the doubling first meets
        // the cap; the point is that the rounds after it do not keep growing.
        repeat(12) { round ->
            vm.failARound()
            length = vm.lockoutLength
            assertTrue(
                "lockout ${round + 1} was ${length}ms, past the ${MAX_GATE_LOCKOUT_MS}ms cap",
                length <= MAX_GATE_LOCKOUT_MS,
            )
            vm.waitOutTheLockout()
        }
        assertEquals("and the cap is actually reached, not merely never exceeded", MAX_GATE_LOCKOUT_MS, length)
    }

    /**
     * Force-stopping the app is a two-tap gesture, so a lockout that a relaunch
     * shortens is a lockout a child can shorten. The trap here is the clamp in
     * `AppViewModel.init`: written against `GATE_LOCKOUT_MS`, it silently pulls
     * every escalated deadline back to the *first* level's thirty seconds and
     * hands back the exact bypass the escalation removes.
     */
    @Test
    fun anEscalatedLockoutSurvivesARelaunchAtItsFullLength() {
        val first = relaunch()
        first.openGate()
        first.failARound()
        first.waitOutTheLockout()
        first.failARound()
        val escalated = first.gate.lockedUntilWallMs
        assertEquals("precondition: the second lockout is a minute", 2 * GATE_LOCKOUT_MS, first.lockoutLength)

        val second = relaunch()
        second.openGate()

        assertEquals(
            "a relaunch must not clamp the escalated lockout back to the first level's length",
            escalated,
            second.gate.lockedUntilWallMs,
        )
    }

    // --------------------------------------------------------- clock changes

    /**
     * The deadline is wall-clock so it can outlive the process, which means a
     * device clock moved backwards would otherwise leave a parent locked out
     * for however far it moved. Capped at one lockout from now — where "one
     * lockout" is however long the level the failure count has reached lasts,
     * which is the whole of what makes this compatible with the escalation.
     */
    @Test
    fun aBackwardsClockCannotStrandAParentInAPermanentLockout() {
        // Two whole rounds on the clock, so the level being clamped to is the
        // second — a minute — and a clamp written against GATE_LOCKOUT_MS
        // would be visibly wrong here rather than accidentally right.
        val yearAway = FakeThemeStore(
            initialLock = GateLock(
                failures = 2 * MAX_GATE_FAILURES,
                lockedUntilWallMs = now + 365L * 24 * 60 * 60 * 1000,
            ),
        )
        val vm = AppViewModel(yearAway, SavedStateHandle(), Random(7)) { now }

        vm.openGate()

        assertTrue("still locked right now", vm.gate.lockedUntilWallMs > now)
        assertEquals(
            "but never for longer than this level's lockout",
            2 * GATE_LOCKOUT_MS,
            vm.gate.lockedUntilWallMs - now,
        )

        now += 2 * GATE_LOCKOUT_MS + 1
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
