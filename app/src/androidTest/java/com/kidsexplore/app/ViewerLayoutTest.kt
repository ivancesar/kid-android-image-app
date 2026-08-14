package com.kidsexplore.app

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.model.THEME_DEFS
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

    private val cars = THEME_DEFS.first { it.id == "cars" }

    /** Comfortably below the 600dp breakpoint — a phone held upright. */
    private val narrow = DpSize(400.dp, 800.dp)

    /** Comfortably above it — a phone on its side, or a tablet. */
    private val wide = DpSize(900.dp, 420.dp)

    private fun setViewer(
        size: DpSize,
        onNext: () -> Unit = {},
        onPrev: () -> Unit = {},
        onHome: () -> Unit = {},
    ) {
        compose.setContent {
            KidsExploreTheme {
                // ForcedSize rather than a sized Box: it rescales density so a
                // window wider than the test device still lands on screen, and
                // the buttons stay visible and clickable instead of being laid
                // out past the edge.
                DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(size)) {
                    ViewerScreen(
                        theme = cars,
                        currentLabel = cars.labels[0],
                        onHome = onHome,
                        onNext = onNext,
                        onPrev = onPrev,
                    )
                }
            }
        }
    }

    @Test
    fun narrowWindowUsesLabelledPillButtons() {
        setViewer(narrow)

        compose.onNodeWithText("Back").assertIsDisplayed()
        compose.onNodeWithText("Next").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText(cars.labels[0]).assertIsDisplayed()
    }

    @Test
    fun wideWindowUsesLabelledPillButtonsToo() {
        setViewer(wide)

        compose.onNodeWithText("Back").assertIsDisplayed()
        compose.onNodeWithText("Next").assertIsDisplayed()
        compose.onNodeWithText("Home").assertIsDisplayed()
        compose.onNodeWithText(cars.labels[0]).assertIsDisplayed()
    }

    /**
     * The point of the wide layout: the buttons sit beside the image rather
     * than under it, and must not overlap it.
     */
    @Test
    fun wideWindowPutsTheButtonsBesideTheImageNotUnderIt() {
        setViewer(wide)

        val back = compose.onNodeWithText("Back").getUnclippedBoundsInRoot()
        val next = compose.onNodeWithText("Next").getUnclippedBoundsInRoot()
        val image = compose.onNodeWithText(cars.labels[0]).getUnclippedBoundsInRoot()

        assert(back.right <= image.left) { "Back ($back) overlaps the image ($image)" }
        assert(next.left >= image.right) { "Next ($next) overlaps the image ($image)" }
    }

    @Test
    fun narrowWindowPutsTheButtonsBelowTheImage() {
        setViewer(narrow)

        val back = compose.onNodeWithText("Back").getUnclippedBoundsInRoot()
        val image = compose.onNodeWithText(cars.labels[0]).getUnclippedBoundsInRoot()

        assert(back.top >= image.bottom) { "Back ($back) is not below the image ($image)" }
    }

    @Test
    fun theThemeNameIsNeverShownInTheViewer() {
        setViewer(narrow)

        // the header was deliberately reduced to just the Home button
        compose.onNodeWithText(cars.name).assertDoesNotExist()
    }

    @Test
    fun navigationCallbacksFireInANarrowWindow() {
        var next = 0
        var prev = 0
        setViewer(narrow, onNext = { next++ }, onPrev = { prev++ })

        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Back").performClick()

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

        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Back").performClick()

        compose.runOnIdle {
            assertEquals("next tapped once", 1, next)
            assertEquals("prev tapped once", 1, prev)
        }
    }

    @Test
    fun homeButtonFiresItsCallback() {
        var home = 0
        setViewer(narrow, onHome = { home++ })

        compose.onNodeWithText("Home").performClick()

        compose.runOnIdle { assertEquals(1, home) }
    }
}
