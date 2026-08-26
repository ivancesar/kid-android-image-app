package com.kidsexplore.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.LayoutDirection as LayoutDirectionOverride
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.then
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.model.ThemeDef
import com.kidsexplore.app.ui.viewerImageTestTag
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Viewer uses the same labelled "Back"/"Next" pill buttons at every size:
 * below the image when the window is narrow, flanking it when it is wide.
 *
 * The layout is chosen from the width actually available, so these give the
 * screen a real width to measure against rather than overriding
 * `LocalConfiguration` — which reported an orientation the window did not
 * actually have, and so proved only that a branch was reachable.
 */
@RunWith(AndroidJUnit4::class)
class ViewerLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources

    // Expected text is resolved from resources, never written out in English:
    // the suite is meant to exercise whichever language the app is set to.
    private fun str(id: Int) = resources.getString(id)

    /**
     * The theme these drive the Viewer with.
     *
     * A photograph is the whole content of the screen, so the test tag naming
     * the drawable is the only handle on it — nothing there is text. Which
     * theme carries it is immaterial; Cars is named rather than resolved so
     * the index used below is stable against a content change elsewhere.
     */
    private val cars = THEME_DEFS.first { it.id == "cars" }

    /** The node the picture is drawn into, for the theme and index on screen. */
    private fun imageNode(theme: ThemeDef = cars, index: Int = 0) =
        compose.onNodeWithTag(viewerImageTestTag(theme.imageRes[index]))

    /** Comfortably below the 600dp breakpoint — a phone held upright. */
    private val narrow = DpSize(400.dp, 800.dp)

    /** Comfortably above it — a phone on its side, or a tablet. */
    private val wide = DpSize(900.dp, 420.dp)

    private fun setViewer(
        size: DpSize,
        direction: LayoutDirection = LayoutDirection.Ltr,
        onNext: () -> Unit = {},
        onPrev: () -> Unit = {},
        onHome: () -> Unit = {},
        theme: ThemeDef = cars,
        index: Int = 0,
    ) {
        compose.setContent {
            KidsExploreTheme {
                // ForcedSize rather than a sized Box: it rescales density so a
                // window wider than the test device still lands on screen, and
                // the buttons stay visible and clickable instead of being laid
                // out past the edge.
                DeviceConfigurationOverride(
                    DeviceConfigurationOverride.ForcedSize(size) then
                        DeviceConfigurationOverride.LayoutDirectionOverride(direction)
                ) {
                    ViewerScreen(
                        theme = theme,
                        onHome = onHome,
                        onNext = onNext,
                        onPrev = onPrev,
                        currentImage = theme.imageRes[index],
                    )
                }
            }
        }
    }

    /**
     * The point of the wide layout: the buttons sit beside the image rather
     * than under it, and must not overlap it.
     */
    @Test
    fun wideWindowPutsTheButtonsBesideTheImageNotUnderIt() {
        setViewer(wide)

        val back = compose.onNodeWithText(str(R.string.viewer_back)).getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText(str(R.string.viewer_next)).getUnclippedBoundsInRoot()
        val image = imageNode().getUnclippedBoundsInRoot()

        assert(back.right <= image.left) { "Back ($back) overlaps the image ($image)" }
        assert(next.left >= image.right) { "Next ($next) overlaps the image ($image)" }
    }

    @Test
    fun narrowWindowPutsTheButtonsBelowTheImage() {
        setViewer(narrow)

        val back = compose.onNodeWithText(str(R.string.viewer_back)).getUnclippedBoundsInRoot()
        val image = imageNode().getUnclippedBoundsInRoot()

        assert(back.top >= image.bottom) { "Back ($back) is not below the image ($image)" }
    }

    @Test
    fun theThemeNameIsNeverShownInTheViewer() {
        setViewer(narrow)

        // the header was deliberately reduced to just the Home button
        compose.onNodeWithText(resources.getString(cars.nameRes)).assertDoesNotExist()
    }

    @Test
    fun navigationCallbacksFireInANarrowWindow() {
        var next = 0
        var prev = 0
        setViewer(narrow, onNext = { next++ }, onPrev = { prev++ })

        compose.onNodeWithText(str(R.string.viewer_next)).performClick()
        compose.onNodeWithText(str(R.string.viewer_back)).performClick()

        compose.runOnIdle {
            assertEquals("next tapped once", 1, next)
            assertEquals("prev tapped once", 1, prev)
        }
    }

    @Test
    fun navigationCallbacksFireInAWideWindow() {
        var next = 0
        var prev = 0
        setViewer(wide, onNext = { next++ }, onPrev = { prev++ })

        compose.onNodeWithText(str(R.string.viewer_next)).performClick()
        compose.onNodeWithText(str(R.string.viewer_back)).performClick()

        compose.runOnIdle {
            assertEquals("next tapped once", 1, next)
            assertEquals("prev tapped once", 1, prev)
        }
    }

    @Test
    fun homeButtonFiresItsCallback() {
        var home = 0
        setViewer(narrow, onHome = { home++ })

        compose.onNodeWithText(str(R.string.viewer_home)).performClick()

        compose.runOnIdle { assertEquals(1, home) }
    }

    // ------------------------------------------------------------------ RTL

    /**
     * The manifest declares supportsRtl, so the Viewer has to mean it. In an
     * RTL locale the sequence advances right-to-left: Back sits at the start
     * edge (the right), and a rightward swipe is "next", not "back".
     */
    @Test
    fun rtlPutsBackOnTheRightAndNextOnTheLeft() {
        setViewer(wide, direction = LayoutDirection.Rtl)

        val back = compose.onNodeWithText(str(R.string.viewer_back)).getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText(str(R.string.viewer_next)).getUnclippedBoundsInRoot()

        assert(back.left >= next.right) { "Back ($back) should sit right of Next ($next) in RTL" }
    }

    @Test
    fun rtlInvertsTheSwipeDirection() {
        var next = 0
        var prev = 0
        setViewer(narrow, direction = LayoutDirection.Rtl, onNext = { next++ }, onPrev = { prev++ })

        imageNode().performTouchInput { swipeRight() }
        compose.runOnIdle {
            assertEquals("swiping right in RTL advances", 1, next)
            assertEquals(0, prev)
        }

        imageNode().performTouchInput { swipeLeft() }
        compose.runOnIdle {
            assertEquals("swiping left in RTL goes back", 1, prev)
            assertEquals(1, next)
        }
    }

    /** The same gesture in LTR must still mean the opposite. */
    @Test
    fun ltrKeepsTheOriginalSwipeDirection() {
        var next = 0
        var prev = 0
        setViewer(narrow, onNext = { next++ }, onPrev = { prev++ })

        imageNode().performTouchInput { swipeLeft() }
        compose.runOnIdle {
            assertEquals("swiping left in LTR advances", 1, next)
            assertEquals(0, prev)
        }
    }

    // -------------------------------------------------------- photographs

    /**
     * The picture is shown and nothing else: no caption drawn over it, and no
     * description behind it. This is an app for looking at pictures, and there
     * is no wording anyone chose for a screen reader to read out — so the node
     * must carry neither text nor a content description rather than carrying a
     * placeholder one.
     */
    @Test
    fun aPhotographIsShownWithNoTextAndNoDescription() {
        setViewer(narrow)

        imageNode()
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ContentDescription))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Text))
    }

    /**
     * The Viewer must draw the photograph it was handed, not merely *a*
     * photograph. Nothing on screen distinguishes them, so an off-by-one in
     * the caller would look identical to a working screen without this.
     */
    @Test
    fun theViewerDrawsThePhotographItWasGiven() {
        val index = 6
        setViewer(narrow, index = index)

        imageNode(index = index).assertIsDisplayed()
        imageNode(index = 0).assertDoesNotExist()
    }

    /**
     * The swipe modifier sits on the Box around the card, so a photograph
     * filling that Box edge to edge must not swallow the gesture.
     */
    @Test
    fun swipingAPhotographStillPages() {
        var next = 0
        setViewer(narrow, onNext = { next++ })

        imageNode().performTouchInput { swipeLeft() }

        compose.runOnIdle { assertEquals(1, next) }
    }
}
