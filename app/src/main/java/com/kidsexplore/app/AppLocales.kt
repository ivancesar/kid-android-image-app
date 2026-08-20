package com.kidsexplore.app

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * The app's language selection.
 *
 * Delegates to [AppCompatDelegate], which persists the choice itself, survives
 * process death, and on Android 13+ registers it with the system's own per-app
 * language screen — so there is no preference of our own to keep in sync.
 */
object AppLocales {

    /** Sentinel for "no override" — follow whatever the phone is set to. */
    const val SYSTEM = ""

    /** Must match `res/xml/locales_config.xml`; `LocalesConfigTest` enforces it. */
    val SUPPORTED = listOf("en", "hr")

    /** The selected tag, or [SYSTEM] when the user hasn't overridden the phone. */
    fun current(): String =
        AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }?.get(0)?.language ?: SYSTEM

    fun apply(tag: String) {
        AppCompatDelegate.setApplicationLocales(
            if (tag == SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(tag)
        )
    }

    /**
     * A language's name in its own language ("Hrvatski", not "Croatian") — the
     * only form that's readable to the person who needs to pick it.
     */
    fun endonym(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase(locale) }
    }
}
