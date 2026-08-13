package com.kidsexplore.app.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "KIDS EXPLORE",
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
            text = "Pick something to look at!",
            style = HeavyTextStyle,
            fontSize = 25.sp,
            color = NeutralColors.labelDark,
            lineHeight = 29.sp,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(themes, key = { it.id }) { theme ->
                ThemeCard(theme = theme, onClick = { onOpenTheme(theme.id) })
            }
        }
    }
}

private val BoldLabelStyle = TextStyle(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)

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
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    ThemeIconGlyph(icon = theme.icon, accent = palette.cardBorder)
                }
                Text(
                    text = theme.name,
                    style = HeavyTextStyle.copy(
                        shadow = Shadow(color = Color.Black.copy(alpha = 0.12f), offset = Offset(0f, 2f), blurRadius = 1f),
                    ),
                    fontSize = 17.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
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
