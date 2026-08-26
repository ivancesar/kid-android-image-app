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
- The picture itself, in a card framed by a diagonal two-tone stripe pattern in the theme's colors. The card is cut to the photograph's own aspect ratio — the largest rectangle of that shape the space allows — so a portrait and a landscape shot each fill what they can without letterboxing, and both grow to fill a tablet. The stripes survive as a thin frame, which is the only thing on this otherwise dark screen that says which category a child is in.
- A theme with no photography keeps the original placeholder: the same striped card, filling the space, with the item's label in monospace text, centered. Every theme ships pictures now, so nothing on Home reaches it — see [On images](#on-images) below.
- The same labelled "◀ Back" / "▶ Next" pill buttons are used at every size, so the controls look and behave identically regardless of how the device is held. The arrows and the swipe direction mirror in an RTL locale, where the sequence advances right-to-left. Which arrangement is used depends on the window's width, not its orientation — the breakpoint is 600dp:
  - **Narrow (under 600dp)**: the card fills the remaining space above a button row, with Back/Next side by side underneath it.
  - **Wide (600dp and up)**: Back/Next flank the card in a single row — the card sits between them rather than under them — and the row fills the full screen height (down to, but not under, the status bar), so the image gets as much vertical room as the display allows. The Home button floats over the top-left corner of the image instead of sitting in its own header row, since there's no header row to spare the height for.
- Either way, Back/Next cycle through the theme's items, wrapping around at both ends. Counts are per theme and range from 7 (Dinosaurs) to 24 (Animals). The placeholder card announces its label to a screen reader as a live region; a photograph announces nothing (see [On images](#on-images)).
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
- An **Attribution** section sits at the foot of the list: one notice per image source, each saying which categories it covers (Unsplash for every category but Space, NASA for Space), plus the Unsplash photographers' names — 183 of them, clamped to three lines behind a **Show more** control so the credits do not sit between a parent and the end of the screen. The control appears only when the list actually overflows, so a shorter roster or a wider window simply shows the lot. Behind the gate rather than on Home, because it is a notice for the adult who installed the app; last in the list, because nothing in it is an action and it should not sit between a parent and the controls that are. The Unsplash License asks for no credit at all, so the names are a courtesy — the notice is the part that has to stay.

## Themes

Fourteen fixed themes, each with a hue, an icon, and — in `strings.xml` — a name and a set of item labels. Construction carries 14, Space 17; the rest carry 8. `ThemeDef` holds only ids, resource ids, a list of image drawables and a `labelCount`, which is what keeps `AppViewModel` free of Android: it pages an index, the UI resolves the text. `ThemeResourcesTest` asserts each `labelCount` matches both its label array and its image list, since nothing else in the build ties those files together.

| Theme | id | Hue | Items | Artwork |
|---|---|---|---|---|
| Cars | `cars` | 15 | 14 | photographs (Unsplash) |
| Construction | `construction` | 45 | 14 | photographs (Unsplash) |
| Trains | `trains` | 350 | 15 | photographs (Unsplash) |
| Animals | `animals` | 320 | 25 | photographs (Unsplash) |
| Birds | `bird` | 225 | 19 | photographs (Unsplash) |
| Insects | `insects` | 200 | 14 | photographs (Unsplash) |
| Ocean | `ocean` | 175 | 17 | photographs (Unsplash) |
| Farm | `farm` | 75 | 17 | photographs (Unsplash) |
| Dinosaurs | `dinosaurs` | 285 | 7 | photographs (Unsplash) |
| Flowers | `flowers` | 296 | 14 | photographs (Unsplash) |
| Forest | `forest` | 150 | 19 | photographs (Unsplash) |
| Fruit | `fruit` | 100 | 12 | photographs (Unsplash) |
| Vegetables | `vegetable` | 125 | 14 | photographs (Unsplash) |
| Space | `space` | 250 | 17 | photographs (NASA) |

