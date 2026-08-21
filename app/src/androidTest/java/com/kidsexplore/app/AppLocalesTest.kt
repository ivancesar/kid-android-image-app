package com.kidsexplore.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * [AppLocales] is small but sits on the one path with no UI test coverage, so
 * its pure parts are pinned here. Applying a locale is deliberately not
 * exercised: it recreates the activity, which fights the Compose test rule.
 */
@RunWith(AndroidJUnit4::class)
class AppLocalesTest {

    @Test
    fun everySupportedTagNamesItselfInItsOwnLanguage() {
        assertEquals("English", AppLocales.endonym("en"))
        assertEquals("Hrvatski", AppLocales.endonym("hr"))
    }


}
