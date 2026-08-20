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

The app rotates freely between portrait and landscape (no orientation lock). Screen state lives in an `AndroidViewModel`, which survives the activity recreation Android does on rotation, so nothing resets when the device turns. Layouts reflow rather than using dedicated landscape-specific arrangements — the Home grid and Settings list scroll, and the Gate screen falls back to scrolling instead of clipping if its content doesn't fit the available height.

## Screens

### Home

- Header: "KIDS EXPLORE" label, a settings gear button (top right) that opens the **parental gate**, and the "Pick something to look at!" title.
- The header fades out once the grid is scrolled away from the top, and fades back in when scrolled back — it floats over the grid rather than sharing layout space with it, so its own size never affects the grid's, which would otherwise cause the list to bounce as it hid/showed itself.
- A grid of theme cards — one per enabled theme, with as many columns as fit the available width (2 in portrait, more in landscape or on a wider screen) rather than a fixed count. Each card shows a themed icon on a near-white circle, the theme name, and a color pair (fill + a darker bottom accent stripe) derived from the theme's hue. The circle takes all the height the name leaves it, so the icon is as large as the card allows and the card colour reads as a frame around it.
- Only themes enabled in **Settings** appear here; disabling a theme removes it from this grid immediately.
- Tapping a card opens the **Viewer** for that theme.

### Viewer

- Dark full-bleed background.
- A small square "Home" button returns to Home — that's the entire header here, no theme name shown, to keep the focus on the image.
- A large rounded placeholder card, with a diagonal two-tone stripe pattern in the theme's colors, showing the current item's label in monospace text, centered.
- The same labelled "◀ Back" / "▶ Next" pill buttons are used in both orientations, so the controls look and behave identically regardless of how the device is held:
  - **Portrait**: the card fills the remaining space above a button row, with Back/Next side by side underneath it.
  - **Landscape**: Back/Next flank the card in a single row — the card sits between them rather than under them — and the row fills the full screen height (down to, but not under, the status bar), so the image gets as much vertical room as the display allows. The Home button floats over the top-left corner of the image instead of sitting in its own header row, since there's no header row to spare the height for in landscape.
- Either way, Back/Next cycle through the theme's 8 items, wrapping around at both ends.
- The card also responds to a horizontal swipe — swipe left for next, right for back — as an alternative to the buttons, in both orientations.

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
- A language dropdown sits above the theme list — "Same as phone settings" plus each shipped language named in its own language. Selecting one applies immediately.

## Themes

Fourteen fixed themes, each with a name, a hue, an icon, and 8 item labels:

| Theme | id | Hue |
|---|---|---|
| Cars | `cars` | 15 |
| Construction | `construction` | 45 |
| Trains | `trains` | 350 |
| Animals | `animals` | 320 |
| Birds | `bird` | 225 |
| Insects | `insects` | 200 |
| Ocean | `ocean` | 175 |
| Farm | `farm` | 75 |
| Dinosaurs | `dinosaurs` | 285 |
| Flowers | `flowers` | 302 |
| Forest | `forest` | 150 |
| Fruit | `fruit` | 100 |
| Vegetables | `vegetable` | 125 |
| Space | `space` | 250 |

Names shown are the English ones; a theme's display name and its 8 labels are `@StringRes`/`@ArrayRes` ids, not literals, so both translate. See **Languages** below.

They are declared in that order in `THEME_DEFS`, loosely grouped (things that go, creatures, growing things, space), and that one list drives both the Home grid and the Settings list.

Every theme's color palette (card fill, stripe, and border/accent) is generated from just its hue using the same OKLCH formula throughout the app, so palettes stay visually consistent without hand-picked hex values:

- Card fill: `oklch(72% 0.16 hue)`
- Stripe: `oklch(66% 0.17 hue)`
- Border / accent: `oklch(50% 0.19 hue)`

### Icons

Each theme's icon is a black-and-white vector drawable in `app/src/main/res/drawable/ic_theme_<id>.xml`, drawn untinted on a near-white disc on the theme's card. The artwork is two-tone by design, so it is deliberately never tinted — a drawable-wide tint would collapse the white detail into the black shapes and leave a flat silhouette.

Those drawables are generated, not hand-written. The source SVGs live in `icons-src/<id>.svg` and are converted by `tools/svg2vd.py`:

```bash
python3 tools/svg2vd.py icons-src app/src/main/res/drawable
```

The converter inlines the SVGs' CSS classes into path attributes, re-expresses `<circle>`/`<rect>`/`<ellipse>` as cubic Bézier path data, and bakes element transforms into the path — all things VectorDrawable can't express directly.

**To add a theme:** drop `icons-src/<id>.svg` in, re-run the converter, add `theme_<id>_name` and a `labels_<id>` array to every `values*/strings.xml`, and add one `ThemeDef` entry using `R.drawable.ic_theme_<id>`. The theme id, the SVG filename and the drawable name are kept identical on purpose; `ThemeDefsTest` asserts that, so a mismatch fails the build's test run rather than rendering a blank card.

## Languages

The app ships English and Croatian. Every user-visible string lives in `res/values/strings.xml`; Croatian overrides sit in `res/values-hr/strings.xml`.

Croatian deliberately translates only part of the set. Keys it leaves out fall back to the English file, which is Android's normal behaviour and avoids maintaining a duplicate that has to be re-edited whenever the English changes:

- `app_name` and `home_brand` are the product name.
- `gate_equation` is nothing but `%1$d`/`%2$d` placeholders.
- The 112 image labels are stand-in text for artwork the app does not ship yet, so translating them would be translating scaffolding.

