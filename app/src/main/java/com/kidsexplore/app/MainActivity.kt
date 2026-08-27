package com.kidsexplore.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.screens.GateScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kidsexplore.app.ui.images.Photos
import com.kidsexplore.app.ui.screens.HomeScreen
import com.kidsexplore.app.ui.screens.PolicyScreen
import com.kidsexplore.app.ui.screens.SettingsScreen
import com.kidsexplore.app.ui.screens.ViewerScreen
import com.kidsexplore.app.ui.theme.KidsExploreTheme

// AppCompatActivity rather than ComponentActivity: per-app language below
// Android 13 needs a live AppCompatDelegate to apply the override.
class MainActivity : AppCompatActivity() {
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

            // 24 MiB of decoded photographs is worth holding while a child is
            // paging, and not worth holding while the app is in the background —
            // it only makes the process a better candidate for being killed.
            // Trimmed rather than emptied, so coming back from the recents
            // screen does not cost a decode. Via the lifecycle rather than
            // Activity.onTrimMemory, which is deprecated as of API 34.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) Photos.cache.trimToOnePhoto()
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
    BackHandler(enabled = state !is UiState.Home) {
        // From the policy, Back returns to the screen that opened it. Sending a
        // parent to Home from there would make Back the one control that
        // discards where they were, and they would have to pass the gate again.
        if (state is UiState.Policy) viewModel.closePolicy() else viewModel.goHome()
    }

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
            // The ViewModel wraps the index against the same list this reads,
            // so it is already in range; coerced anyway, because a Viewer that
            // crashed on a child's screen would be a worse way to find out.
            val index = state.imageIndex.coerceIn(theme.imageRes.indices)
            ViewerScreen(
                theme = theme,
                onHome = viewModel::goHome,
                onNext = viewModel::next,
                onPrev = viewModel::prev,
                currentImage = theme.imageRes[index],
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
            // AppCompat owns the stored choice and recreates the activity when
            // it changes, so this reads back fresh rather than being mirrored
            // in ViewModel state.
            currentLanguage = AppLocales.current(),
            onPickLanguage = AppLocales::apply,
            onOpenPolicy = viewModel::openPolicy,
        )

        UiState.Policy -> PolicyScreen(onBack = viewModel::closePolicy)
    }
}
