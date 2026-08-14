package com.kidsexplore.app

import com.kidsexplore.app.data.GateLock
import com.kidsexplore.app.data.ThemeStore

/**
 * In-memory [ThemeStore] standing in for SharedPreferences.
 *
 * Shared by the JVM tests because it is also how they model a *restart*: hand a
 * second [AppViewModel] the same store and a fresh `SavedStateHandle`, and you
 * have exactly what the app does when the Activity finishes and relaunches.
 * `SharedPreferencesThemeStore` itself is covered on-device by
 * `SharedPreferencesThemeStoreTest`.
 */
class FakeThemeStore(
    initialDisabled: Set<String> = emptySet(),
    initialLock: GateLock = GateLock(),
) : ThemeStore {

    var savedDisabled: Set<String> = initialDisabled
        private set

    var savedLock: GateLock = initialLock
        private set

    override fun loadDisabled(): Set<String> = savedDisabled

    override fun saveDisabled(disabled: Set<String>) {
        savedDisabled = disabled
    }

    override fun loadGateLock(): GateLock = savedLock

    override fun saveGateLock(lock: GateLock) {
        savedLock = lock
    }
}
