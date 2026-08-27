# Play Store submission checklist — Kids Explore

Everything the repository can fix has been fixed. This file is the other half:
the work that lives in the Play Console, in a graphics editor, or in a keystore
that must never be committed. It is written for whoever presses "Publish", so it
states the answers rather than pointing at Google's documentation and wishing
them luck.

Facts the answers below rest on, all verified against the built app:

* one permission in the merged release manifest,
  `com.kidsexplore.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (signature-level,
  contributed by `androidx.core`);
* no `INTERNET` permission, no advertising ID, no networking code anywhere in
  `app/src/main`;
* no ads, no analytics, no crash reporting, no in-app purchases, no accounts;
* the only path off the device is Android Auto Backup of one SharedPreferences
  file: up to fourteen category ids plus the parental gate's failure count and
  lockout deadline.

If any of that stops being true, this file and `privacy-policy.md` both stop
being true with it.

---

## 1. The hosted privacy policy

**Done.** The repository is public, GitHub Pages serves `/docs` from `main`, and
the policy is live at:

    https://ivancesar.github.io/kid-android-image-app/privacy-policy.html

That URL goes in two places in the Console, and they must match: the store
listing, and the Target Audience and Content section (see section 5, where a
policy URL is separately mandatory for a child-directed app).

The app does not depend on this. `docs/privacy-policy.md` is bundled as an asset
and Parent Settings renders it, so a parent can read the policy offline. The
hosted copy exists because Play requires a public URL and checks that it
resolves.

`docs/privacy-policy.md` is the source of truth — it is what the app renders.
`docs/privacy-policy.html` is the hosted copy. **Change one and change the
other**, or the two versions of the same promise start to disagree.

Policy questions go to the repository's issue tracker rather than an email
address. Play separately requires a developer contact email on the Console
account itself; that is an account setting and has nothing to do with these
documents.

### If the policy ever needs rehosting

`docs/privacy-policy.html` is deliberately self-contained — no external
stylesheet, font, script or image — so any static host serves it as-is with no
build step. To reproduce the current setup from scratch: make the repository
public, then *Settings → Pages → Source: Deploy from a branch*, branch `main`,
folder `/docs`. The policy then sits at
`https://<user>.github.io/<repo>/privacy-policy.html`. Load it in a browser
before submitting; Play checks that it resolves.

---

## 2. The upload keystore

`app/build.gradle.kts` now signs the release build from four values it reads out
of `keystore.properties` at the repository root, falling back to environment
variables. Neither the keystore nor the properties file is committed — both are
in `.gitignore`. When the values are absent the signing config is simply not
created, so a fresh clone still builds.

Generate the key once. **The passwords below are yours to choose; do not reuse
an example.** Losing this file means losing the ability to update the app, so
back it up somewhere that is not this repository:

```bash
keytool -genkeypair -v \
  -keystore ~/keystores/kids-explore-upload.jks \
  -alias kids-explore-upload \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storetype PKCS12
```

`keytool` prompts for the store password and for a name and organisation; the
name fields are cosmetic for an upload key. Then create
`keystore.properties` in the repository root — it is gitignored, and it is the
one file in this process that holds secrets:

```properties
storeFile=/Users/you/keystores/kids-explore-upload.jks
storePassword=<the store password you just chose>
keyAlias=kids-explore-upload
keyPassword=<the key password; keytool -storetype PKCS12 makes it the store password>
```

`storeFile` may be absolute or relative to the repository root. On CI, skip the
file and export `KIDS_EXPLORE_STORE_FILE`, `KIDS_EXPLORE_STORE_PASSWORD`,
`KIDS_EXPLORE_KEY_ALIAS` and `KIDS_EXPLORE_KEY_PASSWORD` instead, with the
keystore itself materialised from an encrypted secret at build time.

Enrol in **Play App Signing** when creating the app in the Console (it is the
default). Play then holds the app signing key and this keystore is only the
*upload* key — which means that if it is ever lost, Google can reset it, whereas
without Play App Signing the app would be unupdatable forever.

---

## 3. Build and verify

Gradle here needs Android Studio's bundled JDK; the default `java` on this
machine is JDK 8 and the build fails without the prefix.

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Everything that gates a merge: unit tests, lint, the icon-sync task.
./gradlew build

# The artefact Play wants.
./gradlew bundleRelease
#   -> app/build/outputs/bundle/release/app-release.aab

