package com.kidsexplore.app.model

import androidx.annotation.DrawableRes
import com.kidsexplore.app.R

/**
 * One browsable theme. [iconRes] points at `res/drawable/ic_theme_<id>.xml`,
 * generated from the matching `icons-src/<id>.svg` by `tools/svg2vd.py` — the
 * id, the source filename, and the drawable name are deliberately kept
 * identical so adding a theme is just dropping in an SVG and adding an entry.
 */
data class ThemeDef(
    val id: String,
    val name: String,
    val hue: Float,
    @DrawableRes val iconRes: Int,
    val labels: List<String>,
)

val THEME_DEFS: List<ThemeDef> = listOf(
    // Things that go
    ThemeDef(
        id = "cars", name = "Cars", hue = 15f, iconRes = R.drawable.ic_theme_cars,
        labels = listOf(
            "Red sports car, side view", "Yellow taxi, front view", "Blue pickup truck", "Green race car with number",
            "Police car with lights on", "Yellow school bus", "Motorcycle, side view", "Orange convertible car",
        )
    ),
    ThemeDef(
        id = "construction", name = "Construction", hue = 45f, iconRes = R.drawable.ic_theme_construction,
        labels = listOf(
            "Yellow bulldozer pushing dirt", "Crane lifting a steel beam", "Excavator digging a hole", "Cement mixer truck",
            "Dump truck full of dirt", "Road roller flattening asphalt", "Yellow forklift with pallet", "Backhoe loader digging",
        )
    ),
    ThemeDef(
        id = "trains", name = "Trains", hue = 350f, iconRes = R.drawable.ic_theme_trains,
        labels = listOf(
            "Red steam train, side view", "Bullet train speeding by", "Freight train with cargo cars", "Toy train on a track",
            "Train crossing a bridge", "Subway train at a station", "Yellow train engine, front view", "Train going through tunnel",
        )
    ),

    // Creatures
    ThemeDef(
        id = "animals", name = "Animals", hue = 320f, iconRes = R.drawable.ic_theme_animals,
        labels = listOf(
            "Lion resting in tall grass", "Elephant walking in savanna", "Giraffe eating tree leaves", "Panda sitting with bamboo",
            "Owl perched on a branch", "Rabbit hopping in a field", "Puppy playing with a ball", "Kitten napping in sun",
        )
    ),
    ThemeDef(
        id = "bird", name = "Birds", hue = 225f, iconRes = R.drawable.ic_theme_bird,
        labels = listOf(
            "Robin perched on a branch", "Blue jay with bright feathers", "Woodpecker on a tree trunk", "Flamingo standing in water",
            "Penguin waddling on ice", "Eagle soaring over mountains", "Hummingbird at a flower", "Parrot with rainbow feathers",
        )
    ),
    ThemeDef(
        id = "insects", name = "Insects", hue = 200f, iconRes = R.drawable.ic_theme_insects,
        labels = listOf(
            "Butterfly on a purple flower", "Bumblebee gathering pollen", "Ladybug on a green leaf", "Dragonfly over a pond",
            "Ant carrying a crumb", "Grasshopper in tall grass", "Caterpillar on a stem", "Firefly glowing at dusk",
        )
    ),
    ThemeDef(
        id = "ocean", name = "Ocean", hue = 175f, iconRes = R.drawable.ic_theme_ocean,
        labels = listOf(
            "Clownfish swimming by coral", "Sea turtle gliding underwater", "Octopus with curled arms", "Starfish on the sand",
            "Whale breaching the surface", "Seahorse near seaweed", "Crab on the beach", "School of tropical fish",
        )
    ),
    ThemeDef(
        id = "farm", name = "Farm", hue = 75f, iconRes = R.drawable.ic_theme_farm,
        labels = listOf(
            "Pig in a muddy pen", "Cow grazing in a field", "Chicken by the barn", "Sheep with wooly coat",
            "Horse in a green pasture", "Duck swimming in a pond", "Red barn with silo", "Tractor in a wheat field",
        )
    ),
    ThemeDef(
        id = "dinosaurs", name = "Dinosaurs", hue = 285f, iconRes = R.drawable.ic_theme_dinosaurs,
        labels = listOf(
            "T-Rex roaring, side view", "Triceratops grazing on plants", "Stegosaurus walking, side view", "Pterodactyl flying over cliffs",
            "Brontosaurus beside a lake", "Velociraptor running fast", "Ankylosaurus with tail spikes", "Baby dinosaur hatching from egg",
        )
    ),

    // Growing things
    ThemeDef(
        id = "flowers", name = "Flowers", hue = 302f, iconRes = R.drawable.ic_theme_flowers,
        labels = listOf(
            "Sunflower facing the sun", "Red rose with green leaves", "Tulips in a spring garden", "Daisy with white petals",
            "Lavender swaying in a field", "Cherry blossoms on a branch", "Orchid in a small pot", "Poppy in a summer meadow",
        )
    ),
    ThemeDef(
        id = "forest", name = "Forest", hue = 150f, iconRes = R.drawable.ic_theme_forest,
        labels = listOf(
            "Tall pine trees in a row", "Mushrooms on a mossy log", "Sunlight through the leaves", "Fox trotting between trees",
            "Stream running over rocks", "Acorns under an oak tree", "Deer standing in a clearing", "Autumn leaves on the ground",
        )
    ),
    ThemeDef(
        id = "fruit", name = "Fruit", hue = 100f, iconRes = R.drawable.ic_theme_fruit,
        labels = listOf(
            "Red apple on a branch", "Bunch of yellow bananas", "Strawberries in a basket", "Orange cut in half",
            "Bunch of purple grapes", "Watermelon slice on a plate", "Ripe pear on a table", "Pineapple with spiky leaves",
        )
    ),
    ThemeDef(
        id = "vegetable", name = "Vegetables", hue = 125f, iconRes = R.drawable.ic_theme_vegetable,
        labels = listOf(
            "Orange carrot with green top", "Broccoli floret on a board", "Red tomato on the vine", "Corn on the cob",
            "Green peas in a pod", "Purple eggplant, side view", "Potatoes in a wooden crate", "Bell peppers in three colors",
        )
    ),

    // Out there
    ThemeDef(
        id = "space", name = "Space", hue = 250f, iconRes = R.drawable.ic_theme_space,
        labels = listOf(
            "Rocket launching into sky", "Astronaut floating in space", "Planet Saturn with its rings", "Full moon close-up",
            "Space shuttle on launch pad", "Cartoon alien spaceship", "Stars and a bright galaxy", "Sun with orange flares",
        )
    ),
)
