package com.kidsexplore.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.model.ThemeDef
import kotlin.random.Random

enum class Screen { HOME, VIEWER, GATE, SETTINGS }

/** One parental-gate challenge: [a] + [b] = [correct], asked as four [values] to choose from. */
data class GateQuestion(val a: Int, val b: Int, val correct: Int, val values: List<Int>)

// internal, not private, so instrumented tests can clear the very same
// preferences file the ViewModel writes to between runs.
internal const val PREFS_NAME = "kids_explore_prefs"
internal const val PREFS_DISABLED_THEMES_KEY = "disabled_theme_ids"

/**
 * Holds every piece of app state there is: the current screen, the selected
 * theme and position within it, which themes a parent has enabled, and the
 * active gate question.
 *
 * There is no navigation library — [screen] *is* the navigation state, and
 * MainActivity just switches on it. Being an [AndroidViewModel] is what keeps
 * all of this alive across the activity recreation Android performs on
 * rotation (the manifest declares no `configChanges`), so turning the device
 * never resets the viewer or bounces the user back Home.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var screen by mutableStateOf(Screen.HOME)
        private set
    var themeId by mutableStateOf<String?>(null)
        private set
    var imageIndex by mutableStateOf(0)
        private set
    var enabledThemes by mutableStateOf(loadEnabledThemes())
        private set
    var gate by mutableStateOf<GateQuestion?>(null)
        private set
    var gateWrong by mutableStateOf(false)
        private set

    // `!= false` rather than `== true`: a theme with no entry at all counts as
    // enabled. SettingsScreen applies the same convention when drawing its
    // checkboxes, and loadEnabledThemes() below explains why it matters.
    val visibleThemes: List<ThemeDef>
        get() = THEME_DEFS.filter { enabledThemes[it.id] != false }

    val activeTheme: ThemeDef?
        get() = THEME_DEFS.find { it.id == themeId }

    val currentLabel: String?
        get() = activeTheme?.labels?.getOrNull(imageIndex)

    // Only the *disabled* ids are persisted, never the enabled ones. That way a
    // theme added to THEME_DEFS in a later release shows up for existing users
    // instead of being silently hidden because it's missing from their stored
    // set. Note the ids double as persistence keys — see ThemeDef.
    private fun loadEnabledThemes(): Map<String, Boolean> {
        val disabled = prefs.getStringSet(PREFS_DISABLED_THEMES_KEY, emptySet()) ?: emptySet()
        return THEME_DEFS.associate { it.id to (it.id !in disabled) }
    }

    private fun persistEnabledThemes(themes: Map<String, Boolean>) {
        val disabled = themes.filterValues { !it }.keys
        prefs.edit().putStringSet(PREFS_DISABLED_THEMES_KEY, disabled).apply()
    }

    /**
     * Builds a fresh challenge: two operands in 2..7, plus three distractors.
     *
     * Distractors sit within ±3 of the answer so they're plausible rather than
     * obviously wrong, and [used] is seeded with [correct] so no distractor can
     * collide with the answer or with another distractor. The loop always
     * terminates: [correct] is at least 4, so all six non-zero offsets in range
     * yield positive candidates — six to fill three slots.
     */
    private fun buildGateQuestion(): GateQuestion {
        val a = 2 + Random.nextInt(6)
        val b = 2 + Random.nextInt(6)
        val correct = a + b
        val used = mutableSetOf(correct)
        val wrongs = mutableListOf<Int>()
        while (wrongs.size < 3) {
            val w = correct + Random.nextInt(7) - 3
            if (w > 0 && used.add(w)) wrongs.add(w)
        }
        val values = (listOf(correct) + wrongs).shuffled()
        return GateQuestion(a, b, correct, values)
    }

    fun openGate() {
        screen = Screen.GATE
        gate = buildGateQuestion()
        gateWrong = false
    }

    fun goHome() {
        screen = Screen.HOME
        themeId = null
        gate = null
        gateWrong = false
    }

    fun openTheme(id: String) {
        screen = Screen.VIEWER
        themeId = id
        imageIndex = 0
    }

    fun next() {
        val count = activeTheme?.labels?.size ?: return
        imageIndex = (imageIndex + 1) % count
    }

    fun prev() {
        // `+ count` before the modulo because Kotlin's % takes the sign of the
        // dividend: (0 - 1) % 8 is -1, not the 7 we want to wrap around to.
        val count = activeTheme?.labels?.size ?: return
        imageIndex = (imageIndex - 1 + count) % count
    }

    fun toggleThemeEnabled(id: String) {
        val updated = enabledThemes.toMutableMap()
        // Reads inverted, but is the correct negation under the "missing means
        // enabled" convention: only an explicit false flips back to true.
        updated[id] = enabledThemes[id] == false
        enabledThemes = updated
        persistEnabledThemes(updated)
    }

    fun pickGateAnswer(value: Int) {
        val question = gate ?: return
        // A wrong answer keeps the same question on screen rather than
        // generating a new one, so a parent who mis-taps isn't made to start
        // over on a different sum.
        if (value == question.correct) {
            screen = Screen.SETTINGS
            gate = null
            gateWrong = false
        } else {
            gateWrong = true
        }
    }
}
