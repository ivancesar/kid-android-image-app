package com.kidsexplore.app.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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

/** How much of the theme's striped card shows around a photograph. */
private val CardFrameWidth = 8.dp

@Composable
fun ViewerScreen(
    theme: ThemeDef,
    currentLabel: String,
    onHome: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * The photograph for the item on screen, or null for a theme that has
     * none yet — those keep the striped placeholder card. Defaulted so the
     * layout tests can drive the screen without picking a theme with artwork.
     */
    @DrawableRes currentImage: Int? = null,
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
                    ImageCard(palette = palette, currentLabel = currentLabel, image = currentImage)
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
                    ImageCard(palette = palette, currentLabel = currentLabel, image = currentImage)
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

/**
 * The whole content of the screen: a photograph where the theme has one, and
 * the striped placeholder card carrying the item's label where it does not.
 *
 * Either way the label is what TalkBack announces, and it is a live region:
 * Back/Next/swipe replace the content without moving focus, so without one
 * TalkBack says nothing at all as a child pages through the set.
 */
@Composable
private fun ImageCard(palette: ThemePalette, currentLabel: String, @DrawableRes image: Int?) {
    if (image != null) {
        PhotoCard(palette = palette, label = currentLabel, image = image)
    } else {
        PlaceholderCard(palette = palette, label = currentLabel)
    }
}

/**
 * A photograph, in a card cut to the photograph's own shape.
 *
 * The set mixes landscape and portrait shots, so a card that filled the space
 * and fitted the photo inside it left a tall phone showing more stripe than
 * picture. Sizing the card from the image's aspect ratio instead gives the
 * largest rectangle of that shape the space allows: no letterboxing, and the
 * picture grows to fill a tablet rather than sitting at whatever size it
 * happens to have been shot at.
 *
 * The theme's stripes survive as a thin frame around it — the Viewer is
 * otherwise all dark grey, and the frame is what still says which category a
 * child is in.
 */
@Composable
private fun PhotoCard(palette: ThemePalette, label: String, @DrawableRes image: Int) {
    val painter = painterResource(image)
    val size = painter.intrinsicSize
    // A painter with no intrinsic size cannot be shaped to fit; fill the space
    // instead of dividing by an unspecified height.
    val ratio = if (size.isSpecified && size.height > 0f) size.width / size.height else null

    Box(
        modifier = Modifier
            .then(if (ratio != null) Modifier.aspectRatio(ratio) else Modifier.fillMaxSize())
            .clip(RoundedCornerShape(24.dp))
            .background(palette.cardBg)
            .diagonalStripes(palette.stripe)
            .padding(CardFrameWidth),
    ) {
        Image(
            painter = painter,
            // Through the parameter, not a semantics block of our own: Image
            // sets Role.Image in the same branch it sets the description, so
            // passing null here and declaring the description ourselves would
            // announce the label without ever saying it labels a picture. The
            // live region is a different property and composes with it.
            contentDescription = label,
            // Crop, not Fit: the frame's padding leaves an opening a few
            // pixels off the photo's own ratio, and cropping that away is
            // invisible where a second round of letterboxing would not be.
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp - CardFrameWidth))
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

/** The label on a striped card, for a theme whose photographs do not exist yet. */
@Composable
private fun PlaceholderCard(palette: ThemePalette, label: String) {
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
            text = label,
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