Two icon-glyph strings the app draws as text (the gear, tick, house and the two nav arrows) are kept in code rather than resources — they are symbols, not words.

### Switching language

Parent Settings has a language dropdown above the theme list, behind the parental gate. "Same as phone settings" is the first entry in the same list as the languages, each named in its own language (`Hrvatski`, not `Croatian`). A dropdown rather than a row of options so that shipping a fourth or tenth language costs no extra room and changes nothing about how the screen reads. Selecting one applies immediately — Compose recomposes against the new configuration, so nothing restarts.

The choice is stored by `AppCompatDelegate.setApplicationLocales()`, which is why the app runs an `AppCompatActivity` on a `Theme.AppCompat` parent rather than a bare `ComponentActivity`. AppCompat persists the selection, survives process death, and on Android 13+ registers it with the system's own per-app language screen. Going straight to `LocaleManager` would avoid the dependency but is API 33+, and `minSdk` here is 26.

`res/xml/locales_config.xml` lists the shipped languages for Android 13+; `AppLocales.SUPPORTED` lists them for the in-app picker. Nothing links the two at compile time, so `LocalesConfigTest` asserts they match.

### Adding a language

1. Copy `res/values/strings.xml` to `res/values-<code>/strings.xml` and translate the values, leaving every `name="..."` alone. Omit any key that should stay as the English default.
2. Add the code to `res/xml/locales_config.xml` **and** to `AppLocales.SUPPORTED`.
3. Run the tests — `ThemeDefsTest` checks every shipped language has a non-blank, unique name per theme and exactly `LABELS_PER_THEME` labels per array, so a dropped array item fails there rather than crashing the Viewer.

Card names render on one line and ellipsize rather than wrap, so keep theme names short; `Construction` is about the practical limit at the current card width.

### On images

The Viewer never shows real photos — there are none bundled with the app. Each "image" is a short text label (e.g. "Red sports car, side view") rendered on a themed striped placeholder card. This is intentional: it avoids bundling or fetching any copyrighted or externally-sourced photos. Swapping in real photography per item would be the natural next step if this app were to ship.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), single `ComponentActivity`
- One `AndroidViewModel` (`AppViewModel`) holds all app state: current screen, selected theme, viewer position, enabled themes, and the active gate question
- No navigation library, no networking, no local database — `SharedPreferences` holds the enabled-theme set, and `AppCompatDelegate` holds the language choice
- `androidx.appcompat` is present for one reason: per-app language selection below Android 13
- Gradle Kotlin DSL, AGP's built-in Kotlin support (no separate `org.jetbrains.kotlin.android` plugin)

## Project structure

```
app/src/main/java/com/kidsexplore/app/
├── MainActivity.kt              # hosts Compose content, switches on Screen
├── AppViewModel.kt              # screen state machine, gate logic, persistence
├── model/
│   └── ThemeDef.kt              # theme data + the 14 THEME_DEFS entries
├── AppLocales.kt                # language selection, backed by AppCompatDelegate
└── ui/
    ├── theme/
    │   ├── OklchColor.kt        # OKLCH → sRGB conversion, per-theme palettes
    │   └── Theme.kt             # MaterialTheme wrapper, type styles
    ├── icons/
    │   └── ThemeIcons.kt        # draws a theme's vector-drawable icon
    └── screens/
        ├── HomeScreen.kt
        ├── ViewerScreen.kt
        ├── GateScreen.kt
        └── SettingsScreen.kt

res/values/strings.xml           # all user-visible text (English)
res/values-hr/strings.xml        # Croatian overrides; missing keys fall back
res/xml/locales_config.xml       # languages the app ships
icons-src/                       # source SVGs, one per theme id
tools/svg2vd.py                  # icons-src/*.svg -> res/drawable/ic_theme_*.xml
```

## Building & running

Requires JDK 17+ and the Android SDK (compileSdk/targetSdk 37).

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Open the project in Android Studio to run it directly on a device or emulator, or install manually with `adb install`.

## Tests

With an emulator or device connected:

```bash
./gradlew connectedDebugAndroidTest
```

That runs the whole suite and writes an HTML report to `app/build/reports/androidTests/connected/debug/index.html`.

The tests are instrumented rather than plain JVM tests because they need a real `Context` — the ViewModel reads and writes `SharedPreferences`, and the UI tests render real Compose content.

| File | Covers |
|---|---|
| `AppViewModelTest` | Screen transitions, image paging and wrap-around at both ends, gate question generation and answer handling, theme toggling, and that disabled themes survive a ViewModel restart. |
| `KidsExploreFlowTest` | The end-to-end journey through the real screens: Home → Viewer (paging by button and by swipe) → Home → gate (wrong answer, cancel, correct answer) → Settings (toggle a theme) → Home, asserting the grid updates. |
| `ViewerLayoutTest` | The Viewer's per-orientation layout. Drives `LocalConfiguration` directly instead of rotating the device (the test host activity doesn't reliably follow rotation), so both the portrait and landscape pill-button branches are exercised deterministically. |

To run a single class:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kidsexplore.app.KidsExploreFlowTest
```

## Known limitations

- Placeholder text labels stand in for real images (see [On images](#on-images) above).
- No confirmation/undo when a parent disables a theme a child was mid-viewing — they're just returned to Home the next time they tap Home.
- Font is the system sans-serif at heavy weights, approximating the source design's Nunito 800/900; no font file is bundled.
