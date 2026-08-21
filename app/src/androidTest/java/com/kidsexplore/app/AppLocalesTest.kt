package com.kidsexplore.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AppLocales] is small but sits on a path nothing else covers.
 *
 * The round trip matters more than it looks: if [AppLocales.current] does not
 * return what [AppLocales.apply] stored, the picker shows nothing ticked and
 * re-applies on every tap. That is exactly what flattening a tag like `pt-BR`
 * to `pt` would cause, which is why `current()` keeps the full tag.
 */
@RunWith(AndroidJUnit4::class)
class AppLocalesTest {

    @Test
    fun everySupportedTagNamesItselfInItsOwnLanguage() {
        assertEquals("English", AppLocales.endonym("en"))
        assertEquals("Hrvatski", AppLocales.endonym("hr"))
    }

    @After
    fun restoreSystemDefault() {
        // Applying a locale is process-wide; leaking it would change the
        // language every later test runs in.
        setLocalesBlocking(AppLocales.SYSTEM)
    }

    private fun setLocalesBlocking(tag: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { AppLocales.apply(tag) }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    @Test
    fun everyOfferedChoiceSurvivesTheRoundTrip() {
        (listOf(AppLocales.SYSTEM) + AppLocales.SUPPORTED).forEach { tag ->
            setLocalesBlocking(tag)
            assertEquals(
                "apply(\"$tag\") then current() must give the same tag back, or the " +
                    "picker shows nothing selected and re-applies on every tap",
                tag,
                AppLocales.current(),
            )
        }
    }
}
