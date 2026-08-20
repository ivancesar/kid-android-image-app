package com.kidsexplore.app.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Renders a theme's icon from its generated vector drawable.
 *
 * The artwork is two-tone by design, so it is drawn deliberately untinted — a
 * drawable-wide tint would collapse the white detail into the black shapes and
 * leave a flat silhouette. Sizing is entirely up to the caller's [modifier] so
 * the glyph scales with whatever container it's placed in.
 */
@Composable
fun ThemeIconGlyph(@DrawableRes iconRes: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(iconRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
