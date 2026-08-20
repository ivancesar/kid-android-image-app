package com.kidsexplore.app

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.model.LABELS_PER_THEME
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Guards the theme catalogue and its resources. A theme's id, its source SVG
 * filename, its drawable and its two string resources are kept identical on
 * purpose, so these checks make a theme added without its artwork or its text
 * fail loudly here rather than render as a blank card or a crash.
 */
@RunWith(AndroidJUnit4::class)
class ThemeDefsTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /** A Context forced to one language, so every shipped locale can be checked. */
    private fun localized(tag: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(config)
    }

    @Test
    fun everyThemeHasAResolvableIcon() {
        THEME_DEFS.forEach { theme ->
            assertTrue("${theme.id} has no iconRes", theme.iconRes != 0)
            assertNotNull("${theme.id} icon does not inflate", ContextCompat.getDrawable(context, theme.iconRes))
        }
    }

    @Test
    fun everyThemeResourceIsNamedAfterItsId() {
        THEME_DEFS.forEach { theme ->
            val names = context.resources
            assertEquals("ic_theme_${theme.id}", names.getResourceEntryName(theme.iconRes))
            assertEquals("theme_${theme.id}_name", names.getResourceEntryName(theme.nameRes))
            assertEquals("labels_${theme.id}", names.getResourceEntryName(theme.labelsRes))
        }
    }

    @Test
    fun themeIdsAreUnique() {
        assertEquals(THEME_DEFS.size, THEME_DEFS.map { it.id }.toSet().size)
    }

    /**
     * Runs per shipped language: a translation that drops an item would
     * otherwise leave the Viewer paging onto an index that no longer exists.
     */
    @Test
    fun everyLanguageHasCompleteNamesAndLabels() {
        AppLocales.SUPPORTED.forEach { tag ->
            val res = localized(tag).resources
            val names = mutableSetOf<String>()
            THEME_DEFS.forEach { theme ->
                val name = res.getString(theme.nameRes)
                assertTrue("[$tag] ${theme.id} name is blank", name.isNotBlank())
                assertTrue("[$tag] duplicate theme name '$name'", names.add(name))

                val labels = res.getStringArray(theme.labelsRes)
                assertEquals("[$tag] ${theme.id} label count", theme.labelCount, labels.size)
                assertEquals("[$tag] ${theme.id} labels are not unique", labels.size, labels.toSet().size)
                labels.forEachIndexed { i, l ->
                    assertTrue("[$tag] ${theme.id} label $i is blank", l.isNotBlank())
                }
            }
        }
    }

    @Test
    fun everyThemeShipsTheSameNumberOfLabels() {
        THEME_DEFS.forEach { assertEquals(LABELS_PER_THEME, it.labelCount) }
    }
}
