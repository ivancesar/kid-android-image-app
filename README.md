# Kids Explore

A native Android app for kids to safely browse a curated set of picture categories on a parent's phone. Everything a child can reach is a fixed, parent-controlled list of themes — there's no search, no internet browsing, and no way to add or remove content from the child-facing side.

Built with Kotlin + Jetpack Compose. Ported from a Claude Design prototype (`Kids Image Browser.dc.html`).

[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/N6V325T57X)

## Overview

The app is a single activity with four screens, driven by one piece of state (`UiState.Home | Viewer | Gate | Settings`):

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
- A grid of theme cards — one per enabled theme, with as many columns as fit the available width rather than a fixed count: `GridCells.Adaptive(minSize = 160.dp)`, 14dp between columns, 22dp of content padding either side. Two columns therefore need 378dp of window — `(width − 44 + 14) / (160 + 14) ≥ 2` — which a 393dp or 411dp phone clears and a 360dp one, which most budget Android still is, does not. So the portrait grid is two columns on a mid-range phone and a single column on a small one; landscape and tablets get more. Each card shows a themed icon on a near-white circle, the theme name, and a color pair (fill + a darker bottom accent stripe) derived from the theme's hue. The circle takes all the height the name leaves it, so the icon is as large as the card allows and the card colour reads as a frame around it.
- Only themes enabled in **Settings** appear here; disabling a theme removes it from this grid immediately. If a parent switches every theme off, the grid is replaced by a short message pointing back at parent settings.
- Tapping a card opens the **Viewer** for that theme.

### Viewer

