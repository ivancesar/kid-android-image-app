plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // No signingConfig here on purpose — release signing needs a
            // keystore, which is a deployment decision, not a build one.
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