# An installable release build, for testing the shrunk/obfuscated app on a device.
./gradlew assembleRelease
#   -> app/build/outputs/apk/release/app-release.apk
```

Three things are worth checking by hand on the first release, because each is
invisible until it is too late:

**The bundle is actually signed.** An unsigned AAB is rejected at upload.

```bash
jarsigner -verify -verbose:summary app/build/outputs/bundle/release/app-release.aab
```

`jar verified` means the signing config was picked up. "no manifest" or "jar is
unsigned" means `keystore.properties` was not found or was incomplete — the
build succeeds either way, by design.

**Language splitting is off.** `bundle { language { enableSplit = false } }` in
`app/build.gradle.kts` is there because the in-app language picker uses
`AppCompatDelegate.setApplicationLocales()`, which does not trigger an on-demand
split install: with splitting on, a parent on an English device who picks
Hrvatski gets English back, and this reproduces on no local build. To confirm
what Play would serve, using
[bundletool](https://github.com/google/bundletool/releases):

```bash
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=/tmp/kids-explore.apks
unzip -l /tmp/kids-explore.apks | grep -i 'splits/'
```

There must be **no** `*-hr.apk` or `*-en.apk` language split in the output. Base
and density/ABI splits are expected and fine.

**The permission list is what the policy claims.** The Data Safety answers below
depend on it:

```bash
unzip -p app/build/outputs/apk/release/app-release.apk AndroidManifest.xml \
  | strings | grep -i permission
