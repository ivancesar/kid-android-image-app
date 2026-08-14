package com.kidsexplore.app.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.GateQuestion
import com.kidsexplore.app.R
import com.kidsexplore.app.ui.theme.HeavyTextStyle
import com.kidsexplore.app.ui.theme.NeutralColors
import kotlinx.coroutines.delay

@Composable
fun GateScreen(
    question: GateQuestion,
    wrong: Boolean,
    lockedUntilElapsedMs: Long,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Counts the lockout down so the screen re-enables itself without needing
    // the ViewModel to hold a timer. The ViewModel still re-checks the deadline
    // in pickGateAnswer(), so this is presentation, not enforcement.
    var remainingSeconds by remember(lockedUntilElapsedMs) {
        mutableIntStateOf(secondsUntil(lockedUntilElapsedMs))
    }
    LaunchedEffect(lockedUntilElapsedMs) {
        while (secondsUntil(lockedUntilElapsedMs) > 0) {
            delay(200)
            remainingSeconds = secondsUntil(lockedUntilElapsedMs)
        }
    }
    val locked = remainingSeconds > 0

    // Box centers the Column when it fits; the Column's own verticalScroll
    // takes over once landscape's shorter height can't fit everything.
    Box(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(
                text = stringResource(R.string.gate_eyebrow),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = NeutralColors.labelMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.gate_prompt),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = NeutralColors.labelDark,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.gate_question, question.a, question.b),
                style = HeavyTextStyle,
                fontSize = 40.sp,
                color = NeutralColors.labelDark,
                textAlign = TextAlign.Center,
            )
            // Always exactly 4 answers in a 2x2 grid — a plain Row/Column
            // avoids nesting a lazy layout inside a scrollable Column, which
            // Compose disallows (infinite height constraint).
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                question.values.chunked(2).forEach { rowValues ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        rowValues.forEach { value ->
                            Text(
                                text = value.toString(),
                                style = HeavyTextStyle,
                                fontSize = 26.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (locked) NeutralColors.gateOptionLockedBg
                                        else NeutralColors.gateOptionBg
                                    )
                                    .clickable(enabled = !locked, role = Role.Button) { onPick(value) }
                                    .padding(vertical = 18.dp),
                            )
                        }
                    }
                }
            }
            when {
                locked -> Text(
                    text = pluralStringResource(
                        R.plurals.gate_locked,
                        remainingSeconds,
                        remainingSeconds,
                    ),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeutralColors.errorText,
                    textAlign = TextAlign.Center,
                )

                wrong -> Text(
                    text = stringResource(R.string.gate_wrong),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeutralColors.errorText,
                )
            }
            Text(
                text = stringResource(R.string.gate_cancel),
                fontSize = 13.sp,
                color = NeutralColors.cancelText,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    // 6dp left this well under the 48dp touch minimum.
                    .clickable(onClick = onCancel, role = Role.Button)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            )
        }
    }
}

/**
 * Whole seconds left until [deadlineElapsedMs], rounded up so the countdown
 * shows "1 second" rather than "0" for the final tick. 0 means not locked —
 * that is also how the ViewModel encodes "no lockout".
 */
private fun secondsUntil(deadlineElapsedMs: Long): Int {
    if (deadlineElapsedMs <= 0L) return 0
    val remaining = deadlineElapsedMs - SystemClock.elapsedRealtime()
    if (remaining <= 0L) return 0
    return ((remaining + 999L) / 1000L).toInt()
}
