package com.kidsexplore.app

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
     * Runs per shipped language. Note this cannot detect a language that simply
     * isn't translated — resource fallback resolves missing keys to `values/`,
     * which is deliberate. What it does catch is a translation that declares a
     * *shorter* label array: arrays replace rather than merge, so a dropped item
     * would leave the Viewer paging onto an index that no longer exists.
     */
    @Test
    fun everyLanguageHasCompleteNamesAndLabels() {
        val expectedCounts = THEME_DEFS.associate {
            it.id to context.resources.getStringArray(it.labelsRes).size
        }
        AppLocales.SUPPORTED.forEach { tag ->
            val res = localized(tag).resources
            val names = mutableSetOf<String>()
            THEME_DEFS.forEach { theme ->
                val name = res.getString(theme.nameRes)
                assertTrue("[$tag] ${theme.id} name is blank", name.isNotBlank())
                assertTrue("[$tag] duplicate theme name '$name'", names.add(name))

                val labels = res.getStringArray(theme.labelsRes)
                assertEquals("[$tag] ${theme.id} label count", expectedCounts[theme.id], labels.size)
                assertEquals("[$tag] ${theme.id} labels are not unique", labels.size, labels.toSet().size)
                labels.forEachIndexed { i, l ->
                    assertTrue("[$tag] ${theme.id} label $i is blank", l.isNotBlank())
                }
            }
        }
    }

    /**
     * The complement to the test above: because fallback is silent, an empty or
     * missing `values-hr` would leave every other assertion passing while the
     * app shipped in English only. This pins that Croatian actually overrides.
     */
    @Test
    fun croatianOverridesTheDefaultsRatherThanFallingBackWholesale() {
        val en = localized("en").resources
        val hr = localized("hr").resources

        assertTrue(
            "settings_title is identical in en and hr - is values-hr present?",
            en.getString(R.string.settings_title) != hr.getString(R.string.settings_title),
        )

        val translated = THEME_DEFS.count { en.getString(it.nameRes) != hr.getString(it.nameRes) }
        // 13 of 14 differ; "Ocean" is the same word in both languages.
        assertTrue("only $translated of ${THEME_DEFS.size} theme names differ in hr", translated >= 10)
    }

    /**
     * The rest of the suite resolves expected text from resources, which makes
     * it locale-independent but also blind to the content itself. This pins a
     * couple of default strings so an empty or garbled `values/` fails here.
     */
    @Test
    fun defaultStringsSayWhatTheyShould() {
        val en = localized("en").resources
        assertEquals("Cars", en.getString(THEME_DEFS.first { it.id == "cars" }.nameRes))
        assertEquals("Done", en.getString(R.string.settings_done))
        assertEquals("Pick something to look at!", en.getString(R.string.home_title))
    }
}
