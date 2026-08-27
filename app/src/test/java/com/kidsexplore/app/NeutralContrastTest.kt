package com.kidsexplore.app

import androidx.compose.ui.graphics.Color
import com.kidsexplore.app.ui.theme.KidsColorScheme
import com.kidsexplore.app.ui.theme.NeutralColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every neutral text tone, against every background it is actually drawn on.
 *
 * [CardContrastTest] covers the fourteen card palettes, which are one function
 * of one hue: get that right once and a fifteenth theme is safe by
 * construction. The neutrals are the opposite shape — colors written down by
 * hand, used in pairs chosen screen by screen — and nothing gated them, which
 * is exactly how `cancelText` shipped at 4.34:1 on the app background. What
 * this pins is the table below, not a formula.
 *
 * The table is read off the screens rather than crossed with every background
 * in [NeutralColors]. A pairing that never happens would fail this test for a
 * color nobody can see, and — worse — would invite the next person to relax the
 * threshold for it. Each entry names where it is drawn, so the table can be
 * checked against the screen rather than against this comment.
 */
class NeutralContrastTest {

    /** One text color, the fill it sits on, and where in the app that happens. */
    private data class Drawn(val where: String, val text: Color, val on: Color)

    private val n = NeutralColors

    /**
     * Home, the Gate and Settings paint no background of their own, so their
     * text sits on `KidsExploreTheme`'s Surface, which is [NeutralColors.appBackground].
     * The policy is the one screen that paints [NeutralColors.screenBackground]
     * itself. The Viewer's dark ground carries no text at all — every label
     * there is on a white or accent-filled button.
     *
     * The one exception is the language menu: `DropdownMenu` paints Material's
     * own `surfaceContainer`, which this app does not override, so its two rows
     * are measured against the scheme rather than against a neutral.
     */
    private val pairings = listOf(
        // ------------------------------------------------------------- Home
        Drawn("Home's KIDS EXPLORE eyebrow, 15sp", n.labelMuted, n.appBackground),
        Drawn("Home's gear glyph, 16sp", n.labelMuted, n.gearButtonBg),
        Drawn("Home's title, 25sp", n.labelDark, n.appBackground),
        Drawn("Home's empty-state title, 20sp", n.labelDark, n.appBackground),
        Drawn("Home's empty-state body, 14sp", n.subtitleText, n.appBackground),

        // ----------------------------------------------------------- Viewer
        Drawn("the Viewer's Home button label, 11sp", n.labelDarker, Color.White),

        // ------------------------------------------------------------- Gate
        Drawn("the gate's GROWN-UPS ONLY eyebrow, 15sp", n.labelMuted, n.appBackground),
        Drawn("the gate's prompt and sum, 20sp and 40sp", n.labelDark, n.appBackground),
        Drawn("the gate's wrong-answer and lockout messages, 14sp", n.errorText, n.appBackground),
        Drawn("the gate's Cancel link, 13sp", n.cancelText, n.appBackground),

        // --------------------------------------------------------- Settings
        Drawn("Settings' title, 22sp", n.labelDark, n.appBackground),
        Drawn("Settings' section headings and privacy link, 13sp", n.subtitleText, n.appBackground),
        Drawn("Settings' subtitle and attribution body, 12sp", n.cancelText, n.appBackground),
        Drawn("a category row's name, 16sp", n.labelDark, n.rowBgEnabled),
        Drawn("a switched-off category row's name, 16sp", n.labelDark, n.rowBgDisabled),
        Drawn("the language row's current choice, 16sp", n.labelDark, n.rowBgEnabled),
        Drawn("the language row's chevron, 16sp", n.subtitleText, n.rowBgEnabled),
        Drawn("a language menu row, 16sp", n.labelDark, KidsColorScheme.surfaceContainer),
        Drawn("the language menu's tick, 16sp", n.doneButtonBg, KidsColorScheme.surfaceContainer),

        // ----------------------------------------------------------- Policy
        Drawn("the policy's title and headings, 22sp and 15sp", n.labelDark, n.screenBackground),
        Drawn("the policy's body copy and bullets, 14sp", n.subtitleText, n.screenBackground),
    )

    /**
     * 4.5:1 throughout, with no large-text allowance claimed anywhere.
     *
     * Nothing in the table above is large text by WCAG's measure except Home's
     * 25sp title and the gate's 40sp sum, and both of those are `labelDark`,
     * the darkest tone here — so holding the whole table to the stricter floor
     * costs nothing and removes a per-row judgement call that would rot the
     * first time a font size changed.
     */
    @Test
    fun everyNeutralTextToneClearsWcagAaWhereItIsDrawn() {
        pairings.forEach { (where, text, on) ->
            val ratio = contrast(text, on)
            assertTrue(
                "$where: %.2f:1, needs 4.5".format(ratio),
                ratio >= 4.5,
            )
        }
    }

    /**
     * The table above is written by hand, which is the same weakness that let
     * `cancelText` through in the first place — so this is what stops a
     * sixteenth neutral being added and quietly left out of it.
     *
     * Reflection over the object's fields rather than a count: a rename should
     * fail here too, and with the name in the message.
     *
     * Filtered to `long` fields because `Color` is a value class over a ULong,
     * so each neutral is a bare `long` in the bytecode — which is also what
     * separates them from the `INSTANCE` and `$stable` fields Kotlin and the
     * Compose compiler add alongside.
     */
    @Test
    fun everyNeutralColorIsAccountedFor() {
        val accountedFor = setOf(
            // Text tones, all present in the table above.
            "labelMuted", "labelDark", "labelDarker", "subtitleText", "cancelText", "errorText",
            // Grounds that carry neutral text, all present in the table above.
            "appBackground", "screenBackground", "rowBgEnabled", "rowBgDisabled", "gearButtonBg",
            // Button fills. As grounds these carry Color.White rather than a
            // neutral tone, so the text on them is outside what this measures.
            // Two notes rather than leaving that unexplained:
            //
            // doneButtonBg is also used as a *text* colour — the tick in the
            // language menu — and is measured as one in the table above.
            //
            // White on gateOptionLockedBg is 2.47:1, under AA's 3:1 even
            // counting the 26sp answers as large text. WCAG exempts disabled
            // controls and those buttons are exactly that, so it is defensible
            // — but the locked state is very faint and wants a look at.
            "doneButtonBg", "gateOptionBg", "gateOptionLockedBg",
            // The Viewer's ground. No text is drawn directly on it.
            "viewerBackground",
        )
        val declared = NeutralColors::class.java.declaredFields
            .filter { it.type == java.lang.Long.TYPE }
            .map { it.name }
            .toSet()
        assertTrue("reflection found no neutrals at all", declared.isNotEmpty())

        assertEquals(
            "a neutral was added or renamed: put it in the table above, or name it here",
            emptySet<String>(),
            declared - accountedFor,
        )
        assertEquals(
            "a neutral named here no longer exists",
            emptySet<String>(),
            accountedFor - declared,
        )
    }
}