Names shown are the English ones; a theme's display name and its labels are `@StringRes`/`@ArrayRes` ids, so both translate. See **Languages** below. No theme's labels reach a screen while it has photographs, though — see [On images](#on-images).

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

**To add a theme:** drop `icons-src/<id>.svg` in, re-run the converter, add `theme_<id>_name` and a `labels_<id>` array to every `values*/strings.xml`, and add one `ThemeDef` entry using `R.drawable.ic_theme_<id>`. Photographs are optional and separate — see [On images](#on-images). The theme id, the SVG filename and the resource names are kept identical on purpose. `ThemeResourcesTest` (instrumented, needs a device) asserts the resource names match the id; the `checkIconsInSync` Gradle task (wired into `check`, so it runs in `./gradlew build`) re-runs the converter and fails if any committed drawable disagrees with its source.

## Languages

The app ships English and Croatian. Every user-visible string lives in `res/values/strings.xml`; Croatian overrides sit in `res/values-hr/strings.xml`.

Croatian deliberately translates only part of the set. Keys it leaves out fall back to the English file, which is Android's normal behaviour and avoids maintaining a duplicate that has to be re-edited whenever the English changes:

- `app_name` and `home_brand` are the product name.
- `gate_equation` is nothing but `%1$d`/`%2$d` placeholders.
- `attribution_photographers` is a list of names.
- No image label reaches a screen in Croatian. Every theme ships photographs, and a theme with photographs shows the picture and nothing else — translating a roster nothing displays would be translating scaffolding.

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

A theme either ships photographs or it does not, and each one decides for itself. All fourteen now do — 217 pictures in total, `img_<id>_01.jpg` upward in `res/drawable-nodpi/`, listed in display order by that theme's `<ID>_IMAGES` list in `ThemeDef.kt`:

| Themes | Images | Source |
|---|---|---|
| All but Space | 200, from 7 (Dinosaurs) to 24 (Animals) | [Unsplash](https://unsplash.com), under the [Unsplash License](https://unsplash.com/license) |
| Space | 17 | [NASA](https://www.nasa.gov/) |

The counts differ on purpose — a theme carries however many good images it has — which is why `labelCount` is per theme. **Parent Settings → Attribution** carries a notice per source saying which categories it covers, plus the Unsplash photographers' names.

That artwork is most of the download: the release APK is 40 MB with it and 1.4 MB with none of it. Play's limit for an app bundle's base download is far above that, so this is a cost rather than a problem — but if it needs to come down, in rough order of effort:

1. **Re-encode as lossy WebP**, files staying in `drawable-nodpi`. No code change at all: `R.drawable` and `painterResource` do not care about the container, and WebP has been decodable since long before this app's `minSdk` of 26. Typically 25–35% off a JPEG-q55 set. Note that quality 55 being at the knee of the curve is a claim about the *JPEG* quality scale — it is not an argument against a better codec.
2. **Resample to 1080px on the long edge.** The card is inset by its striped frame inside the screen padding, so the drawn width is well under 1280 on most phones.
3. **Fewer pictures per theme.**

An install-time asset pack is *not* the lever it looks like: install-time packs are delivered with the app at install, so they do not reduce the download at all, and an asset pack carries `assets/` rather than `res/` — moving the images into one means giving up `R.drawable` and `painterResource` for `AssetManager`, which changes `ThemeDef.imageRes`'s type and makes `everyPhotographIsDensityIndependent` meaningless. Fast-follow or on-demand packs would shrink the initial download, at the same cost.

The UI tests resolve their fixture with `THEME_DEFS.first { it.imageRes.isNotEmpty() }` rather than naming a theme, so they keep passing whichever themes are photographed.

No theme ships the placeholder, and `ThemeRosterTest.everyThemeShipsPhotographs` fails `./gradlew build` if one does — a child must never land on stand-in text, so a theme reaching the grid without its pictures is a shipping bug rather than a work-in-progress state. That is the assertion a new theme trips until it has been photographed; write the entry and drop the images in together.

It lives in `app/src/test`, not beside its siblings in `androidTest`, on purpose: it only counts entries in `THEME_DEFS` and needs no device, and in `androidTest` it would have run solely under `connectedDebugAndroidTest` — which needs an emulator, is in neither the recommended local command below nor any CI, and so would have gated nothing.

The machinery behind the placeholder is still there — `ThemeDef.imageRes` defaults to empty and the Viewer renders the striped card whenever `currentImage` is null — so a half-added theme renders sensibly, it just does not ship. `ViewerLayoutTest` exercises that path by calling `ViewerScreen` with a null image, since it is no longer reachable from Home.

Every theme keeps its `labels_<id>` array, but nothing displays it. **A photograph is shown undescribed** — no caption over it, no `contentDescription` behind it. This app is for looking at pictures, its labels were written as artwork stand-ins rather than as prose worth reading aloud, and a screen reader announcing "Yellow digger with a big scooping bucket" adds nothing for the child holding the phone. The array survives as the roster of what the theme holds: one entry per image, in the order `ThemeDef` lists them, which is what `ThemeResourcesTest` counts `labelCount` against, and a note to the next maintainer about which photograph is which.

The practical cost is that nothing in the semantics tree says *which* photograph is on screen. The image therefore carries a test tag naming the drawable it is drawing (`viewerImageTestTag(image)`), which is what the tests match on — what matters about this screen is that the expected image loaded, not what is pictured in it.

**To give a theme photographs:**

1. Downscale the sources to 1280px on the long edge and re-encode them. On macOS, with no ImageMagick needed:

   ```bash
   sips -s format jpeg -s formatOptions 55 --resampleHeightWidthMax 1280 in.jpg --out out.jpg
   ```

   That lands each file around 30–460 KB, and a theme's set at roughly 2–4 MB. Skip the resample when a source is already under 1280px — it would only upscale it.
2. Drop them in `res/drawable-nodpi/` as `img_<id>_NN.jpg`, numbered from `01`. **`-nodpi` matters**: a drawable in a plain `drawable/` folder is treated as mdpi artwork and upscaled by the device's density, which decodes a 1280px JPEG into a bitmap several times that size for nothing. `everyPhotographIsDensityIndependent` fails if one lands anywhere else.
3. List them in `ThemeDef.kt` and set the theme's `labelCount` from the list's size.
4. Keep that theme's `labels_<id>` array one entry per image, in the same order, and **look at each picture to write its entry** — the array is the only written record of which photograph sits at which index. It is not displayed while the theme has artwork, but `labelCount` is still checked against it. Translations of it can be dropped — nothing shows them.
5. Add any new photographers to `attribution_photographers`. The list is derived from the Unsplash source filenames, which are ASCII-folded, so a name with a diacritic needs correcting by hand against the photographer's profile.

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

res/drawable-nodpi/              # bundled photographs, img_<theme>_NN.jpg
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
| `KidsExploreFlowTest` | `androidTest` | The end-to-end journey through the real screens: Home → Viewer (paging by button and by swipe, through a theme's whole set of photographs) → Home → gate (wrong answer, lockout, cancel, correct answer) → Settings (toggle a theme) → Home, asserting the grid updates. Photographs carry no description, so every paging assertion matches the drawable it should be showing, by the test tag naming it. |
| `ViewerLayoutTest` | `androidTest` | The Viewer's layout at both sides of the 600dp breakpoint, using `DeviceConfigurationOverride(ForcedSize(...))` so a window wider than the test device still renders on screen. Asserts the buttons actually sit beside the image when wide and below it when narrow, rather than only that a branch was taken — and that both the button order and the swipe direction mirror under an RTL override. Covers both card kinds: that a photograph shows its label in no form at all, that it does not swallow the swipe, and that the wide layout holds for it too. |
| `SettingsBehaviourTest` | `androidTest` | The language dropdown, the empty Home state, the attribution notice at the foot of the settings list, and the credit list's collapse/expand control. |
| `SharedPreferencesThemeStoreTest` | `androidTest` | The real store against real preferences: themes and the gate lock round-tripping through a *second* store instance, the two keys not treading on each other, ids for removed themes being pruned on read, and a lockout surviving a relaunch through actual SharedPreferences rather than a fake. |
| `ThemeResourcesTest` | `androidTest` | Holds `ThemeDef`, `strings.xml` and the bundled photographs together, for everything that needs real resources: every theme's `labelCount` matches the length of its string array, names and labels are non-blank, ids and names are unique, and every photograph is named after its theme and position, decodes, and lives in `drawable-nodpi`. |
| `ThemeRosterTest` | `test` | The two roster facts that need no device, so a plain `./gradlew build` gates them: every theme ships photographs, and a photographed theme has exactly one per label. |

To run a single instrumented class:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kidsexplore.app.KidsExploreFlowTest
```

There is no CI; run `./gradlew lint testDebugUnitTest assembleDebug` locally, and the instrumented suite against a device or emulator, before releasing.

## Known limitations

- The artwork dominates the download: 38.5 MB of the 40 MB release APK is bundled JPEGs (see [On images](#on-images) above). That is a deliberate trade for now — well inside Play's limits, and the pictures are the product. Lossy WebP is the first lever if it stops being acceptable; it needs no code change, only an encoder, which `sips` is not (it reads WebP but cannot write it).
- The photographer credits in `attribution_photographers` are derived from Unsplash filenames, which are ASCII-folded — names carrying diacritics are currently spelled without them.
- The Viewer decodes each photograph with `painterResource`, which has no downsampling, no caching across compositions and no preloading, so every Back/Next/swipe blocks a frame on a full JPEG decode. At 1280px that is a ~6.6 MB `ARGB_8888` bitmap against the app heap, and `drawable-nodpi` correctly prevents density upscaling but still decodes the full 1280px onto a card that draws far narrower. Now that all fourteen themes are photographed this is the next thing to reach for: `BitmapFactory.Options.inSampleSize` sized to the card, off the main thread, behind a small `LruCache` — or an image loader.
- Nothing stops a child leaving the app for the launcher. The gate protects **Settings**, not the app's boundary; Android's screen pinning is what would deliver that, and it is not wired up.
- No confirmation/undo when a parent disables a theme a child was mid-viewing — they're just returned to Home the next time they tap Home.
- Font is the system sans-serif at heavy weights, approximating the source design's Nunito 800/900; no font file is bundled.
- Light theme only, by design: the palette is a fixed bright one with no dark counterpart, so the colour scheme is pinned rather than following the system setting.
