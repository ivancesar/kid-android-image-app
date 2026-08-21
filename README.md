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

There is no back-stack navigation library involved — the current screen is a single sealed `UiState` on the app's `ViewModel`, and each screen is a plain Compose function that reads it. Modelling it as a sealed type rather than a `Screen` enum plus loose `themeId`/`imageIndex`/`gate` fields means a Viewer cannot exist without a theme and a Gate cannot exist without a question, so there is no combination the UI has to render as a blank screen.

The system Back button returns to Home from any screen; on Home it leaves the app, as usual.

The app rotates freely between portrait and landscape (no orientation lock). Screen state lives in a `ViewModel` and is additionally written to a `SavedStateHandle`, so the current theme and image survive not just rotation but the process being killed in the background. Settings is deliberately *not* restored — coming back into the parent screen without passing the gate again would defeat it.

Layouts reflow rather than using dedicated landscape-specific arrangements — the Home grid and Settings list scroll, and the Gate screen falls back to scrolling instead of clipping if its content doesn't fit the available height. Where a layout does branch, it branches on the width actually available rather than on device orientation, so a tablet held upright and a split-screen window get the layout that fits them.

The app draws edge to edge (required from `targetSdk` 35 on). Screens pad their own content with `WindowInsets.safeDrawing`, and the system-bar icons switch between light and dark to stay legible against whichever screen is showing.

## Screens

### Home

- Header: "KIDS EXPLORE" label, a settings gear button (top right) that opens the **parental gate**, and the "Pick something to look at!" title.
- The header fades out once the grid is scrolled away from the top, and fades back in when scrolled back — it floats over the grid rather than sharing layout space with it, so its own size never affects the grid's, which would otherwise cause the list to bounce as it hid/showed itself.
- The gear is pinned rather than part of the fading header: it is the app's only route into parent settings, and it used to disappear the moment the grid scrolled.
- A grid of theme cards — one per enabled theme, with as many columns as fit the available width (2 in portrait, more in landscape or on a wider screen) rather than a fixed count. Each card shows a themed icon on a near-white circle, the theme name, and a color pair (fill + a darker bottom accent stripe) derived from the theme's hue. The circle takes all the height the name leaves it, so the icon is as large as the card allows and the card colour reads as a frame around it.
- Only themes enabled in **Settings** appear here; disabling a theme removes it from this grid immediately. If a parent switches every theme off, the grid is replaced by a short message pointing back at parent settings.
- Tapping a card opens the **Viewer** for that theme.

### Viewer

- Dark full-bleed background.
- A small square "Home" button returns to Home — that's the entire header here, no theme name shown, to keep the focus on the image.
- A large rounded placeholder card, with a diagonal two-tone stripe pattern in the theme's colors, showing the current item's label in monospace text, centered.
- The same labelled "◀ Back" / "▶ Next" pill buttons are used at every size, so the controls look and behave identically regardless of how the device is held. The arrows and the swipe direction mirror in an RTL locale, where the sequence advances right-to-left. Which arrangement is used depends on the window's width, not its orientation — the breakpoint is 600dp:
  - **Narrow (under 600dp)**: the card fills the remaining space above a button row, with Back/Next side by side underneath it.
  - **Wide (600dp and up)**: Back/Next flank the card in a single row — the card sits between them rather than under them — and the row fills the full screen height (down to, but not under, the status bar), so the image gets as much vertical room as the display allows. The Home button floats over the top-left corner of the image instead of sitting in its own header row, since there's no header row to spare the height for.
- Either way, Back/Next cycle through the theme's 8 items, wrapping around at both ends.
- The card also responds to a horizontal swipe — swipe left for next, right for back — as an alternative to the buttons, in both orientations.

### Parental Gate ("Grown-ups only")

