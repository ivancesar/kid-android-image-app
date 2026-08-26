package com.kidsexplore.app

import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The two facts about the roster that need no device, so that they gate a
 * plain `./gradlew build`.
 *
 * Their siblings in `ThemeResourcesTest` genuinely need Android — they resolve
 * string arrays, inflate drawables and read resource entry names. These two
 * only count entries in [THEME_DEFS], and living in `androidTest` meant they
 * ran only under `connectedDebugAndroidTest`, which needs an emulator and is
 * in neither the README's recommended local command nor any CI (there is no
 * CI). A theme could ship without its photographs and nothing in the default
 * build would say so, which is the opposite of what the assertion is for.
 */
class ThemeRosterTest {

    /**
     * Every theme has artwork.
     *
     * This started as "Construction has photographs", was loosened to "some
     * theme does" while the set was growing, and is tight again now the set is
     * finished: no theme ships the placeholder card. A child must never land
     * on stand-in text, so a theme reaching the grid without its pictures is a
     * shipping bug, not a work-in-progress state.
     *
     * That makes this the assertion that fails when a theme is added before it
     * has been photographed. The failure is the point — write the entry and
     * drop the images in together. `ThemeDef.imageRes` still defaults to empty
     * and the Viewer still renders the placeholder for a null image, so
     * nothing is broken in the meantime; it just does not ship.
     */
    @Test
    fun everyThemeShipsPhotographs() {
        val bare = THEME_DEFS.filter { it.imageRes.isEmpty() }.map { it.id }
        assertTrue(
            "these themes would show the placeholder card rather than a " +
                "photograph: $bare",
            bare.isEmpty(),
        )
    }

    /**
     * A theme with photographs must have exactly as many as it has labels.
     *
     * The Viewer pairs them by index, and [com.kidsexplore.app.AppViewModel]
     * wraps its index on `labelCount` without being able to see either list —
     * so a short list would drop back to the placeholder card partway through
     * the set, and a long one would leave photographs a child can never reach.
     *
     * `ThemeResourcesTest.everyThemeLabelCountMatchesItsStringArray` covers the
     * third leg, `labelCount` against the string array, and has to stay on a
     * device to resolve it.
     */
    @Test
    fun everyThemeWithPhotographsHasOnePerLabel() {
        THEME_DEFS.filter { it.imageRes.isNotEmpty() }.forEach { theme ->
            assertEquals(
                "theme '${theme.id}' declares labelCount=${theme.labelCount} " +
                    "but ships ${theme.imageRes.size} photographs",
                theme.labelCount,
                theme.imageRes.size,
            )
        }
    }
}
