import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Release signing, read from outside the repository.
 *
 * Play rejects an unsigned bundle at upload, so `bundleRelease` has to be able
 * to sign — but a keystore and its passwords are the one thing that must never
 * be committed, and hardcoding them here would put them in every clone and in
 * every diff forever. So the four values come from `keystore.properties` at the
 * repository root (gitignored, see `.gitignore`), falling back to environment
 * variables so CI can supply them without ever writing a secret to disk.
 *
 * All four are optional. When any is missing the release signing config is not
 * created at all and `release` stays unsigned, which is what keeps a fresh
 * clone, a debug build and `./gradlew build` on CI working for someone who has
 * no keystore and no business having one.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/**
 * One signing setting: the file wins over the environment, so a developer with
 * a local keystore is not surprised by a stale exported variable. Blank counts
 * as absent — a half-filled template file should degrade to "no signing", not
 * fail the build with an empty password.
 */
fun signingSetting(key: String, environmentVariable: String): String? =
    keystoreProperties.getProperty(key)?.takeIf { it.isNotBlank() }
        ?: System.getenv(environmentVariable)?.takeIf { it.isNotBlank() }

val releaseStorePath = signingSetting("storeFile", "KIDS_EXPLORE_STORE_FILE")
val releaseStorePassword = signingSetting("storePassword", "KIDS_EXPLORE_STORE_PASSWORD")
val releaseKeyAlias = signingSetting("keyAlias", "KIDS_EXPLORE_KEY_ALIAS")
val releaseKeyPassword = signingSetting("keyPassword", "KIDS_EXPLORE_KEY_PASSWORD")

/** The name is referenced twice below; it is not worth misspelling once. */
val releaseSigningConfigName = "release"

