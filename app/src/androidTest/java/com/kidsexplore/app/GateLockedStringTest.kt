package com.kidsexplore.app

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
 * Cheap to rule out: format every second of a lockout in every shipped
 * language.
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
    fun everySecondOfALockoutFormatsInEveryLanguage() {
        val longestLockoutSeconds = (GATE_LOCKOUT_MS / 1000).toInt()
        AppLocales.SUPPORTED.forEach { tag ->
            val res = localized(tag)
            for (n in 1..longestLockoutSeconds) {
                val text = res.getQuantityString(R.plurals.gate_locked, n, n)
                assertTrue("[$tag] n=$n produced blank text", text.isNotBlank())
                assertTrue(
                    "[$tag] n=$n did not interpolate the count: '$text'",
                    text.contains(n.toString()),
                )
            }
        }
    }
}
