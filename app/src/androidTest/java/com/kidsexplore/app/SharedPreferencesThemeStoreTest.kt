package com.kidsexplore.app

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.kidsexplore.app.data.GateLock
import com.kidsexplore.app.data.PREFS_DISABLED_THEMES_KEY
import com.kidsexplore.app.data.PREFS_NAME
import com.kidsexplore.app.data.SharedPreferencesThemeStore
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The real store, against real SharedPreferences.
 *
 * Everything else fakes this seam, so without these the only logic in the
 * class — pruning ids for themes that no longer exist, and the gate lock that
 * the whole parental gate now leans on — ran nowhere but production.
 *
 * A second store instance is used to read back wherever the point is
 * durability, since reading through the same object could be satisfied by an
 * in-memory field that never reached disk.
 */
@RunWith(AndroidJUnit4::class)
class SharedPreferencesThemeStoreTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun newStore() = SharedPreferencesThemeStore(context)

    @Before
    @After
    fun clearPrefs() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // ------------------------------------------------------------- themes

    @Test
    fun nothingIsDisabledOnAFreshInstall() {
        assertEquals(emptySet<String>(), newStore().loadDisabled())
    }

    @Test
    fun disabledThemesRoundTripThroughANewStoreInstance() {
        newStore().saveDisabled(setOf("ocean", "farm"))

        assertEquals(setOf("ocean", "farm"), newStore().loadDisabled())
    }

    @Test
    fun savingAnEmptySetClearsTheSelection() {
        val store = newStore()
        store.saveDisabled(setOf("ocean"))
        store.saveDisabled(emptySet())

        assertEquals(emptySet<String>(), newStore().loadDisabled())
    }

    /**
     * The pruning branch. A theme removed or renamed in a later release leaves
     * a dead id behind; without this it accumulates in the file forever.
     */
    @Test
    fun idsForThemesThatNoLongerExistArePrunedOnRead() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(
                PREFS_DISABLED_THEMES_KEY,
                setOf("ocean", "a-theme-removed-in-a-later-release", ""),
            )
            .commit()

        assertEquals(setOf("ocean"), newStore().loadDisabled())
    }

    @Test
    fun everyRealThemeIdSurvivesTheRoundTrip() {
        val all = THEME_DEFS.mapTo(mutableSetOf()) { it.id }
        newStore().saveDisabled(all)

        assertEquals(all, newStore().loadDisabled())
    }

    // ---------------------------------------------------------- gate lock

    @Test
    fun theGateStartsUnlockedOnAFreshInstall() {
        assertEquals(GateLock(failures = 0, lockedUntilWallMs = 0L), newStore().loadGateLock())
    }

    /** The fix for the bypass: this is the state that has to outlive the process. */
    @Test
    fun theGateLockRoundTripsThroughANewStoreInstance() {
        val deadline = System.currentTimeMillis() + GATE_LOCKOUT_MS
        newStore().saveGateLock(GateLock(failures = 2, lockedUntilWallMs = deadline))

        assertEquals(GateLock(2, deadline), newStore().loadGateLock())
    }

    @Test
    fun clearingTheGateLockPersistsToo() {
        val store = newStore()
        store.saveGateLock(GateLock(failures = 2, lockedUntilWallMs = 12_345L))
        store.saveGateLock(GateLock())

        assertEquals(GateLock(), newStore().loadGateLock())
    }

    /** The two keys must not tread on each other — they share one file. */
    @Test
    fun themesAndTheGateLockArePersistedIndependently() {
        val store = newStore()
        store.saveDisabled(setOf("space"))
        store.saveGateLock(GateLock(failures = 1, lockedUntilWallMs = 999L))

        val reopened = newStore()
        assertEquals(setOf("space"), reopened.loadDisabled())
        assertEquals(GateLock(1, 999L), reopened.loadGateLock())

        reopened.saveDisabled(emptySet())
        assertEquals("toggling a theme must not clear the lock", GateLock(1, 999L), newStore().loadGateLock())
    }

    /**
     * A full trip through the real store: lock the gate out via one ViewModel,
     * then build a fresh one the way a relaunch does. `GateLockPersistenceTest`
     * proves this against a fake; this proves the fake is telling the truth.
     */
    @Test
    fun aLockoutSurvivesARelaunchThroughRealPreferences() {
        val first = AppViewModel(newStore(), androidx.lifecycle.SavedStateHandle())
        first.openGate()
        repeat(MAX_GATE_FAILURES) {
            val q = (first.uiState as UiState.Gate).question
            first.pickGateAnswer(q.values.first { it != q.correct })
        }

        val relaunched = AppViewModel(newStore(), androidx.lifecycle.SavedStateHandle())
        relaunched.openGate()

        assertTrue(
            "still locked after a relaunch through real preferences",
            (relaunched.uiState as UiState.Gate).lockedUntilWallMs > System.currentTimeMillis(),
        )
        relaunched.pickGateAnswer((relaunched.uiState as UiState.Gate).question.correct)
        assertTrue("Settings stayed shut", relaunched.uiState is UiState.Gate)
    }
}
