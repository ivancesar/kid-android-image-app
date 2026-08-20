package com.kidsexplore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.kidsexplore.app.AppLocales
import com.kidsexplore.app.R
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.palette

@Composable
fun SettingsScreen(
    enabledThemes: Map<String, Boolean>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
    currentLanguage: String,
    onPickLanguage: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = NeutralColors.labelDark,
        )
        Text(
            text = stringResource(R.string.settings_subtitle),
            fontSize = 14.sp,
            color = NeutralColors.subtitleText,
        )
        LazyColumn(
            modifier = Modifier.weight(1f).testTag(THEME_LIST_TEST_TAG),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "language") {
                LanguagePicker(current = currentLanguage, onPick = onPickLanguage)
            }

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
                        text = stringResource(theme.nameRes),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = NeutralColors.labelDark,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.settings_done),
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

/**
 * Language choice, at the head of the settings list.
 *
 * The options wrap rather than scrolling sideways: a translated "match my
 * phone" can be long enough to push the last language off a single row, and a
 * clipped option the user just chose reads as a bug. Each language names itself
 * — "Hrvatski", not "Croatian" — since whoever needs it may not read the
 * language currently on screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguagePicker(current: String, onPick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_language),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = NeutralColors.subtitleText,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LanguagePill(
                label = stringResource(R.string.settings_language_system),
                selected = current == AppLocales.SYSTEM,
                onClick = { onPick(AppLocales.SYSTEM) },
            )
            AppLocales.SUPPORTED.forEach { tag ->
                LanguagePill(
                    label = AppLocales.endonym(tag),
                    selected = current == tag,
                    onClick = { onPick(tag) },
                )
            }
        }
    }
}

@Composable
private fun LanguagePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        color = if (selected) Color.White else NeutralColors.labelDark,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) NeutralColors.doneButtonBg else NeutralColors.rowBgEnabled)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