- A simple math question: two random numbers between 2 and 7, added together (`a + b = ?`).
- Four answer buttons in a 2×2 grid — the correct sum plus three distinct wrong values, none of them within 1 of the answer. Keeping the distractors adjacent to the sum rewarded a child who could nearly add.
- Tapping the correct answer opens **Settings**. Tapping a wrong answer shows "Not quite, try again!" **and replaces the question** — otherwise a child reaches Settings by exhausting all four buttons.
- After 3 wrong answers the gate stops accepting taps for 30 seconds and shows a countdown. The lockout is enforced in the ViewModel, not just by disabling the buttons, and both it and the failure count are persisted through `ThemeStore` — so they survive Cancel, and they survive the app being closed and relaunched. That last part matters more than it sounds: saved instance state is discarded when the Activity finishes, and Back from Home finishes it, so holding this in a `SavedStateHandle` meant a child could clear a lockout with two taps and reopen to a fresh set of free guesses. A correct answer clears the failure count.
- Because the deadline outlives the process it is wall-clock rather than elapsed-realtime, which would be measured from boot. A stored deadline more than one lockout in the future means the device clock moved backwards; it is pulled back on load, so a clock change cannot lock a parent out of their own settings.
- "Cancel" returns to Home without opening Settings.
- A fresh question is generated every time the gate is opened.

### Settings ("Parent Settings")

- A scrollable list of **all** themes (regardless of current enabled state), each as a row with a checkbox and the theme name.
- Tapping a row toggles that theme's visibility on the Home screen.
- Enabled/disabled state is persisted to `SharedPreferences`, so it survives app restarts.
- "Done" returns to Home, where the grid now reflects the updated theme selection.
- A language dropdown sits above the theme list — "Same as phone settings" plus each shipped language named in its own language. Selecting one applies immediately.
- "Language" and "Categories" section headings separate the two, so the language row does not read as the first entry in the category list. "Choose which themes your child can see." sits under the Categories heading as that section's explanation, rather than under the screen title where it described only half the screen.

## Themes

Fourteen fixed themes, each with a hue, an icon, and — in `strings.xml` — a name and 8 item labels. `ThemeDef` holds only ids, resource ids and a `labelCount`, which is what keeps `AppViewModel` free of Android: it pages an index, the UI resolves the text. `ThemeResourcesTest` asserts each `labelCount` matches its array, since nothing else in the build ties the two files together.

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
| Flowers | `flowers` | 296 |
| Forest | `forest` | 150 |
| Fruit | `fruit` | 100 |
| Vegetables | `vegetable` | 125 |
| Space | `space` | 250 |

Names shown are the English ones; a theme's display name and its 8 labels are `@StringRes`/`@ArrayRes` ids, so both translate. See **Languages** below.

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

It deliberately supports only the subset this icon set uses, and raises on anything else rather than guessing: group-level fills or classes, `style="..."` attributes, `fill-rule`/`clip-rule`, opacity, a non-zero `viewBox` origin, and colours carrying alpha. A silently mis-converted icon looks plausible and ships; a refusal costs one line of support code. Pass `--check` to verify the committed drawables without writing anything.

**To add a theme:** drop `icons-src/<id>.svg` in, re-run the converter, add `theme_<id>_name` and a `labels_<id>` array to every `values*/strings.xml`, and add one `ThemeDef` entry using `R.drawable.ic_theme_<id>`. The theme id, the SVG filename and the resource names are kept identical on purpose. `ThemeResourcesTest` (instrumented, needs a device) asserts the resource names match the id; the `checkIconsInSync` Gradle task (wired into `check`, so it runs in `./gradlew build`) re-runs the converter and fails if any committed drawable disagrees with its source.

## Languages

The app ships English and Croatian. Every user-visible string lives in `res/values/strings.xml`; Croatian overrides sit in `res/values-hr/strings.xml`.

Croatian deliberately translates only part of the set. Keys it leaves out fall back to the English file, which is Android's normal behaviour and avoids maintaining a duplicate that has to be re-edited whenever the English changes:

- `app_name` and `home_brand` are the product name.
- `gate_equation` is nothing but `%1$d`/`%2$d` placeholders.
- The 112 image labels are stand-in text for artwork the app does not ship yet, so translating them would be translating scaffolding.

