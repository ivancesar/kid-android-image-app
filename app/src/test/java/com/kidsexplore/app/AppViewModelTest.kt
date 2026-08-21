package com.kidsexplore.app

import androidx.lifecycle.SavedStateHandle
import com.kidsexplore.app.data.ThemeStore
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * The whole state machine, off-device.
 *
 * [AppViewModel] takes its storage, its randomness and its clock as
 * constructor parameters, so none of this needs an emulator: the gate's
 * lockout is driven by a hand-advanced clock rather than by waiting 30
 * real seconds. The Compose UI on top is covered by the instrumented
 * `KidsExploreFlowTest`.
 */
class AppViewModelTest {

    private var now = 1_000_000L
    private val store = FakeThemeStore()

    private fun newViewModel(
        store: ThemeStore = this.store,
        savedState: SavedStateHandle = SavedStateHandle(),
        seed: Int = 1,
    ) = AppViewModel(
        store = store,
        savedState = savedState,
        random = Random(seed),
        wallClock = { now },
    )

    private val AppViewModel.gateState: UiState.Gate
        get() = uiState as UiState.Gate

    private fun AppViewModel.answerWrong() {
        val question = gateState.question
        pickGateAnswer(question.values.first { it != question.correct })
    }

    // ---------------------------------------------------------------- basics

    @Test
    fun startsOnHomeWithEveryThemeVisible() {
        val vm = newViewModel()

        assertEquals(UiState.Home, vm.uiState)
        assertEquals(THEME_DEFS.size, vm.visibleThemes.size)
    }

    @Test
    fun openingAThemeShowsItsFirstImage() {
        val vm = newViewModel()
        vm.openTheme("cars")

        assertEquals(UiState.Viewer("cars", 0), vm.uiState)
    }

    /** A Viewer with no resolvable theme is exactly the blank screen the sealed state exists to prevent. */
    @Test
    fun openingAThemeThatDoesNotExistIsIgnored() {
        val vm = newViewModel()
        vm.openTheme("no-such-theme")

        assertEquals(UiState.Home, vm.uiState)
    }

    @Test
    fun nextWalksEveryImageAndWrapsToTheStart() {
        val vm = newViewModel()
        vm.openTheme("cars")
        val count = THEME_DEFS.first { it.id == "cars" }.labelCount

        repeat(count) { i ->
            assertEquals("image at index $i", i, (vm.uiState as UiState.Viewer).imageIndex)
            vm.next()
        }
        assertEquals(0, (vm.uiState as UiState.Viewer).imageIndex)
    }

    @Test
    fun prevFromTheFirstImageWrapsToTheLast() {
        val vm = newViewModel()
        vm.openTheme("dinosaurs")
        val count = THEME_DEFS.first { it.id == "dinosaurs" }.labelCount

        vm.prev()
        assertEquals(count - 1, (vm.uiState as UiState.Viewer).imageIndex)
        vm.next()
        assertEquals(0, (vm.uiState as UiState.Viewer).imageIndex)
    }

    @Test
    fun goHomeClearsTheActiveTheme() {
        val vm = newViewModel()
        vm.openTheme("space")
        vm.goHome()

        assertEquals(UiState.Home, vm.uiState)
    }

    // ------------------------------------------------------------ the gate

    @Test
    fun everyGateQuestionOffersFourDistinctPositiveAnswersIncludingTheSum() {
        repeat(500) { seed ->
            val vm = newViewModel(savedState = SavedStateHandle(), seed = seed)
            vm.openGate()
            val q = vm.gateState.question

            assertEquals("operands sum to the correct answer", q.a + q.b, q.correct)
            assertEquals("four answers", 4, q.values.size)
            assertEquals("all distinct", 4, q.values.toSet().size)
            assertTrue("correct answer is offered", q.correct in q.values)
            assertTrue("no zero or negative answers", q.values.all { it > 0 })
        }
    }

