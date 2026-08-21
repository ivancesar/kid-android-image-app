package com.kidsexplore.app

import com.kidsexplore.app.ui.screens.secondsUntil
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The gate's countdown rounds up on purpose: a parent watching it should see
 * "1 second" for the final tick rather than "0 seconds" while still locked out.
 * That is a stated requirement with no other coverage, and it is off-by-one
 * bait.
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
}
