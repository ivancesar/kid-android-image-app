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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.R
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.ThemePalette
import com.kidsexplore.app.ui.theme.palette
import kotlin.math.hypot

/** Arrows live here rather than inside a translatable string. */
private const val BACK_GLYPH = "◀"
private const val NEXT_GLYPH = "▶"

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

    // pointerInput is keyed on theme.id alone, so it would otherwise hold the
    // callbacks captured the first time a theme was shown.
    val currentOnNext by rememberUpdatedState(onNext)
    val currentOnPrev by rememberUpdatedState(onPrev)

    val swipeModifier = Modifier.pointerInput(theme.id) {
        val thresholdPx = 60.dp.toPx()
        // A plain local: the gesture callbacks share this scope, and nothing in
        // composition reads it, so snapshot state bought nothing here.
        var dragTotal = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragTotal = 0f },
            onDragCancel = { dragTotal = 0f },
            onDragEnd = {
                if (dragTotal <= -thresholdPx) currentOnNext()
                else if (dragTotal >= thresholdPx) currentOnPrev()
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
                    NavPillButton(BACK_GLYPH, stringResource(R.string.viewer_back), palette.cardBorder, onPrev)
                    Spacer(modifier = Modifier.width(24.dp))
                    NavPillButton(NEXT_GLYPH, stringResource(R.string.viewer_next), palette.cardBorder, onNext)
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
                NavPillButton(BACK_GLYPH, stringResource(R.string.viewer_back), palette.cardBorder, onPrev)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageCard(palette = palette, currentLabel = currentLabel)
                }
                NavPillButton(NEXT_GLYPH, stringResource(R.string.viewer_next), palette.cardBorder, onNext)
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
        Text(
            text = currentLabel,
            fontFamily = FontFamily.Monospace,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
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
