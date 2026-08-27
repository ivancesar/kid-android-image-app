package com.kidsexplore.app

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.images.PhotoCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The cache itself, against real resources.
 *
 * On a device rather than the JVM because `android.util.LruCache` and
 * `BitmapFactory` are both stubs there, and because the thing worth proving is
 * that a real photograph round-trips at its real size.
 */
@RunWith(AndroidJUnit4::class)
class PhotoCacheTest {

    private val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
    private val cars = THEME_DEFS.first { it.id == "cars" }.imageRes

    /** Big enough for the working set, as the shipped budget is. */
    private fun cache() = PhotoCache(24 * 1024 * 1024)

    @Test
    fun aSecondReadReturnsTheSameBitmapRatherThanDecodingAgain() {
        // The entire point of the cache. Equality would pass on two separate
        // decodes of the same file, so this has to be identity.
        val cache = cache()
        val first = cache.getOrDecode(resources, cars[0])
        val second = cache.getOrDecode(resources, cars[0])
        assertSame("second read decoded again instead of using the cache", first, second)
    }

    @Test
    fun aDecodedPhotographKeepsItsOwnPixelSize() {
        // Guards the density trap: decodeResource, or setting inDensity by hand,
        // would scale a nodpi drawable up to the device's density and nothing
        // else in the suite would notice — the Viewer's test tag is built from
        // the drawable id, not from the pixels.
        val cache = cache()
        val decoded = cache.getOrDecode(resources, cars[0]).asAndroidBitmap()
        val options = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resources.openRawResource(cars[0]).use {
            android.graphics.BitmapFactory.decodeStream(it, null, options)
        }
        assertEquals("decoded width does not match the file", options.outWidth, decoded.width)
        assertEquals("decoded height does not match the file", options.outHeight, decoded.height)
    }

    @Test
    fun theCacheEvictsByBytesAndKeepsTheNewest() {
        // Sized to hold roughly one photograph, so putting three must drop the
        // oldest. Bytes rather than entries is what makes this predictable when
        // a 1280x1280 costs half again what a 1280x853 does.
        val small = PhotoCache(5 * 1024 * 1024)
        small.getOrDecode(resources, cars[0])
        small.getOrDecode(resources, cars[1])
        small.getOrDecode(resources, cars[2])
        assertNull("the oldest entry survived a budget it should have blown", small.get(cars[0]))
        assertNotNull("the newest entry was evicted", small.get(cars[2]))
    }

    @Test
    fun anEvictedBitmapIsNotRecycled() {
        // An evicted bitmap can still be on screen inside a BitmapPainter, or in
        // a frame the compositor has not finished with. Recycling it there is an
        // immediate crash, so eviction must only drop the reference.
        val small = PhotoCache(5 * 1024 * 1024)
        val first = small.getOrDecode(resources, cars[0])
        small.getOrDecode(resources, cars[1])
        small.getOrDecode(resources, cars[2])
        assertNull(small.get(cars[0]))
        assertFalse("an evicted bitmap was recycled", first.asAndroidBitmap().isRecycled)
    }

    @Test
    fun prefetchFillsTheCacheWithoutBeingAskedForThePhotographTwice() {
        val cache = cache()
        runBlocking { cache.prefetch(resources, listOf(cars[1], cars[2])) }
        assertTrue(cache.contains(cars[1]))
        assertTrue(cache.contains(cars[2]))

        // And a subsequent read is a hit, i.e. the prefetch and the synchronous
        // path agree about the key.
        val prefetched = cache.get(cars[1])
        assertSame(prefetched, cache.getOrDecode(resources, cars[1]))
    }

    @Test
    fun trimmingKeepsAPhotographRatherThanEmptyingTheCache() {
        // What happens when the app stops: the current image should survive so a
        // return from the recents screen is not a decode.
        val cache = cache()
        cache.getOrDecode(resources, cars[0])
        cache.trimToOnePhoto()
        assertTrue("trimming emptied the cache instead of trimming it", cache.contains(cars[0]))
    }

    @Test
    fun evictingEmptiesTheCache() {
        val cache = cache()
        cache.getOrDecode(resources, cars[0])
        cache.evictAll()
        assertFalse(cache.contains(cars[0]))
    }
}
