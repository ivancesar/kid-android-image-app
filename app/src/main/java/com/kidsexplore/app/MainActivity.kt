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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kidsexplore.app.model.THEME_DEFS
import com.kidsexplore.app.ui.images.Photos
import com.kidsexplore.app.ui.screens.GateScreen
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
            //
            // The photograph to keep is worked out when the event fires rather
            // than when this composes, so it is whatever is on screen at the
            // moment the app stops.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, appViewModel) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_STOP) {
                        Photos.cache.trimKeeping(appViewModel.currentPhotographOrNull())
                    }
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

    // Home is the only screen a user leaves and comes straight back to, and
    // the `when` below takes it out of composition while they are away — which
    // discarded its grid position, so every trip into a theme dropped the menu
    // back to the top. This holds the `rememberSaveable` state of the Home
    // branch (the grid's scroll position, and the measured header height that
    // sets the grid's top padding) while that branch is gone, and hands it back
    // when it returns. The holder is itself saveable, so the position also
    // survives a rotation and process death.
    val screenState = rememberSaveableStateHolder()

    // That slot is only meaningful for the menu it was scrolled in. Enabling or
    // disabling a theme changes what the grid holds, so a restored index lands
    // on a different card than the one left behind — and with every theme off
    // it points into a grid that has no rows to clamp it, which leaves the grid
    // reporting a position that is not the top, the header never composing, and
    // the empty screen with no title and no explanation on it. Drop the slot on
    // any change and let Home start from the top.
    //
    // Watched here rather than hung off the Settings toggle, so it holds for
    // every route that changes the roster while Home is off screen — which is
    // all of them today, Parent Settings being the only place a theme can be
    // toggled. Were the roster ever to change with Home composed, dropping the
    // slot would not move the grid already on screen; the empty-list guard in
    // HomeScreen is what carries that case. Compared against the last set
    // rather than keyed on an effect, so neither the first composition nor a
    // restore after process death is mistaken for a change — both of those are
    // exactly when the retained position is still the right one.
    val disabledThemeIds = viewModel.disabledThemeIds
    var lastDisabledThemeIds by remember { mutableStateOf(disabledThemeIds) }
    SideEffect {
        if (lastDisabledThemeIds != disabledThemeIds) {
            lastDisabledThemeIds = disabledThemeIds
            screenState.removeState(HOME_STATE_KEY)
        }
    }

    when (state) {
        UiState.Home -> screenState.SaveableStateProvider(HOME_STATE_KEY) {
            HomeScreen(
                themes = viewModel.visibleThemes,
                onOpenTheme = viewModel::openTheme,
                onOpenGate = viewModel::openGate,
            )
        }

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

/** Key for Home's slot in the app's [rememberSaveableStateHolder]; only one screen uses it. */
private const val HOME_STATE_KEY = "home"
