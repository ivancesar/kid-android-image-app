package com.kidsexplore.app.data

import android.content.Context
import androidx.core.content.edit
import com.kidsexplore.app.model.THEME_DEFS

internal const val PREFS_NAME = "kids_explore_prefs"
internal const val PREFS_DISABLED_THEMES_KEY = "disabled_theme_ids"

/**
 * Which themes a parent has hidden from the Home grid.
 *
 * Stored as the *disabled* set rather than a map of every theme's state: it is
 * what actually gets persisted, it makes "a theme nobody has an opinion about
 * is visible" the default without a tri-state, and it keeps the ViewModel
 * testable off-device — [com.kidsexplore.app.AppViewModel] never touches
 * SharedPreferences directly.
 */
interface ThemeStore {
    fun loadDisabled(): Set<String>
    fun saveDisabled(disabled: Set<String>)
}

class SharedPreferencesThemeStore(context: Context) : ThemeStore {

    // SharedPreferencesImpl parses the backing file on its own worker thread
    // and only blocks on the first read, so obtaining the instance here is
    // cheap. Constructing the store early (from the ViewModel factory, before
    // the first composition) is what keeps that parse off the critical path.
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
}