android {
    namespace = "com.kidsexplore.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.kidsexplore.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // AppCompat brings ~100 locale folders of its own strings. The app ships
        // two languages, so the rest are dead weight in every APK.
        localeFilters += listOf("en", "hr")
    }

    /**
     * How Play splits the uploaded bundle back into what it serves a device.
     *
     * Language splitting is off, and this is the block someone will one day be
     * tempted to delete as clutter — so, at length: with it on, Play installs
     * only the resources matching the device's system language, and the other
     * language arrives later as an on-demand split. The in-app picker sets the
     * language with `AppCompatDelegate.setApplicationLocales()`, which changes
     * the app's locale but does not ask Play for a split it has not installed.
     * A parent on an English phone who picks Hrvatski would therefore get
     * English straight back, because the Croatian resources are simply not on
     * the device.
     *
     * The saving this gives up is one extra language of short UI strings.
     * `androidResources.localeFilters` above has already thrown away the ~100
     * locales AppCompat ships, which is where the real weight was.
     *
     * None of this reproduces locally: every build made here contains both
     * languages, so the bug exists only in what Play serves. That is the whole
     * reason it is written down rather than left to be rediscovered.
     */
    bundle {
        language {
            enableSplit = false
        }
    }

    signingConfigs {
        // Copied into locals first so the null check below actually narrows the
        // type inside the nested lambda — a script-level property would not.
        val storePath = releaseStorePath
        val storePass = releaseStorePassword
        val alias = releaseKeyAlias
        val keyPass = releaseKeyPassword
        // Created only when every value is present — see the block comment at
        // the top of this file for why an absent keystore is a supported state
        // rather than an error.
        if (storePath != null && storePass != null && alias != null && keyPass != null) {
            create(releaseSigningConfigName) {
                // `rootProject.file` leaves an absolute path alone and resolves
                // a relative one against the repository root, so the property
                // can say either.
                storeFile = rootProject.file(storePath)
                storePassword = storePass
                keyAlias = alias
                keyPassword = keyPass
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // `?.let` rather than a plain assignment: with no keystore
            // configured there is no config to point at, and the build type is
            // left exactly as it was before — unsigned, and still buildable.
            signingConfigs.findByName(releaseSigningConfigName)?.let { signingConfig = it }
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    // Used directly: AppViewModel takes a SavedStateHandle so screen state
    // survives process death, not just rotation.
    implementation(libs.lifecycle.viewmodel.savedstate)
    implementation(libs.core.ktx)
    // Per-app language: AppCompatDelegate.setApplicationLocales() persists the
    // choice and backports it below Android 13, which minSdk 26 still has to serve.
    implementation(libs.appcompat)

    // Plain JVM tests (./gradlew testDebugUnitTest) — the whole state machine
    // runs here, because AppViewModel talks to a ThemeStore interface rather
    // than to SharedPreferences directly.
    testImplementation(libs.junit)

    // Instrumented tests (./gradlew connectedDebugAndroidTest) — Compose UI,
    // the string resources behind ThemeDef, and SharedPreferencesThemeStore
    // itself, so they need a device. The flow tests fake the store; only
    // SharedPreferencesThemeStoreTest touches the real one.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}

/**
 * The theme drawables are generated from the SVGs in `icons-src` by
 * `tools/svg2vd.py`. This re-runs the converter in --check mode and fails if any
 * committed drawable disagrees with its source.
 *
 * A task rather than a unit test because only a task can declare the SVGs and
 * the converter as inputs: a test's up-to-date check keys off its own classpath,
 * so it would be skipped after an SVG-only edit — precisely when it must run.
 */
val checkIconsInSync = tasks.register<Exec>("checkIconsInSync") {
    group = "verification"
    description = "Fails if res/drawable/ic_theme_*.xml is out of sync with icons-src."

    val repoRoot = rootProject.layout.projectDirectory
    val sources = repoRoot.dir("icons-src")
    val converter = repoRoot.file("tools/svg2vd.py")
    val drawables = layout.projectDirectory.dir("src/main/res/drawable")

    inputs.dir(sources).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(converter).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(drawables).withPathSensitivity(PathSensitivity.RELATIVE)
    // Nothing is produced; the marker only gives Gradle an output to key
    // up-to-dateness on. It is written in doLast, so a failing run never
    // records success.
    outputs.file(layout.buildDirectory.file("icons-in-sync.marker"))

    // Paths derived from the inputs above rather than written out a second
    // time, so the two cannot drift apart.
    fun relative(f: File) = f.relativeTo(repoRoot.asFile).path
    workingDir = repoRoot.asFile
    commandLine(
        "python3", relative(converter.asFile),
        relative(sources.asFile), relative(drawables.asFile), "--check",
    )

    doLast {
        outputs.files.singleFile.apply { parentFile.mkdirs(); writeText("ok") }
    }
}

tasks.named("check") { dependsOn(checkIconsInSync) }

/**
 * Stages `docs/privacy-policy.md` as an app asset, so `PolicyScreen` renders the
 * very document `docs/` publishes.
 *
 * A task with declared input and output rather than a copy done at configuration
 * time: this way editing the policy re-runs it, and not editing it does not.
 */
abstract class StagePrivacyPolicy : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val policy: RegularFileProperty

    @get:OutputDirectory
    abstract val stagedAssets: DirectoryProperty

    @TaskAction
    fun stage() {
        val target = stagedAssets.get().asFile
        target.mkdirs()
        policy.get().asFile.copyTo(target.resolve("privacy-policy.md"), overwrite = true)
    }
}

/**
 * Registered through the variant API rather than `sourceSets`, which AGP no
 * longer lets a provider into — and this way the asset directory carries its
 * producing task with it, so no build ever races the copy.
 */
androidComponents {
    onVariants { variant ->
        val stage = tasks.register<StagePrivacyPolicy>(
            "stagePrivacyPolicy${variant.name.replaceFirstChar { it.uppercase() }}",
        ) {
            description = "Stages docs/privacy-policy.md as an app asset."
            policy.set(rootProject.layout.projectDirectory.file("docs/privacy-policy.md"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(stage, StagePrivacyPolicy::stagedAssets)
    }
}

/**
 * `bundleRelease` refuses to produce an unsigned bundle.
 *
 * Everywhere else, missing signing degrades quietly and on purpose: a fresh
 * clone has no keystore and must still build. But an unsigned AAB is named
 * `app-release.aab`, exactly like a signed one — where `assembleRelease` at
 * least names its output `app-release-unsigned.apk` — so the one command whose
 * output goes to Play is also the one that gives no sign of the problem. A
 * mistyped CI secret would archive a normal-looking artifact and the news would
 * arrive from Play's rejection instead of from here.
 */
tasks.matching { it.name == "bundleRelease" }.configureEach {
    doFirst {
        check(android.buildTypes.getByName("release").signingConfig != null) {
            "bundleRelease would produce an UNSIGNED bundle, which Play rejects. " +
                "Create keystore.properties at the repository root, or export " +
                "KIDS_EXPLORE_STORE_FILE / _STORE_PASSWORD / _KEY_ALIAS / _KEY_PASSWORD. " +
                "See docs/play-store-submission.md section 2. " +
                "(assembleRelease still builds unsigned, if that is what you want.)"
        }
    }
}
