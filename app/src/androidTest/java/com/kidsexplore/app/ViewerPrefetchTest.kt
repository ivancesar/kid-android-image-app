package com.kidsexplore.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.images.Photos
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * That the Viewer actually warms its neighbours.
 *
 * `PhotoCacheTest` proves the cache works and `PhotoNeighboursTest` proves the
 * right ids are chosen; this is the only test that proves the two are wired
 * together, which is the difference between the feature working and the feature
 * being present.
 */
@RunWith(AndroidJUnit4::class)
class ViewerPrefetchTest {

    @get:Rule
    val compose = createComposeRule()

    private val cars = THEME_DEFS.first { it.id == "cars" }

    @After
    fun leaveNothingBehind() {
        // A process singleton holding 24 MiB of photographs across the other
        // eighty tests is a poor neighbour.
        Photos.cache.evictAll()
    }

    @Before
    fun startCold() {
        // The cache is a process singleton, so without this the test passes or
        // fails depending on what ran before it.
        Photos.cache.evictAll()
    }

    /**
     * `waitUntil`, deliberately, and not `waitForIdle`.
     *
     * The prefetch runs on its own thread and never awaits the frame clock, so
     * Compose's idling machinery does not know about it — that is a design
     * property, not an oversight: it is what stops an in-flight decode from
     * changing what the other instrumented tests observe. The cost is that
     * observing a prefetch means polling for it. Anyone who "fixes" this to
     * `waitForIdle` will get a pass on a fast device and a flake on a slow one.
     */
    private fun waitForCache(image: Int) {
        compose.waitUntil(timeoutMillis = 5_000) { Photos.cache.contains(image) }
    }

    @Test
    fun openingAPhotographWarmsTheOneAfterIt() {
        compose.setContent {
            KidsExploreTheme {
                ViewerScreen(
                    theme = cars,
                    onHome = {},
                    onNext = {},
                    onPrev = {},
                    currentImage = cars.imageRes[0],
                )
            }
        }
        waitForCache(cars.imageRes[1])
    }

    @Test
    fun openingTheFirstPhotographAlsoWarmsTheLastOne() {
        // Back from the first image wraps to the last, so it is one tap away and
        // deserves the same treatment as the second.
        compose.setContent {
            KidsExploreTheme {
                ViewerScreen(
                    theme = cars,
                    onHome = {},
                    onNext = {},
                    onPrev = {},
                    currentImage = cars.imageRes[0],
                )
            }
        }
        waitForCache(cars.imageRes.last())
    }

    @Test
    fun thePhotographOnScreenIsCachedByTheTimeItIsDrawn() {
        // The synchronous path populates the cache too, so leaving a theme and
        // coming back is free. This one needs no waiting at all, which is the
        // point of it.
        compose.setContent {
            KidsExploreTheme {
                ViewerScreen(
                    theme = cars,
                    onHome = {},
                    onNext = {},
                    onPrev = {},
                    currentImage = cars.imageRes[3],
                )
            }
        }
        compose.waitForIdle()
        assertTrue(
            "the displayed photograph was not put in the cache",
            Photos.cache.contains(cars.imageRes[3]),
        )
    }
}
