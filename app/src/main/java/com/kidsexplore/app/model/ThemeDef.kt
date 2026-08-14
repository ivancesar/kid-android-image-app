package com.kidsexplore.app.model

import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.kidsexplore.app.R

enum class ThemeIcon {
    CAR, DOZER, ANIMAL, DINO, PLANET, OCEAN, FARM, TRAIN
}

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
    val id: String,
    @param:StringRes val nameRes: Int,
    @param:ArrayRes val labelsRes: Int,
    val labelCount: Int,
    val hue: Float,
    val icon: ThemeIcon,
)

/** Every theme ships this many items; [ThemeDef.labelCount] records it per theme. */
private const val LABELS_PER_THEME = 8

val THEME_DEFS: List<ThemeDef> = listOf(
    ThemeDef(
        id = "cars", nameRes = R.string.theme_cars, labelsRes = R.array.labels_cars,
        labelCount = LABELS_PER_THEME, hue = 15f, icon = ThemeIcon.CAR,
    ),
    ThemeDef(
        id = "construction", nameRes = R.string.theme_construction, labelsRes = R.array.labels_construction,
        labelCount = LABELS_PER_THEME, hue = 45f, icon = ThemeIcon.DOZER,
    ),
    ThemeDef(
        id = "animals", nameRes = R.string.theme_animals, labelsRes = R.array.labels_animals,
        labelCount = LABELS_PER_THEME, hue = 320f, icon = ThemeIcon.ANIMAL,
    ),
    ThemeDef(
        id = "dinosaurs", nameRes = R.string.theme_dinosaurs, labelsRes = R.array.labels_dinosaurs,
        labelCount = LABELS_PER_THEME, hue = 285f, icon = ThemeIcon.DINO,
    ),
    ThemeDef(
        id = "space", nameRes = R.string.theme_space, labelsRes = R.array.labels_space,
        labelCount = LABELS_PER_THEME, hue = 250f, icon = ThemeIcon.PLANET,
    ),
    ThemeDef(
        id = "trains", nameRes = R.string.theme_trains, labelsRes = R.array.labels_trains,
        labelCount = LABELS_PER_THEME, hue = 350f, icon = ThemeIcon.TRAIN,
    ),
    ThemeDef(
        id = "ocean", nameRes = R.string.theme_ocean, labelsRes = R.array.labels_ocean,
        labelCount = LABELS_PER_THEME, hue = 175f, icon = ThemeIcon.OCEAN,
    ),
    ThemeDef(
        id = "farm", nameRes = R.string.theme_farm, labelsRes = R.array.labels_farm,
        labelCount = LABELS_PER_THEME, hue = 75f, icon = ThemeIcon.FARM,
    ),
)
