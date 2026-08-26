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
    /**
     * The theme's photographs, in the same order as [labelsRes] — item *n*'s
     * label describes image *n*, as a note to whoever edits this next. Nothing
     * displays or announces it: a theme with photographs shows the picture
     * alone.
     *
     * Every theme now carries a set, so the default below is currently unused;
     * it stays because the Viewer's striped placeholder card is what a theme
     * added ahead of its artwork would fall back to, and dropping the default
     * would make adding such a theme a change to this class rather than one
     * more entry in the list.
     *
     * Photographs, unlike names and labels, are the same in every language, so
     * they are referenced here rather than through a per-locale typed array.
     * A non-empty list must be exactly [labelCount] long, which is also the
     * length of the theme's label array; `ThemeRosterTest` asserts the images
     * against the array off-device, and `ThemeResourcesTest` asserts
     * [labelCount] against the array on one.
     */
    val imageRes: List<Int> = emptyList(),
)

/**
 * Every theme's photographs, in the order the Viewer pages them.
 *
 * Each list is however many usable pictures that theme has and no particular
 * number — they run from Dinosaurs' seven to Birds' twenty-two — which is
 * what [ThemeDef.labelCount] being per-theme is for. Nothing anywhere expects
 * two themes to agree on a count.
 *
 * Sources are Unsplash for every theme but Space, under the Unsplash License,
 * with the photographers credited in Parent Settings
 * (`R.string.attribution_photographers`); Space is public-domain NASA imagery,
 * credited separately by `R.string.attribution_nasa`.
 */
private val CARS_IMAGES = listOf(
    R.drawable.img_cars_01,
    R.drawable.img_cars_02,
    R.drawable.img_cars_03,
    R.drawable.img_cars_04,
    R.drawable.img_cars_05,
    R.drawable.img_cars_06,
    R.drawable.img_cars_07,
    R.drawable.img_cars_08,
    R.drawable.img_cars_09,
    R.drawable.img_cars_10,
    R.drawable.img_cars_11,
    R.drawable.img_cars_12,
    R.drawable.img_cars_13,
    R.drawable.img_cars_14,
)

private val CONSTRUCTION_IMAGES = listOf(
    R.drawable.img_construction_01,
    R.drawable.img_construction_02,
    R.drawable.img_construction_03,
    R.drawable.img_construction_04,
    R.drawable.img_construction_05,
    R.drawable.img_construction_06,
    R.drawable.img_construction_07,
    R.drawable.img_construction_08,
    R.drawable.img_construction_09,
    R.drawable.img_construction_10,
    R.drawable.img_construction_11,
    R.drawable.img_construction_12,
    R.drawable.img_construction_13,
    R.drawable.img_construction_14,
)

private val TRAINS_IMAGES = listOf(
    R.drawable.img_trains_01,
    R.drawable.img_trains_02,
    R.drawable.img_trains_03,
    R.drawable.img_trains_04,
    R.drawable.img_trains_05,
    R.drawable.img_trains_06,
    R.drawable.img_trains_07,
    R.drawable.img_trains_08,
    R.drawable.img_trains_09,
    R.drawable.img_trains_10,
    R.drawable.img_trains_11,
    R.drawable.img_trains_12,
    R.drawable.img_trains_13,
    R.drawable.img_trains_14,
    R.drawable.img_trains_15,
)

private val ANIMALS_IMAGES = listOf(
    R.drawable.img_animals_01,
    R.drawable.img_animals_02,
    R.drawable.img_animals_03,
    R.drawable.img_animals_04,
    R.drawable.img_animals_05,
    R.drawable.img_animals_06,
    R.drawable.img_animals_07,
    R.drawable.img_animals_08,
    R.drawable.img_animals_09,
    R.drawable.img_animals_10,
    R.drawable.img_animals_11,
    R.drawable.img_animals_12,
    R.drawable.img_animals_13,
    R.drawable.img_animals_14,
    R.drawable.img_animals_15,
    R.drawable.img_animals_16,
    R.drawable.img_animals_17,
    R.drawable.img_animals_18,
    R.drawable.img_animals_19,
    R.drawable.img_animals_20,
)

private val BIRD_IMAGES = listOf(
    R.drawable.img_bird_01,
    R.drawable.img_bird_02,
    R.drawable.img_bird_03,
    R.drawable.img_bird_04,
    R.drawable.img_bird_05,
    R.drawable.img_bird_06,
    R.drawable.img_bird_07,
    R.drawable.img_bird_08,
    R.drawable.img_bird_09,
    R.drawable.img_bird_10,
    R.drawable.img_bird_11,
    R.drawable.img_bird_12,
    R.drawable.img_bird_13,
    R.drawable.img_bird_14,
    R.drawable.img_bird_15,
    R.drawable.img_bird_16,
    R.drawable.img_bird_17,
    R.drawable.img_bird_18,
    R.drawable.img_bird_19,
    R.drawable.img_bird_20,
    R.drawable.img_bird_21,
    R.drawable.img_bird_22,
)

