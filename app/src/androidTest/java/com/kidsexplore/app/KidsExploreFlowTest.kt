package com.kidsexplore.app

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests covering the whole journey a user can take through the
 * app: Home → Viewer (paging by button and by swipe) → Home → parental gate
 * (wrong answer, then correct) → Settings (toggle a theme) → Home.
 *
 * The app is hosted with a ViewModel the test owns, so each test starts from
 * cleared preferences and a known screen.
 */
@RunWith(AndroidJUnit4::class)
class KidsExploreFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var viewModel: AppViewModel

    private val carLabels = THEME_DEFS.first { it.id == "cars" }.labels

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        viewModel = AppViewModel(app)
        compose.setContent {
            KidsExploreTheme { KidsExploreApp(viewModel) }
        }
    }

    @Test
    fun homeListsEveryTheme() {
        THEME_DEFS.forEach { theme ->
            compose.onNodeWithText(theme.name).assertIsDisplayed()
        }
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
    }

    @Test
    fun tappingAThemeOpensItsViewer() {
        compose.onNodeWithText("Cars").performClick()

        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun nextButtonWalksEveryImageAndWrapsAround() {
        compose.onNodeWithText("Cars").performClick()

        carLabels.forEach { expected ->
            compose.onNodeWithText(expected).assertIsDisplayed()
            compose.onNodeWithText("▶").performClick()
        }
        // wrapped back to the first image
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun backButtonFromTheFirstImageWrapsToTheLast() {
        compose.onNodeWithText("Cars").performClick()

        compose.onNodeWithText("◀").performClick()
        compose.onNodeWithText(carLabels.last()).assertIsDisplayed()
    }

    @Test
    fun swipingLeftAndRightChangesTheImage() {
        compose.onNodeWithText("Cars").performClick()

        compose.onNodeWithText(carLabels[0]).performTouchInput { swipeLeft() }
        compose.onNodeWithText(carLabels[1]).assertIsDisplayed()

        compose.onNodeWithText(carLabels[1]).performTouchInput { swipeRight() }
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun homeButtonReturnsFromTheViewer() {
        compose.onNodeWithText("Cars").performClick()
        compose.onNodeWithText("Home").performClick()

        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
    }

    @Test
    fun wrongGateAnswerShowsRetryMessageAndBlocksSettings() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText("Solve this to continue").assertIsDisplayed()

        val gate = viewModel.gate!!
        val wrong = gate.values.first { it != gate.correct }
        compose.onNodeWithText(wrong.toString()).performClick()

        compose.onNodeWithText("Not quite, try again!").assertIsDisplayed()
        assertEquals("must not reach Settings", Screen.GATE, viewModel.screen)
    }

    @Test
    fun cancellingTheGateReturnsHome() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
    }

    @Test
    fun correctGateAnswerOpensParentSettings() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()

        compose.onNodeWithText("Parent Settings").assertIsDisplayed()
        compose.onNodeWithText("Choose which themes your child can see.").assertIsDisplayed()
    }

    @Test
    fun disablingAThemeInSettingsRemovesItFromHome() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()

        compose.onNodeWithText("Ocean").performClick() // untick it
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
        compose.onNodeWithText("Ocean").assertDoesNotExist()
        compose.onNodeWithText("Cars").assertIsDisplayed()
    }

    @Test
    fun reEnablingAThemeBringsItBackToHome() {
        viewModel.toggleThemeEnabled("ocean")
        compose.onNodeWithText("Ocean").assertDoesNotExist()

        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()
        compose.onNodeWithText("Ocean").performClick() // tick it again
        compose.onNodeWithText("Done").performClick()

        compose.onNodeWithText("Ocean").assertIsDisplayed()
    }

    @Test
    fun fullJourneyHomeToViewerToSettingsAndBack() {
        // browse a theme
        compose.onNodeWithText("Dinosaurs").performClick()
        val dinoLabels = THEME_DEFS.first { it.id == "dinosaurs" }.labels
        compose.onNodeWithText(dinoLabels[0]).assertIsDisplayed()
        compose.onNodeWithText("▶").performClick()
        compose.onNodeWithText(dinoLabels[1]).assertIsDisplayed()

        // back home
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()

        // through the gate into settings
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()
        compose.onNodeWithText("Parent Settings").assertIsDisplayed()

        // and back home again
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()

        // reopening a theme starts from its first image again
        compose.onNodeWithText("Dinosaurs").performClick()
        compose.onNodeWithText(dinoLabels[0]).assertIsDisplayed()
    }
}
