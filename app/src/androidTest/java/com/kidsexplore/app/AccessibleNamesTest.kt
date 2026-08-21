package com.kidsexplore.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every control a screen reader can focus must carry its own name.
 *
 * `clickable` and `toggleable` do not merge descendant semantics, so a control
 * whose label lives in a child composable exposes a focusable node with no name
 * at all. TalkBack often recovers by reading descendant text, but that is a
 * heuristic — these assert the name is actually on the node.
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
                )
            }
        }
        compose.onNode(
            hasClickAction() and hasText(str(R.string.settings_language_system)),
        ).assertIsDisplayed()
    }
}
