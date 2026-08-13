# Kids Explore

A native Android app for kids to safely browse a curated set of picture categories on a parent's phone. Everything a child can reach is a fixed, parent-controlled list of themes — there's no search, no internet browsing, and no way to add or remove content from the child-facing side.

Built with Kotlin + Jetpack Compose. Ported from a Claude Design prototype (`Kids Image Browser.dc.html`).

## Overview

The app is a single activity with four screens, driven by one piece of state (`Screen.HOME | VIEWER | GATE | SETTINGS`):

```
Home ──tap theme──▶ Viewer ──Home──▶ Home
  │                                    ▲
  └──gear──▶ Gate ──correct answer──▶ Settings ──Done──▶ Home
              │
              └──Cancel──▶ Home
```

There is no back-stack navigation library involved — the current screen is just a field on the app's `ViewModel`, and each screen is a plain Compose function that reads it.

## Screens

### Home

- Header: "KIDS EXPLORE" label and a settings gear button (top right) that opens the **parental gate**.
- Title: "Pick something to look at!"
- A 2-column grid of theme cards — one per enabled theme. Each card shows a themed icon on a translucent circle, the theme name, and a color pair (fill + a darker bottom accent stripe) derived from the theme's hue.
- Only themes enabled in **Settings** appear here; disabling a theme removes it from this grid immediately.
- Tapping a card opens the **Viewer** for that theme.

### Viewer

- Dark full-bleed background.
- Top row: a "Home" pill button (top left) that returns to Home, and the current theme's name (centered).
- A large rounded placeholder card filling the middle of the screen, with a diagonal two-tone stripe pattern in the theme's colors. It shows the current item's label in monospace text, centered.
- Bottom row: "◀ Back" and "▶ Next" pill buttons that cycle through the theme's 8 items, wrapping around at both ends.

### Parental Gate ("Grown-ups only")

- A simple math question: two random numbers between 2 and 7, added together (`a + b = ?`).
- Four answer buttons in a 2×2 grid — the correct sum plus three distinct, randomly generated wrong values.
- Tapping the correct answer opens **Settings**. Tapping a wrong answer shows "Not quite, try again!" and leaves the same question up (it does not regenerate).
- "Cancel" returns to Home without opening Settings.
- A fresh question is generated every time the gate is opened.

### Settings ("Parent Settings")

- A scrollable list of **all** themes (regardless of current enabled state), each as a row with a checkbox and the theme name.
- Tapping a row toggles that theme's visibility on the Home screen.
- Enabled/disabled state is persisted to `SharedPreferences`, so it survives app restarts.
- "Done" returns to Home, where the grid now reflects the updated theme selection.

## Themes

Eight fixed themes, each with a name, a hue, an icon, and 8 item labels:

| Theme | Hue | Icon |
|---|---|---|
| Cars | 15 | car |
| Construction | 45 | bulldozer |
| Animals | 320 | animal face |
| Dinosaurs | 285 | dinosaur |
| Space | 250 | ringed planet |
| Trains | 350 | train |
| Ocean | 175 | fish |
| Farm | 75 | farm animal face |

Every theme's color palette (card fill, stripe, and border/accent) is generated from just its hue using the same OKLCH formula throughout the app, so palettes stay visually consistent without hand-picked hex values:

- Card fill: `oklch(72% 0.16 hue)`
- Stripe: `oklch(66% 0.17 hue)`
- Border / accent: `oklch(50% 0.19 hue)`

Each theme's icon is drawn with Compose `Canvas`, shape-for-shape, rather than using a generic icon set.

### On images

The Viewer never shows real photos — there are none bundled with the app. Each "image" is a short text label (e.g. "Red sports car, side view") rendered on a themed striped placeholder card. This is intentional: it avoids bundling or fetching any copyrighted or externally-sourced photos. Swapping in real photography per item would be the natural next step if this app were to ship.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), single `ComponentActivity`
- One `AndroidViewModel` (`AppViewModel`) holds all app state: current screen, selected theme, viewer position, enabled themes, and the active gate question
- No navigation library, no networking, no local database — `SharedPreferences` is the only persistence
- Gradle Kotlin DSL, AGP's built-in Kotlin support (no separate `org.jetbrains.kotlin.android` plugin)

## Project structure

```
app/src/main/java/com/kidsexplore/app/
├── MainActivity.kt              # hosts Compose content, switches on Screen
├── AppViewModel.kt              # screen state machine, gate logic, persistence
├── model/
│   └── ThemeDef.kt              # theme data + the 8 THEME_DEFS entries
└── ui/
    ├── theme/
    │   ├── OklchColor.kt        # OKLCH → sRGB conversion, per-theme palettes
    │   └── Theme.kt             # MaterialTheme wrapper, type styles
    ├── icons/
    │   └── ThemeIcons.kt        # Canvas-drawn icon per theme
    └── screens/
        ├── HomeScreen.kt
        ├── ViewerScreen.kt
        ├── GateScreen.kt
        └── SettingsScreen.kt
```

## Building & running

Requires JDK 17+ and the Android SDK (compileSdk/targetSdk 37).

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Open the project in Android Studio to run it directly on a device or emulator, or install manually with `adb install`.

## Known limitations

- Placeholder text labels stand in for real images (see [On images](#on-images) above).
- No confirmation/undo when a parent disables a theme a child was mid-viewing — they're just returned to Home the next time they tap Home.
- Font is the system sans-serif at heavy weights, approximating the source design's Nunito 800/900; no font file is bundled.