    /**
     * The old generator drew distractors from correct±3, so three of the four
     * buttons sat next to the answer and a child who could nearly add was
     * better than even money.
     */
    @Test
    fun noDistractorSitsAdjacentToTheCorrectAnswer() {
        repeat(500) { seed ->
            val vm = newViewModel(savedState = SavedStateHandle(), seed = seed)
            vm.openGate()
            val q = vm.gateState.question

            q.values.filter { it != q.correct }.forEach { wrong ->
                assertTrue(
                    "distractor $wrong is adjacent to ${q.correct}",
                    abs(wrong - q.correct) >= 2,
                )
            }
        }
    }

    /** Leaving the same four buttons up let a child reach Settings in four taps. */
    @Test
    fun aWrongAnswerReplacesTheQuestion() {
        val vm = newViewModel()
        vm.openGate()
        val first = vm.gateState.question

        vm.answerWrong()

        assertTrue("stays on the gate", vm.uiState is UiState.Gate)
        assertTrue("flags the mistake", vm.gateState.wrong)
        assertNotEquals("a fresh question", first, vm.gateState.question)
    }

    @Test
    fun correctGateAnswerOpensSettings() {
        val vm = newViewModel()
        vm.openGate()
        vm.pickGateAnswer(vm.gateState.question.correct)

        assertEquals(UiState.Settings, vm.uiState)
    }

    @Test
    fun theGateLocksAfterRepeatedWrongAnswersAndRefusesEvenTheCorrectOne() {
        val vm = newViewModel()
        vm.openGate()

        repeat(MAX_GATE_FAILURES) { vm.answerWrong() }

        assertTrue("a lockout is in force", vm.gateState.lockedUntilWallMs > now)
        vm.pickGateAnswer(vm.gateState.question.correct)
        assertTrue("the lockout outranks a correct answer", vm.uiState is UiState.Gate)
    }

    @Test
    fun theLockoutExpiresOnItsOwn() {
        val vm = newViewModel()
        vm.openGate()
        repeat(MAX_GATE_FAILURES) { vm.answerWrong() }

        now += GATE_LOCKOUT_MS + 1

        vm.pickGateAnswer(vm.gateState.question.correct)
        assertEquals(UiState.Settings, vm.uiState)
    }

    /** A lockout a child can clear by tapping Cancel and reopening is no lockout. */
    @Test
    fun theLockoutSurvivesLeavingAndReopeningTheGate() {
        val vm = newViewModel()
        vm.openGate()
        repeat(MAX_GATE_FAILURES) { vm.answerWrong() }

        vm.goHome()
        vm.openGate()

        assertTrue("still locked", vm.gateState.lockedUntilWallMs > now)
        vm.pickGateAnswer(vm.gateState.question.correct)
        assertTrue(vm.uiState is UiState.Gate)
    }

    @Test
    fun aSuccessfulAnswerClearsTheFailureCount() {
        val vm = newViewModel()
        vm.openGate()
        repeat(MAX_GATE_FAILURES - 1) { vm.answerWrong() }
        vm.pickGateAnswer(vm.gateState.question.correct)
        assertEquals(UiState.Settings, vm.uiState)

        // One more wrong answer must not immediately re-lock: the counter reset.
        vm.goHome()
        vm.openGate()
        vm.answerWrong()

        assertEquals("not locked", 0L, vm.gateState.lockedUntilWallMs)
    }

    // --------------------------------------------------------- theme toggles

    @Test
    fun disablingAThemeHidesItAndPersistsTheChange() {
        val vm = newViewModel()
        vm.toggleThemeEnabled("ocean")

        assertTrue(vm.visibleThemes.none { it.id == "ocean" })
        assertEquals(THEME_DEFS.size - 1, vm.visibleThemes.size)
        assertEquals(setOf("ocean"), store.savedDisabled)

        vm.toggleThemeEnabled("ocean")
        assertTrue(vm.visibleThemes.any { it.id == "ocean" })
        assertEquals(emptySet<String>(), store.savedDisabled)
    }

