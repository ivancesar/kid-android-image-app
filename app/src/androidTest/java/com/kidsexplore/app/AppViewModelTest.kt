package com.kidsexplore.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kidsexplore.app.model.THEME_DEFS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * State-machine and persistence tests for [AppViewModel].
 *
 * Instrumented rather than plain JVM tests because the ViewModel reads and
 * writes real SharedPreferences.
 */
@RunWith(AndroidJUnit4::class)
class AppViewModelTest {

    private lateinit var app: Application

    private fun newViewModel() = AppViewModel(app)

    @Before
    fun clearPrefs() {
        app = ApplicationProvider.getApplicationContext()
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun startsOnHomeWithEveryThemeVisible() {
        val vm = newViewModel()
        assertEquals(Screen.HOME, vm.screen)
        assertEquals(THEME_DEFS.size, vm.visibleThemes.size)
        assertNull(vm.activeTheme)
    }

    @Test
    fun openingAThemeShowsItsFirstImage() {
        val vm = newViewModel()
        vm.openTheme("cars")

        assertEquals(Screen.VIEWER, vm.screen)
        assertEquals("cars", vm.activeTheme?.id)
        assertEquals(THEME_DEFS.first { it.id == "cars" }.labels[0], vm.currentLabel)
    }

    @Test
    fun nextWalksEveryImageAndWrapsToTheStart() {
        val vm = newViewModel()
        vm.openTheme("cars")
        val labels = THEME_DEFS.first { it.id == "cars" }.labels

        labels.forEachIndexed { i, expected ->
            assertEquals("image at index $i", expected, vm.currentLabel)
            vm.next()
        }
        // one extra next() past the end wraps back around
        assertEquals(labels[0], vm.currentLabel)
    }

    @Test
    fun prevFromTheFirstImageWrapsToTheLast() {
        val vm = newViewModel()
        vm.openTheme("dinosaurs")
        val labels = THEME_DEFS.first { it.id == "dinosaurs" }.labels

        vm.prev()
        assertEquals(labels.last(), vm.currentLabel)
        vm.next()
        assertEquals(labels.first(), vm.currentLabel)
    }

    @Test
    fun goHomeClearsTheActiveTheme() {
        val vm = newViewModel()
        vm.openTheme("space")
        vm.goHome()

        assertEquals(Screen.HOME, vm.screen)
        assertNull(vm.activeTheme)
        assertNull(vm.currentLabel)
    }

    @Test
    fun gateOffersFourAnswersIncludingTheCorrectSum() {
        val vm = newViewModel()
        vm.openGate()

        val gate = vm.gate
        assertNotNull(gate)
        gate!!
        assertEquals(Screen.GATE, vm.screen)
        assertEquals(gate.a + gate.b, gate.correct)
        assertEquals(4, gate.values.size)
        assertEquals("answers are distinct", 4, gate.values.toSet().size)
        assertTrue("correct answer is offered", gate.correct in gate.values)
        assertTrue("all answers positive", gate.values.all { it > 0 })
    }

    @Test
    fun wrongGateAnswerKeepsTheSameQuestionAndFlagsTheMistake() {
        val vm = newViewModel()
        vm.openGate()
        val gate = vm.gate!!
        val wrong = gate.values.first { it != gate.correct }

        vm.pickGateAnswer(wrong)

        assertEquals("stays on the gate", Screen.GATE, vm.screen)
        assertTrue(vm.gateWrong)
        assertEquals("question is not regenerated", gate, vm.gate)
    }

    @Test
    fun correctGateAnswerOpensSettings() {
        val vm = newViewModel()
        vm.openGate()

        vm.pickGateAnswer(vm.gate!!.correct)

        assertEquals(Screen.SETTINGS, vm.screen)
        assertNull(vm.gate)
        assertFalse(vm.gateWrong)
    }

    @Test
    fun cancellingTheGateReturnsHome() {
        val vm = newViewModel()
        vm.openGate()
        vm.goHome()

        assertEquals(Screen.HOME, vm.screen)
        assertNull(vm.gate)
    }

    @Test
    fun disablingAThemeHidesItFromHome() {
        val vm = newViewModel()
        vm.toggleThemeEnabled("ocean")

        assertFalse(vm.enabledThemes.getValue("ocean"))
        assertTrue(vm.visibleThemes.none { it.id == "ocean" })
        assertEquals(THEME_DEFS.size - 1, vm.visibleThemes.size)

        vm.toggleThemeEnabled("ocean")
        assertTrue(vm.visibleThemes.any { it.id == "ocean" })
    }

    @Test
    fun disabledThemesSurviveAViewModelRestart() {
        newViewModel().toggleThemeEnabled("farm")

        // a fresh ViewModel reads back what the previous one persisted
        val restarted = newViewModel()
        assertFalse(restarted.enabledThemes.getValue("farm"))
        assertTrue(restarted.visibleThemes.none { it.id == "farm" })
    }
}
