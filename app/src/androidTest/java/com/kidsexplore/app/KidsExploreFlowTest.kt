package com.kidsexplore.app

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.R
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
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

    private val res = ApplicationProvider.getApplicationContext<Application>().resources

    // Every expected string is resolved from resources rather than written out
    // in English, so the suite exercises whichever language the app is set to
    // instead of silently only ever covering the default one.
    private fun str(id: Int) = res.getString(id)
    private fun nameOf(theme: com.kidsexplore.app.model.ThemeDef) = res.getString(theme.nameRes)
    private fun themeNamed(id: String) = nameOf(THEME_DEFS.first { it.id == id })
    private fun labelsOf(id: String): Array<String> =
        res.getStringArray(THEME_DEFS.first { it.id == id }.labelsRes)

    private val carLabels = labelsOf("cars")

    /**
     * Home's grid and the Settings list both scroll now that there are 14
     * themes, and a lazy list never composes its off-screen items — so a theme
     * has to be scrolled into view before it can be clicked or asserted on.
     * Targeted by tag rather than "the scrollable on screen", which is
     * ambiguous the moment a screen holds more than one scrollable.
     */
    private fun scrollToTheme(name: String) {
        compose.onNodeWithTag(THEME_LIST_TEST_TAG).performScrollToNode(hasText(name))
    }

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
        // Asserted before scrolling: the header deliberately fades out once the
        // grid leaves the top, and walking all 14 themes scrolls past that point.
        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()

        THEME_DEFS.forEach { theme ->
            scrollToTheme(nameOf(theme))
            compose.onNodeWithText(nameOf(theme)).assertIsDisplayed()
        }
    }

    @Test
    fun tappingAThemeOpensItsViewer() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.viewer_home_button)).assertIsDisplayed()
    }

    @Test
    fun nextButtonWalksEveryImageAndWrapsAround() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        carLabels.forEach { expected ->
            compose.onNodeWithText(expected).assertIsDisplayed()
            compose.onNodeWithText("▶").performClick()
        }
        // wrapped back to the first image
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun backButtonFromTheFirstImageWrapsToTheLast() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        compose.onNodeWithText("◀").performClick()
        compose.onNodeWithText(carLabels.last()).assertIsDisplayed()
    }

    @Test
    fun swipingLeftAndRightChangesTheImage() {
        compose.onNodeWithText(themeNamed("cars")).performClick()

        compose.onNodeWithText(carLabels[0]).performTouchInput { swipeLeft() }
        compose.onNodeWithText(carLabels[1]).assertIsDisplayed()

        compose.onNodeWithText(carLabels[1]).performTouchInput { swipeRight() }
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun homeButtonReturnsFromTheViewer() {
        compose.onNodeWithText(themeNamed("cars")).performClick()
        compose.onNodeWithText(str(R.string.viewer_home_button)).performClick()

        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun wrongGateAnswerShowsRetryMessageAndBlocksSettings() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(str(R.string.gate_title)).assertIsDisplayed()

        val gate = viewModel.gate!!
        val wrong = gate.values.first { it != gate.correct }
        compose.onNodeWithText(wrong.toString()).performClick()

        compose.onNodeWithText(str(R.string.gate_wrong)).assertIsDisplayed()
        assertEquals("must not reach Settings", Screen.GATE, viewModel.screen)
    }

    @Test
    fun cancellingTheGateReturnsHome() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(str(R.string.gate_cancel)).performClick()

        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()
    }

    @Test
    fun correctGateAnswerOpensParentSettings() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()

        compose.onNodeWithText(str(R.string.settings_title)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.settings_subtitle)).assertIsDisplayed()
    }

    @Test
    fun disablingAThemeInSettingsRemovesItFromHome() {
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()

        scrollToTheme(themeNamed("ocean"))
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

        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()
        scrollToTheme(themeNamed("ocean"))
        compose.onNodeWithText(themeNamed("ocean")).performClick() // tick it again
        compose.onNodeWithText(str(R.string.settings_done)).performClick()

        scrollToTheme(themeNamed("ocean"))
        compose.onNodeWithText(themeNamed("ocean")).assertIsDisplayed()
    }

    @Test
    fun fullJourneyHomeToViewerToSettingsAndBack() {
        // browse a theme
        scrollToTheme(themeNamed("dinosaurs"))
        compose.onNodeWithText(themeNamed("dinosaurs")).performClick()
        val dinoLabels = labelsOf("dinosaurs")
        compose.onNodeWithText(dinoLabels[0]).assertIsDisplayed()
        compose.onNodeWithText("▶").performClick()
        compose.onNodeWithText(dinoLabels[1]).assertIsDisplayed()

        // back home
        compose.onNodeWithText(str(R.string.viewer_home_button)).performClick()
        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()

        // through the gate into settings
        compose.onNodeWithText("⚙").performClick()
        compose.onNodeWithText(viewModel.gate!!.correct.toString()).performClick()
        compose.onNodeWithText(str(R.string.settings_title)).assertIsDisplayed()

        // and back home again
        compose.onNodeWithText(str(R.string.settings_done)).performClick()
        compose.onNodeWithText(str(R.string.home_title)).assertIsDisplayed()

        // reopening a theme starts from its first image again
        scrollToTheme(themeNamed("dinosaurs"))
        compose.onNodeWithText(themeNamed("dinosaurs")).performClick()
        compose.onNodeWithText(dinoLabels[0]).assertIsDisplayed()
    }
}
