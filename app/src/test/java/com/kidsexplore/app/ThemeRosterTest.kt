package com.kidsexplore.app

import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The two facts about the roster that need no device, so that they gate a
 * plain `./gradlew build`.
 *
 * Their siblings in `ThemeResourcesTest` genuinely need Android — they resolve
 * string arrays, inflate drawables and read resource entry names. These two
 * need no `Resources`, and living in `androidTest` meant they
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
     * A theme ships exactly as many photographs as its label array has entries.
     *
     * This is the leg of the invariant that can actually come apart. Every
     * entry in `THEME_DEFS` derives `labelCount` from the same list it passes
     * as `imageRes`, so comparing those two to each other only catches a
     * copy-paste crossover — it cannot catch the case the roster exists to
     * prevent, which is images and labels drifting out of step. That needs the
     * string array, so this reads `strings.xml` off disk rather than through
     * `Resources`, which is what keeps it on the JVM.
     *
     * The parsing is deliberately shallow: `labels_<id>` and `<item>` are both
     * conventions `ThemeResourcesTest.everyThemeResourceIsNamedAfterItsId`
     * already pins on a device, so a rename fails there rather than silently
     * emptying this.
     */
    @Test
    fun everyThemeShipsOnePhotographPerLabel() {
        val xml = defaultStrings()
        THEME_DEFS.filter { it.imageRes.isNotEmpty() }.forEach { theme ->
            val array = Regex(
                """<string-array name="labels_${theme.id}">(.*?)</string-array>""",
                RegexOption.DOT_MATCHES_ALL,
            ).find(xml)
            assertNotNull("no labels_${theme.id} array in strings.xml", array)
            val labels = Regex("<item>").findAll(array!!.groupValues[1]).count()
            assertEquals(
                "theme '${theme.id}' ships ${theme.imageRes.size} photographs " +
                    "but its labels_${theme.id} array holds $labels",
                labels,
                theme.imageRes.size,
            )
        }
    }

    /**
     * `strings.xml` as text.
     *
     * Gradle runs unit tests with the module directory as the working
     * directory; the walk up covers being run from the repo root instead.
     */
    private fun defaultStrings(): String {
        val relative = "src/main/res/values/strings.xml"
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            for (candidate in listOf(File(dir, relative), File(dir, "app/$relative"))) {
                if (candidate.isFile) return candidate.readText()
            }
            dir = dir.parentFile
        }
        throw AssertionError("could not locate $relative from ${File("").absolutePath}")
    }
}
