package com.kidsexplore.app

import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The facts about the roster that need no device, so that they gate a plain
 * `./gradlew build`.
 *
 * Their siblings in `ThemeResourcesTest` genuinely need Android — they resolve
 * strings, inflate drawables and read resource entry names. These need no
 * `Resources`, and living in `androidTest` would mean running only under
 * `connectedDebugAndroidTest`, which needs an emulator and is in neither the
 * README's recommended local command nor any CI (there is no CI). A theme
 * could arrive with nothing to show and the default build would say nothing,
 * which is the opposite of what the assertion is for.
 */
class ThemeRosterTest {

    /**
     * Every theme ships at least one photograph.
     *
     * The photographs are now a theme's entire content: the Viewer draws the
     * picture and nothing else, `ThemeDef.imageRes` is what the ViewModel
     * wraps the index around, and `MainActivity` indexes straight into it. A
     * theme with an empty list has nothing to put on screen and would take the
     * Viewer out of range the moment a child tapped it, so it is a build
     * failure rather than a runtime one.
     *
     * This is what replaced the old label-array check. Labels used to be the
     * second half of the invariant — one `<item>` per photograph, asserted by
     * parsing `strings.xml` off disk — but every theme ships pictures now, the
     * placeholder card they backed is gone, and the arrays with it.
     */
    @Test
    fun everyThemeShipsPhotographs() {
        val bare = THEME_DEFS.filter { it.imageRes.isEmpty() }.map { it.id }
        assertTrue(
            "themes with no photographs, which the Viewer cannot render: $bare",
            bare.isEmpty(),
        )
    }

    /** And the roster itself is not empty, which would make the above vacuous. */
    @Test
    fun theRosterIsNotEmpty() {
        assertTrue("THEME_DEFS is empty", THEME_DEFS.isNotEmpty())
    }

    /**
     * Ids are the persistence key, the grid key and the naming convention every
     * one of a theme's resources follows. A blank one breaks all three, and a
     * duplicate silently makes two themes share a parent's on/off choice.
     */
    @Test
    fun themeIdsAreNonBlankAndUnique() {
        THEME_DEFS.forEach { theme ->
            assertTrue("a theme has a blank id", theme.id.isNotBlank())
        }
        val ids = THEME_DEFS.map { it.id }
        assertEquals(ids.toString(), ids.size, ids.toSet().size)
    }

    /**
     * No photograph is listed twice, within a theme or across the roster.
     *
     * The drawable id is the Viewer's only handle on which picture is up — it
     * is what the test tag is built from — so a copy-paste duplicate would
     * show a child the same photograph twice while every count still agreed.
     */
    @Test
    fun noPhotographIsListedTwice() {
        val all = THEME_DEFS.flatMap { it.imageRes }
        assertEquals("a drawable appears in more than one slot", all.size, all.toSet().size)
    }
}
