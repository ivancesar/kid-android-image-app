package com.kidsexplore.app.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The source uses Google Fonts "Nunito" at weights 600/700/800/900. No font
// file is bundled here (that would require downloading one); FontWeight.Black
// / .ExtraBold on the default sans-serif approximates the same heavy, rounded
// feel used throughout the design.
private val KidsFontFamily = FontFamily.SansSerif

val HeavyTextStyle = TextStyle(fontFamily = KidsFontFamily, fontWeight = FontWeight.Black)

/** The tracked-out eyebrow label style, e.g. Home's "KIDS EXPLORE". */
val BoldTextStyle = TextStyle(
    fontFamily = KidsFontFamily,
    fontWeight = FontWeight.ExtraBold,
    letterSpacing = 0.5.sp,
)

/**
 * Light only, on purpose.
 *
 * The design is a fixed bright palette — fourteen saturated card colors on warm
 * off-white — and there is no dark counterpart designed for it. Rather than
 * let the system setting pull half the app into colors nobody chose, the
 * scheme is pinned here and `MainActivity` matches the system bar icons to it.
 */
// internal rather than private: NeutralContrastTest measures text drawn on the
// Material surfaces this scheme resolves — the language menu is the one place
// the app does not paint its own ground — and it must read the real scheme
// rather than a copy of these four overrides that could drift from them.
internal val KidsColorScheme = lightColorScheme(
    background = NeutralColors.appBackground,
    surface = NeutralColors.screenBackground,
    onBackground = NeutralColors.labelDark,
    onSurface = NeutralColors.labelDark,
)

@Composable
fun KidsExploreTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = KidsColorScheme) {
        // Nothing painted a background before this: MaterialTheme's colorScheme
        // is inert without a Surface, so every screen fell through to the white
        // windowBackground while Home's header painted the warm appBackground
        // over it — a visible seam along the header's bottom edge.
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NeutralColors.appBackground,
            content = content,
        )
    }
}
