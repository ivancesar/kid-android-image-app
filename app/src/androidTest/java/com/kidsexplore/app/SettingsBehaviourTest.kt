package com.kidsexplore.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.THEME_LIST_TEST_TAG
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The language picker and the empty Home state, neither of which had coverage.
 *
 * These render the screens directly with fake callbacks rather than walking in
 * through the gate: the behaviours worth pinning are local to the screen, and
 * selecting a language for real recreates the activity, which fights the
 * Compose test rule.
 */
@RunWith(AndroidJUnit4::class)
class SettingsBehaviourTest {

    @get:Rule
    val compose = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private fun str(id: Int) = resources.getString(id)

    private fun settings(
        current: String = AppLocales.SYSTEM,
        onPick: (String) -> Unit = {},
        size: DpSize? = null,
    ) {
        compose.setContent {
            KidsExploreTheme {
                val screen = @Composable {
                    SettingsScreen(
                        disabledThemeIds = emptySet(),
                        onToggle = {},
                        onDone = {},
                        currentLanguage = current,
                        onPickLanguage = onPick,
                        onOpenPolicy = {},
                    )
                }
                // ForcedSize rather than the device's own: anything asserting
                // on the credit list's clamp depends on the text actually
                // overflowing three lines, which a tablet or a small font
                // scale would not. Same reason `ViewerLayoutTest` forces a
                // size rather than trusting the test device.
                if (size == null) {
                    screen()
                } else {
                    DeviceConfigurationOverride(
                        DeviceConfigurationOverride.ForcedSize(size),
                    ) { screen() }
                }
            }
        }
    }

    /** A phone held upright — narrow enough that 183 names cannot fit in three lines. */
    private val narrow = DpSize(400.dp, 800.dp)

    @Test
    fun theRowShowsTheSystemChoiceByItsLabelNotAsABlank() {
        // AppLocales.SYSTEM is the empty string; without languageLabel() the row
        // would render nothing at all.
        settings(current = AppLocales.SYSTEM)
        compose.onNodeWithText(str(R.string.settings_language_system)).assertIsDisplayed()
    }

    @Test
    fun theRowShowsAChosenLanguageInItsOwnLanguage() {
        settings(current = "hr")
        compose.onNodeWithText("Hrvatski").assertIsDisplayed()
    }

    @Test
    fun pickingADifferentLanguageReportsIt() {
        var picked: String? = null
        settings(current = AppLocales.SYSTEM, onPick = { picked = it })

        compose.onNodeWithText(str(R.string.settings_language_system)).performClick()
        // Only the menu carries "Hrvatski" here — the row shows the system label.
        compose.onNodeWithText("Hrvatski").performClick()

        assertEquals("hr", picked)
    }

    @Test
    fun pickingTheLanguageAlreadyInUseDoesNothing() {
        // Re-applying would recreate the activity for no reason; the guard is
        // `if (tag != current)`.
        var picked: String? = null
        settings(current = "hr", onPick = { picked = it })

        // The row and the menu entry both read "Hrvatski", so the first click
        // only opens the menu; clicking the row again would re-open it and
        // never reach SettingsScreen's `if (tag != current)` guard. Selected by
        // being inside the popup rather than by index — traversal order between
        // the composition and the popup is not guaranteed, and an index that
        // flipped would quietly make this test vacuous again.
        compose.onNodeWithText("Hrvatski").performClick()
        compose.onAllNodesWithText("Hrvatski")
            .filterToOne(hasAnyAncestor(isPopup()))
            .performClick()

        assertNull("re-picking the current language must not re-apply it", picked)
    }

    @Test
    fun anEmptyHomeStillOffersAWayIntoSettings() {
        // A parent who switches every category off must not be stranded: the
        // gear is the only route back in, and it is drawn over an empty grid.
        var opened = false
        compose.setContent {
            KidsExploreTheme {
                HomeScreen(themes = emptyList(), onOpenTheme = {}, onOpenGate = { opened = true })
            }
        }
        compose.onNodeWithText(str(R.string.home_empty_title)).assertIsDisplayed()
        compose.onNodeWithContentDescription(str(R.string.home_settings_button))
            .assertIsDisplayed()
            .performClick()
        assertEquals(true, opened)
    }

