package com.kidsexplore.app

import androidx.compose.ui.graphics.Color
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.palette
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Text on a theme card must stay readable on every hue.
 *
 * White was the original choice and reached only 2.2–2.7:1 against the card
 * fill — under WCAG AA's 3:1 floor for large text, on all fourteen hues, with
 * the Viewer's body-sized item label needing 4.5:1. The fix is a hue-derived
 * dark tone, and this is what stops a fifteenth theme quietly reintroducing the
 * problem: the palette is generated from a hue, so a bad hue is a bad card.
 */
class CardContrastTest {

    /** WCAG relative luminance. Compose colours are already linear-ish sRGB floats. */
    private fun luminance(c: Color): Double {
        fun channel(v: Float): Double {
            val d = v.toDouble()
            return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (hi, lo) = luminance(a).let { la ->
            luminance(b).let { lb -> max(la, lb) to min(la, lb) }
        }
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun everyThemeLabelClearsWcagAaOnItsCardAndStripe() {
        THEME_DEFS.forEach { theme ->
            val p = theme.palette()
            val onCard = contrast(p.labelOnCard, p.cardBg)
            val onStripe = contrast(p.labelOnCard, p.stripe)
            // 4.5:1 is the normal-text floor; the Viewer's label is 16sp, which
            // does not qualify as large text, so both surfaces are held to it.
            assertTrue(
                "${theme.id}: label on cardBg is %.2f:1, needs 4.5".format(onCard),
                onCard >= 4.5,
            )
            assertTrue(
                "${theme.id}: label on stripe is %.2f:1, needs 4.5".format(onStripe),
                onStripe >= 4.5,
            )
        }
    }
}
