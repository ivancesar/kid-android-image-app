package com.kidsexplore.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.kidsexplore.app.R
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.ThemePalette
import com.kidsexplore.app.ui.theme.palette
import kotlin.math.hypot

@Composable
fun ViewerScreen(
    theme: ThemeDef,
    currentLabel: String,
    onHome: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
) {
    val palette = theme.palette()
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT

    var dragTotal by remember(theme.id) { mutableFloatStateOf(0f) }
    val swipeModifier = Modifier.pointerInput(theme.id) {
        val thresholdPx = 60.dp.toPx()
        detectHorizontalDragGestures(
            onDragStart = { dragTotal = 0f },
            onDragCancel = { dragTotal = 0f },
            onDragEnd = {
                if (dragTotal <= -thresholdPx) onNext() else if (dragTotal >= thresholdPx) onPrev()
                dragTotal = 0f
            },
        ) { change, dragAmount ->
            change.consume()
            dragTotal += dragAmount
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeutralColors.viewerBackground),
    ) {
        if (isPortrait) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(10.dp),
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
                        .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 30.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    NavPillButton(icon = "◀", label = stringResource(R.string.viewer_back), accent = palette.cardBorder, onClick = onPrev)
                    Spacer(modifier = Modifier.width(24.dp))
                    NavPillButton(icon = "▶", label = stringResource(R.string.viewer_next), accent = palette.cardBorder, onClick = onNext)
                }
            }
        } else {
            // The image runs down to the status bar (not under it) so the
            // Home button can float over it instead of reserving a header row.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp)
                    .then(swipeModifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavPillButton(icon = "◀", label = stringResource(R.string.viewer_back), accent = palette.cardBorder, onClick = onPrev)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageCard(palette = palette, currentLabel = currentLabel)
                }
                NavPillButton(icon = "▶", label = stringResource(R.string.viewer_next), accent = palette.cardBorder, onClick = onNext)
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
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
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("⌂", fontSize = 15.sp)
        Text(stringResource(R.string.viewer_home_button), fontWeight = FontWeight.ExtraBold, fontSize = 9.sp, color = NeutralColors.labelDarker)
    }
}

/**
 * [icon] is an arrow glyph and stays out of resources; [label] is the word
 * beside it. Kept as two arguments rather than one concatenated string so a
 * translation containing a space cannot split in the wrong place, and so the
 * glyph does not end up on the wrong side of the word under RTL.
 */
@Composable
private fun NavPillButton(icon: String, label: String, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .size(width = 100.dp, height = 80.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(accent)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button; contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon, fontSize = 28.sp, color = Color.White)
        Text(
            text = label,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
