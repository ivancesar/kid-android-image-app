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

data class GateQuestion(val a: Int, val b: Int, val correct: Int, val values: List<Int>)

internal const val PREFS_NAME = "kids_explore_prefs"
internal const val PREFS_DISABLED_THEMES_KEY = "disabled_theme_ids"

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

    val visibleThemes: List<ThemeDef>
        get() = THEME_DEFS.filter { enabledThemes[it.id] != false }

    val activeTheme: ThemeDef?
        get() = THEME_DEFS.find { it.id == themeId }

    /**
     * How many images the active theme has. Read from the label array rather
     * than mirrored on [ThemeDef], so the two can never drift; the count is the
     * same in every language, so which locale resolves here does not matter.
     */
    private val activeLabelCount: Int?
        get() = activeTheme?.let { getApplication<Application>().resources.getStringArray(it.labelsRes).size }

    private fun loadEnabledThemes(): Map<String, Boolean> {
        val disabled = prefs.getStringSet(PREFS_DISABLED_THEMES_KEY, emptySet()) ?: emptySet()
        return THEME_DEFS.associate { it.id to (it.id !in disabled) }
    }

    private fun persistEnabledThemes(themes: Map<String, Boolean>) {
        val disabled = themes.filterValues { !it }.keys
        prefs.edit().putStringSet(PREFS_DISABLED_THEMES_KEY, disabled).apply()
    }

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
        val count = activeLabelCount ?: return
        imageIndex = (imageIndex + 1) % count
    }

    fun prev() {
        val count = activeLabelCount ?: return
        imageIndex = (imageIndex - 1 + count) % count
    }

    fun toggleThemeEnabled(id: String) {
        val updated = enabledThemes.toMutableMap()
        updated[id] = enabledThemes[id] == false
        enabledThemes = updated
        persistEnabledThemes(updated)
    }

    fun pickGateAnswer(value: Int) {
        val question = gate ?: return
        if (value == question.correct) {
            screen = Screen.SETTINGS
            gate = null
            gateWrong = false
        } else {
            gateWrong = true
        }
    }
}
