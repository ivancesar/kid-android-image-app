package com.kidsexplore.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every control a screen reader can focus must carry its own name.
 *
 * `clickable` and `toggleable` merge descendant semantics for us, so a control
 * whose label sits in a child composable is named already. These do not guard
 * that mechanism — they guard the cases it does not cover: a control whose
 * label is removed, hidden behind `clearAndSetSemantics`, or icon-only and
 * relying on an explicit `contentDescription`, as the gear does.
 */
@RunWith(AndroidJUnit4::class)
class AccessibleNamesTest {

    @get:Rule
    val compose = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun str(id: Int) = resources.getString(id)

    @Test
    fun aThemeCardCarriesItsNameOnTheClickableNode() {
        compose.setContent {
            KidsExploreTheme {
                HomeScreen(themes = THEME_DEFS, onOpenTheme = {}, onOpenGate = {})
            }
        }
        val name = resources.getString(THEME_DEFS.first().nameRes)
        compose.onNode(hasClickAction() and hasText(name)).assertIsDisplayed()
    }

    @Test
    fun theGearCarriesItsNameOnTheClickableNode() {
        compose.setContent {
            KidsExploreTheme {
                HomeScreen(themes = THEME_DEFS, onOpenTheme = {}, onOpenGate = {})
            }
        }
        compose.onNodeWithContentDescription(str(R.string.home_settings_button))
            .assertIsDisplayed()
        compose.onNode(
            hasClickAction() and
                androidx.compose.ui.test.hasContentDescription(str(R.string.home_settings_button)),
        ).assertIsDisplayed()
    }

    @Test
    fun aThemeRowCarriesItsNameOnTheToggleableNode() {
        compose.setContent {
            KidsExploreTheme {
                SettingsScreen(
                    disabledThemeIds = emptySet(),
                    onToggle = {},
                    onDone = {},
                    currentLanguage = AppLocales.SYSTEM,
                    onPickLanguage = {},
                        onOpenPolicy = {},
                )
            }
        }
        val name = resources.getString(THEME_DEFS.first().nameRes)
        compose.onNode(hasClickAction() and hasText(name)).assertIsDisplayed()
    }

    @Test
    fun theLanguageRowCarriesItsCurrentValue() {
        compose.setContent {
            KidsExploreTheme {
                SettingsScreen(
                    disabledThemeIds = emptySet(),
                    onToggle = {},
                    onDone = {},
                    currentLanguage = AppLocales.SYSTEM,
                    onPickLanguage = {},
                        onOpenPolicy = {},
                )
            }
        }
        compose.onNode(
            hasClickAction() and hasText(str(R.string.settings_language_system)),
        ).assertIsDisplayed()
    }

    /**
     * Collapsing the photographer credits has to actually collapse them for a
     * screen reader too.
     *
     * `maxLines` clamps at draw time only: Compose publishes the whole string
     * to the semantics tree regardless, so without an explicit override a
     * TalkBack user hears all 183 names in the collapsed state and is then
     * offered a control that changes nothing they can perceive. This is the
     * assertion that the collapsed node announces the summary instead — the
     * one thing `theCreditListCollapsesAndExpands` cannot see, since it
     * matches on the control rather than on the text.
     */
    @Test
    fun theCollapsedCreditListAnnouncesASummaryRatherThanEveryName() {
        compose.setContent {
            KidsExploreTheme {
                // Forced narrow: the summary only replaces the names while the
                // text is actually clamped, which a wide window would not do.
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(DpSize(400.dp, 800.dp)),
                ) {
                    SettingsScreen(
                        disabledThemeIds = emptySet(),
                        onToggle = {},
                        onDone = {},
                        currentLanguage = AppLocales.SYSTEM,
                        onPickLanguage = {},
                        onOpenPolicy = {},
                    )
                }
            }
        }
        val names = str(R.string.attribution_photographers)
        val count = names.split(", ").size
        val summary = resources.getQuantityString(
            R.plurals.attribution_photographers_summary, count, count,
        )

        compose.onNodeWithTag(THEME_LIST_TEST_TAG).performScrollToNode(hasText(summary))
        compose.onNodeWithText(summary).assertIsDisplayed()
        // The run-on list of names must not be what gets announced.
        compose.onNodeWithText(names, substring = true).assertDoesNotExist()

        compose.onNodeWithText(str(R.string.attribution_show_more)).performClick()
        // Expanded, the names are the point and the summary steps aside.
        compose.onNodeWithTag(THEME_LIST_TEST_TAG)
            .performScrollToNode(hasText(names, substring = true))
        compose.onNodeWithText(summary).assertDoesNotExist()
    }
}
