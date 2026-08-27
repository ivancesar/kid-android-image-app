package com.kidsexplore.app

import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.palette
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Text on a theme card must stay readable on every hue.
 *
 * White was the original choice and reached only 2.2–2.7:1 against the card
 * fill — under WCAG AA's 3:1 floor for large text, on all fourteen hues, where
 * the card name needs 4.5:1. The fix is a hue-derived dark tone, and this is
 * what stops a fifteenth theme quietly reintroducing the problem: the palette
 * is generated from a hue, so a bad hue is a bad card.
 *
 * The hand-written neutrals are a different shape of risk and are covered by
 * [NeutralContrastTest] instead.
 */
class CardContrastTest {

    @Test
    fun everyThemeLabelClearsWcagAaOnItsCardAndStripe() {
        THEME_DEFS.forEach { theme ->
            val p = theme.palette()
            val onCard = contrast(p.labelOnCard, p.cardBg)
            val onStripe = contrast(p.labelOnCard, p.stripe)
            // 4.5:1 is the normal-text floor, and Home's card name is normal
            // text: 17sp is below the 18.66sp where WCAG's large-text allowance
            // starts for bold, whatever FontWeight.Black makes it look like.
            //
            // The stripe carries no text today — it is the Viewer's photo
            // frame, and the frame has no label on it. Held to the same floor
            // anyway, because the two fills are one card as far as the design
            // is concerned and moving the name onto the striped surface should
            // not be the thing that discovers a contrast problem.
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