    @Test
    fun disabledThemesAreReadBackOnRestart() {
        val restarted = newViewModel(store = FakeThemeStore(initialDisabled = setOf("farm")))

        assertTrue(restarted.visibleThemes.none { it.id == "farm" })
        assertTrue("cars" !in restarted.disabledThemeIds)
    }

    /**
     * `List<ThemeDef>` is unstable to the Compose compiler, so strong skipping
     * compares HomeScreen's `themes` parameter by reference. A getter that
     * re-filtered on every read returned a new list each time and made the
     * screen unskippable; this pins the caching that fixed it.
     */
    @Test
    fun visibleThemesKeepsTheSameInstanceUntilTheSelectionChanges() {
        val vm = newViewModel()

        assertSame("re-reading must not reallocate", vm.visibleThemes, vm.visibleThemes)

        // Unrelated state changing must not invalidate it either — every
        // navigation recomposes the caller that passes this down.
        val beforeNavigation = vm.visibleThemes
        vm.openTheme("cars")
        vm.next()
        vm.goHome()
        assertSame("navigation must not reallocate", beforeNavigation, vm.visibleThemes)

        vm.toggleThemeEnabled("cars")
        assertNotSame("a real change must reallocate", beforeNavigation, vm.visibleThemes)
        assertTrue(vm.visibleThemes.none { it.id == "cars" })
    }

    @Test
    fun aParentCanTurnEverythingOff() {
        val vm = newViewModel()
        THEME_DEFS.forEach { vm.toggleThemeEnabled(it.id) }

        assertTrue("Home has an empty state for exactly this", vm.visibleThemes.isEmpty())
    }

    // ------------------------------------------------------- process death

    @Test
    fun theViewerIsRestoredWhereItLeftOff() {
        val handle = SavedStateHandle()
        val before = newViewModel(savedState = handle)
        before.openTheme("trains")
        before.next()
        before.next()

        val after = newViewModel(savedState = handle)

        assertEquals(UiState.Viewer("trains", 2), after.uiState)
    }

    /** Coming back into Settings would hand a child the parent screen with no gate. */
    @Test
    fun settingsIsNeverRestored() {
        val handle = SavedStateHandle()
        val before = newViewModel(savedState = handle)
        before.openGate()
        before.pickGateAnswer(before.gateState.question.correct)
        assertEquals(UiState.Settings, before.uiState)

        val after = newViewModel(savedState = handle)

        assertEquals(UiState.Home, after.uiState)
    }

    @Test
    fun aHalfAnsweredGateIsNotRestored() {
        val handle = SavedStateHandle()
        val before = newViewModel(savedState = handle)
        before.openGate()

        val after = newViewModel(savedState = handle)

        assertEquals(UiState.Home, after.uiState)
    }

    /** MainActivity indexes straight into labels, so a stale index must not survive. */
    @Test
    fun anOutOfRangeRestoredIndexIsCoercedIntoRange() {
        val handle = SavedStateHandle(
            mapOf(
                KEY_SCREEN to "viewer",
                KEY_VIEWER_THEME to "cars",
                KEY_VIEWER_INDEX to 99,
            )
        )

        val vm = newViewModel(savedState = handle)

        val count = THEME_DEFS.first { it.id == "cars" }.labelCount
        assertEquals(UiState.Viewer("cars", count - 1), vm.uiState)
    }

    @Test
    fun aRestoredThemeThatNoLongerExistsFallsBackToHome() {
        val handle = SavedStateHandle(
            mapOf(
                KEY_SCREEN to "viewer",
                KEY_VIEWER_THEME to "theme-removed-in-a-later-release",
                KEY_VIEWER_INDEX to 0,
            )
        )

        assertEquals(UiState.Home, newViewModel(savedState = handle).uiState)
    }
}
