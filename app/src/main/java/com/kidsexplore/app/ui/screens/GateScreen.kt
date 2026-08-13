package com.kidsexplore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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

@Composable
fun GateScreen(
    question: GateQuestion,
    wrong: Boolean,
    onPick: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterVertically),
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 280.dp)
                .wrapContentHeight(),
        ) {
            items(question.values) { value ->
                Text(
                    text = value.toString(),
                    style = HeavyTextStyle,
                    fontSize = 26.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeutralColors.gateOptionBg)
                        .clickable { onPick(value) }
                        .padding(vertical = 18.dp),
                )
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
