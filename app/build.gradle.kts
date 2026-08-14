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