- Dark full-bleed background.
- A small square "Home" button returns to Home — that's the entire header here, no theme name shown, to keep the focus on the image.
- The picture itself, in a card framed by a diagonal two-tone stripe pattern in the theme's colors. The card is cut to the photograph's own aspect ratio — the largest rectangle of that shape the space allows — so a portrait and a landscape shot each fill what they can without letterboxing, and both grow to fill a tablet. The stripes survive as a thin frame, which is the only thing on this otherwise dark screen that says which category a child is in.
- There is no second kind of card. A photograph is the whole content of this screen, and a theme without one cannot be declared — see [On images](#on-images) below.
- The same labelled "◀ Back" / "▶ Next" pill buttons are used at every size, so the controls look and behave identically regardless of how the device is held. The arrows and the swipe direction mirror in an RTL locale, where the sequence advances right-to-left. Which arrangement is used depends on the window's width, not its orientation — the breakpoint is 600dp:
  - **Narrow (under 600dp)**: the card fills the remaining space above a button row, with Back/Next side by side underneath it.
  - **Wide (600dp and up)**: Back/Next flank the card in a single row — the card sits between them rather than under them — and the row fills the full screen height (down to, but not under, the status bar), so the image gets as much vertical room as the display allows. The Home button floats over the top-left corner of the image instead of sitting in its own header row, since there's no header row to spare the height for.
- Either way, Back/Next cycle through the theme's photographs, wrapping around at both ends. Counts are per theme and range from 7 (Dinosaurs) to 22 (Birds). A photograph announces nothing to a screen reader (see [On images](#on-images)).
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
- A **Privacy policy** link sits below the categories, opening the policy **inside the app** as its own screen. Behind the gate deliberately: Play's Families policy wants the policy reachable from within the app, and equally does not want a child one tap from the open web — rendering it here satisfies the first without going near the second. Above Attribution because it is an action and Attribution is not; below the categories because it is not what a parent came in here to do.

### Privacy policy

- The document a parent reads is `docs/privacy-policy.md` itself. Gradle stages that file as an app asset (`StagePrivacyPolicy` in `app/build.gradle.kts`, wired through the variant API), so the copy Play hosts and the copy the app renders cannot drift — there is only one file to edit.
- `model/PolicyDocument.kt` parses it: titles, section headings, paragraphs, bullets, bold, and links. Deliberately not a Markdown library — the document is one known file written in this repository, and a general parser would be a dependency and a much larger surface to be wrong about in an app whose pitch is that it carries nothing it does not need. The Markdown that file uses must stay within what the parser handles; `PolicyDocumentTest` is where those rules are written down.
- HTML comments are stripped whole, because the source file opens with a note to whoever edits it next. `PolicyScreenTest.theEditorsNoteIsNotShipped` pins that.
- Links keep their label and lose their address. Nothing in the app opens a browser — which is the reason the policy is bundled at all — so an underlined address a reader cannot follow would be worse than plain text. The two addresses the document wants a reader to have are written out in full in its source.
- **Done** returns to Settings, and so does Back. Back going Home from here would make it the one control that discards where a parent was and costs them the gate again.
- Not restored after process death, for the same reason Settings is not: it is only reachable through the gate.

- An **Attribution** section sits at the foot of the list: one notice per image source, each saying which categories it covers (Unsplash for every category but Space, NASA for Space), plus the Unsplash photographers' names — 183 of them, clamped to three lines behind a **Show more** control so the credits do not sit between a parent and the end of the screen. The control appears only when the list actually overflows, so a shorter roster or a wider window simply shows the lot. Behind the gate rather than on Home, because it is a notice for the adult who installed the app; last in the list, because nothing in it is an action and it should not sit between a parent and the controls that are. The Unsplash License asks for no credit at all, so the names are a courtesy — the notice is the part that has to stay.

## Themes

Fourteen fixed themes, each with a hue, an icon, a set of photographs, and — in `strings.xml` — a name. Counts run from Dinosaurs' 7 to Birds' 22; the table below is the roster. `ThemeDef` holds only the id, the two resource ids, the hue and the list of image drawables, which is what keeps `AppViewModel` free of Android: it pages an index around `imageRes.size` and the UI resolves the drawable. `ThemeResourcesTest` asserts each theme's resources are named after its id and that every photograph decodes, since nothing else in the build ties `ThemeDef.kt` to the files on disk.

| Theme | id | Hue | Photographs | Source |
|---|---|---|---|---|
| Cars | `cars` | 15 | 14 | photographs (Unsplash) |
| Construction | `construction` | 45 | 14 | photographs (Unsplash) |
| Trains | `trains` | 350 | 15 | photographs (Unsplash) |
| Animals | `animals` | 320 | 20 | photographs (Unsplash) |
| Birds | `bird` | 225 | 22 | photographs (Unsplash) |
| Insects | `insects` | 200 | 14 | photographs (Unsplash) |
| Ocean | `ocean` | 175 | 18 | photographs (Unsplash) |
| Farm | `farm` | 75 | 17 | photographs (Unsplash) |
| Dinosaurs | `dinosaurs` | 285 | 7 | photographs (Unsplash) |
| Flowers | `flowers` | 296 | 14 | photographs (Unsplash) |
| Forest | `forest` | 150 | 19 | photographs (Unsplash) |
| Fruit | `fruit` | 100 | 11 | photographs (Unsplash) |
| Vegetables | `vegetable` | 125 | 14 | photographs (Unsplash) |
| Space | `space` | 250 | 17 | photographs (NASA) |

Names shown are the English ones; a theme's display name is a `@StringRes` id, so it translates. See **Languages** below. The photographs do not — they are the same in every locale, which is why they are `@DrawableRes` ids on `ThemeDef` rather than per-locale resources.

They are declared in that order in `THEME_DEFS`, loosely grouped (things that go, creatures, growing things, space), and that one list drives both the Home grid and the Settings list.

Every theme's color palette (card fill, stripe, and border/accent) is generated from just its hue using the same OKLCH formula throughout the app, so palettes stay visually consistent without hand-picked hex values:

- Card fill: `oklch(72% 0.16 hue)`
- Stripe: `oklch(66% 0.17 hue)`
- Border / accent: `oklch(50% 0.19 hue)`
- Label on the card: `oklch(26% 0.10 hue)`

The label tone is the one that is not in the source design. White was, and it reaches only 2.2–2.7:1 against the card fill — under WCAG AA's 3:1 floor for large text, on all fourteen hues. A dark tone derived from the same hue clears 4.5:1 against both the fill and the lighter stripe (worst case 4.73:1), and `CardContrastTest` recomputes that for every hue in `THEME_DEFS`, so a fifteenth theme cannot quietly reintroduce the problem.

### Icons

Each theme's icon is a black-and-white vector drawable in `app/src/main/res/drawable/ic_theme_<id>.xml`, drawn untinted on a near-white disc on the theme's card. The artwork is two-tone by design, so it is deliberately never tinted — a drawable-wide tint would collapse the white detail into the black shapes and leave a flat silhouette.

Those drawables are generated, not hand-written. The source SVGs live in `icons-src/<id>.svg` and are converted by `tools/svg2vd.py`:

```bash
python3 tools/svg2vd.py icons-src app/src/main/res/drawable
```

The converter inlines the SVGs' CSS classes into path attributes, re-expresses `<circle>`/`<rect>`/`<ellipse>` as cubic Bézier path data, and bakes element transforms into the path — all things VectorDrawable can't express directly.

It deliberately supports only the subset this icon set uses, and raises on anything else rather than guessing: group-level fills or classes, `style="..."` attributes, `fill-rule`/`clip-rule`, opacity, a non-zero `viewBox` origin, colours carrying alpha, and any colour keyword but `black` (every other colour in the set is written as a hex literal). A silently mis-converted icon looks plausible and ships; a refusal costs one line of support code. Pass `--check` to verify the committed drawables without writing anything.

**To add a theme:** drop `icons-src/<id>.svg` in, re-run the converter, add a `theme_<id>` string to `values/strings.xml` and to every translation, prepare its photographs (see [On images](#on-images) — they are not optional), and add one `ThemeDef` entry using `R.drawable.ic_theme_<id>` and the theme's image list. The theme id, the SVG filename and the resource names are kept identical on purpose. `ThemeResourcesTest` (instrumented, needs a device) asserts the resource names match the id; the `checkIconsInSync` Gradle task (wired into `check`, so it runs in `./gradlew build`) re-runs the converter and fails if any committed drawable disagrees with its source.

## Languages

The app ships English and Croatian. Every user-visible string lives in `res/values/strings.xml`; Croatian overrides sit in `res/values-hr/strings.xml`.

Croatian deliberately translates only part of the set. Keys it leaves out fall back to the English file, which is Android's normal behaviour and avoids maintaining a duplicate that has to be re-edited whenever the English changes:

- `app_name` and `home_brand` are the product name.
- `gate_question` is nothing but `%1$d`/`%2$d` placeholders.
- `attribution_photographers` is a list of names.

`ThemeResourcesTest.croatianTranslatesEverythingExceptTheKeysItDeliberatelyLeaves` asserts exactly that policy — those four keys identical between `en` and `hr`, everything else different — rather than a count, so it survives a rename and still fails if four translations quietly vanish.

Six glyphs the app draws as text — the gear, tick, house, dropdown chevron and the two nav arrows — are kept in code rather than resources. They are symbols, not words. The controls carrying them are announced by name rather than by glyph. The gear is icon-only and declares a `contentDescription`; every other container-level control — theme card, theme row, language row, Home and the nav pills — takes its name from the label inside it, which `clickable` and `toggleable` merge into the control's own semantics node (`AbstractClickableNode.shouldMergeDescendantSemantics` is `final` and `true`). `AccessibleNamesTest` covers four of them — the theme card, the gear, a theme row and the language row — asserting each ends up with a name and a click action; a fifth case there covers the collapsed credit list, which has to announce its summary rather than all 183 names, since `maxLines` clamps at draw time only and leaves the whole string in the semantics tree.

### Switching language

Parent Settings has a language dropdown above the theme list, behind the parental gate. "Same as phone settings" is the first entry in the same list as the languages, each named in its own language (`Hrvatski`, not `Croatian`). A dropdown rather than a row of options so that shipping a fourth or tenth language costs no extra room and changes nothing about how the screen reads. Selecting one applies immediately: AppCompat recreates the activity against the new configuration. The `AppViewModel` survives that, so the current screen and position are kept and the change looks instant.

The choice is stored by `AppCompatDelegate.setApplicationLocales()`, which is why the app runs an `AppCompatActivity` on a `Theme.AppCompat` parent rather than a bare `ComponentActivity`. Going straight to `LocaleManager` would avoid the dependency but is API 33+, and `minSdk` here is 26.

On Android 13+ the framework owns the storage and registers the choice with the system's own per-app language screen. **Below 13 there is no such store, and AppCompat only keeps its own if the app opts in** by declaring `AppLocalesMetadataHolderService` with `autoStoreLocales=true` in the manifest. Without that declaration the language applies for the session and is silently lost on the next cold start — on exactly the API range (26–32) the dependency exists to serve.

`res/xml/locales_config.xml` lists the shipped languages for Android 13+; `AppLocales.SUPPORTED` lists them for the in-app picker. Nothing links the two at compile time, so `LocalesConfigTest` asserts they match.

### Adding a language

1. Copy `res/values/strings.xml` to `res/values-<code>/strings.xml` and translate the values, leaving every `name="..."` alone. Omit any key that should stay as the English default.
2. Add the code to `res/xml/locales_config.xml` **and** to `AppLocales.SUPPORTED`.
3. Run the tests. `ThemeResourcesTest.everyShippedLanguageKeepsTheSameNames` checks every shipped language for a non-blank, unique name per theme — a translation that dropped one, or gave two themes the same one, would leave Settings ambiguous and Home mislabelled in that language only. `GateLockedStringTest` separately formats every second of a lockout in every language, since that string carries a plural. Neither can detect a language that simply isn't translated: fallback makes that indistinguishable from a deliberate omission, which is why `croatianTranslatesEverythingExceptTheKeysItDeliberatelyLeaves` pins Croatian's policy by hand. A new language gets no such test unless someone writes one.

Card names wrap to two lines and ellipsize only past that, so a long name costs the icon some height rather than losing characters — `Construction` was clipped to `Constructi…` at 2x font scale when it was one line.

### On images

Photographs are a theme's entire content. All fourteen ship them — 216 pictures in total, `img_<id>_01.jpg` upward in `res/drawable-nodpi/`, listed in display order by that theme's `<ID>_IMAGES` list in `ThemeDef.kt`:

| Themes | Images | Source |
|---|---|---|
| All but Space | 199, from 7 (Dinosaurs) to 22 (Birds) | [Unsplash](https://unsplash.com), under the [Unsplash License](https://unsplash.com/license) |
| Space | 17 | [NASA](https://www.nasa.gov/) |

The counts differ on purpose — a theme carries however many good images it has — which is why the ViewModel wraps on `imageRes.size` rather than on any shared number. **Parent Settings → Attribution** carries a notice per source saying which categories it covers, plus the Unsplash photographers' names.

That artwork is most of the download. Measured on the current tree: the release APK is 39.9 MB, of which 38.3 MB is the 216 JPEGs; everything else — code, icons, strings, AppCompat — is 1.6 MB. The release bundle is 41.5 MB.

An install-time asset pack is *not* the lever it looks like: install-time packs are delivered with the app at install, so they do not reduce the download at all, and an asset pack carries `assets/` rather than `res/` — moving the images into one means giving up `R.drawable` and `painterResource` for `AssetManager`, which changes `ThemeDef.imageRes`'s type and makes `everyPhotographIsDensityIndependent` meaningless. Fast-follow or on-demand packs would shrink the initial download, at the same cost.

**A theme cannot land ahead of its photographs.** `ThemeDef.imageRes` has no default and is required at every call site; an entry without a picture list does not compile.

That is a deliberate reversal. A theme could once ship with an empty `imageRes` and the Viewer would draw a striped placeholder card carrying the item's label in monospace text until the pictures arrived. The placeholder is gone, and with it the entire string-array path it existed to display: fourteen `labels_<id>` arrays and 217 `<item>` elements, some 270 lines of `strings.xml` that had rendered nothing on any screen since the last theme was photographed. Keeping the workflow alive was costing a card composable, a chooser in `ViewerScreen`, a nullable image threaded down from `MainActivity`, a JVM test that parsed `strings.xml` off disk with a Gradle input declaration to go with it, a per-theme `labelCount` field that had to be kept equal to `imageRes.size` by an instrumented assertion, and a paragraph of translation policy explaining why none of it was translated — a whole second content model, for a state nothing had been in for months. Croatian never had the arrays at all; they always fell back.

What the reversal gives up is real and worth naming: adding a theme is now one bigger step. The icon and the name cannot land on Monday with the pictures on Friday. Do the whole thing at once, or keep it on a branch.

`ThemeRosterTest.everyThemeShipsPhotographs` is what makes the runtime half of that a `./gradlew build` failure rather than a crash on a child's screen — a `ThemeDef` assembled some other way, by a future loader or a careless test, would otherwise take the Viewer out of range the moment the theme was tapped. It lives in `app/src/test` rather than beside its siblings in `androidTest` on purpose: it needs no device, and in `androidTest` it would have run solely under `connectedDebugAndroidTest` — which needs an emulator, is in neither the recommended local command below nor any CI, and so would have gated nothing. Its three siblings there are as cheap and as structural: the roster is not empty (which would make the first assertion vacuous), theme ids are non-blank and unique, and no drawable is listed in two slots — a copy-paste duplicate would show a child the same photograph twice while every count still agreed.

The UI tests name Cars as their Viewer fixture (`THEME_DEFS.first { it.id == "cars" }`) rather than searching for a photographed theme, since there is no longer such a thing as an unphotographed one to search past. Naming it also keeps the fixed indices those tests page to stable against a content change somewhere else in the roster.

**A photograph is shown undescribed** — no caption over it, no `contentDescription` behind it. This app is for looking at pictures, the labels that used to exist were written as artwork stand-ins rather than as prose worth reading aloud, and a screen reader announcing "Yellow digger with a big scooping bucket" adds nothing for the child holding the phone. `ViewerLayoutTest.aPhotographIsShownWithNoTextAndNoDescription` pins that as a decision rather than an oversight: the node must carry neither `Text` nor `ContentDescription`, rather than carrying a placeholder one. There is also no `Role.Image` to declare — Compose's `Image` sets that role only alongside a description.

The practical cost is that nothing in the semantics tree says *which* photograph is on screen — and, now that the arrays are gone, nothing written down anywhere says which picture sits at which index either. Look at the folder. The image carries a test tag naming the drawable it is drawing (`viewerImageTestTag(image)`), which is what the tests match on: what matters about this screen is that the expected image loaded, not what is pictured in it.

**To prepare a theme's photographs** — which now has to happen before the `ThemeDef` entry can be written at all:

1. Downscale the sources to 1280px on the long edge and re-encode them. On macOS, with no ImageMagick needed:

   ```bash
   sips -s format jpeg -s formatOptions 55 --resampleHeightWidthMax 1280 in.jpg --out out.jpg
   ```

   That lands each file between 33 and 453 KB across the set as it stands, and a theme's set between 1.8 MB (Dinosaurs) and 3.9 MB (Forest). Skip the resample when a source is already under 1280px — it would only upscale it.
2. Drop them in `res/drawable-nodpi/` as `img_<id>_NN.jpg`, numbered from `01`. **`-nodpi` matters**: a drawable in a plain `drawable/` folder is treated as mdpi artwork and upscaled by the device's density, which decodes a 1280px JPEG into a bitmap several times that size for nothing. `everyPhotographIsDensityIndependent` fails if one lands anywhere else.
3. List them in `ThemeDef.kt` as that theme's `<ID>_IMAGES`, in display order, and pass the list as the theme's `imageRes`. `ThemeResourcesTest.everyPhotographIsNamedAfterItsThemeAndPosition` fails if the list and the filenames disagree about position, so the order in the file and the order on disk stay the same fact.
4. Add any new photographers to `attribution_photographers`. The list is derived from the Unsplash source filenames, which are ASCII-folded, so a name with a diacritic needs correcting by hand against the photographer's profile.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), single `AppCompatActivity`
- One `ViewModel` (`AppViewModel`) holds all app state as a sealed `UiState`, plus the set of disabled themes and the gate's failure/lockout counters
- User-facing text, including every theme name, lives in `strings.xml`
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
│   └── ThemeDef.kt              # theme ids/hues/icons/photographs + the 14 THEME_DEFS entries
├── AppLocales.kt                # language selection, backed by AppCompatDelegate
└── ui/
    ├── TestTags.kt              # the two test tags the UI declares, and why
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
icons-src/                       # source SVGs, one per theme id, plus launcher.png
tools/svg2vd.py                  # icons-src/*.svg -> res/drawable/ic_theme_*.xml
docs/                            # privacy policy (md + html) and the Play submission checklist
shots/                           # Play listing assets: screenshots, feature graphic, store icon
```

## Branches

Two long-lived branches, and the distinction matters before you push:

| Branch | Holds | You |
|---|---|---|
| `develop` | integration; the GitHub default and the base for every PR | branch from it, open PRs against it |
| `main` | the release branch — whatever is currently live on Play | merge into it on a release, never work on it; the merge tags and publishes the release |

Feature work branches off `develop` and returns to it by PR. `main` moves only when a release is cut, so at any moment it answers "what is on a child's device right now?" — which is also why `docs/` is served to GitHub Pages from `main`: the hosted privacy policy should match the shipped app, not the next one.

The two were identical until the first tester-reported bug, so a clone that predates the split may still have `main` as its default. `git remote set-head origin -a` repoints it.

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
| `ViewerLayoutTest` | `androidTest` | The Viewer's layout at both sides of the 600dp breakpoint, using `DeviceConfigurationOverride(ForcedSize(...))` so a window wider than the test device still renders on screen. Asserts the buttons actually sit beside the image when wide and below it when narrow, rather than only that a branch was taken — and that both the button order and the swipe direction mirror under an RTL override. Then the photograph itself: that it carries neither text nor a content description, that it does not swallow the swipe, and — `theViewerDrawsThePhotographItWasGiven` — that the drawable on screen is the one it was handed, which nothing visible would otherwise distinguish from an off-by-one. |
| `PhotoNeighboursTest` | `test` (JVM) | Which photographs get warmed ahead of the one on screen, including that the wrap agrees with `AppViewModel.stepImage` — warming an image Back will not go to would waste a decode silently — and that the cache budget still holds the three-image working set. |
| `PhotoCacheTest` | `androidTest` | The cache against real resources: that a second read returns the *same* bitmap rather than decoding again, that a decoded photograph keeps its own pixel size (the density trap `decodeResource` would reintroduce, invisible to every other test), that eviction is by bytes, and that an evicted bitmap is **not** recycled — recycling one still held by a frame in flight is an immediate crash. |
| `ViewerPrefetchTest` | `androidTest` | That the Viewer and the cache are actually wired together, which is the difference between the feature working and the feature being present. Polls with `waitUntil` rather than `waitForIdle`, because the prefetch deliberately sits outside Compose's idling machinery. |
| `SettingsBehaviourTest` | `androidTest` | The language dropdown, the empty Home state, the attribution notice at the foot of the settings list, and the credit list's collapse/expand control. |
| `SharedPreferencesThemeStoreTest` | `androidTest` | The real store against real preferences: themes and the gate lock round-tripping through a *second* store instance, the two keys not treading on each other, ids for removed themes being pruned on read, and a lockout surviving a relaunch through actual SharedPreferences rather than a fake. |
| `ThemeResourcesTest` | `androidTest` | Holds `ThemeDef`, `strings.xml` and the bundled photographs together, for everything that needs real resources: names are non-blank and unique, each theme's icon and name resolve to `ic_theme_<id>` and `theme_<id>`, every photograph is named `img_<id>_NN` after its theme and position, decodes, and lives in `drawable-nodpi`. It also checks every shipped language for names, and pins Croatian's fallback policy and a handful of English defaults by literal. |
| `PolicyDocumentTest` | `test` (JVM) | The privacy policy's Markdown: that headings, wrapped paragraphs, wrapped bullets and bold parse as intended, that links flatten to something a reader without a browser can use, and — the one that matters — that the editor's HTML comment at the top of the source file never reaches a reader. |
| `PolicyScreenTest` | `androidTest` | That the policy is actually *in* the APK. Gradle staging `docs/privacy-policy.md` into assets is the step that can silently stop happening, and a JVM test reading the file off disk would pass either way. Also that the screen renders the document and that Done dismisses it. |
| `ThemeRosterTest` | `test` (JVM) | The four roster facts that need no device, so a plain `./gradlew build` gates them: every theme ships photographs, the roster is not empty, ids are non-blank and unique, and no drawable is listed twice. It reads `THEME_DEFS` and nothing else — the version that parsed `strings.xml` off disk, and the `strings.xml` test-task input in `app/build.gradle.kts` that had to accompany it, went with the label arrays. |
| `CardContrastTest` | `test` (JVM) | Recomputes the WCAG contrast of every theme's card label against both its fill and its stripe, and fails under 4.5:1. The palette is generated from a hue, so a fifteenth theme with a bad hue is a bad card; this is what stops that shipping. |

To run a single instrumented class:

```bash
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.kidsexplore.app.KidsExploreFlowTest
```

Two GitHub Actions workflows run `./gradlew build` — unit tests, lint, and the icon-sync task. `.github/workflows/ci.yml` runs it on every PR into `develop` or `main` and on pushes to `develop`; `.github/workflows/release.yml` runs it again when `main` moves, before it publishes anything. Deliberately the same command in both, so a green PR has already cleared almost everything the release build will check. Two things it has not: `ci.yml` is given no keystore and no secrets at all — which is what makes it safe to run on a PR from a fork, and means the release APK it produces is unsigned — and it never runs `bundleRelease`, so a failure specific to the bundle path is still first seen at release time.

**The instrumented suite never runs on CI**, because the runner has no device. `./gradlew connectedDebugAndroidTest` against a device or emulator stays a local step before you open a PR.

**One instrumented test is currently red:** `AppLocalesTest.everyOfferedChoiceSurvivesTheRoundTrip`, which sets each offered locale through `AppCompatDelegate` and reads it back. It has failed on the emulator for a while and has been treated as an emulator quirk, but nobody has confirmed that on real hardware or found the cause — so treat it as an open question about the app's own per-app-language handling, not as a settled property of the test rig. The rest of the suite passes.

## Releasing

Merging `develop` into `main` cuts the release. `.github/workflows/release.yml` picks it up from there: it reads `versionName` out of `app/build.gradle.kts`, refuses to go on if `v<versionName>` is already tagged, runs `./gradlew build bundleRelease` with the signing secrets in the environment, and publishes a GitHub Release at `v<versionName>` with the signed `.aab`, the `.apk` and the R8 mapping file attached and a changelog generated from the commits since the previous one. The `.aab` is then what you upload to the Play Console — the pipeline stops at GitHub and does not talk to Play.

**So bumping the version is part of cutting a release, not an afterthought.** `versionName` and `versionCode` are edited on `develop` like any other change; if you forget, the workflow fails in seconds on the duplicate tag rather than publishing over a shipped version. The check compares versions, not tag strings, so `1.0` is recognised as already released under `v1.0.0` — the two are the same version written two ways, and a plain string compare would sail straight past it.

`v1.0.0` shipped as `versionCode 1`; `develop` carries `1.0.1` / `versionCode 2` for the next one.

The four signing secrets the release workflow needs are in [`docs/play-store-submission.md`](docs/play-store-submission.md) section 2. It also attaches `mapping-<version>.txt`: the build is minified, so that file is the only way to read a crash report from this release, and it exists nowhere but the runner that produced these bytes.

By hand — which is still what you do to test a release build before merging — Play wants an app bundle, and it rejects an unsigned one at upload:

```bash
./gradlew bundleRelease   # app/build/outputs/bundle/release/app-release.aab
```

Signing is read from `keystore.properties` at the repository root, falling back to `KIDS_EXPLORE_STORE_FILE` / `_STORE_PASSWORD` / `_KEY_ALIAS` / `_KEY_PASSWORD` in the environment so CI can supply the values without writing a secret to disk. The file wins over the environment, so a local keystore is not shadowed by a stale exported variable. Both the keystore and the properties file are gitignored.

**All four values are optional, and their absence is a supported state.** With any of them missing the release signing config is not created at all and `release` simply stays unsigned — which is what keeps a fresh clone, `./gradlew build` and `./gradlew assembleRelease` working for someone who has no keystore and no business having one. The APK path says so in the filename — `app-release-unsigned.apk` rather than `app-release.apk`. The bundle path cannot: an unsigned AAB is named `app-release.aab`, exactly like a signed one, and that is the artifact that goes to Play. So `bundleRelease` alone refuses to run without signing, and says what to do about it; `assembleRelease` and `./gradlew build` still degrade quietly.

Two other release-only pieces are worth knowing about before someone tidies them away:

- `bundle { language { enableSplit = false } }` in `app/build.gradle.kts`. With language splitting on, Play installs only the resources matching the device's system language. The in-app picker sets the language with `AppCompatDelegate.setApplicationLocales()`, which changes the app's locale but does not ask Play for a split it never installed — so a parent on an English phone who picked Hrvatski would get English straight back. None of this reproduces locally, because every build made here contains both languages; the bug would exist only in what Play serves. The saving given up is one extra language of short UI strings, `androidResources.localeFilters` having already thrown away the ~100 locales AppCompat ships.
- The app no longer contains a policy URL at all. Play still needs one for the **Console listing**, and `docs/privacy-policy.html` is the copy to host — once the repository is public and GitHub Pages serves `/docs` from `main`, it lands at `https://ivancesar.github.io/kid-android-image-app/privacy-policy.html`. Hosting is on the submission's critical path, not the app's: nothing in the app breaks if that page is down, which is the whole point of bundling the document. `docs/play-store-submission.md` has the steps.

The rest of the submission — the store listing, the Data safety and Families answers, the content rating, what to do about the keystore — is in **[`docs/play-store-submission.md`](docs/play-store-submission.md)**, written for whoever presses Publish. The policy text itself is `docs/privacy-policy.md`, with a self-contained `docs/privacy-policy.html` alongside it for hosting. Both rest on the same handful of facts about the app — no `INTERNET` permission, no ads, no analytics, no accounts, and one SharedPreferences file leaving the device via Auto Backup. If any of those stops being true, both documents stop being true with it.

## Known limitations

- The artwork dominates the download: 38.4 MB of the 39.9 MB release APK is bundled JPEGs, and the release bundle is 41.5 MB (see [On images](#on-images) above). That is a deliberate trade for now — well inside Play's limits, and the pictures are the product. Lossy WebP is the first lever if it stops being acceptable; it needs no code change, only an encoder, which `sips` is not (it reads WebP but cannot write it).
- **The first photograph of a theme still decodes on the main thread.** Every other page turn reads a cache. `PhotoCache` holds decoded photographs — 24 MiB, which is the three a child can reach without waiting (previous, current, next) at the largest shape the app ships — and `ViewerScreen` warms both neighbours on a background thread while the child looks at the current one. A page turn that hits the cache is a map lookup; a miss decodes synchronously exactly as `painterResource` used to, so the card still sizes itself in the same composition and the Viewer can never be slower than it was. What is left is the cold decode when a theme is first opened, and the same on process restore straight into the Viewer. Measured on an API 36 emulator with `atrace`, opening Cars and tapping Next six times: **7 main-thread decodes totalling 51.7 ms before, 1 totalling 9.3 ms after**, with 8 decodes moved to the `photo-prefetch` thread. Frame counters (`dumpsys gfxinfo`) could not resolve the difference on an emulator — 2 janky frames before, 4 after, which is noise — so the decode counts are the evidence, not the frame stats. A real phone would show the jank the emulator does not.
- **The parental gate is a speed bump, not a lock.** The lockout now escalates — three wrong answers cost 30 seconds, the next three 60, then 2, 4 and 8 minutes, and only a correct answer resets it — so grinding at it gets expensive. What escalation does not change is the front door: the question is one addition of two numbers between 2 and 7 with four buttons, so a random guess is right one time in four and a round of three clears it 57.8% of the time. More than half of random tapping therefore never meets a lockout at all, and a child who can add is through on the first tap regardless. This was weighed and deliberately left as it stands; a gate with more options, or one that is not arithmetic, is what would change it.
- Nothing stops a child leaving the app for the launcher. The gate protects **Settings**, not the app's boundary; Android's screen pinning is what would deliver that, and it is not wired up.
- Font is the system sans-serif at heavy weights, approximating the source design's Nunito 800/900; no font file is bundled.
- Light theme only, by design: the palette is a fixed bright one with no dark counterpart, so the colour scheme is pinned rather than following the system setting.
