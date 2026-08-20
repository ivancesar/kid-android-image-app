package com.kidsexplore.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the theme catalogue itself. A theme's id, its source SVG filename and
 * its generated drawable name are kept identical on purpose, so these checks
 * exist to make a theme added without its artwork fail loudly here rather than
 * render as a blank card.
 */
@RunWith(AndroidJUnit4::class)
class ThemeDefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun everyThemeHasAResolvableIcon() {
        THEME_DEFS.forEach { theme ->
            assertTrue("${theme.id} has no iconRes", theme.iconRes != 0)
            assertNotNull(
                "${theme.id} icon does not inflate",
                androidx.core.content.ContextCompat.getDrawable(context, theme.iconRes),
            )
        }
    }

    @Test
    fun everyThemeIconIsNamedAfterItsId() {
        THEME_DEFS.forEach { theme ->
            assertEquals(
                "ic_theme_${theme.id}",
                context.resources.getResourceEntryName(theme.iconRes),
            )
        }
    }

    @Test
    fun themeIdsAndNamesAreUnique() {
        assertEquals(THEME_DEFS.size, THEME_DEFS.map { it.id }.toSet().size)
        assertEquals(THEME_DEFS.size, THEME_DEFS.map { it.name }.toSet().size)
    }

    @Test
    fun everyThemeHasLabels() {
        THEME_DEFS.forEach { theme ->
            assertTrue("${theme.id} has no labels", theme.labels.isNotEmpty())
            assertEquals("${theme.id} label set is not unique", theme.labels.size, theme.labels.toSet().size)
        }
    }
}