# or, more readably, after any release build:
grep -i permission app/build/outputs/logs/manifest-merger-release-report.txt
```

Expect exactly one: `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`.

### The native debug symbols warning

Uploading the bundle produces a Console warning:

> This App Bundle contains native code, and you've not uploaded debug symbols.

**Ignore it.** It is a warning, not an error, and it does not block the upload,
the review or the release. Acting on it is impossible rather than merely
optional, for a reason worth writing down once:

* This project has no native code. There is no `externalNativeBuild`, no
  `CMakeLists.txt`, no `jniLibs` — nothing in `app/build.gradle.kts` builds a
  `.so`.
* The bundle nevertheless carries exactly one native library, about 10 KB per
  ABI, pulled in transitively by AndroidX:

      base/lib/{arm64-v8a,armeabi-v7a,x86,x86_64}/libandroidx.graphics.path.so

* That library arrives **already stripped**. `file` reports `stripped` and `nm`
  reports `no symbols`, so there is no symbol table in it to extract:

  ```bash
  unzip -o -j app/build/outputs/bundle/release/app-release.aab \
    "base/lib/arm64-v8a/libandroidx.graphics.path.so" -d /tmp
  file /tmp/libandroidx.graphics.path.so   # ... stripped
  nm   /tmp/libandroidx.graphics.path.so   # no symbols
  ```

The usual remedy — `ndk { debugSymbolLevel = "FULL" }` in the release build type
— tells AGP to package symbols it finds in the merged native libraries. Here it
would find none, produce no symbol file, and leave the warning exactly where it
is. Adding it would be cargo cult, so it is deliberately absent.

If this app ever gains native code of its own, that changes: add

```kotlin
buildTypes {
    release {
        ndk { debugSymbolLevel = "FULL" }
    }
}
```

and the warning becomes worth clearing, because then the crash reports it
affects would be ones you can actually act on.


`versionCode` is `1` in `app/build.gradle.kts`. Every subsequent upload — closed
testing builds included — needs it incremented.

---

## 4. Store listing assets

`shots/` holds every asset in this section that is a file. Only the two
descriptions and the category still have to be written in the Console.

* **App icon — 512 × 512 PNG, under 1 MB.** `shots/play-icon-512.png`, which is
  `icons-src/launcher.png` flattened onto white. Play does *not* crop this the
  way a device crops an adaptive icon, so unlike the launcher icon it keeps the
  source art's full square and its frame — no safe-zone inset, no rounded
  corners, no transparency. The launcher icon is a separate asset built from the
  same source: `mipmap-anydpi/ic_launcher.xml`, a raster foreground matted out
  of the digger over the two-tone `drawable/ic_launcher_background.xml`.
* **Feature graphic — 1024 × 500 PNG or JPEG, no transparency.**
  `shots/feature-graphic.png`, composited from six category cards lifted out of
  `shots/tablet/01-home.png` at full resolution. Shown at the top of the listing
  and required for a Families listing. No text near the edges; it gets cropped
  on some surfaces.
* **Phone screenshots — 2 to 8, 16:9 or 9:16, each side 320–3840 px.**
  `shots/phone/`, six at 1080 × 1920: the category grid, two Viewer
  photographs, the parental gate, Parent Settings and the privacy policy.
* **Tablet screenshots — 7" and 10".** `shots/tablet/`, five at 2560 × 1440.
  Both sides fall inside Play's 1080–7680 px tablet range, so the same five
  serve both slots.
* **Short description** (80 characters) and **full description** (4000). Not
  files in this repository; written in the Console.
* **App category:** Education, or Entertainment. **Tags:** pick from the
  Console's fixed list.

### Recapturing the screenshots

Play requires 16:9 or 9:16 and rejects anything else, which the emulator's own
1080 × 2400 and 2560 × 1600 are not. Cropping afterwards only loses the ratio
again, so set the device to the target geometry before capturing:

```
adb shell wm size 1080x1920 && adb shell wm density 440   # phone
adb shell wm size 2560x1440 && adb shell wm density 320   # tablet
adb shell wm size reset && adb shell wm density reset     # afterwards
```

At 440 dpi a 1080 × 1920 window is 393 dp wide, so the app takes the same layout
branches it takes on a real phone rather than a scaled-down tablet's. Status
bars are SystemUI demo mode, not the live clock and battery.

The gate screenshots do show a question and its four options. That is safe
rather than a leak: `AppViewModel` draws both operands at random and reshuffles
the options on every open, so a published instance is not the answer to any
future gate. What must not happen is presenting the gate as though its answer
were fixed.

---

## 5. Console declarations

### Target Audience and Content

* **Target age groups:** *Ages 5 and under* and *Ages 6–8* (the app is aimed at
  young children; picking any child age band makes it child-directed).
* **Appeals to children:** Yes. This puts the app in the **Families** programme
  and enrols it in the Designed for Families requirements — which is correct,
  and is why the privacy policy link inside Parent Settings exists.
* **Store listing presence:** the app is intended for children.
* A privacy policy URL is **mandatory** here as well as in the main listing, and
  the two must be the same URL.

### Data safety

Answer the form as follows.

* **Does your app collect or share any of the required user data types?** → **No.**
* Consequently: no data types to declare, no "shared with third parties", no
  purposes, no "data is encrypted in transit" question, and no deletion-request
  URL required.
* **Justification if it is ever questioned:** the app has no `INTERNET`
  permission and no networking code, so it transmits nothing. The only path off
  the device is Android's own Auto Backup of one SharedPreferences file, which
  goes to the *user's own* Google account, is an OS feature rather than
  app-initiated collection, and is not accessible to the developer — Play's
  Data safety guidance excludes data the developer cannot access. Keep
  `docs/privacy-policy.md`'s "Backup" section as the written record of that
  reasoning.
* **Families programme note:** child-directed apps must not use an advertising
  ID. This app does not request `AD_ID` and declares no ads SDK, so there is
  nothing to declare here either.

### Ads

* **Does your app contain ads?** → **No.** (Answering yes would additionally
  require the Families Ads programme and a certified ad SDK.)

### Content rating (IARC questionnaire)

Complete it in the Console; it generates the ratings and cannot be filled in
from here. The truthful answers for this app: no violence, no sexuality, no
profanity, no controlled substances, no gambling, no user-generated content, no
user-to-user communication, no sharing of location or personal information, no
purchases. Expected outcome: **Everyone / PEGI 3 / USK 0**. An incorrect
questionnaire is grounds for removal, so answer it rather than guessing at the
rating.

### Other declarations

* **App access:** all functionality is available without any special access — no
  login, no credentials to provide to the review team. Say so explicitly;
  leaving it blank stalls review.
* **Government apps:** No. **Financial features:** None. **Health apps:** No.
* **News app:** No.
* **Data deletion:** no account exists, so there is nothing to link. The privacy
  policy covers uninstall as the deletion route.

---

## 6. Closed testing requirement (personal developer accounts)

If the developer account is a **personal** account created on or after
**13 November 2023**, production access must be earned first:

* run a **closed test** with at least **12 testers opted in**,
* held **continuously for 14 days**,
* every tester must remain opted in for the whole period — someone leaving
  resets the clock,
* then apply for production access, which is reviewed manually.

Practically: create the closed testing track and upload the signed AAB as soon
as the keystore exists, recruit the twelve, and start the fortnight running
while the listing assets in section 4 are being made. This is usually the
longest pole in the whole submission. Organisation accounts and personal
accounts older than that date are exempt.

---

## 7. Order of operations

1. Host the policy (section 1) and publish the policy page.
2. Create the keystore and `keystore.properties` (section 2).
3. `./gradlew build && ./gradlew bundleRelease`, then run the three verification
   checks (section 3).
4. Create the app in the Console, enrol in Play App Signing, upload the AAB to
   the closed testing track.
5. Start the 14-day / 12-tester clock (section 6).
6. Complete the declarations (section 5) and the listing assets (section 4)
   while it runs.
7. Apply for production access, then promote the release.
