package com.kidsexplore.app.ui.images

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.os.Process
import android.os.Trace
import android.util.LruCache
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * How many bytes of decoded photograph to hold.
 *
 * The working set is the three images a child can reach without waiting —
 * previous, current, next — plus one being decoded during a page turn. At the
 * largest shape the app ships (1280×1280, 6.25 MiB as `ARGB_8888`) that is 25
 * MiB; at the common one (1280×853, 4.17 MiB) the same budget holds nearly six.
 *
 * `PhotoNeighboursTest.theCacheCanHoldThreeOfTheLargestPhotographs` pins this,
 * because
 * shrinking it below the working set would turn every page turn back into a
 * decode while leaving every test green.
 */
internal const val PHOTO_CACHE_BYTES = 24 * 1024 * 1024

/**
 * Decoded photographs, kept so a page turn does not have to decode one.
 *
 * Keyed on the drawable id alone. That is sufficient *because* every photograph
 * lives in `drawable-nodpi` with no locale or density variant — pinned by
 * `ThemeResourcesTest.everyPhotographIsDensityIndependent` — so one id means one
 * set of pixels on every device and in every configuration. If a variant is ever
 * introduced, or if decoding ever samples down, the key has to grow a component:
 * serving the wrong-sized entry for an id would pass every test in the suite,
 * because `viewerImageTestTag` is built from the id and not from the pixels.
 *
 * Bounded by bytes rather than entries, since the shapes differ by half again.
 */
class PhotoCache(private val maxBytes: Int) {

    private val entries = object : LruCache<Int, ImageBitmap>(maxBytes) {
        override fun sizeOf(key: Int, value: ImageBitmap): Int =
            value.asAndroidBitmap().allocationByteCount

        // Deliberately no entryRemoved() calling recycle(). An evicted bitmap
        // may still be held by the BitmapPainter on screen, or by a frame in
        // flight in the compositor, and recycling it there is an immediate
        // "trying to use a recycled bitmap" crash. From API 26 the pixels are
        // native and the GC frees them once nothing refers to them, so eviction
        // dropping the reference is the whole of the job.
    }

    fun get(@DrawableRes id: Int): ImageBitmap? = entries.get(id)

    fun contains(@DrawableRes id: Int): Boolean = entries.get(id) != null

    fun evictAll() {
        entries.evictAll()
    }

    /**
     * Drop everything but roughly one photograph.
     *
     * Called when the app stops. Holding 24 MiB while backgrounded makes the app
     * a better candidate for being killed, but evicting outright would cost a
     * decode on the way back from the recents screen — so the current image
     * stays and its neighbours go.
     */
    fun trimToOnePhoto() {
        entries.trimToSize(maxBytes / 3)
    }

    /**
     * The photograph, from the cache if it is there and by decoding if not.
     *
     * The decode is synchronous and on the caller's thread, which is the point:
     * this runs during composition, and the Viewer derives the card's aspect
     * ratio from the bitmap's own dimensions. A cold read therefore costs
     * exactly what the old `painterResource` call cost and lays out identically;
     * a warm one costs a map lookup. Nothing here can be slower than what it
     * replaced, and [prefetch] is what makes it usually warm.
     */
    fun getOrDecode(res: Resources, @DrawableRes id: Int): ImageBitmap =
        entries.get(id) ?: decodePhoto(res, id, "photo.decode.sync").also { entries.put(id, it) }

