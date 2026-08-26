package com.kidsexplore.app

import android.content.Context
import android.content.res.Configuration
import android.util.TypedValue
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

    /**
     * A theme with photographs must have exactly as many as it has labels.
     * The Viewer pairs them by index — item *n*'s label is what TalkBack reads
     * out for image *n* — so a short list would silently drop back to the
     * placeholder card partway through the set, and a long one would leave
     * photographs a child can never reach.
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

    /**
     * `img_<id>_NN`, one-based and zero-padded, in the order the Viewer pages
     * them. Named after the theme for the same reason its icon and its string
     * resources are, and numbered so a mismatch between the list in
     * `ThemeDef.kt` and the files on disk is visible by reading either one.
     */
    @Test
    fun everyPhotographIsNamedAfterItsThemeAndPosition() {
        THEME_DEFS.filter { it.imageRes.isNotEmpty() }.forEach { theme ->
            theme.imageRes.forEachIndexed { i, id ->
                assertEquals(
                    String.format(Locale.ROOT, "img_%s_%02d", theme.id, i + 1),
                    resources.getResourceEntryName(id),
                )
            }
        }
    }

    /**
     * Photographs are configuration-independent, so they belong in
     * `drawable-nodpi`: anywhere else and the framework treats them as mdpi
     * artwork and upscales them by the device's density, decoding a 1280px
     * JPEG into a bitmap several times that size for no gain in what is on
     * screen.
     */
    @Test
    fun everyPhotographIsDensityIndependent() {
        THEME_DEFS.flatMap { it.imageRes }.forEach { id ->
            val value = TypedValue()
            resources.getValue(id, value, true)
            val path = value.string.toString()
            assertTrue(
                "${resources.getResourceEntryName(id)} resolves to $path, " +
                    "which is not a nodpi resource",
                path.contains("drawable-nodpi"),
            )
        }
    }

    @Test
    fun everyPhotographDecodes() {
        THEME_DEFS.flatMap { it.imageRes }.forEach { id ->
            assertNotNull(
                "${resources.getResourceEntryName(id)} does not decode",
                ContextCompat.getDrawable(context, id),
            )
        }
    }

    /**
     * Every theme has artwork.
     *
     * This started as "Construction has photographs", was loosened to "some
     * theme does" while the set was growing, and is now tight again because
     * the set is finished: no theme ships the placeholder card. A child must
     * never land on stand-in text, so a theme reaching the grid without its
     * pictures is a shipping bug, not a work-in-progress state.
     *
     * That makes this the assertion that fails when a new theme is added
     * before it has been photographed. The failure is the point — write the
     * entry and drop the images in together. [ThemeDef.imageRes] still
     * defaults to empty and the Viewer still renders the placeholder for a
     * null image, so nothing is broken in the meantime; it just does not ship.
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
     *
     * The policy is written down in the header of `values-hr/strings.xml`:
     * three keys are deliberately left to fall back, everything else is
     * translated. This asserts that policy rather than a count, so it does not
     * break on a rename and does not pass if four translations quietly vanish.
     */
    @Test
    fun croatianTranslatesEverythingExceptTheKeysItDeliberatelyLeaves() {
        val en = localized("en")
        val hr = localized("hr")

        // Product name, a string that is nothing but placeholders, and a list
        // of photographers' names.
        listOf(
            R.string.app_name, R.string.home_brand, R.string.gate_question,
            R.string.attribution_photographers,
        ).forEach { id ->
            val name = resources.getResourceEntryName(id)
            assertEquals(
                "$name is meant to fall back to the default, unchanged",
                en.getString(id),
                hr.getString(id),
            )
        }

        // Everything a parent or child actually reads must differ.
        val mustTranslate = listOf(
            R.string.home_title, R.string.viewer_home, R.string.viewer_back,
            R.string.viewer_next, R.string.gate_eyebrow, R.string.gate_prompt,
            R.string.gate_wrong, R.string.gate_cancel, R.string.settings_title,
            R.string.settings_subtitle, R.string.settings_categories,
            R.string.settings_done, R.string.settings_language,
            R.string.settings_language_system, R.string.settings_attribution,
            R.string.attribution_images, R.string.attribution_photographers_line,
            R.string.attribution_nasa, R.string.attribution_show_more,
            R.string.attribution_show_less,
        )
        mustTranslate.forEach { id ->
            val name = resources.getResourceEntryName(id)
            assertTrue(
                "$name is identical in en and hr - is it missing from values-hr?",
                en.getString(id) != hr.getString(id),
            )
        }

        // Theme names too, except Ocean, which is the same word in both.
        THEME_DEFS.filterNot { it.id == "ocean" }.forEach { theme ->
            assertTrue(
                "theme_${theme.id} is identical in en and hr",
                en.getString(theme.nameRes) != hr.getString(theme.nameRes),
            )
        }

    }

    /**
     * The UI suite resolves every expected string from resources, which makes
     * it locale-independent and, by the same token, blind to the content: an
     * empty or garbled `values/` would pass it. This is the anchor — a few
     * defaults pinned by literal so that cannot happen silently.
     */
    @Test
    fun defaultStringsSayWhatTheyShould() {
        val en = localized("en")
        assertEquals("Cars", en.getString(THEME_DEFS.first { it.id == "cars" }.nameRes))
        assertEquals("Done", en.getString(R.string.settings_done))
        assertEquals("Pick something to look at!", en.getString(R.string.home_title))
        // The notices the app owes its image sources, pinned by literal rather
        // than left to a resolve-from-resources assertion that would pass on
        // any wording at all. Each says which categories it covers: a blanket
        // line was true only while one theme had pictures, and a notice that
        // credits the wrong source for a category is worse than none. Unsplash
        // supplies all thirteen themes NASA does not, so its line carves Space
        // out by name rather than listing the rest.
        assertEquals(
            "Every category except Space uses images provided by Unsplash under their Unsplash Licence",
            en.getString(R.string.attribution_images),
        )
        assertEquals(
            "Space images are courtesy of NASA",
            en.getString(R.string.attribution_nasa),
        )
    }
}
