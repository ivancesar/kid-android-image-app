package com.kidsexplore.app

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG contrast, shared by the two tests that measure it.
 *
 * One copy rather than one per test: two tests quietly disagreeing about the
 * arithmetic would be a far more baffling failure than either of them
 * disagreeing with the palette, which is the failure they exist to produce.
 */

/**
 * WCAG relative luminance. [com.kidsexplore.app.ui.theme.oklch] gamma-encodes on
 * the way out, so these components are non-linear sRGB and have to be
 * linearised first.
 */
internal fun luminance(c: Color): Double {
    fun channel(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
}

internal fun contrast(a: Color, b: Color): Double {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}
