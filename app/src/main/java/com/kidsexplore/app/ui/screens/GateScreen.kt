package com.kidsexplore.app.ui.screens

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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
    lockedUntilWallMs: Long,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Counts the lockout down so the screen re-enables itself without needing
    // the ViewModel to hold a timer. The ViewModel still re-checks the deadline
    // in pickGateAnswer(), so this is presentation, not enforcement.
    var remainingSeconds by remember(lockedUntilWallMs) {
        mutableIntStateOf(secondsUntil(lockedUntilWallMs))
    }
    LaunchedEffect(lockedUntilWallMs) {
        while (secondsUntil(lockedUntilWallMs) > 0) {
            delay(200)
            remainingSeconds = secondsUntil(lockedUntilWallMs)
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
                    // widthIn BEFORE fillMaxWidth. The other order is inert:
                    // fillMaxWidth passes fixed constraints down, and widthIn
                    // then clamps its 280dp into [W, W] and discards it — which
                    // in landscape gave four ~440dp answer buttons spanning the
                    // display.
                    .widthIn(max = 280.dp)
                    .fillMaxWidth(),
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
                                    // enabled = false also marks the node
                                    // disabled to TalkBack, so the reason the
                                    // buttons stopped responding is the live
                                    // countdown below plus "disabled" here.
                                    .clickable(enabled = !locked, role = Role.Button) { onPick(value) }
                                    .padding(vertical = 18.dp),
                            )
                        }
                    }
                }
            }
            // Both messages are the screen's only feedback, and both appear
            // without the focus moving, so TalkBack would otherwise never read
            // them. Polite: they follow the child's own tap, nothing is urgent
            // enough to cut off whatever is already being spoken.
            when {
                locked -> {
                    val countdown = when (val left = countdownFor(remainingSeconds)) {
                        is Countdown.Seconds ->
                            pluralStringResource(R.plurals.gate_locked, left.value, left.value)

                        is Countdown.Minutes ->
                            pluralStringResource(R.plurals.gate_locked_minutes, left.value, left.value)
                    }
                    // The visible text reticks every second. If that drove the
                    // live region TalkBack would read a fresh countdown thirty
                    // times over, so the node is described once from the
                    // deadline and cleared of the changing text — sighted users
                    // still get the tick, screen-reader users get one sentence.
                    val announcement = remember(lockedUntilWallMs) { countdown }
                    Text(
                        text = countdown,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NeutralColors.errorText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clearAndSetSemantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = announcement
                        },
                    )
                }

                wrong -> Text(
                    text = stringResource(R.string.gate_wrong),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeutralColors.errorText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
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
 * Whole seconds left until [deadlineWallMs], rounded up so the countdown shows
 * "1 second" rather than "0" for the final tick. 0 means not locked — that is
 * also how the ViewModel encodes "no lockout".
 *
 * Wall-clock to match the deadline the ViewModel persists; it already caps that
 * deadline at the current lockout level's length, so a backwards clock change
 * cannot make this count down from something absurd.
 *
 * [nowWallMs] defaults to the real clock but is a parameter so the round-up —
 * a stated requirement, "shows 1, never 0" — can be tested without waiting.
 */
internal fun secondsUntil(deadlineWallMs: Long, nowWallMs: Long = System.currentTimeMillis()): Int {
    if (deadlineWallMs <= 0L) return 0
    val remaining = deadlineWallMs - nowWallMs
    if (remaining <= 0L) return 0
    return ((remaining + 999L) / 1000L).toInt()
}

private const val SECONDS_PER_MINUTE = 60

/**
 * Which unit the lockout message counts in, and how many of it are left.
 *
 * The lockout escalates to eight minutes, and a seconds-only countdown would
 * read "Wait 480 seconds" — a number a parent has to do arithmetic on before it
 * tells them anything. Two units mean two `plurals`, and which one to use is
 * part of the answer rather than something the caller works out afterwards; a
 * sealed type is what makes the screen unable to pair one unit's count with the
 * other's string.
 */
internal sealed interface Countdown {
    data class Seconds(val value: Int) : Countdown

    data class Minutes(val value: Int) : Countdown
}

/**
 * Seconds below a minute, whole minutes at or above one.
 *
 * Minutes round up for the same reason [secondsUntil] does: the message must
 * never claim less time is left than actually is. So 61 seconds reads as two
 * minutes, and the last tick before the switch is a flat "1 minute" rather than
 * a minute that has already partly gone.
 */
internal fun countdownFor(remainingSeconds: Int): Countdown =
    if (remainingSeconds < SECONDS_PER_MINUTE) {
        Countdown.Seconds(remainingSeconds)
    } else {
        Countdown.Minutes((remainingSeconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE)
    }