    /**
     * The app bundles Unsplash photography, so it carries the notice for it.
     * It sits at the foot of a lazy list, past fourteen category rows, so it
     * has to be scrolled to — asserting on it without scrolling would pass on
     * a tall enough test device and fail on a phone.
     */
    @Test
    fun settingsCarriesTheImageAttributionNotice() {
        settings()

        val notice = str(R.string.attribution_images)
        compose.onNodeWithTag(THEME_LIST_TEST_TAG).performScrollToNode(hasText(notice))

        compose.onNodeWithText(str(R.string.settings_attribution)).assertIsDisplayed()
        compose.onNodeWithText(notice).assertIsDisplayed()
    }

    /**
     * The credit list is clamped to three lines, and the control opens and
     * closes it.
     *
     * Asserting on the control rather than on the text: "Show more" being
     * present *is* the assertion that something was clamped, since the
     * composable only emits it when the layout actually overflowed, and it
     * flipping to "Show less" and back is the toggle. What the collapsed node
     * *announces* is a separate question, pinned by
     * `AccessibleNamesTest.theCollapsedCreditListAnnouncesASummaryRatherThan`
     * `EveryName`.
     *
     * Each state is scrolled to before it is asserted on. Expanding pushes the
     * control far below the fold — 183 names is several screens — so it is
     * present but off-screen until the list is moved, which is the difference
     * `assertIsDisplayed` catches and `assertExists` would not.
     */
    @Test
    fun theCreditListCollapsesAndExpands() {
        settings(size = narrow)

        val more = str(R.string.attribution_show_more)
        val less = str(R.string.attribution_show_less)
        val list = compose.onNodeWithTag(THEME_LIST_TEST_TAG)

        list.performScrollToNode(hasText(more))
        compose.onNodeWithText(more).assertIsDisplayed()
        compose.onNodeWithText(less).assertDoesNotExist()

        compose.onNodeWithText(more).performClick()
        list.performScrollToNode(hasText(less))
        compose.onNodeWithText(less).assertIsDisplayed()
        compose.onNodeWithText(more).assertDoesNotExist()

        compose.onNodeWithText(less).performClick()
        list.performScrollToNode(hasText(more))
        compose.onNodeWithText(more).assertIsDisplayed()
        compose.onNodeWithText(less).assertDoesNotExist()
    }

    /**
     * Expanding, scrolling the credits out of view and coming back must leave
     * the control there.
     *
     * The attribution is the last item in a `LazyColumn`, so scrolling past it
     * disposes it. `expanded` survives that in `rememberSaveable`, and if the
     * flag saying the text overflowed does not survive with it, the restored
     * state is "expanded, nothing overflowed" — the text lays out unclamped,
     * the `!expanded` guard on `onTextLayout` skips the re-measure, and the
     * control is never emitted again. The credits are then stuck open with no
     * way to close them short of leaving Settings entirely.
     *
     * That is not hypothetical: it shipped for one commit, from moving
     * `overflows` to a plain `remember` on the reasoning that a measurement
     * should not be persisted.
     */
    @Test
    fun theCreditListKeepsItsControlAfterScrollingAwayWhileExpanded() {
        settings(size = narrow)

        val more = str(R.string.attribution_show_more)
        val less = str(R.string.attribution_show_less)
        val list = compose.onNodeWithTag(THEME_LIST_TEST_TAG)

        list.performScrollToNode(hasText(more))
        compose.onNodeWithText(more).performClick()

        // Away far enough to dispose the item, then back.
        list.performScrollToNode(hasText(str(R.string.settings_categories)))
        list.performScrollToNode(hasText(less))

        compose.onNodeWithText(less).assertIsDisplayed()
        compose.onNodeWithText(less).performClick()
        list.performScrollToNode(hasText(more))
        compose.onNodeWithText(more).assertIsDisplayed()
    }

    @Test
    fun aPopulatedHomeShowsNoEmptyMessage() {
        compose.setContent {
            KidsExploreTheme {
                HomeScreen(themes = THEME_DEFS, onOpenTheme = {}, onOpenGate = {})
            }
        }
        compose.onNodeWithText(str(R.string.home_empty_title)).assertDoesNotExist()
    }
}
