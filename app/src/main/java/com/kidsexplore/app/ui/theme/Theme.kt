package com.kidsexplore.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// The source uses Google Fonts "Nunito" at weights 600/700/800/900. No font
// file is bundled here (that would require downloading one); FontWeight.Black
// / .ExtraBold on the default sans-serif approximates the same heavy, rounded
// feel used throughout the design.
val KidsFontFamily = FontFamily.SansSerif

val HeavyTextStyle = TextStyle(fontFamily = KidsFontFamily, fontWeight = FontWeight.Black)

// Nothing is wrapped in a Surface, so these two are near-inert: the backdrop
// the user actually sees is `android:windowBackground` from themes.xml, and
// HomeScreen paints NeutralColors.appBackground itself behind its header.
// They stay for the Material components that consult the scheme by default.
private val KidsColorScheme = lightColorScheme(
    background = NeutralColors.appBackground,
    surface = NeutralColors.screenBackground,
)

@Composable
fun KidsExploreTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidsColorScheme,
        content = content,
    )
}
