package com.kidsexplore.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.data.ThemeStore
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests covering the whole journey a user can take through the
 * app: Home → Viewer (paging by button and by swipe) → Home → parental gate
 * (wrong answer, lockout, then correct) → Settings (toggle a theme) → Home.
 *
 * These are UI tests. The state machine itself is covered off-device by
 * `AppViewModelTest` in the unit-test source set, so the ViewModel here is
 * backed by an in-memory [ThemeStore] and a fresh [SavedStateHandle] rather
 * than by real SharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
class KidsExploreFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var viewModel: AppViewModel

    private val carLabels = THEME_DEFS.first { it.id == "cars" }.labels

    private class FakeThemeStore : ThemeStore {
        private var disabled: Set<String> = emptySet()
        override fun loadDisabled(): Set<String> = disabled
        override fun saveDisabled(disabled: Set<String>) {
            this.disabled = disabled
        }
    }

    @Before
    fun setUp() {
        viewModel = AppViewModel(store = FakeThemeStore(), savedState = SavedStateHandle())
        compose.setContent {
            KidsExploreTheme { KidsExploreApp(viewModel) }
        }
    }

    private fun state(): UiState = compose.runOnIdle { viewModel.uiState }

    private fun gate(): UiState.Gate = state() as UiState.Gate

    /**
     * Both the Home grid and the Settings list are lazy, so an item far enough
     * down is not composed at all. Scroll it into view before asserting on it
     * rather than assuming the whole list fits the device under test.
     */
    private fun scrollTo(text: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun openGate() {
        // The gear is icon-only, so it is addressed by its description —
        // which is also what TalkBack reads.
        compose.onNodeWithContentDescription("Parent settings").performClick()
    }

    private fun answerWrongOnce() {
        val question = gate().question
        val wrong = question.values.first { it != question.correct }
        compose.onNodeWithText(wrong.toString()).performClick()
    }

    private fun enterSettings() {
        openGate()
        compose.onNodeWithText(gate().question.correct.toString()).performClick()
    }

    @Test
    fun homeListsEveryTheme() {
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
        THEME_DEFS.forEach { theme ->
            scrollTo(theme.name)
            compose.onNodeWithText(theme.name).assertIsDisplayed()
        }
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
            compose.onNodeWithText("Next").performClick()
        }
        // wrapped back to the first image
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun backButtonFromTheFirstImageWrapsToTheLast() {
        compose.onNodeWithText("Cars").performClick()

        compose.onNodeWithText("Back").performClick()
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
        openGate()
        compose.onNodeWithText("Solve this to continue").assertIsDisplayed()

        answerWrongOnce()

        compose.onNodeWithText("Not quite, try again!").assertIsDisplayed()
        assertTrue("must not reach Settings", state() is UiState.Gate)
    }

    /**
     * The gate's whole purpose. Before this, a wrong answer left the same four
     * buttons up, so a child reached Settings by exhausting them.
     */
    @Test
    fun repeatedWrongAnswersLockTheGateEvenAgainstTheCorrectOne() {
        openGate()
        repeat(MAX_GATE_FAILURES) { answerWrongOnce() }

        compose.onNodeWithText("Too many tries", substring = true).assertIsDisplayed()

        // The right answer is now refused too, until the lockout expires.
        compose.onNodeWithText(gate().question.correct.toString()).performClick()
        assertTrue("lockout must outrank a correct answer", state() is UiState.Gate)
    }

    /** A lockout a child can clear by backing out and reopening is no lockout. */
    @Test
    fun theLockoutSurvivesLeavingAndReopeningTheGate() {
        openGate()
        repeat(MAX_GATE_FAILURES) { answerWrongOnce() }

        compose.onNodeWithText("Cancel").performClick()
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
        openGate()

        compose.onNodeWithText("Too many tries", substring = true).assertIsDisplayed()
        compose.onNodeWithText(gate().question.correct.toString()).performClick()
        assertTrue(state() is UiState.Gate)
    }

    @Test
    fun cancellingTheGateReturnsHome() {
        openGate()
        compose.onNodeWithText("Cancel").performClick()

        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()
    }

    @Test
    fun correctGateAnswerOpensParentSettings() {
        enterSettings()

        compose.onNodeWithText("Parent Settings").assertIsDisplayed()
        compose.onNodeWithText("Choose which themes your child can see.").assertIsDisplayed()
    }

    @Test
    fun disablingAThemeInSettingsRemovesItFromHome() {
        enterSettings()

        scrollTo("Ocean")
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

        enterSettings()
        scrollTo("Ocean")
        compose.onNodeWithText("Ocean").performClick() // tick it again
        compose.onNodeWithText("Done").performClick()

        scrollTo("Ocean")
        compose.onNodeWithText("Ocean").assertIsDisplayed()
    }

    @Test
    fun fullJourneyHomeToViewerToSettingsAndBack() {
        // browse a theme
        compose.onNodeWithText("Dinosaurs").performClick()
        val dinoLabels = THEME_DEFS.first { it.id == "dinosaurs" }.labels
        compose.onNodeWithText(dinoLabels[0]).assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText(dinoLabels[1]).assertIsDisplayed()

        // back home
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()

        // through the gate into settings
        enterSettings()
        compose.onNodeWithText("Parent Settings").assertIsDisplayed()

        // and back home again
        compose.onNodeWithText("Done").performClick()
        compose.onNodeWithText("Pick something to look at!").assertIsDisplayed()

        // reopening a theme starts from its first image again
        compose.onNodeWithText("Dinosaurs").performClick()
        compose.onNodeWithText(dinoLabels[0]).assertIsDisplayed()
    }
}
