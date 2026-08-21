package com.kidsexplore.app

import android.content.Context
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

/**
 * Holds `ThemeDef` and `strings.xml` together.
 *
 * The names and labels moved into resources so they can be translated, which
 * split one fact across two files: the ViewModel wraps an index around
 * [com.kidsexplore.app.model.ThemeDef.labelCount] and cannot see the string
 * array it is really indexing. If a translator or a later edit adds a ninth
 * `<item>` — or drops one — nothing else in the build would notice, and the
 * Viewer would quietly stop reaching the last image (or land out of range and
 * be silently coerced back). This is the check that makes that loud.
 */
@RunWith(AndroidJUnit4::class)
class ThemeResourcesTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resources = context.resources

    /** A Context forced to one language, so every shipped locale can be checked. */
    private fun localized(tag: String) =
        context.createConfigurationContext(
            Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) },
        ).resources

    @Test
    fun everyThemeLabelCountMatchesItsStringArray() {
        THEME_DEFS.forEach { theme ->
            val labels = resources.getStringArray(theme.labelsRes)
            assertEquals(
                "theme '${theme.id}' declares labelCount=${theme.labelCount} " +
                    "but its array holds ${labels.size}",
                theme.labelCount,
                labels.size,
            )
        }
    }

    @Test
    fun everyThemeResolvesANameAndNonBlankLabels() {
        THEME_DEFS.forEach { theme ->
            assertTrue(
                "theme '${theme.id}' has a blank name",
                resources.getString(theme.nameRes).isNotBlank(),
            )
            resources.getStringArray(theme.labelsRes).forEachIndexed { i, label ->
                assertTrue("theme '${theme.id}' label $i is blank", label.isNotBlank())
            }
        }
    }

    /** Ids are the persistence key and the grid key; duplicates would break both. */
    @Test
    fun themeIdsAreUnique() {
        assertEquals(THEME_DEFS.size, THEME_DEFS.map { it.id }.toSet().size)
    }

    /** Two themes sharing a name would make the Settings list ambiguous to a parent. */
    @Test
    fun themeNamesAreUnique() {
        val names = THEME_DEFS.map { resources.getString(it.nameRes) }
        assertEquals(names.toString(), THEME_DEFS.size, names.toSet().size)
    }

    /**
     * A theme's id, its source SVG filename, its generated drawable and its two
     * string resources are kept identical on purpose, so a theme added without
     * its artwork or its text fails here rather than rendering a blank card.
     */
    @Test
    fun everyThemeResourceIsNamedAfterItsId() {
        THEME_DEFS.forEach { theme ->
            assertEquals("ic_theme_${theme.id}", resources.getResourceEntryName(theme.iconRes))
            assertEquals("theme_${theme.id}", resources.getResourceEntryName(theme.nameRes))
            assertEquals("labels_${theme.id}", resources.getResourceEntryName(theme.labelsRes))
        }
    }

    @Test
    fun everyThemeIconInflates() {
        THEME_DEFS.forEach { theme ->
            assertNotNull("${theme.id} icon does not inflate", ContextCompat.getDrawable(context, theme.iconRes))
        }
    }

    /**
     * The checks above run in the default locale. Arrays replace rather than
     * merge, so a translation that drops an item would leave the Viewer paging
     * onto an index that no longer exists — in that language only.
     */
    @Test
    fun everyShippedLanguageKeepsTheSameNamesAndLabelCounts() {
        AppLocales.SUPPORTED.forEach { tag ->
            val res = localized(tag)
            val names = mutableSetOf<String>()
            THEME_DEFS.forEach { theme ->
                val name = res.getString(theme.nameRes)
                assertTrue("[$tag] ${theme.id} name is blank", name.isNotBlank())
                assertTrue("[$tag] duplicate theme name '$name'", names.add(name))

                val labels = res.getStringArray(theme.labelsRes)
                assertEquals("[$tag] ${theme.id} label count", theme.labelCount, labels.size)
                labels.forEachIndexed { i, l ->
                    assertTrue("[$tag] ${theme.id} label $i is blank", l.isNotBlank())
                }
            }
        }
    }

    /**
     * Fallback is silent, so an empty or missing `values-hr` would leave every
     * other assertion passing while the app shipped in English only.
     */
    @Test
    fun croatianOverridesTheDefaultsRatherThanFallingBackWholesale() {
        val en = localized("en")
        val hr = localized("hr")
        assertTrue(
            "settings_title is identical in en and hr - is values-hr present?",
            en.getString(R.string.settings_title) != hr.getString(R.string.settings_title),
        )
        val translated = THEME_DEFS.count { en.getString(it.nameRes) != hr.getString(it.nameRes) }
        // 13 of 14 differ; "Ocean" is the same word in both languages.
        assertTrue("only $translated of ${THEME_DEFS.size} theme names differ in hr", translated >= 10)
    }
}
