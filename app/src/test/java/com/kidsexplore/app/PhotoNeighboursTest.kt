package com.kidsexplore.app

import androidx.lifecycle.SavedStateHandle
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.images.PHOTO_CACHE_BYTES
import com.kidsexplore.app.ui.images.neighboursOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Which photographs get decoded ahead of the one on screen.
 *
 * The wrap is the part worth pinning: it has to agree with
 * `AppViewModel.stepImage`, or the prefetch warms an image the Back button will
 * not go to. `AppViewModelTest` covers the same wrap from the state machine's
 * side; this is the cache's side of the same fact.
 */
class PhotoNeighboursTest {

    private val images = listOf(10, 11, 12, 13)

    private fun viewModel() = AppViewModel(FakeThemeStore(), SavedStateHandle())

    @Test
    fun theNextImageComesFirst() {
        // Order is priority: the prefetch decodes one at a time, and forward is
        // where a child goes next far more often than back.
        assertEquals(listOf(12, 10), neighboursOf(images, 11))
    }

    @Test
    fun theFirstImageWarmsTheLastOne() {
        // Back from the first image wraps to the last, so it is a neighbour.
        assertEquals(listOf(11, 13), neighboursOf(images, 10))
    }

    @Test
    fun theLastImageWarmsTheFirstOne() {
        assertEquals(listOf(10, 12), neighboursOf(images, 13))
    }

    @Test
    fun aTwoImageThemeHasOneNeighbourReachableBothWays() {
        assertEquals(listOf(11), neighboursOf(listOf(10, 11), 10))
    }

    @Test
    fun aSingleImageThemeHasNothingToWarm() {
        assertEquals(emptyList<Int>(), neighboursOf(listOf(10), 10))
    }

    @Test
    fun anImageFromAnotherThemeWarmsNothing() {
        // Cannot happen through the app — MainActivity indexes into the theme it
        // hands down — but returning the wrong theme's photographs would be a
        // silent waste of decodes rather than a visible failure, so it is pinned.
        assertEquals(emptyList<Int>(), neighboursOf(images, 99))
    }

    @Test
    fun anEmptyThemeWarmsNothing() {
        assertEquals(emptyList<Int>(), neighboursOf(emptyList(), 10))
    }

    @Test
    fun theViewerReportsThePhotographItIsShowing() {
        // What the cache is told to keep when the app stops.
        val vm = viewModel()
        val cars = THEME_DEFS.first { it.id == "cars" }
        assertEquals(null, vm.currentPhotographOrNull())
        vm.openTheme("cars")
        assertEquals(cars.imageRes[0], vm.currentPhotographOrNull())
        vm.next()
        assertEquals(cars.imageRes[1], vm.currentPhotographOrNull())
        vm.goHome()
        assertEquals("Home has no photograph to keep", null, vm.currentPhotographOrNull())
    }

    /**
     * The budget has to hold the working set the prefetch creates — previous,
     * current and next — at the largest shape the app ships (1280×1280
     * `ARGB_8888`). Below that the cache thrashes on every page turn while every
     * other test stays green.
     */
    @Test
    fun theCacheCanHoldThreeOfTheLargestPhotographs() {
        val worstCasePhotoBytes = 1280 * 1280 * 4
        assertTrue(
            "cache budget is $PHOTO_CACHE_BYTES, needs ${3 * worstCasePhotoBytes}",
            PHOTO_CACHE_BYTES >= 3 * worstCasePhotoBytes,
        )
    }
}
