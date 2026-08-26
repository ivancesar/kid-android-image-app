package com.kidsexplore.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.kidsexplore.app.AppLocales
import com.kidsexplore.app.R
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
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
            modifier = Modifier.weight(1f).testTag(THEME_LIST_TEST_TAG),
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

            // Above Attribution because it is an action and Attribution is
            // not, and below the categories because it is not the thing a
            // parent came in here to do.
            item(key = "privacy") {
                PrivacyPolicyLink()
            }

            item(key = "attribution") {
                Attribution()
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
 * Where the hosted privacy policy lives.
 *
 * TODO: replace with the real public URL before uploading to Play. The same
 * address goes in the Play Console listing, and the two have to agree — Play
 * checks that the policy is reachable, and a child-directed app is expected to
 * offer it from inside the app as well, which is what [PrivacyPolicyLink] is.
 * The document itself is `docs/privacy-policy.html` in this repository, which
 * GitHub Pages will serve as-is.
 */
private const val PRIVACY_POLICY_URL = "https://example.com/kids-explore/privacy-policy"

/**
 * A way out to the privacy policy, behind the parental gate.
 *
 * Behind the gate deliberately. Play's Families policy wants the policy
 * reachable from within the app, and it equally does not want a child one tap
 * away from a browser — Settings is the one screen that is already gated, so it
 * is the only place an outbound link belongs.
 *
 * Set as a link rather than as a button: this leaves the app, and it should not
 * look like the controls above it that do not.
 */
@Composable
private fun PrivacyPolicyLink() {
    val context = LocalContext.current
    // Read here rather than inside the click: `stringResource` is a composable
    // read, and it is also what makes the message follow a language change.
    val unavailable = stringResource(R.string.settings_privacy_policy_unavailable, PRIVACY_POLICY_URL)
    Text(
        text = stringResource(R.string.settings_privacy_policy),
        fontSize = 13.sp,
        fontWeight = FontWeight.ExtraBold,
        color = NeutralColors.subtitleText,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .padding(top = 20.dp)
            // Same order as PhotographerCredits' control and ViewerScreen's
            // HomeButton: heightIn before clickable is what makes the clickable
            // node itself 48dp tall rather than only the box drawn around it.
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button) {
                // A device with no browser is rare but real — a stripped kids'
                // tablet, a locked-down enterprise profile — and an
                // uncaught ActivityNotFoundException there would crash the app
                // on a link that exists to satisfy a policy requirement. The
                // address is read out instead, so the tap still leads somewhere.
                //
                // try/catch rather than checking `resolveActivity` first: from
                // API 30 package visibility makes that return null even when a
                // browser is installed, unless the manifest declares a
                // <queries> element for it. Launching and catching needs no
                // such declaration and is the check that cannot be wrong.
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_POLICY_URL.toUri()))
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, unavailable, Toast.LENGTH_LONG).show()
                }
            }
            .padding(horizontal = 4.dp)
            .wrapContentHeight(),
    )
}

/**
 * Where the photographs come from, at the foot of the settings list.
 *
 * Behind the gate rather than on Home: it is a notice for the adult who
 * installed the app, and a child paging through diggers has no use for it.
 * Last in the list for the same reason — nothing here is an action, so it
 * should not sit between a parent and the controls that are.
 *
 * One line per source, each naming the category it covers: a single blanket
 * line stopped being true the moment a second theme got its pictures
 * somewhere else. The Unsplash License asks for no credit and NASA's
 * material is not copyrighted at all, so the photographers' names are a
 * courtesy; the notices are the part that has to stay.
 */
@Composable
private fun Attribution() {
    Column(
        modifier = Modifier.padding(top = 20.dp),
        // Wider than the gap inside a block, so the two sources read as two
        // things. The names sit under Unsplash's notice because they are
        // Unsplash's photographers; a flat list spaced evenly left the NASA
        // line looking like the tail of that name list.
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_attribution),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            color = NeutralColors.subtitleText,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AttributionLine(stringResource(R.string.attribution_images))
            PhotographerCredits()
        }
        // Its own block rather than another clause on the Unsplash one: the
        // two sources cover different categories and carry different terms,
        // and NASA's material is not licensed so much as simply not
        // copyrighted.
        AttributionLine(stringResource(R.string.attribution_nasa))
    }
}

/** How much of the credit list shows before a parent asks for the rest. */
private const val COLLAPSED_CREDIT_LINES = 3

