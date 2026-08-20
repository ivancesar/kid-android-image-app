package com.kidsexplore.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidsexplore.app.ui.screens.GateScreen
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme

class MainActivity : AppCompatActivity() {
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
            val theme = viewModel.activeTheme
            if (theme != null) {
                // Resolved here rather than in the ViewModel so that switching
                // language recomposes the label with the new configuration.
                val labels = stringArrayResource(theme.labelsRes)
                ViewerScreen(
                    theme = theme,
                    currentLabel = labels[viewModel.imageIndex.coerceIn(0, labels.lastIndex)],
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
            // AppCompat owns the stored choice and recreates the activity when it
            // changes, so this reads back fresh rather than being mirrored in state.
            currentLanguage = AppLocales.current(),
            onPickLanguage = AppLocales::apply,
        )
    }
}
