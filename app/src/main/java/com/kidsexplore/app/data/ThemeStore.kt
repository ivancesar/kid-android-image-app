package com.kidsexplore.app.data

import android.content.Context
import androidx.core.content.edit
import com.kidsexplore.app.model.THEME_DEFS

internal const val PREFS_NAME = "kids_explore_prefs"
internal const val PREFS_DISABLED_THEMES_KEY = "disabled_theme_ids"
private const val PREFS_GATE_FAILURES_KEY = "gate_failures"
private const val PREFS_GATE_LOCKED_UNTIL_KEY = "gate_locked_until_wall_ms"

/**
 * The parental gate's failure count and lockout deadline.
 *
 * [lockedUntilWallMs] is wall-clock (`System.currentTimeMillis`), not
 * `SystemClock.elapsedRealtime`: this outlives the process, and an
 * elapsed-realtime deadline is measured from boot, so a reboot would make a
 * stored one either expire instantly or sit days in the future. The ViewModel
 * clamps the deadline on read so a clock change cannot strand a parent.
 */
data class GateLock(val failures: Int = 0, val lockedUntilWallMs: Long = 0L)

/**
 * Everything the app persists: which themes a parent has hidden, and the state
 * of the parental gate.
 *
 * Themes are stored as the *disabled* set rather than a map of every theme's
 * state: it is what actually gets persisted, it makes "a theme nobody has an
 * opinion about is visible" the default without a tri-state, and it keeps the
 * ViewModel testable off-device — [com.kidsexplore.app.AppViewModel] never
 * touches SharedPreferences directly.
 *
 * The gate's counters live here rather than in a `SavedStateHandle` because
 * saved instance state is discarded when the Activity finishes. Back from Home
 * exits the app, so a child could clear a lockout with two taps and a relaunch;
 * a lockout that a relaunch resets is no lockout. `GateLockPersistenceTest`
 * covers exactly that route.
 */
interface ThemeStore {
    fun loadDisabled(): Set<String>
    fun saveDisabled(disabled: Set<String>)
    fun loadGateLock(): GateLock
    fun saveGateLock(lock: GateLock)
}

class SharedPreferencesThemeStore(context: Context) : ThemeStore {

    // Obtaining the instance kicks SharedPreferencesImpl off parsing the
    // backing file on its own worker thread; the first read then blocks until
    // that finishes. AppViewModel reads immediately after constructing this,
    // so the parse is effectively on the main thread either way — the file is
    // a handful of keys, which is the only reason that is acceptable. Anything
    // larger belongs behind DataStore and a coroutine.
    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun loadDisabled(): Set<String> {
        val stored = prefs.getStringSet(PREFS_DISABLED_THEMES_KEY, emptySet()).orEmpty()
        // Drop ids for themes that no longer exist, so entries can't accumulate
        // forever across releases that rename or remove a theme.
        val known = THEME_DEFS.mapTo(mutableSetOf()) { it.id }
        return stored.intersect(known)
    }

    override fun saveDisabled(disabled: Set<String>) {
        prefs.edit { putStringSet(PREFS_DISABLED_THEMES_KEY, disabled) }
    }

    override fun loadGateLock(): GateLock = GateLock(
        failures = prefs.getInt(PREFS_GATE_FAILURES_KEY, 0),
        lockedUntilWallMs = prefs.getLong(PREFS_GATE_LOCKED_UNTIL_KEY, 0L),
    )

    override fun saveGateLock(lock: GateLock) {
        prefs.edit {
            putInt(PREFS_GATE_FAILURES_KEY, lock.failures)
            putLong(PREFS_GATE_LOCKED_UNTIL_KEY, lock.lockedUntilWallMs)
        }
    }
}
