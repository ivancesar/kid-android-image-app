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
