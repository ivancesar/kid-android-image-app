package com.kidsexplore.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.res.stringArrayResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.screens.GateScreen
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Required from targetSdk 35 on, where the system draws behind the
        // bars whether or not the app asks. Screens pad themselves with
        // WindowInsets.safeDrawing.
        enableEdgeToEdge()
        setContent {
            val appViewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)

            // The Viewer is the only dark screen. Everything else is near
            // white, where the framework's default light system-bar icons are
            // invisible, so the icon treatment has to follow the screen.
            val darkBackground = appViewModel.uiState is UiState.Viewer
            DisposableEffect(darkBackground) {
                val style = if (darkBackground) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            KidsExploreTheme {
                KidsExploreApp(appViewModel)
            }
        }
    }
}

/** Internal rather than private so instrumented tests can host it with their own ViewModel. */
@Composable
internal fun KidsExploreApp(viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory)) {
    val state = viewModel.uiState

    // Back is the most-pressed button on an Android device; without this it
    // quit the app from every screen. Home stays unhandled so Back still exits.
    BackHandler(enabled = state !is UiState.Home) { viewModel.goHome() }

    when (state) {
        UiState.Home -> HomeScreen(
            themes = viewModel.visibleThemes,
            onOpenTheme = viewModel::openTheme,
            onOpenGate = viewModel::openGate,
        )

        is UiState.Viewer -> {
            // Both invariants are enforced on the way in: openTheme() rejects
            // unknown ids, stepImage() wraps within bounds, and restoreState()
            // coerces a restored index. There is no valid Viewer to fall back
            // from, which is the point of the sealed state.
            val theme = THEME_DEFS.first { it.id == state.themeId }
            // The ViewModel wraps the index against ThemeDef.labelCount, which
            // it trusts to match this array; ThemeResourcesTest holds them
            // together. Coerced anyway — a mismatch should not crash a child's
            // screen, and the test is what makes the drift loud.
            val labels = stringArrayResource(theme.labelsRes)
            ViewerScreen(
                theme = theme,
                currentLabel = labels[state.imageIndex.coerceIn(labels.indices)],
                onHome = viewModel::goHome,
                onNext = viewModel::next,
                onPrev = viewModel::prev,
            )
        }

        is UiState.Gate -> GateScreen(
            question = state.question,
            wrong = state.wrong,
            lockedUntilWallMs = state.lockedUntilWallMs,
            onPick = viewModel::pickGateAnswer,
            onCancel = viewModel::goHome,
        )

        UiState.Settings -> SettingsScreen(
            disabledThemeIds = viewModel.disabledThemeIds,
            onToggle = viewModel::toggleThemeEnabled,
            onDone = viewModel::goHome,
        )
    }
}
