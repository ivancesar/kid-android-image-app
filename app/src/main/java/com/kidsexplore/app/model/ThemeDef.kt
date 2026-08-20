package com.kidsexplore.app.model

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kidsexplore.app.R

/**
 * A theme names its content by resource id rather than holding the strings.
 *
 * The names and item labels are the app's primary content, so they belong in
 * `strings.xml` with everything else translatable — and keeping them out of
 * here is also what lets [com.kidsexplore.app.AppViewModel] stay free of
 * Android: it pages an index around [labelCount] and the UI resolves the text.
 *
 * [labelCount] therefore duplicates the length of the [labelsRes] array, which
 * the ViewModel cannot see. `ThemeResourcesTest` asserts the two agree for
 * every theme, so the two cannot drift apart unnoticed.
 *
 * `@Immutable` marks this class stable for Compose. Note that it says nothing
 * about a `List<ThemeDef>` parameter, which stays unstable and is compared by
 * reference under strong skipping — see `AppViewModel.visibleThemes`.
 */
@Immutable
data class ThemeDef(
    /**
     * Stable identifier, and also the key persisted by
     * [com.kidsexplore.app.data.ThemeStore] — renaming one silently re-enables
     * that theme for every existing user, since their stored id no longer
     * matches anything. Treat these as permanent; only [nameRes] is meant to
     * change.
     */
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:ArrayRes val labelsRes: Int,
    val labelCount: Int,
    val hue: Float,
    /**
     * `res/drawable/ic_theme_<id>.xml`, generated from `icons-src/<id>.svg` by
     * `tools/svg2vd.py`. The id, the SVG filename and the drawable name are kept
     * identical so adding a theme is dropping in an SVG and adding an entry.
     */
    @param:DrawableRes val iconRes: Int,
)

/** Every theme ships this many items; [ThemeDef.labelCount] records it per theme. */
private const val LABELS_PER_THEME = 8

val THEME_DEFS: List<ThemeDef> = listOf(
    // Things that go
    ThemeDef(
        id = "cars", nameRes = R.string.theme_cars, labelsRes = R.array.labels_cars,
        labelCount = LABELS_PER_THEME, hue = 15f, iconRes = R.drawable.ic_theme_cars,
    ),
    ThemeDef(
        id = "construction", nameRes = R.string.theme_construction, labelsRes = R.array.labels_construction,
        labelCount = LABELS_PER_THEME, hue = 45f, iconRes = R.drawable.ic_theme_construction,
    ),
    ThemeDef(
        id = "trains", nameRes = R.string.theme_trains, labelsRes = R.array.labels_trains,
        labelCount = LABELS_PER_THEME, hue = 350f, iconRes = R.drawable.ic_theme_trains,
    ),

    // Creatures
    ThemeDef(
        id = "animals", nameRes = R.string.theme_animals, labelsRes = R.array.labels_animals,
        labelCount = LABELS_PER_THEME, hue = 320f, iconRes = R.drawable.ic_theme_animals,
    ),
    ThemeDef(
        id = "bird", nameRes = R.string.theme_bird, labelsRes = R.array.labels_bird,
        labelCount = LABELS_PER_THEME, hue = 225f, iconRes = R.drawable.ic_theme_bird,
    ),
    ThemeDef(
        id = "insects", nameRes = R.string.theme_insects, labelsRes = R.array.labels_insects,
        labelCount = LABELS_PER_THEME, hue = 200f, iconRes = R.drawable.ic_theme_insects,
    ),
    ThemeDef(
        id = "ocean", nameRes = R.string.theme_ocean, labelsRes = R.array.labels_ocean,
        labelCount = LABELS_PER_THEME, hue = 175f, iconRes = R.drawable.ic_theme_ocean,
    ),
    ThemeDef(
        id = "farm", nameRes = R.string.theme_farm, labelsRes = R.array.labels_farm,
        labelCount = LABELS_PER_THEME, hue = 75f, iconRes = R.drawable.ic_theme_farm,
    ),
    ThemeDef(
        id = "dinosaurs", nameRes = R.string.theme_dinosaurs, labelsRes = R.array.labels_dinosaurs,
        labelCount = LABELS_PER_THEME, hue = 285f, iconRes = R.drawable.ic_theme_dinosaurs,
    ),

    // Growing things
    ThemeDef(
        id = "flowers", nameRes = R.string.theme_flowers, labelsRes = R.array.labels_flowers,
        labelCount = LABELS_PER_THEME, hue = 296f, iconRes = R.drawable.ic_theme_flowers,
    ),
    ThemeDef(
        id = "forest", nameRes = R.string.theme_forest, labelsRes = R.array.labels_forest,
        labelCount = LABELS_PER_THEME, hue = 150f, iconRes = R.drawable.ic_theme_forest,
    ),
    ThemeDef(
        id = "fruit", nameRes = R.string.theme_fruit, labelsRes = R.array.labels_fruit,
        labelCount = LABELS_PER_THEME, hue = 100f, iconRes = R.drawable.ic_theme_fruit,
    ),
    ThemeDef(
        id = "vegetable", nameRes = R.string.theme_vegetable, labelsRes = R.array.labels_vegetable,
        labelCount = LABELS_PER_THEME, hue = 125f, iconRes = R.drawable.ic_theme_vegetable,
    ),

    // Out there
    ThemeDef(
        id = "space", nameRes = R.string.theme_space, labelsRes = R.array.labels_space,
        labelCount = LABELS_PER_THEME, hue = 250f, iconRes = R.drawable.ic_theme_space,
    ),
)
