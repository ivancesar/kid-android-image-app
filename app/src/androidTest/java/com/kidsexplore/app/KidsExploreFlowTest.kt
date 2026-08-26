package com.kidsexplore.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
import com.kidsexplore.app.ui.viewerImageTestTag
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertEquals
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
 * backed by an in-memory [FakeThemeStore] and a fresh [SavedStateHandle];
 * `SharedPreferencesThemeStoreTest` covers the real store separately.
 */
@RunWith(AndroidJUnit4::class)
class KidsExploreFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var viewModel: AppViewModel

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    // Expected text is resolved from resources, never written out in English:
    // the suite is meant to exercise whichever language the app is set to.
    private fun str(id: Int) = resources.getString(id)
    private fun themeNamed(id: String) =
        resources.getString(THEME_DEFS.first { it.id == id }.nameRes)

    /** Names and labels live in strings.xml now, so the tests resolve them the same way the UI does. */
    private fun ThemeDef.displayName(): String = resources.getString(nameRes)

    /**
     * The theme these tests drive through the Viewer.
     *
     * Every theme ships photographs now, so a photograph is what the Viewer
     * shows and the paging assertions below match on the test tag naming the
     * drawable rather than on a label — nothing on that screen is text. The
     * placeholder card a theme without artwork would fall back to is covered
     * at the composable level by `ViewerLayoutTest`, which can pass a null
     * image directly; it is no longer reachable from Home.
     */
    private val cars = THEME_DEFS.first { it.id == "cars" }
    private val carImages by lazy { cars.imageRes }

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
        // By tag, not "the scrollable on screen": Settings has a second
        // scrollable inside the language dropdown, and hasScrollAction()
        // resolves two nodes the moment that menu is open.
        compose.onNodeWithTag(THEME_LIST_TEST_TAG).performScrollToNode(hasText(text))
    }

    private fun openGate() {
        // The gear is icon-only, so it is addressed by its description —
        // which is also what TalkBack reads.
        compose.onNodeWithContentDescription(str(R.string.home_settings_button)).performClick()
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
        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
        THEME_DEFS.forEach { theme ->
            scrollTo(theme.displayName())
            compose.onNodeWithText(theme.displayName()).assertIsDisplayed()
        }
    }

    @Test
    fun tappingAThemeOpensItsViewer() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        compose.onNodeWithTag(viewerImageTestTag(carImages[0])).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.viewer_home)).assertIsDisplayed()
    }

    @Test
    fun backButtonFromTheFirstImageWrapsToTheLast() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        compose.onNodeWithText(str(R.string.viewer_back)).performClick()
        compose.onNodeWithTag(viewerImageTestTag(carImages.last())).assertIsDisplayed()
    }

    @Test
    fun swipingLeftAndRightChangesTheImage() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        compose.onNodeWithTag(viewerImageTestTag(carImages[0]))
            .performTouchInput { swipeLeft() }
        compose.onNodeWithTag(viewerImageTestTag(carImages[1])).assertIsDisplayed()

        compose.onNodeWithTag(viewerImageTestTag(carImages[1]))
            .performTouchInput { swipeRight() }
        compose.onNodeWithTag(viewerImageTestTag(carImages[0])).assertIsDisplayed()
    }

    /**
     * Paging a theme has to reach every photograph and wrap.
     *
     * The count is per theme — they run from Dinosaurs' seven to Animals'
     * twenty-five — and it is `labelCount` the ViewModel wraps on, so this
     * walks the whole set rather than a fixed number of steps.
     *
     * The photographs carry no description, so the test tag naming the
     * drawable is the only thing that says which one is up. What matters here
     * is exactly that: the expected image loaded at the expected step, not
     * what is pictured in it.
     *
     * The theme is resolved by having artwork rather than named, so this keeps
     * passing whichever themes are photographed.
     */
    @Test
    fun nextButtonWalksEveryPhotographAndWrapsAround() {
        val photoTheme = THEME_DEFS.first { it.imageRes.isNotEmpty() }
        scrollTo(photoTheme.displayName())
        compose.onNodeWithText(photoTheme.displayName()).performClick()

        photoTheme.imageRes.forEachIndexed { i, image ->
            compose.onNodeWithTag(viewerImageTestTag(image)).assertIsDisplayed()
            assertEquals("step $i", i, (state() as UiState.Viewer).imageIndex)
            compose.onNodeWithText(str(R.string.viewer_next)).performClick()
        }
        // wrapped back to the first photograph
        compose.onNodeWithTag(viewerImageTestTag(photoTheme.imageRes.first()))
            .assertIsDisplayed()
    }

    @Test
    fun homeButtonReturnsFromTheViewer() {
        compose.onNodeWithText(themeNamed("cars")).performClick()
        compose.onNodeWithText(str(R.string.viewer_home)).performClick()

        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun wrongGateAnswerShowsRetryMessageAndBlocksSettings() {
        openGate()
        compose.onNodeWithText(str(R.string.gate_prompt)).assertIsDisplayed()

        answerWrongOnce()

        compose.onNodeWithText(str(R.string.gate_wrong)).assertIsDisplayed()
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

        // Matched by description, not text: the countdown reticks every
        // second, so its semantics are cleared and replaced with a single
        // frozen sentence — otherwise a live region would make TalkBack
        // read a fresh countdown thirty times over.
        compose.onNodeWithContentDescription("Too many tries", substring = true)
            .assertIsDisplayed()

        // The right answer is now refused too, until the lockout expires.
        compose.onNodeWithText(gate().question.correct.toString()).performClick()
        assertTrue("lockout must outrank a correct answer", state() is UiState.Gate)
    }

    /** A lockout a child can clear by backing out and reopening is no lockout. */
    @Test
    fun theLockoutSurvivesLeavingAndReopeningTheGate() {
        openGate()
        repeat(MAX_GATE_FAILURES) { answerWrongOnce() }

        compose.onNodeWithText(str(R.string.gate_cancel)).performClick()
        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
        openGate()

        // Matched by description, not text: the countdown reticks every
        // second, so its semantics are cleared and replaced with a single
        // frozen sentence — otherwise a live region would make TalkBack
        // read a fresh countdown thirty times over.
        compose.onNodeWithContentDescription("Too many tries", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText(gate().question.correct.toString()).performClick()
        assertTrue(state() is UiState.Gate)
    }

    /**
     * The gear used to sit inside the header, which fades out as soon as the
     * grid leaves the top — so the app's only route into parent settings
     * disappeared the moment a parent scrolled. It is pinned now.
     */
    @Test
    fun theSettingsGearStaysReachableAfterScrollingTheGrid() {
        scrollTo(THEME_DEFS.last().displayName())
        compose.onNodeWithText(str(R.string.home_title)).assertDoesNotExist()

        compose.onNodeWithContentDescription(str(R.string.home_settings_button)).assertIsDisplayed().performClick()

        compose.onNodeWithText(str(R.string.gate_prompt)).assertIsDisplayed()
    }

    @Test
    fun cancellingTheGateReturnsHome() {
        openGate()
        compose.onNodeWithText(str(R.string.gate_cancel)).performClick()

        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun correctGateAnswerOpensParentSettings() {
        enterSettings()

        compose.onNodeWithText(str(R.string.settings_title)).assertIsDisplayed()
        compose.onNodeWithText(resources.getString(R.string.settings_subtitle)).assertIsDisplayed()
    }

    @Test
    fun disablingAThemeInSettingsRemovesItFromHome() {
        enterSettings()

        scrollTo(themeNamed("ocean"))
        compose.onNodeWithText(themeNamed("ocean")).performClick() // untick it
        compose.onNodeWithText(str(R.string.settings_done)).performClick()

        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
        compose.onNodeWithText(themeNamed("ocean")).assertDoesNotExist()
        compose.onNodeWithText(themeNamed("cars")).assertIsDisplayed()
    }

    @Test
    fun reEnablingAThemeBringsItBackToHome() {
        viewModel.toggleThemeEnabled("ocean")
        compose.onNodeWithText(themeNamed("ocean")).assertDoesNotExist()

        enterSettings()
        scrollTo(themeNamed("ocean"))
        compose.onNodeWithText(themeNamed("ocean")).performClick() // tick it again
        compose.onNodeWithText(str(R.string.settings_done)).performClick()

        scrollTo(themeNamed("ocean"))
        compose.onNodeWithText(themeNamed("ocean")).assertIsDisplayed()
    }

}
