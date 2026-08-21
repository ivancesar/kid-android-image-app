package com.kidsexplore.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.R
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
import com.kidsexplore.app.ui.icons.ThemeIconGlyph
import com.kidsexplore.app.ui.theme.BoldTextStyle
import com.kidsexplore.app.ui.theme.HeavyTextStyle
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.palette

@Composable
fun HomeScreen(
    themes: List<ThemeDef>,
    onOpenTheme: (String) -> Unit,
    onOpenGate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()
    val isAtTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }

    // The header floats over the grid instead of sharing a Column with it —
    // that way the grid's own size never depends on the header's animated
    // visibility, which used to feed back into the scroll position and make
    // the list bounce as it hid/showed itself.
    //
    // Saveable so a rotation doesn't drop it back to 0 and reflow the grid
    // against a 16dp top inset for a frame before the header remeasures.
    var headerHeightPx by rememberSaveable { mutableIntStateOf(0) }
    val headerHeightDp = with(LocalDensity.current) { headerHeightPx.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Top and sides are consumed here so the header sits clear of the
            // status bar and any cutout. The bottom is deliberately left for
            // the grid's contentPadding, so cards scroll under the navigation
            // bar rather than stopping short of it.
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
            ),
    ) {
        val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

        LazyVerticalGrid(
            // Adaptive rather than a fixed 2 columns: in landscape (or on a
            // tablet) this fits more, narrower columns instead of stretching
            // each card — and its icon — to an oversized square.
            columns = GridCells.Adaptive(minSize = 160.dp),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                start = 22.dp,
                end = 22.dp,
                top = headerHeightDp + 16.dp,
                bottom = 12.dp + bottomInset,
            ),
            modifier = Modifier.fillMaxSize().testTag(THEME_LIST_TEST_TAG),
        ) {
            items(themes, key = { it.id }) { theme ->
                ThemeCard(theme = theme, onClick = { onOpenTheme(theme.id) })
            }
        }

        // A parent can switch every theme off, which otherwise leaves a header
        // floating over nothing and no hint at where the content went.
        if (themes.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeightDp)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = stringResource(R.string.home_empty_title),
                    style = HeavyTextStyle,
                    fontSize = 20.sp,
                    color = NeutralColors.labelDark,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.home_empty_body),
                    fontSize = 14.sp,
                    color = NeutralColors.subtitleText,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp,
                )
            }
        }

        AnimatedVisibility(
            visible = isAtTop,
            modifier = Modifier.align(Alignment.TopStart).fillMaxWidth(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeutralColors.appBackground)
                    .onSizeChanged { headerHeightPx = it.height }
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    // Reserves the height the gear used to contribute, now
                    // that the gear is pinned outside this Column — otherwise
                    // the header shrinks and the grid's top padding with it.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_brand),
                        style = BoldTextStyle,
                        fontSize = 15.sp,
                        color = NeutralColors.labelMuted,
                    )
                }

                Text(
                    text = stringResource(R.string.home_title),
                    style = HeavyTextStyle,
                    fontSize = 25.sp,
                    color = NeutralColors.labelDark,
                    lineHeight = 29.sp,
                )
            }
        }

        // Pinned rather than living in the fading header: this is the only
        // route to parent settings anywhere in the app, and it used to vanish
        // as soon as the grid scrolled, leaving a parent to hunt back to the
        // top for it. Drawn last so it stays above the cards it now floats on.
        val settingsLabel = stringResource(R.string.home_settings_button)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 26.dp)
                // The visible dial stays 36dp — small on purpose, it is the
                // parent's control — inside a 48dp touch target.
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onOpenGate, role = Role.Button)
                .semantics { contentDescription = settingsLabel },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeutralColors.gearButtonBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "⚙",
                    fontSize = 16.sp,
                    color = NeutralColors.labelMuted,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

// The icons are black-and-white line art drawn for a light ground, so the disc
// behind them is kept close to white — at the card's own tint the white fills
// in the artwork would sink into the background and only the outlines would read.
private const val IconDiscAlpha = 0.85f

// How much of the disc the artwork fills. Past ~0.71 the corners of a square
// icon cross the disc's edge, but these icons carry no detail right in their
// corners, so a little over that still reads as contained.
private const val IconInsetInDisc = 0.78f

@Composable
private fun ThemeCard(theme: ThemeDef, onClick: () -> Unit) {
    val palette = theme.palette()
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(26.dp))
            .background(palette.cardBg)
            // clickable already merges descendant semantics
            // (AbstractClickableNode.shouldMergeDescendantSemantics is final and
            // true), so the theme name below is the control's accessible name
            // without anything further here.
            .clickable(onClick = onClick, role = Role.Button),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        // Claims every bit of height the name leaves behind and
                        // then matches it in width, so the icon is as large as
                        // the card can make it however wide the column ends up
                        // — e.g. the much wider landscape grid.
                        .weight(1f)
                        .aspectRatio(1f, matchHeightConstraintsFirst = true)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = IconDiscAlpha)),
                    contentAlignment = Alignment.Center,
                ) {
                    ThemeIconGlyph(
                        iconRes = theme.iconRes,
                        modifier = Modifier.fillMaxSize(IconInsetInDisc),
                    )
                }
                Text(
                    text = stringResource(theme.nameRes),
                    style = HeavyTextStyle,
                    fontSize = 17.sp,
                    color = palette.labelOnCard,
                    textAlign = TextAlign.Center,
                    // Two lines at large font scales: "Construction" ellipsized
                    // to "Constructi…" at 2x, and the disc above yields height
                    // rather than the name losing characters.
                    maxLines = 2,
                    // Clipped mid-glyph reads as a rendering fault; an ellipsis
                    // reads as a name that didn't fit.
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(palette.cardBorder),
        )
    }
}
