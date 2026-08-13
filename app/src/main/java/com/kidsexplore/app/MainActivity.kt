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

@Composable
private fun KidsExploreApp(viewModel: AppViewModel = viewModel()) {
    when (viewModel.screen) {
        Screen.HOME -> HomeScreen(
            themes = viewModel.visibleThemes,
            onOpenTheme = viewModel::openTheme,
            onOpenGate = viewModel::openGate,
        )

        Screen.VIEWER -> {
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
