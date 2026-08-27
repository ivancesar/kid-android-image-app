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
 * largest shape the app ships (1280×1280, 6.25 MiB as `ARGB_8888`) three of them
 * are 18.75 MiB; at the common one (1280×853, 4.17 MiB) this budget holds five.
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

    /**
     * Whether [id] is cached, **without** counting as a use of it.
     *
     * `LruCache` is built on an access-ordered `LinkedHashMap`, so `get()`
     * promotes what it returns. Probing with `get()` would therefore make the
     * neighbours look more recently used than the photograph actually on
     * screen — and [trimKeeping] would then evict the one image worth keeping.
     */
    fun contains(@DrawableRes id: Int): Boolean = entries.snapshot().containsKey(id)

    fun evictAll() {
        entries.evictAll()
    }

    /**
     * Drop everything but roughly one photograph, keeping [keep] if it is given.
     *
     * Called when the app stops: holding 24 MiB while backgrounded only makes
     * the process a better candidate for being killed, but evicting outright
     * would cost a decode on the way back from the recents screen.
     *
     * [keep] is named rather than inferred from recency, because recency does
     * not say what a reader expects here. A freshly decoded neighbour is
     * inserted as the most recently used entry, so after a page turn the
     * *current* photograph is the least recently used of the three — trimming
     * on recency alone would evict precisely the one that has to survive.
     */
    fun trimKeeping(@DrawableRes keep: Int?) {
        if (keep != null) entries.get(keep) // promote it out of harm's way
        entries.trimToSize(maxBytes / 3)
    }

    /**
     * The photograph, from the cache if it is there and by decoding if not.
     *
     * The decode is synchronous and on the caller's thread, which is the point:
     * this runs during composition, and the Viewer derives the card's aspect
     * ratio from the bitmap's own dimensions. A cold read costs a decode and lays
     * out identically to the `painterResource` call it replaced; a warm one
     * costs a map lookup. What changes is *where* the work happens rather than
     * how much of it there is — [prefetch] speculatively decodes neighbours, so
     * a session does strictly more decoding in total, on a thread where it does
     * not cost a frame.
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
     * the last is cancelled while still queued — so this thread never has more
     * than one decode in flight and never wastes more than one. Cancelling
     * cannot interrupt a decode already inside `BitmapFactory`, which is exactly
     * why the work is serialised rather than merely cancellable.
     *
     * The main thread can still decode the same photograph concurrently, when a
     * page turn arrives before the prefetch of that image has landed. Left as
     * it is: it happens only in the case that was going to decode on the main
     * thread anyway, one of the two results simply wins the map, and the
     * alternative is a per-key monitor whose deadlock surface is not worth
     * buying a duplicate decode back in a case that is already the slow one.
     */
    suspend fun prefetch(res: Resources, ids: List<Int>) {
        withContext(prefetchDispatcher) {
            for (id in ids) {
                ensureActive()
                // snapshot(), not get(): see contains(). A prefetch checking
                // whether it can skip an image must not thereby mark that image
                // as more recently used than the one being looked at.
                if (entries.snapshot().containsKey(id)) continue
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
 * `decodeResourceStream` is given the resource's own `TypedValue` and nothing
 * else: for a `nodpi` resource the density comes back as `DENSITY_NONE`, so no
 * scaling happens and the bitmap is the file's own pixel size. Reaching for
 * `BitmapFactory.decodeResource`, or setting `inDensity`/`inTargetDensity` by
 * hand, is how density upscaling gets quietly reintroduced —
 * `ThemeResourcesTest.everyPhotographIsDensityIndependent` exists because that
 * matters, and `PhotoCacheTest.aDecodedPhotographKeepsItsOwnPixelSize` pins it
 * for this path.
 *
 * This is not the call `painterResource` made. That went through
 * `Resources.getDrawable` and took the bitmap off the resulting
 * `BitmapDrawable`; the pixels are equivalent, but the decoder is not the same
 * one, so per-image timings are not directly comparable between the two.
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
