package com.kidsexplore.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.model.ThemeDef
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Converts an OKLCH color (as used throughout the source design, e.g.
 * `oklch(72% 0.16 15)`) to a Compose [Color], via the standard OKLab
 * round-trip. Keeps every theme's palette derived from just its hue, the
 * same way the source stylesheet does.
 *
 * [l] is lightness as a 0..1 fraction, not a percentage — `oklch(72% ...)` in
 * the stylesheet is `oklch(0.72f, ...)` here.
 *
 * Colors outside the sRGB gamut are clamped per channel in linear light by
 * [linearToSrgb], which shifts hue rather than preserving it. Every palette in
 * this app sits comfortably inside the gamut, so nothing currently relies on
 * the out-of-gamut behavior being principled.
 */
fun oklch(l: Float, c: Float, hDegrees: Float): Color {
    val hRad = Math.toRadians(hDegrees.toDouble())
    val a = c * cos(hRad).toFloat()
    val b = c * sin(hRad).toFloat()

    // The two matrices below are the published OKLab constants, used verbatim.
    // None of them are tuned for this app, so don't "adjust" one to nudge a
    // color — change the hue passed in instead.
    // OKLab -> nonlinear LMS:
    val l_ = l + 0.3963377774f * a + 0.2158037573f * b
    val m_ = l - 0.1055613458f * a - 0.0638541728f * b
    val s_ = l - 0.0894841775f * a - 1.2914855480f * b

    val lCubed = l_ * l_ * l_
    val mCubed = m_ * m_ * m_
    val sCubed = s_ * s_ * s_

    // LMS -> linear sRGB:
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
    /**
     * Text drawn on [cardBg] or [stripe].
     *
     * White reads as the obvious choice on these saturated fills, but it only
     * reaches 2.2–2.7:1 against [cardBg] — under WCAG AA's 3:1 floor for large
     * text on all fourteen hues — and 2.7–3.4:1 against the lighter [stripe],
     * which clears 3:1 on some hues but never the 4.5:1 the card name needs.
     * 4.5 rather than 3 because the name is 17sp: WCAG's large-text allowance
     * starts at 18.66sp for bold text, so Home's card name is normal-sized
     * however heavy [HeavyTextStyle] makes it look. This
     * tone is derived from the same hue rather than picked by hand, and clears
     * 4.5:1 on both surfaces (worst case 4.73:1, against the stripe). It also
     * sits closer to the black line art in the icons.
     */
    val labelOnCard: Color,
)

private fun paletteFor(hue: Float) = ThemePalette(
    cardBg = oklch(0.72f, 0.16f, hue),
    stripe = oklch(0.66f, 0.17f, hue),
    cardBorder = oklch(0.50f, 0.19f, hue),
    labelOnCard = oklch(0.26f, 0.10f, hue),
)

// Every hue is a compile-time constant, so the nine pow() calls a palette costs
// need happen once rather than on every recomposition of every card and row.
private val PALETTES: Map<String, ThemePalette> =
    THEME_DEFS.associate { it.id to paletteFor(it.hue) }

fun ThemeDef.palette(): ThemePalette = PALETTES[id] ?: paletteFor(hue)

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
    val gateOptionLockedBg = oklch(0.72f, 0.03f, 250f)
    val rowBgEnabled = oklch(0.94f, 0.02f, 90f)
    val rowBgDisabled = oklch(0.97f, 0.005f, 90f)
    val subtitleText = oklch(0.48f, 0.02f, 90f)

    /**
     * The lightest text tone in the app: the gate's Cancel link and Settings'
     * small print.
     *
     * It was 0.55, which is 4.34:1 on [appBackground] — the only ground it is
     * ever drawn on, and short of AA's 4.5:1. Every place it appears is 12–13sp,
     * so there is no large-text allowance to fall back on. Darkened to 0.51,
     * which reaches 5.12:1 there while staying visibly the quietest tone in the
     * table. `NeutralContrastTest` is what now keeps it that way.
     */
    val cancelText = oklch(0.51f, 0.02f, 90f)
}
