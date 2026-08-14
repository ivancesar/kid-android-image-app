package com.kidsexplore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.GateQuestion
import com.kidsexplore.app.ui.theme.HeavyTextStyle
import com.kidsexplore.app.ui.theme.NeutralColors

/**
 * The parental gate standing between the Home screen's gear button and
 * Settings. An addition problem is enough to stop a pre-reader without being an
 * obstacle to an adult — this guards a preference screen, not anything
 * sensitive, so it is intentionally not a real authentication barrier.
 */
@Composable
fun GateScreen(
    question: GateQuestion,
    wrong: Boolean,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    // Box centers the Column when it fits; the Column's own verticalScroll
    // takes over once landscape's shorter height can't fit everything.
    Box(
        modifier = Modifier.fillMaxSize(),
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
                text = "GROWN-UPS ONLY",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = NeutralColors.labelMuted,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Solve this to continue",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = NeutralColors.labelDark,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "${question.a} + ${question.b} = ?",
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
                                    .background(NeutralColors.gateOptionBg)
                                    .clickable { onPick(value) }
                                    .padding(vertical = 18.dp),
                            )
                        }
                    }
                }
            }
            if (wrong) {
                Text(
                    text = "Not quite, try again!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = NeutralColors.errorText,
                )
            }
            Text(
                text = "Cancel",
                fontSize = 13.sp,
                color = NeutralColors.cancelText,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .clickable(onClick = onCancel)
                    .padding(6.dp),
            )
        }
    }
}
