package com.kidsexplore.app.model

import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kidsexplore.app.R

/**
 * One browsable theme.
 *
 * Everything a user reads is a resource id, not a literal, so the whole
 * catalogue translates: [nameRes] is the card and settings title, [labelsRes]
 * a `<string-array>` of the item labels in paging order. [iconRes] points at
 * `res/drawable/ic_theme_<id>.xml`, generated from `icons-src/<id>.svg` by
 * `tools/svg2vd.py`. The id, the SVG filename, the drawable name and the two
 * resource names are deliberately kept identical, so adding a theme is
 * dropping in an SVG, adding the strings, and adding one entry here.
 */
data class ThemeDef(
    val id: String,
    @StringRes val nameRes: Int,
    val hue: Float,
    @DrawableRes val iconRes: Int,
    @ArrayRes val labelsRes: Int,
)

val THEME_DEFS: List<ThemeDef> = listOf(
    // Things that go
    ThemeDef(
        id = "cars", nameRes = R.string.theme_cars_name, hue = 15f,
        iconRes = R.drawable.ic_theme_cars, labelsRes = R.array.labels_cars,
    ),
    ThemeDef(
        id = "construction", nameRes = R.string.theme_construction_name, hue = 45f,
        iconRes = R.drawable.ic_theme_construction, labelsRes = R.array.labels_construction,
    ),
    ThemeDef(
        id = "trains", nameRes = R.string.theme_trains_name, hue = 350f,
        iconRes = R.drawable.ic_theme_trains, labelsRes = R.array.labels_trains,
    ),

    // Creatures
    ThemeDef(
        id = "animals", nameRes = R.string.theme_animals_name, hue = 320f,
        iconRes = R.drawable.ic_theme_animals, labelsRes = R.array.labels_animals,
    ),
    ThemeDef(
        id = "bird", nameRes = R.string.theme_bird_name, hue = 225f,
        iconRes = R.drawable.ic_theme_bird, labelsRes = R.array.labels_bird,
    ),
    ThemeDef(
        id = "insects", nameRes = R.string.theme_insects_name, hue = 200f,
        iconRes = R.drawable.ic_theme_insects, labelsRes = R.array.labels_insects,
    ),
    ThemeDef(
        id = "ocean", nameRes = R.string.theme_ocean_name, hue = 175f,
        iconRes = R.drawable.ic_theme_ocean, labelsRes = R.array.labels_ocean,
    ),
    ThemeDef(
        id = "farm", nameRes = R.string.theme_farm_name, hue = 75f,
        iconRes = R.drawable.ic_theme_farm, labelsRes = R.array.labels_farm,
    ),
    ThemeDef(
        id = "dinosaurs", nameRes = R.string.theme_dinosaurs_name, hue = 285f,
        iconRes = R.drawable.ic_theme_dinosaurs, labelsRes = R.array.labels_dinosaurs,
    ),

    // Growing things
    ThemeDef(
        id = "flowers", nameRes = R.string.theme_flowers_name, hue = 296f,
        iconRes = R.drawable.ic_theme_flowers, labelsRes = R.array.labels_flowers,
    ),
    ThemeDef(
        id = "forest", nameRes = R.string.theme_forest_name, hue = 150f,
        iconRes = R.drawable.ic_theme_forest, labelsRes = R.array.labels_forest,
    ),
    ThemeDef(
        id = "fruit", nameRes = R.string.theme_fruit_name, hue = 100f,
        iconRes = R.drawable.ic_theme_fruit, labelsRes = R.array.labels_fruit,
    ),
    ThemeDef(
        id = "vegetable", nameRes = R.string.theme_vegetable_name, hue = 125f,
        iconRes = R.drawable.ic_theme_vegetable, labelsRes = R.array.labels_vegetable,
    ),

    // Out there
    ThemeDef(
        id = "space", nameRes = R.string.theme_space_name, hue = 250f,
        iconRes = R.drawable.ic_theme_space, labelsRes = R.array.labels_space,
    ),
)