Six glyphs the app draws as text — the gear, tick, house, dropdown chevron and the two nav arrows — are kept in code rather than resources. They are symbols, not words. The controls carrying them are announced by name rather than by glyph. The gear is icon-only and declares a `contentDescription`; every other container-level control — theme card, theme row, language row, Home and the nav pills — takes its name from the label inside it, which `clickable` and `toggleable` merge into the control's own semantics node (`AbstractClickableNode.shouldMergeDescendantSemantics` is `final` and `true`). `AccessibleNamesTest` covers four of them — the theme card, the gear, a theme row and the language row — asserting each ends up with a name and a click action.

### Switching language

Parent Settings has a language dropdown above the theme list, behind the parental gate. "Same as phone settings" is the first entry in the same list as the languages, each named in its own language (`Hrvatski`, not `Croatian`). A dropdown rather than a row of options so that shipping a fourth or tenth language costs no extra room and changes nothing about how the screen reads. Selecting one applies immediately: AppCompat recreates the activity against the new configuration. The `AppViewModel` survives that, so the current screen and position are kept and the change looks instant.

The choice is stored by `AppCompatDelegate.setApplicationLocales()`, which is why the app runs an `AppCompatActivity` on a `Theme.AppCompat` parent rather than a bare `ComponentActivity`. Going straight to `LocaleManager` would avoid the dependency but is API 33+, and `minSdk` here is 26.

On Android 13+ the framework owns the storage and registers the choice with the system's own per-app language screen. **Below 13 there is no such store, and AppCompat only keeps its own if the app opts in** by declaring `AppLocalesMetadataHolderService` with `autoStoreLocales=true` in the manifest. Without that declaration the language applies for the session and is silently lost on the next cold start — on exactly the API range (26–32) the dependency exists to serve.

`res/xml/locales_config.xml` lists the shipped languages for Android 13+; `AppLocales.SUPPORTED` lists them for the in-app picker. Nothing links the two at compile time, so `LocalesConfigTest` asserts they match.

### Adding a language

1. Copy `res/values/strings.xml` to `res/values-<code>/strings.xml` and translate the values, leaving every `name="..."` alone. Omit any key that should stay as the English default.
2. Add the code to `res/xml/locales_config.xml` **and** to `AppLocales.SUPPORTED`.
3. Run the tests. `ThemeResourcesTest` checks every shipped language for a non-blank, unique name per theme and for label arrays that still match the default's length — arrays replace rather than merge, so a dropped item would otherwise leave the Viewer paging onto an index that no longer exists. It cannot detect a language that simply isn't translated; fallback makes that indistinguishable from a deliberate omission, which is why `croatianTranslatesEverythingExceptTheKeysItDeliberatelyLeaves` pins that separately.

Card names wrap to two lines and ellipsize only past that, so a long name costs the icon some height rather than losing characters — `Construction` was clipped to `Constructi…` at 2x font scale when it was one line.

### On images

The Viewer never shows real photos — there are none bundled with the app. Each "image" is a short text label (e.g. "Red sports car, side view") rendered on a themed striped placeholder card. This is intentional: it avoids bundling or fetching any copyrighted or externally-sourced photos. Swapping in real photography per item would be the natural next step if this app were to ship.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), single `AppCompatActivity`
- One `ViewModel` (`AppViewModel`) holds all app state as a sealed `UiState`, plus the set of disabled themes and the gate's failure/lockout counters
- User-facing text, including every theme name and item label, lives in `strings.xml`
- Persistence goes through a `ThemeStore` interface; `SharedPreferencesThemeStore` is the only implementation. That seam is what lets the entire state machine be tested off-device. The language choice is the exception: `AppCompatDelegate` owns that store
- `androidx.appcompat` is present for one reason: per-app language selection below Android 13, which is also why the activity is an `AppCompatActivity` on a `Theme.AppCompat` parent
- No navigation library, no networking, no local database
- Dependencies are declared in a Gradle version catalog (`gradle/libs.versions.toml`)
- Gradle Kotlin DSL, AGP's built-in Kotlin support (no separate `org.jetbrains.kotlin.android` plugin)

