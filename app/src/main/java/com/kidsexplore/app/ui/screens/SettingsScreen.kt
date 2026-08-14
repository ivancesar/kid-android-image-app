package com.kidsexplore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.palette

/**
 * Parent-facing screen for choosing which themes appear on Home. Reachable only
 * through [com.kidsexplore.app.ui.screens.GateScreen].
 */
@Composable
fun SettingsScreen(
    enabledThemes: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Parent Settings",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = NeutralColors.labelDark,
        )
        Text(
            text = "Choose which themes your child can see.",
            fontSize = 14.sp,
            color = NeutralColors.subtitleText,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Every theme, not just the visible ones — a disabled theme has to
            // stay listed here or there would be no way to switch it back on.
            items(THEME_DEFS, key = { it.id }) { theme ->
                val enabled = enabledThemes[theme.id] != false
                val cardBorder = theme.palette().cardBorder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (enabled) NeutralColors.rowBgEnabled else NeutralColors.rowBgDisabled)
                        .clickable { onToggle(theme.id) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (enabled) cardBorder else Color.White)
                            .border(width = 2.dp, color = cardBorder, shape = RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (enabled) {
                            Text("✓", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                    Text(
                        text = theme.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = NeutralColors.labelDark,
                    )
                }
            }
        }
        Text(
            text = "Done",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(NeutralColors.doneButtonBg)
                .clickable(onClick = onDone)
                .padding(16.dp),
        )
    }
}
