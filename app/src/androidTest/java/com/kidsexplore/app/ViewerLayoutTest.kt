package com.kidsexplore.app

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.R
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Viewer uses the same labelled "Back"/"Next" pill buttons in both
 * orientations: below the image in portrait, flanking the image (not
 * overlapping it) in landscape.
 *
 * Rather than rotating the device (which the test host activity does not
 * reliably follow), these drive [LocalConfiguration] directly so both
 * branches are exercised deterministically.
 */
@RunWith(AndroidJUnit4::class)
class ViewerLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private val cars = THEME_DEFS.first { it.id == "cars" }
    private val res by lazy {
        androidx.test.core.app.ApplicationProvider
            .getApplicationContext<android.content.Context>().resources
    }
    private val carLabels: Array<String> by lazy { res.getStringArray(cars.labelsRes) }
    private fun str(id: Int) = res.getString(id)
    private val carsName: String by lazy { res.getString(cars.nameRes) }

    private fun setViewer(
        orientation: Int,
        onNext: () -> Unit = {},
        onPrev: () -> Unit = {},
        onHome: () -> Unit = {},
    ) {
        compose.setContent {
            val config = Configuration(LocalConfiguration.current).apply {
                this.orientation = orientation
            }
            CompositionLocalProvider(LocalConfiguration provides config) {
                KidsExploreTheme {
                    ViewerScreen(
                        theme = cars,
                        currentLabel = carLabels[0],
                        onHome = onHome,
                        onNext = onNext,
                        onPrev = onPrev,
                    )
                }
            }
        }
    }

    @Test
    fun portraitUsesLabelledPillButtons() {
        setViewer(Configuration.ORIENTATION_PORTRAIT)

        compose.onNodeWithText(str(R.string.viewer_back)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.viewer_next)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.viewer_home_button)).assertIsDisplayed()
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun landscapeUsesLabelledPillButtonsToo() {
        setViewer(Configuration.ORIENTATION_LANDSCAPE)

        compose.onNodeWithText(str(R.string.viewer_back)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.viewer_next)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.viewer_home_button)).assertIsDisplayed()
        compose.onNodeWithText(carLabels[0]).assertIsDisplayed()
    }

    @Test
    fun theThemeNameIsNeverShownInTheViewer() {
        setViewer(Configuration.ORIENTATION_PORTRAIT)

        // the header was deliberately reduced to just the Home button
        compose.onNodeWithText(carsName).assertDoesNotExist()
    }

    @Test
    fun navigationCallbacksFireInPortrait() {
        var next = 0
        var prev = 0
        setViewer(Configuration.ORIENTATION_PORTRAIT, onNext = { next++ }, onPrev = { prev++ })

        compose.onNodeWithText(str(R.string.viewer_next)).performClick()
        compose.onNodeWithText(str(R.string.viewer_back)).performClick()

        compose.runOnIdle {
            assertEquals("next tapped once", 1, next)
            assertEquals("prev tapped once", 1, prev)
        }
    }

    @Test
    fun navigationCallbacksFireInLandscape() {
        var next = 0
        var prev = 0
        setViewer(Configuration.ORIENTATION_LANDSCAPE, onNext = { next++ }, onPrev = { prev++ })

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
        setViewer(Configuration.ORIENTATION_PORTRAIT, onHome = { home++ })

        compose.onNodeWithText(str(R.string.viewer_home_button)).performClick()

        compose.runOnIdle { assertEquals(1, home) }
    }
}
