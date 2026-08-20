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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.kidsexplore.app.R
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.icons.ThemeIconGlyph
import com.kidsexplore.app.ui.theme.HeavyTextStyle
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.palette

@Composable
fun HomeScreen(
    themes: List<ThemeDef>,
    onOpenTheme: (String) -> Unit,
    onOpenGate: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val isAtTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }

    // The header floats over the grid instead of sharing a Column with it —
    // that way the grid's own size never depends on the header's animated
    // visibility, which used to feed back into the scroll position and make
    // the list bounce as it hid/showed itself.
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeightDp = with(LocalDensity.current) { headerHeightPx.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
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
                bottom = 12.dp,
            ),
            modifier = Modifier.fillMaxSize().testTag(THEME_LIST_TEST_TAG),
        ) {
            items(themes, key = { it.id }) { theme ->
                ThemeCard(theme = theme, onClick = { onOpenTheme(theme.id) })
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
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.home_brand),
                        style = BoldLabelStyle,
                        fontSize = 15.sp,
                        color = NeutralColors.labelMuted,
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeutralColors.gearButtonBg)
                            .clickable(onClick = onOpenGate),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⚙", fontSize = 16.sp, color = NeutralColors.labelMuted)
                    }
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
    }
}

private val BoldLabelStyle = TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

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
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier
                    // Claims every bit of height the name leaves behind and
                    // then matches it in width, so the icon is as large as the
                    // card can make it however wide the column ends up — e.g.
                    // the much wider landscape grid.
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
                style = HeavyTextStyle.copy(
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.12f), offset = Offset(0f, 2f), blurRadius = 1f),
                ),
                fontSize = 17.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(palette.cardBorder),
        )
    }
}
