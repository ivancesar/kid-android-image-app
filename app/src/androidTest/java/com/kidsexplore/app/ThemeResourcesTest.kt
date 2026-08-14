package com.kidsexplore.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

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

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

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
}
