package com.kidsexplore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.R
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.ThemePalette
import com.kidsexplore.app.ui.theme.palette
import kotlin.math.hypot

/**
 * Arrows live here rather than inside a translatable string — but they do have
 * to mirror, so they are chosen from the layout direction rather than fixed.
 * "Back" always points toward the start edge, "Next" toward the end.
 */
private const val LEFT_GLYPH = "◀"
private const val RIGHT_GLYPH = "▶"

/**
 * Below this the nav buttons sit under the image; at or above it they flank it.
 *
 * Measured against the width actually available, not the device orientation:
 * a tablet in portrait and a split-screen window are both cases where the two
 * disagree, and orientation gave those the cramped layout.
 */
private val WideLayoutMinWidth = 600.dp

@Composable
fun ViewerScreen(
    theme: ThemeDef,
    currentLabel: String,
    onHome: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = theme.palette()

    // The manifest declares supportsRtl, so the controls have to honour it:
    // in an RTL locale the sequence advances right-to-left, which flips both
    // the arrow glyphs and the direction of a "next" swipe. The Row itself
    // needs no help — it already lays Back out at the start edge.
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val backGlyph = if (rtl) RIGHT_GLYPH else LEFT_GLYPH
    val nextGlyph = if (rtl) LEFT_GLYPH else RIGHT_GLYPH
    /** Sign of a drag that means "next": leftward in LTR, rightward in RTL. */
    val nextSign = if (rtl) 1f else -1f

    // pointerInput is keyed on theme.id alone, so it would otherwise hold the
    // callbacks captured the first time a theme was shown.
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrev by rememberUpdatedState(onPrev)

    val swipeModifier = Modifier.pointerInput(theme.id, nextSign) {
        val thresholdPx = 60.dp.toPx()
        // A plain local: the gesture callbacks share this scope, and nothing in
        // composition reads it, so snapshot state bought nothing here.
        var dragTotal = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragTotal = 0f },
            onDragCancel = { dragTotal = 0f },
            onDragEnd = {
                val towardNext = dragTotal * nextSign
                if (towardNext >= thresholdPx) currentOnNext()
                else if (towardNext <= -thresholdPx) currentOnPrev()
                dragTotal = 0f
            },
        ) { change, dragAmount ->
            change.consume()
            dragTotal += dragAmount
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralColors.viewerBackground),
    ) {
        if (maxWidth < WideLayoutMinWidth) {
            // The dark background stays edge to edge; only the controls inside
            // are held clear of the system bars.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                ) {
                    HomeButton(onClick = onHome)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(14.dp)
                        .then(swipeModifier),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageCard(palette = palette, currentLabel = currentLabel)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // 30dp used to stand in for the navigation bar by hand;
                        // safeDrawing on the Column now covers that properly.
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    NavPillButton(backGlyph, stringResource(R.string.viewer_back), palette.cardBorder, onPrev)
                    Spacer(modifier = Modifier.width(24.dp))
                    NavPillButton(nextGlyph, stringResource(R.string.viewer_next), palette.cardBorder, onNext)
                }
            }
        } else {
            // The image runs down to the status bar (not under it) so the
            // Home button can float over it instead of reserving a header row.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(start = 20.dp, end = 20.dp)
                    .then(swipeModifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavPillButton(backGlyph, stringResource(R.string.viewer_back), palette.cardBorder, onPrev)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageCard(palette = palette, currentLabel = currentLabel)
                }
                NavPillButton(nextGlyph, stringResource(R.string.viewer_next), palette.cardBorder, onNext)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(10.dp),
            ) {
                HomeButton(onClick = onHome)
            }
        }
    }
}

@Composable
private fun ImageCard(palette: ThemePalette, currentLabel: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.cardBg)
            .diagonalStripes(palette.stripe)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // The entire content of the screen. Back/Next/swipe replace it without
        // moving focus, so without a live region TalkBack says nothing at all
        // when a child pages through the set.
        Text(
            text = currentLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = palette.labelOnCard,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun HomeButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            // A minimum rather than a fixed size, so the button grows with the
            // system font scale instead of clipping its label — and 48dp is
            // the smallest comfortable touch target.
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick, role = Role.Button)
            // clickable does not merge descendants; without this the focusable
            // node is nameless and "Home" sits on a non-focusable child.
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Decorative: the "Home" label below is what TalkBack should read.
        Text("⌂", fontSize = 15.sp, modifier = Modifier.clearAndSetSemantics {})
        Text(
            text = stringResource(R.string.viewer_home),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp,
            color = NeutralColors.labelDarker,
        )
    }
}

@Composable
private fun NavPillButton(icon: String, label: String, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .sizeIn(minWidth = 100.dp, minHeight = 80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(accent)
            .clickable(onClick = onClick, role = Role.Button)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = icon,
            fontSize = 28.sp,
            color = Color.White,
            modifier = Modifier.clearAndSetSemantics {},
        )
        Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
    }
}

/** Mirrors the source's repeating-linear-gradient(45deg, cardBg 14px, stripe 14px 28px). */
private fun Modifier.diagonalStripes(stripeColor: Color): Modifier = this.drawBehind {
    val stripeWidthPx = 14.dp.toPx()
    val periodPx = 28.dp.toPx()
    val diag = hypot(size.width.toDouble(), size.height.toDouble()).toFloat()
    rotate(degrees = 45f, pivot = Offset(size.width / 2f, size.height / 2f)) {
        var x = -diag
        while (x < diag) {
            drawRect(
                color = stripeColor,
                topLeft = Offset(x, -diag),
                size = Size(stripeWidthPx, diag * 3f),
            )
            x += periodPx
        }
    }
}
