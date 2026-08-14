package com.kidsexplore.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidsexplore.app.ui.screens.GateScreen
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme

/**
 * The app's only activity. Screens are plain composables selected by
 * [AppViewModel.screen] rather than destinations in a nav graph, so there is no
 * back stack — the system Back button leaves the app instead of stepping back a
 * screen. Every screen offers its own explicit way out (Home / Done / Cancel).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KidsExploreTheme {
                KidsExploreApp()
            }
        }
    }
}

/** Internal rather than private so instrumented tests can host it with their own ViewModel. */
@Composable
internal fun KidsExploreApp(viewModel: AppViewModel = viewModel()) {
    when (viewModel.screen) {
        Screen.HOME -> HomeScreen(
            themes = viewModel.visibleThemes,
            onOpenTheme = viewModel::openTheme,
            onOpenGate = viewModel::openGate,
        )

        Screen.VIEWER -> {
            // Both are set together with the screen, so the guard is only for
            // a state that should never occur (e.g. an id with no matching
            // ThemeDef): render nothing rather than crash on a child's device.
            val theme = viewModel.activeTheme
            val label = viewModel.currentLabel
            if (theme != null && label != null) {
                ViewerScreen(
                    theme = theme,
                    currentLabel = label,
                    onHome = viewModel::goHome,
                    onNext = viewModel::next,
                    onPrev = viewModel::prev,
                )
            }
        }

        Screen.GATE -> {
            val question = viewModel.gate
            if (question != null) {
                GateScreen(
                    question = question,
                    wrong = viewModel.gateWrong,
                    onPick = viewModel::pickGateAnswer,
                    onCancel = viewModel::goHome,
                )
            }
        }

        Screen.SETTINGS -> SettingsScreen(
            enabledThemes = viewModel.enabledThemes,
            onToggle = viewModel::toggleThemeEnabled,
            onDone = viewModel::goHome,
        )
    }
}