    /**
     * Decode [ids] that are not cached yet, one at a time, off the caller's thread.
     *
     * Deliberately invisible to Compose's idling machinery: once this suspends
     * into [prefetchDispatcher] it is not awaiting the frame clock, so
     * `waitForIdle` neither waits for it nor is delayed by it. That is what keeps
     * the instrumented tests deterministic — they assert on the image the click
     * composed, and a prefetch landing early or late cannot change what they see.
     * A test that wants to observe a prefetch has to poll for it; see
     * `ViewerPrefetchTest`.
     *
     * One decode at a time, on one thread, with a cancellation check before each.
     * A child mashing Next produces a job per tap, but every one of them except
     * the last is cancelled while still queued — so at most one decode is ever in
     * flight and at most one is ever wasted. Cancelling cannot interrupt a decode
     * already inside `BitmapFactory`, which is exactly why the work is serialised
     * rather than merely cancellable.
     */
    suspend fun prefetch(res: Resources, @DrawableRes ids: List<Int>) {
        withContext(prefetchDispatcher) {
            for (id in ids) {
                ensureActive()
                if (entries.get(id) != null) continue
                // Not put behind ensureActive(): a decode already paid for is
                // worth keeping even if the child has moved on, because moving
                // on is usually moving to it.
                entries.put(id, decodePhoto(res, id, "photo.decode.prefetch"))
            }
        }
    }
}

/** The one cache, for the one Activity this app has. */
object Photos {
    val cache = PhotoCache(PHOTO_CACHE_BYTES)
}

/**
 * The neighbours of [current] in [images], nearest use first.
 *
 * Next before previous because forward is the common direction and the prefetch
 * is serialised, so order is priority. Both, because a swipe goes either way.
 *
 * Wraps exactly as `AppViewModel.stepImage` does, so entering a theme warms the
 * last photograph as well as the second — which is what a child pressing Back
 * from the first image lands on.
 */
internal fun neighboursOf(images: List<Int>, current: Int): List<Int> {
    val index = images.indexOf(current)
    if (index < 0 || images.size < 2) return emptyList()
    val size = images.size
    return listOf(
        images[(index + 1) % size],
        images[(index - 1 + size) % size],
    ).distinct() // a two-image theme has one neighbour, reachable both ways
}

/**
 * Created on first use rather than at class load, so the JVM tests that exercise
 * [neighboursOf] do not spin up a thread they never use.
 *
 * Background priority is not a nicety: at default priority this thread competes
 * with the UI thread for a core, which trades a page-turn stall for a scrolling
 * one. It has hundreds of milliseconds to do tens of milliseconds of work — the
 * gap between a child's taps — so slowing it down costs nothing.
 */
private val prefetchDispatcher by lazy {
    Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "photo-prefetch")
    }.asCoroutineDispatcher()
}

/**
 * Decode one photograph at its full size.
 *
 * `decodeResourceStream` with the resource's own `TypedValue` is what
 * `ImageBitmap.imageResource` calls internally, and it is the reason this does
 * not touch `inDensity`/`inTargetDensity`: for a `nodpi` resource the density is
 * `DENSITY_NONE`, so no scaling happens and the bitmap comes back at the file's
 * own pixel size. Reaching for `BitmapFactory.decodeResource`, or setting those
 * fields by hand, is how density upscaling gets quietly reintroduced —
 * `ThemeResourcesTest.everyPhotographIsDensityIndependent` exists because that
 * matters.
 *
 * [traceLabel] separates the two callers in a trace, which is how you tell
 * whether a page turn actually decoded on the main thread or merely read the
 * cache.
 *
 * `openRawResource` is annotated `@RawRes` and this passes it a `@DrawableRes`,
 * which lint objects to. It is nonetheless correct and is what
 * `ImageBitmap.imageResource` does: the call opens whatever file backs the id,
 * and these ids back JPEGs in `res/drawable-nodpi`. The alternative that lint
 * would accept, `decodeResource`, is the one that reintroduces density scaling.
 */
@Suppress("ResourceType")
private fun decodePhoto(res: Resources, @DrawableRes id: Int, traceLabel: String): ImageBitmap {
    Trace.beginSection(traceLabel)
    try {
        val value = TypedValue()
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = res.openRawResource(id, value).use {
            BitmapFactory.decodeResourceStream(res, value, it, null, options)
        }
        // Every shipped photograph decodes — ThemeResourcesTest.everyPhotographDecodes
        // walks all 217. Failing loudly here rather than returning null keeps the
        // Viewer's "there is always a picture" invariant honest: a null would fall
        // through to the no-aspect-ratio branch and silently drop the image node.
        return checkNotNull(bitmap) { "drawable $id did not decode" }.asImageBitmap()
    } finally {
        Trace.endSection()
    }
}
