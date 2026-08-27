package com.kidsexplore.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsexplore.app.R
import com.kidsexplore.app.model.PolicyBlock
import com.kidsexplore.app.model.PolicySpan
import com.kidsexplore.app.model.parsePolicy
import com.kidsexplore.app.ui.POLICY_LIST_TEST_TAG
import com.kidsexplore.app.ui.theme.NeutralColors

/** The file Gradle copies out of `docs/` into assets; see `app/build.gradle.kts`. */
private const val POLICY_ASSET = "privacy-policy.md"

/**
 * The privacy policy, read from the app's own assets.
 *
 * Bundled rather than linked. Play needs a hosted copy for the Console listing
 * either way, but the copy a parent reads *here* should not depend on a web
 * server, a repository staying public, or the device having a browser — a
 * privacy policy that 404s is worse than one that is merely plain. It also
 * means the app still opens nothing outward, which is the strongest form of
 * the claim the document itself makes.
 *
 * Reached only from Parent Settings, so it inherits the parental gate.
 */
@Composable
fun PolicyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // Read once per composition of this screen, not per recomposition: it is a
    // file open and a full parse, and the document cannot change under us.
    val blocks = remember(context) {
        parsePolicy(context.assets.open(POLICY_ASSET).bufferedReader().use { it.readText() })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NeutralColors.screenBackground)
            // "Done" is pinned below the list, so the navigation-bar inset
            // matters here as much as the status bar does at the top.
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 22.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).testTag(POLICY_LIST_TEST_TAG),
            contentPadding = PaddingValues(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(blocks) { block -> PolicyBlockText(block) }
        }
        Text(
            text = stringResource(R.string.policy_done),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(NeutralColors.doneButtonBg)
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 14.dp)
                .wrapContentHeight(),
        )
    }
}

/** One parsed block, styled by what it is. */
@Composable
private fun PolicyBlockText(block: PolicyBlock) {
    when (block) {
        is PolicyBlock.Title -> Text(
            text = block.text,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = NeutralColors.labelDark,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        is PolicyBlock.Heading -> Text(
            text = block.text,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            color = NeutralColors.labelDark,
            modifier = Modifier.padding(top = 12.dp),
        )

        is PolicyBlock.Paragraph -> Text(
            text = block.spans.annotated(),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            // subtitleText, not cancelText: both clear WCAG AA now, but this
            // is a page of body copy rather than a line of small print, and
            // subtitleText is the tone the rest of the app sets body copy in.
            color = NeutralColors.subtitleText,
        )

        is PolicyBlock.Bullet -> Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 4.dp),
        ) {
            Text(text = "•", fontSize = 14.sp, lineHeight = 20.sp, color = NeutralColors.subtitleText)
            Text(
                text = block.spans.annotated(),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = NeutralColors.subtitleText,
            )
        }
    }
}

/** Spans to an [androidx.compose.ui.text.AnnotatedString], bold where marked. */
@Composable
private fun List<PolicySpan>.annotated() = buildAnnotatedString {
    for (span in this@annotated) {
        if (span.bold) {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold)) { append(span.text) }
        } else {
            append(span.text)
        }
    }
}
