package com.kidsexplore.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.kidsexplore.app.model.ThemeDef
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an OKLCH color (as used throughout the source design, e.g.
 * `oklch(72% 0.16 15)`) to a Compose [Color], via the standard OKLab
 * round-trip. Keeps every theme's palette derived from just its hue, the
 * same way the source stylesheet does.
 */
fun oklch(lPercent: Float, c: Float, hDegrees: Float): Color {
    val l = lPercent
    val hRad = Math.toRadians(hDegrees.toDouble())
    val a = c * cos(hRad).toFloat()
    val b = c * sin(hRad).toFloat()

    val l_ = l + 0.3963377774f * a + 0.2158037573f * b
    val m_ = l - 0.1055613458f * a - 0.0638541728f * b
    val s_ = l - 0.0894841775f * a - 1.2914855480f * b

    val lCubed = l_ * l_ * l_
    val mCubed = m_ * m_ * m_
    val sCubed = s_ * s_ * s_

    val rLinear = 4.0767416621f * lCubed - 3.3077115913f * mCubed + 0.2309699292f * sCubed
    val gLinear = -1.2684380046f * lCubed + 2.6097574011f * mCubed - 0.3413193965f * sCubed
    val bLinear = -0.0041960863f * lCubed - 0.7034186147f * mCubed + 1.7076147010f * sCubed

    return Color(
        red = linearToSrgb(rLinear),
        green = linearToSrgb(gLinear),
        blue = linearToSrgb(bLinear),
    )
}

private fun linearToSrgb(channel: Float): Float {
    val c = channel.coerceIn(0f, 1f)
    val srgb = if (c <= 0.0031308f) {
        12.92f * c
    } else {
        1.055f * c.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f
    }
    return srgb.coerceIn(0f, 1f)
}

/** Mirrors buildThemeVal()'s color trio in the source: cardBg / stripe / cardBorder. */
data class ThemePalette(
    val cardBg: Color,
    val stripe: Color,
    val cardBorder: Color,
)

fun ThemeDef.palette(): ThemePalette = ThemePalette(
    cardBg = oklch(0.72f, 0.16f, hue),
    stripe = oklch(0.66f, 0.17f, hue),
    cardBorder = oklch(0.50f, 0.19f, hue),
)

/** Neutral (near-hue-90) colors used throughout the source outside the theme cards. */
object NeutralColors {
    val appBackground = oklch(0.96f, 0.01f, 90f)
    val screenBackground = oklch(0.97f, 0.012f, 90f)
    val viewerBackground = oklch(0.20f, 0.01f, 90f)
    val labelMuted = oklch(0.45f, 0.02f, 90f)
    val labelDark = oklch(0.22f, 0.02f, 90f)
    val labelDarker = oklch(0.24f, 0.02f, 90f)
    val gearButtonBg = oklch(0.90f, 0.01f, 90f)
    val errorText = oklch(0.55f, 0.18f, 20f)
    val doneButtonBg = oklch(0.45f, 0.16f, 250f)
    val gateOptionBg = oklch(0.55f, 0.16f, 250f)
    val gateCorrectBg = oklch(0.60f, 0.17f, 140f)
    val rowBgEnabled = oklch(0.94f, 0.02f, 90f)
    val rowBgDisabled = oklch(0.97f, 0.005f, 90f)
    val subtitleText = oklch(0.48f, 0.02f, 90f)
    val cancelText = oklch(0.55f, 0.02f, 90f)
}