## Project structure

```
app/src/main/java/com/kidsexplore/app/
├── MainActivity.kt              # hosts Compose content, switches on UiState
├── AppViewModel.kt              # UiState machine, gate logic, saved state
├── data/
│   └── ThemeStore.kt            # persistence seam (themes + gate lock) + SharedPreferences impl
├── model/
│   └── ThemeDef.kt              # theme ids/hues/icons + the 14 THEME_DEFS entries
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

The state machine runs as plain JVM tests, no device needed:

```bash
./gradlew testDebugUnitTest
```

The Compose UI needs an emulator or device connected:

```bash
./gradlew connectedDebugAndroidTest
```

Reports land in `app/build/reports/tests/testDebugUnitTest/index.html` and `app/build/reports/androidTests/connected/debug/index.html`.

| File | Source set | Covers |
|---|---|---|
| `AppViewModelTest` | `test` (JVM) | The whole state machine: transitions, paging and wrap-around, unknown theme ids, gate question generation over 500 seeds, the lockout (driven by a hand-advanced clock rather than a 30-second wait), theme toggling, `visibleThemes` caching, and restore-from-process-death including out-of-range indices. `AppViewModel` takes its store, its `Random` and its clock as parameters, so none of this touches Android. |
| `GateLockPersistenceTest` | `test` (JVM) | The gate's durability, which is the one thing protecting Settings: that the lockout **and** the failure count survive the app being closed and relaunched, that the lockout still expires on its own, that a correct answer clears it, that a backwards device clock cannot strand a parent, and that the question rotates on every wrong answer. A relaunch is modelled the way Android behaves — same store, fresh `SavedStateHandle`. |
| `KidsExploreFlowTest` | `androidTest` | The end-to-end journey through the real screens: Home → Viewer (paging by button and by swipe) → Home → gate (wrong answer, lockout, cancel, correct answer) → Settings (toggle a theme) → Home, asserting the grid updates. |
| `ViewerLayoutTest` | `androidTest` | The Viewer's layout at both sides of the 600dp breakpoint, using `DeviceConfigurationOverride(ForcedSize(...))` so a window wider than the test device still renders on screen. Asserts the buttons actually sit beside the image when wide and below it when narrow, rather than only that a branch was taken — and that both the button order and the swipe direction mirror under an RTL override. |
| `SharedPreferencesThemeStoreTest` | `androidTest` | The real store against real preferences: themes and the gate lock round-tripping through a *second* store instance, the two keys not treading on each other, ids for removed themes being pruned on read, and a lockout surviving a relaunch through actual SharedPreferences rather than a fake. |
| `ThemeResourcesTest` | `androidTest` | Holds `ThemeDef` and `strings.xml` together: every theme's `labelCount` matches the length of its string array, names and labels are non-blank, and ids and names are unique. |

To run a single instrumented class:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kidsexplore.app.KidsExploreFlowTest
```

There is no CI; run `./gradlew lint testDebugUnitTest assembleDebug` locally, and the instrumented suite against a device or emulator, before releasing.

## Known limitations

- Placeholder text labels stand in for real images (see [On images](#on-images) above).
- Nothing stops a child leaving the app for the launcher. The gate protects **Settings**, not the app's boundary; Android's screen pinning is what would deliver that, and it is not wired up.
- No confirmation/undo when a parent disables a theme a child was mid-viewing — they're just returned to Home the next time they tap Home.
- Font is the system sans-serif at heavy weights, approximating the source design's Nunito 800/900; no font file is bundled.
- Only English strings are shipped. The text is all in `strings.xml` now, so a translation is a matter of adding `values-<locale>/`; the Viewer's controls already mirror for RTL.
- Light theme only, by design: the palette is a fixed bright one with no dark counterpart, so the colour scheme is pinned rather than following the system setting.
