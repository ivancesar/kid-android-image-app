package com.kidsexplore.app

import com.kidsexplore.app.ui.screens.Countdown
import com.kidsexplore.app.ui.screens.countdownFor
import com.kidsexplore.app.ui.screens.secondsUntil
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The gate's countdown rounds up on purpose: a parent watching it should see
 * "1 second" for the final tick rather than "0 seconds" while still locked out.
 * That is a stated requirement with no other coverage, and it is off-by-one
 * bait.
 *
 * The seconds/minutes switch below is the same bait one unit up. The lockout
 * escalates to eight minutes, so the boundary is now reached on a real device
 * rather than only in theory.
 */
class GateCountdownTest {

    private val now = 1_000_000L

    @Test
    fun zeroDeadlineMeansNotLocked() {
        assertEquals(0, secondsUntil(0L, now))
    }

    @Test
    fun anElapsedDeadlineMeansNotLocked() {
        assertEquals(0, secondsUntil(now - 1, now))
        assertEquals(0, secondsUntil(now, now))
    }

    @Test
    fun aPartialSecondStillReadsAsOne() {
        assertEquals("1ms left must show 1, never 0", 1, secondsUntil(now + 1, now))
        assertEquals(1, secondsUntil(now + 999, now))
        assertEquals(1, secondsUntil(now + 1000, now))
    }

    @Test
    fun wholeSecondsDoNotRoundUpTwice() {
        assertEquals(2, secondsUntil(now + 1001, now))
        assertEquals(30, secondsUntil(now + 30_000, now))
    }

    // ------------------------------------------------ which unit it reads in

    @Test
    fun anythingUnderAMinuteStillReadsInSeconds() {
        assertEquals(Countdown.Seconds(1), countdownFor(1))
        assertEquals(Countdown.Seconds(30), countdownFor(30))
        assertEquals("59 is the last second that reads as seconds", Countdown.Seconds(59), countdownFor(59))
    }

    @Test
    fun aFullMinuteIsWhereItSwitches() {
        assertEquals(Countdown.Minutes(1), countdownFor(60))
    }

    /** Rounding up, so the message never claims less time than is left. */
    @Test
    fun aPartialMinuteCountsAsAWholeOne() {
        assertEquals("61s must not read as '1 minute'", Countdown.Minutes(2), countdownFor(61))
        assertEquals(Countdown.Minutes(2), countdownFor(119))
        assertEquals(Countdown.Minutes(2), countdownFor(120))
        assertEquals(Countdown.Minutes(3), countdownFor(121))
    }

    /**
     * The longest lockout the escalation can produce. It used to be the case
     * that this read "Wait 480 seconds".
     */
    @Test
    fun theCappedLockoutReadsAsEightMinutes() {
        assertEquals(Countdown.Minutes(8), countdownFor((MAX_GATE_LOCKOUT_MS / 1000).toInt()))
    }
}
