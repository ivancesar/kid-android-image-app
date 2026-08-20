plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
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

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    androidResources {
        // AppCompat brings ~100 locale folders of its own strings. The app ships
        // two languages, so the rest are dead weight in every APK.
        localeFilters += listOf("en", "hr")
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.0")
    // Per-app language: AppCompatDelegate.setApplicationLocales() persists the
    // choice and backports it below Android 13, which minSdk 26 still has to serve.
    implementation("androidx.appcompat:appcompat:1.7.1")

    // Instrumented tests (./gradlew connectedDebugAndroidTest) — they need a
    // real Context for SharedPreferences, so they run on a device/emulator.
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
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
val checkIconsInSync by tasks.registering(Exec::class) {
    group = "verification"
    description = "Fails if res/drawable/ic_theme_*.xml is out of sync with icons-src."

    val repoRoot = rootProject.layout.projectDirectory
    inputs.dir(repoRoot.dir("icons-src")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(repoRoot.file("tools/svg2vd.py")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(layout.projectDirectory.dir("src/main/res/drawable")).withPathSensitivity(PathSensitivity.RELATIVE)
    // Nothing is produced; the marker only gives Gradle somewhere to record success.
    outputs.file(layout.buildDirectory.file("icons-in-sync.marker"))

    workingDir = repoRoot.asFile
    commandLine("python3", "tools/svg2vd.py", "icons-src", "app/src/main/res/drawable", "--check")

    doLast {
        outputs.files.singleFile.apply { parentFile.mkdirs(); writeText("ok") }
    }
}

tasks.named("check") { dependsOn(checkIconsInSync) }