private val INSECTS_IMAGES = listOf(
    R.drawable.img_insects_01,
    R.drawable.img_insects_02,
    R.drawable.img_insects_03,
    R.drawable.img_insects_04,
    R.drawable.img_insects_05,
    R.drawable.img_insects_06,
    R.drawable.img_insects_07,
    R.drawable.img_insects_08,
    R.drawable.img_insects_09,
    R.drawable.img_insects_10,
    R.drawable.img_insects_11,
    R.drawable.img_insects_12,
    R.drawable.img_insects_13,
    R.drawable.img_insects_14,
)

private val OCEAN_IMAGES = listOf(
    R.drawable.img_ocean_01,
    R.drawable.img_ocean_02,
    R.drawable.img_ocean_03,
    R.drawable.img_ocean_04,
    R.drawable.img_ocean_05,
    R.drawable.img_ocean_06,
    R.drawable.img_ocean_07,
    R.drawable.img_ocean_08,
    R.drawable.img_ocean_09,
    R.drawable.img_ocean_10,
    R.drawable.img_ocean_11,
    R.drawable.img_ocean_12,
    R.drawable.img_ocean_13,
    R.drawable.img_ocean_14,
    R.drawable.img_ocean_15,
    R.drawable.img_ocean_16,
    R.drawable.img_ocean_17,
    R.drawable.img_ocean_18,
)

private val FARM_IMAGES = listOf(
    R.drawable.img_farm_01,
    R.drawable.img_farm_02,
    R.drawable.img_farm_03,
    R.drawable.img_farm_04,
    R.drawable.img_farm_05,
    R.drawable.img_farm_06,
    R.drawable.img_farm_07,
    R.drawable.img_farm_08,
    R.drawable.img_farm_09,
    R.drawable.img_farm_10,
    R.drawable.img_farm_11,
    R.drawable.img_farm_12,
    R.drawable.img_farm_13,
    R.drawable.img_farm_14,
    R.drawable.img_farm_15,
    R.drawable.img_farm_16,
    R.drawable.img_farm_17,
)

private val DINOSAURS_IMAGES = listOf(
    R.drawable.img_dinosaurs_01,
    R.drawable.img_dinosaurs_02,
    R.drawable.img_dinosaurs_03,
    R.drawable.img_dinosaurs_04,
    R.drawable.img_dinosaurs_05,
    R.drawable.img_dinosaurs_06,
    R.drawable.img_dinosaurs_07,
)

private val FLOWERS_IMAGES = listOf(
    R.drawable.img_flowers_01,
    R.drawable.img_flowers_02,
    R.drawable.img_flowers_03,
    R.drawable.img_flowers_04,
    R.drawable.img_flowers_05,
    R.drawable.img_flowers_06,
    R.drawable.img_flowers_07,
    R.drawable.img_flowers_08,
    R.drawable.img_flowers_09,
    R.drawable.img_flowers_10,
    R.drawable.img_flowers_11,
    R.drawable.img_flowers_12,
    R.drawable.img_flowers_13,
    R.drawable.img_flowers_14,
)

private val FOREST_IMAGES = listOf(
    R.drawable.img_forest_01,
    R.drawable.img_forest_02,
    R.drawable.img_forest_03,
    R.drawable.img_forest_04,
    R.drawable.img_forest_05,
    R.drawable.img_forest_06,
    R.drawable.img_forest_07,
    R.drawable.img_forest_08,
    R.drawable.img_forest_09,
    R.drawable.img_forest_10,
    R.drawable.img_forest_11,
    R.drawable.img_forest_12,
    R.drawable.img_forest_13,
    R.drawable.img_forest_14,
    R.drawable.img_forest_15,
    R.drawable.img_forest_16,
    R.drawable.img_forest_17,
    R.drawable.img_forest_18,
    R.drawable.img_forest_19,
)

private val FRUIT_IMAGES = listOf(
    R.drawable.img_fruit_01,
    R.drawable.img_fruit_02,
    R.drawable.img_fruit_03,
    R.drawable.img_fruit_04,
    R.drawable.img_fruit_05,
    R.drawable.img_fruit_06,
    R.drawable.img_fruit_07,
    R.drawable.img_fruit_08,
    R.drawable.img_fruit_09,
    R.drawable.img_fruit_10,
    R.drawable.img_fruit_11,
    R.drawable.img_fruit_12,
)

private val VEGETABLE_IMAGES = listOf(
    R.drawable.img_vegetable_01,
    R.drawable.img_vegetable_02,
    R.drawable.img_vegetable_03,
    R.drawable.img_vegetable_04,
    R.drawable.img_vegetable_05,
    R.drawable.img_vegetable_06,
    R.drawable.img_vegetable_07,
    R.drawable.img_vegetable_08,
    R.drawable.img_vegetable_09,
    R.drawable.img_vegetable_10,
    R.drawable.img_vegetable_11,
    R.drawable.img_vegetable_12,
    R.drawable.img_vegetable_13,
    R.drawable.img_vegetable_14,
)

