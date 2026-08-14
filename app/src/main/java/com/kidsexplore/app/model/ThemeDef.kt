package com.kidsexplore.app.model

/** Selects which shape [com.kidsexplore.app.ui.icons.ThemeIconGlyph] draws for a theme. */
enum class ThemeIcon {
    CAR, DOZER, ANIMAL, DINO, PLANET, OCEAN, FARM, TRAIN
}

/**
 * One browsable category.
 *
 * @param id stable identifier — also the key persisted to SharedPreferences,
 *   so renaming one silently re-enables that theme for every existing user.
 *   Treat these as permanent.
 * @param hue the theme's only color input: card fill, stripe and accent are all
 *   derived from it by `ThemeDef.palette()`, so palettes stay consistent
 *   without hand-picked hex values.
 * @param labels stands in for real photographs — deliberately, to avoid
 *   bundling externally-sourced images. The Viewer renders these as text on a
 *   striped placeholder card.
 */
data class ThemeDef(
    val id: String,
    val name: String,
    val hue: Float,
    val icon: ThemeIcon,
    val labels: List<String>,
)

/**
 * The complete, fixed catalogue a child can reach. Nothing outside this file
 * can add to it — no search, no downloads, no user-supplied content — which is
 * the app's core safety property. Parents can only ever *subtract* from this
 * list, via Settings.
 */
val THEME_DEFS: List<ThemeDef> = listOf(
    ThemeDef(
        id = "cars", name = "Cars", hue = 15f, icon = ThemeIcon.CAR,
        labels = listOf(
            "Red sports car, side view", "Yellow taxi, front view", "Blue pickup truck", "Green race car with number",
            "Police car with lights on", "Yellow school bus", "Motorcycle, side view", "Orange convertible car",
        )
    ),
    ThemeDef(
        id = "construction", name = "Construction", hue = 45f, icon = ThemeIcon.DOZER,
        labels = listOf(
            "Yellow bulldozer pushing dirt", "Crane lifting a steel beam", "Excavator digging a hole", "Cement mixer truck",
            "Dump truck full of dirt", "Road roller flattening asphalt", "Yellow forklift with pallet", "Backhoe loader digging",
        )
    ),
    ThemeDef(
        id = "animals", name = "Animals", hue = 320f, icon = ThemeIcon.ANIMAL,
        labels = listOf(
            "Lion resting in tall grass", "Elephant walking in savanna", "Giraffe eating tree leaves", "Panda sitting with bamboo",
            "Owl perched on a branch", "Rabbit hopping in a field", "Puppy playing with a ball", "Kitten napping in sun",
        )
    ),
    ThemeDef(
        id = "dinosaurs", name = "Dinosaurs", hue = 285f, icon = ThemeIcon.DINO,
        labels = listOf(
            "T-Rex roaring, side view", "Triceratops grazing on plants", "Stegosaurus walking, side view", "Pterodactyl flying over cliffs",
            "Brontosaurus beside a lake", "Velociraptor running fast", "Ankylosaurus with tail spikes", "Baby dinosaur hatching from egg",
        )
    ),
    ThemeDef(
        id = "space", name = "Space", hue = 250f, icon = ThemeIcon.PLANET,
        labels = listOf(
            "Rocket launching into sky", "Astronaut floating in space", "Planet Saturn with its rings", "Full moon close-up",
            "Space shuttle on launch pad", "Cartoon alien spaceship", "Stars and a bright galaxy", "Sun with orange flares",
        )
    ),
    ThemeDef(
        id = "trains", name = "Trains", hue = 350f, icon = ThemeIcon.TRAIN,
        labels = listOf(
            "Red steam train, side view", "Bullet train speeding by", "Freight train with cargo cars", "Toy train on a track",
            "Train crossing a bridge", "Subway train at a station", "Yellow train engine, front view", "Train going through tunnel",
        )
    ),
    ThemeDef(
        id = "ocean", name = "Ocean", hue = 175f, icon = ThemeIcon.OCEAN,
        labels = listOf(
            "Clownfish swimming by coral", "Sea turtle gliding underwater", "Octopus with curled arms", "Starfish on the sand",
            "Whale breaching the surface", "Seahorse near seaweed", "Crab on the beach", "School of tropical fish",
        )
    ),
    ThemeDef(
        id = "farm", name = "Farm", hue = 75f, icon = ThemeIcon.FARM,
        labels = listOf(
            "Pig in a muddy pen", "Cow grazing in a field", "Chicken by the barn", "Sheep with wooly coat",
            "Horse in a green pasture", "Duck swimming in a pond", "Red barn with silo", "Tractor in a wheat field",
        )
    ),
)
