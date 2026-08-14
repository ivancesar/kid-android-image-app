package com.kidsexplore.app

import com.kidsexplore.app.data.GateLock
import com.kidsexplore.app.data.ThemeStore

/**
 * In-memory [ThemeStore] for the UI tests, so they never touch the device's
 * real preferences file and cannot leak state between test methods.
 * `SharedPreferencesThemeStoreTest` covers the real implementation.
 */
class FakeThemeStore : ThemeStore {
    private var disabled: Set<String> = emptySet()
    private var lock: GateLock = GateLock()

    override fun loadDisabled(): Set<String> = disabled

    override fun saveDisabled(disabled: Set<String>) {
        this.disabled = disabled
    }

    override fun loadGateLock(): GateLock = lock

    override fun saveGateLock(lock: GateLock) {
        this.lock = lock
    }
}
