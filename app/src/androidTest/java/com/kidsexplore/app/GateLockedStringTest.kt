package com.kidsexplore.app

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.ui.screens.Countdown
import com.kidsexplore.app.ui.screens.countdownFor
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * The lockout countdown is a `plurals` carrying a `%d`, and Croatian takes
 * three quantity classes where English takes two (1 sekundu / 2-4 sekunde /
 * 5+ sekundi). Drop the placeholder from one class, or omit a class a language
 * requires, and the app throws at exactly the second that quantity becomes
 * reachable — inside the parental gate, where a parent is already stuck.
 *
 * Cheap to rule out: format every second of the longest lockout the escalation
 * can produce, in every shipped language. Driven through [countdownFor] rather
 * than against `gate_locked` directly, so the unit it picks and the string it
 * picks are exercised as one thing — pairing a minute count with the seconds
 * plural would read "Wait 8 seconds" for an eight-minute lockout, which no
 * assertion about either string alone would catch.
 */
@RunWith(AndroidJUnit4::class)
class GateLockedStringTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun localized(tag: String) =
        context.createConfigurationContext(
            Configuration(context.resources.configuration)
                .apply { setLocale(Locale.forLanguageTag(tag)) },
        ).resources

    @Test
    fun everySecondOfTheLongestLockoutFormatsInEveryLanguage() {
        val longestLockoutSeconds = (MAX_GATE_LOCKOUT_MS / 1000).toInt()
        AppLocales.SUPPORTED.forEach { tag ->
            val res = localized(tag)
            for (n in 1..longestLockoutSeconds) {
                val (plural, quantity) = when (val left = countdownFor(n)) {
                    is Countdown.Seconds -> R.plurals.gate_locked to left.value
                    is Countdown.Minutes -> R.plurals.gate_locked_minutes to left.value
                }
                val text = res.getQuantityString(plural, quantity, quantity)
                assertTrue("[$tag] ${n}s left produced blank text", text.isNotBlank())
                assertTrue(
                    "[$tag] ${n}s left did not interpolate the count $quantity: '$text'",
                    text.contains(quantity.toString()),
                )
            }
        }
    }
}
