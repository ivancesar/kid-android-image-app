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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.AppLocales
import com.kidsexplore.app.R
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.NeutralColors
import com.kidsexplore.app.ui.theme.palette

@Composable
fun SettingsScreen(
    disabledThemeIds: Set<String>,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
    currentLanguage: String,
    onPickLanguage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // "Done" is pinned to the bottom of this Column, so it needs the
            // navigation-bar inset as much as the title needs the status bar.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = NeutralColors.labelDark,
        )
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "language") {
                LanguagePicker(current = currentLanguage, onPick = onPickLanguage)
            }

            // Names the list below and separates it from the language row, which
            // otherwise reads as the first entry in the category list. The
            // explanation sits here rather than under the screen title so it
            // describes the thing directly beneath it.
            item(key = "categories-heading") {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_categories),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = NeutralColors.subtitleText,
                    )
                    Text(
                        text = stringResource(R.string.settings_subtitle),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = NeutralColors.cancelText,
                    )
                }
            }

            items(THEME_DEFS, key = { it.id }) { theme ->
                val enabled = theme.id !in disabledThemeIds
                val cardBorder = theme.palette().cardBorder
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (enabled) NeutralColors.rowBgEnabled else NeutralColors.rowBgDisabled)
                        // toggleable, not clickable: this is what makes
                        // TalkBack announce the row as a checkbox and read out
                        // whether it is currently ticked.
                        .toggleable(
                            value = enabled,
                            role = Role.Checkbox,
                            onValueChange = { onToggle(theme.id) },
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (enabled) cardBorder else Color.White)
                            .border(width = 2.dp, color = cardBorder, shape = RoundedCornerShape(8.dp))
                            // The tick is a picture of the state the row already
                            // announces; reading "check mark" as well is noise.
                            .clearAndSetSemantics {},
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
                .clickable(onClick = onDone, role = Role.Button)
                .padding(16.dp),
        )
    }
}

/**
 * Language choice, at the head of the settings list.
 *
 * A dropdown rather than a row of options, so shipping a fourth or tenth
 * language costs no extra room and changes nothing about how this reads —
 * "same as phone settings" is just the first entry in the same list. Each
 * language names itself ("Hrvatski", not "Croatian"), since whoever needs it
 * may not read the language currently on screen.
 */
@Composable
private fun LanguagePicker(current: String, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    // The menu is measured from the row it drops out of, so the two line up
    // however wide the screen is.
    var anchorWidthPx by remember { mutableIntStateOf(0) }
    val anchorWidth = with(LocalDensity.current) { anchorWidthPx.toDp() }
    val options = listOf(AppLocales.SYSTEM) + AppLocales.SUPPORTED

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_language),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = NeutralColors.subtitleText,
        )
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { anchorWidthPx = it.width }
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeutralColors.rowBgEnabled)
                    .clickable { expanded = true }
                    .semantics { role = Role.DropdownList }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = languageLabel(current),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = NeutralColors.labelDark,
                    modifier = Modifier.weight(1f),
                )
                Text("\u25BE", fontSize = 16.sp, color = NeutralColors.subtitleText)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.width(anchorWidth),
            ) {
                options.forEach { tag ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = languageLabel(tag),
                                fontWeight = if (tag == current) FontWeight.ExtraBold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = NeutralColors.labelDark,
                            )
                        },
                        trailingIcon = {
                            if (tag == current) {
                                Text("\u2713", fontWeight = FontWeight.Black, color = NeutralColors.doneButtonBg)
                            }
                        },
                        onClick = {
                            expanded = false
                            if (tag != current) onPick(tag)
                        },
                    )
                }
            }
        }
    }
}

/** [AppLocales.SYSTEM] has no endonym of its own, so it carries a translated label. */
@Composable
private fun languageLabel(tag: String): String =
    if (tag == AppLocales.SYSTEM) stringResource(R.string.settings_language_system) else AppLocales.endonym(tag)