/**
 * Unsplash's photographers, clamped to [COLLAPSED_CREDIT_LINES] with a control
 * to open the rest.
 *
 * One sentence rather than a heading and a list: TalkBack reads it as a
 * sentence either way, and 183 names as 183 rows is a wall to swipe through.
 * At that length the sentence was a wall too, which is what the clamp is for —
 * the names are all still here, they just no longer sit between a parent and
 * the end of the screen.
 *
 * A format string, not concatenation: the separator and the word order belong
 * to the language, and gluing them together here puts both out of a
 * translator's reach.
 */
@Composable
private fun PhotographerCredits() {
    // Both rememberSaveable, and they have to agree: this is the last item in
    // a LazyColumn, so it is disposed whenever it scrolls off screen.
    //
    // `overflows` looks like a pure measurement that could be re-derived with
    // a plain `remember`, and it cannot - not while `onTextLayout` below is
    // guarded on `!expanded`. Restore `expanded = true` next to a fresh
    // `overflows = false` and the text lays out unclamped, the guard skips the
    // assignment, and the control never comes back: the credits are stuck open
    // with no way to close them short of leaving Settings. Losing the guard
    // instead has the same effect one step earlier, since an expanded layout
    // reports no overflow. They are one piece of state in two variables; keep
    // them on the same lifetime.
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by rememberSaveable { mutableStateOf(false) }

    val names = stringResource(R.string.attribution_photographers)
    // Counted rather than written down, so the summary cannot drift from the
    // roster it is summarising. The names are one proper-noun list shared by
    // every locale, so the separator is the same everywhere.
    val count = names.split(", ").size
    val summary = pluralStringResource(
        R.plurals.attribution_photographers_summary, count, count,
    )
    Text(
        text = stringResource(R.string.attribution_photographers_line, names),
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = NeutralColors.cancelText,
        maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_CREDIT_LINES,
        overflow = TextOverflow.Ellipsis,
        // `maxLines` clamps at draw time only - the whole string still reaches
        // the semantics tree - so collapsing is a no-op for a screen reader
        // unless the node is given the shorter text to announce as well.
        // Without this, TalkBack reads all 183 names and is then offered a
        // control that changes nothing it can perceive.
        //
        // clearAndSetSemantics rather than semantics: replacing a node's
        // semantics is what it is for, where `semantics` only wins the text
        // property by peer-collapse ordering, which is an implementation
        // detail rather than a contract. It also drops the text-layout action
        // along with the text, instead of leaving one that describes the full
        // 183-name layout while a 40-character summary is what gets announced
        // - a mismatch TalkBack's line-granularity navigation would walk into.
        modifier = if (expanded) {
            Modifier
        } else {
            Modifier.clearAndSetSemantics { text = AnnotatedString(summary) }
        },
        // Only meaningful while collapsed. Expanded there is nothing left to
        // overflow, and assigning it unconditionally would take away the
        // control that closes it again.
        onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
    )
    // Absent when the whole list already fits — a shorter roster, a wider
    // window, or a smaller font scale — rather than offering to expand what
    // is not clamped.
    val expandedState = stringResource(R.string.attribution_state_expanded)
    val collapsedState = stringResource(R.string.attribution_state_collapsed)
    if (overflows) {
        Text(
            text = stringResource(
                if (expanded) R.string.attribution_show_less
                else R.string.attribution_show_more
            ),
            fontSize = 12.sp,
            color = NeutralColors.cancelText,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                // `clickable` on a bare Text does not pick up Material's
                // minimum interactive size - that applies to material3
                // components - so 12sp of text would leave a ~17dp target.
                // heightIn before clickable is what makes the clickable node
                // itself 48dp tall rather than only the box drawn around it;
                // wrapContentHeight then centres the text in that height.
                // Same order as ViewerScreen's HomeButton.
                .heightIn(min = 48.dp)
                .clickable(role = Role.Button) { expanded = !expanded }
                .padding(horizontal = 4.dp)
                .wrapContentHeight()
                // Announced as expand/collapse rather than only as a button,
                // so the action names what it does to the text above it, and
                // with the state said out loud - "Show more" alone does not
                // tell a screen reader which way the credits currently sit.
                .semantics {
                    stateDescription = if (expanded) expandedState else collapsedState
                    if (expanded) {
                        collapse { expanded = false; true }
                    } else {
                        expand { expanded = true; true }
                    }
                },
        )
    }
}

/** Body text in [Attribution]; every line is set the same way. */
@Composable
private fun AttributionLine(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = NeutralColors.cancelText,
    )
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