private val SPACE_IMAGES = listOf(
    R.drawable.img_space_01,
    R.drawable.img_space_02,
    R.drawable.img_space_03,
    R.drawable.img_space_04,
    R.drawable.img_space_05,
    R.drawable.img_space_06,
    R.drawable.img_space_07,
    R.drawable.img_space_08,
    R.drawable.img_space_09,
    R.drawable.img_space_10,
    R.drawable.img_space_11,
    R.drawable.img_space_12,
    R.drawable.img_space_13,
    R.drawable.img_space_14,
    R.drawable.img_space_15,
    R.drawable.img_space_16,
    R.drawable.img_space_17,
)

val THEME_DEFS: List<ThemeDef> = listOf(
    // Things that go
    ThemeDef(
        id = "cars", nameRes = R.string.theme_cars, labelsRes = R.array.labels_cars,
        labelCount = CARS_IMAGES.size, hue = 15f, iconRes = R.drawable.ic_theme_cars,
        imageRes = CARS_IMAGES,
    ),
    ThemeDef(
        id = "construction", nameRes = R.string.theme_construction, labelsRes = R.array.labels_construction,
        labelCount = CONSTRUCTION_IMAGES.size, hue = 45f, iconRes = R.drawable.ic_theme_construction,
        imageRes = CONSTRUCTION_IMAGES,
    ),
    ThemeDef(
        id = "trains", nameRes = R.string.theme_trains, labelsRes = R.array.labels_trains,
        labelCount = TRAINS_IMAGES.size, hue = 350f, iconRes = R.drawable.ic_theme_trains,
        imageRes = TRAINS_IMAGES,
    ),

    // Creatures
    ThemeDef(
        id = "animals", nameRes = R.string.theme_animals, labelsRes = R.array.labels_animals,
        labelCount = ANIMALS_IMAGES.size, hue = 320f, iconRes = R.drawable.ic_theme_animals,
        imageRes = ANIMALS_IMAGES,
    ),
    ThemeDef(
        id = "bird", nameRes = R.string.theme_bird, labelsRes = R.array.labels_bird,
        labelCount = BIRD_IMAGES.size, hue = 225f, iconRes = R.drawable.ic_theme_bird,
        imageRes = BIRD_IMAGES,
    ),
    ThemeDef(
        id = "insects", nameRes = R.string.theme_insects, labelsRes = R.array.labels_insects,
        labelCount = INSECTS_IMAGES.size, hue = 200f, iconRes = R.drawable.ic_theme_insects,
        imageRes = INSECTS_IMAGES,
    ),
    ThemeDef(
        id = "ocean", nameRes = R.string.theme_ocean, labelsRes = R.array.labels_ocean,
        labelCount = OCEAN_IMAGES.size, hue = 175f, iconRes = R.drawable.ic_theme_ocean,
        imageRes = OCEAN_IMAGES,
    ),
    ThemeDef(
        id = "farm", nameRes = R.string.theme_farm, labelsRes = R.array.labels_farm,
        labelCount = FARM_IMAGES.size, hue = 75f, iconRes = R.drawable.ic_theme_farm,
        imageRes = FARM_IMAGES,
    ),
    ThemeDef(
        id = "dinosaurs", nameRes = R.string.theme_dinosaurs, labelsRes = R.array.labels_dinosaurs,
        labelCount = DINOSAURS_IMAGES.size, hue = 285f, iconRes = R.drawable.ic_theme_dinosaurs,
        imageRes = DINOSAURS_IMAGES,
    ),

    // Growing things
    ThemeDef(
        id = "flowers", nameRes = R.string.theme_flowers, labelsRes = R.array.labels_flowers,
        labelCount = FLOWERS_IMAGES.size, hue = 296f, iconRes = R.drawable.ic_theme_flowers,
        imageRes = FLOWERS_IMAGES,
    ),
    ThemeDef(
        id = "forest", nameRes = R.string.theme_forest, labelsRes = R.array.labels_forest,
        labelCount = FOREST_IMAGES.size, hue = 150f, iconRes = R.drawable.ic_theme_forest,
        imageRes = FOREST_IMAGES,
    ),
    ThemeDef(
        id = "fruit", nameRes = R.string.theme_fruit, labelsRes = R.array.labels_fruit,
        labelCount = FRUIT_IMAGES.size, hue = 100f, iconRes = R.drawable.ic_theme_fruit,
        imageRes = FRUIT_IMAGES,
    ),
    ThemeDef(
        id = "vegetable", nameRes = R.string.theme_vegetable, labelsRes = R.array.labels_vegetable,
        labelCount = VEGETABLE_IMAGES.size, hue = 125f, iconRes = R.drawable.ic_theme_vegetable,
        imageRes = VEGETABLE_IMAGES,
    ),

    // Out there
    ThemeDef(
        id = "space", nameRes = R.string.theme_space, labelsRes = R.array.labels_space,
        labelCount = SPACE_IMAGES.size, hue = 250f, iconRes = R.drawable.ic_theme_space,
        imageRes = SPACE_IMAGES,
    ),
)
