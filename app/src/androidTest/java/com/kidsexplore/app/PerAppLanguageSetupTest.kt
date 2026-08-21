package com.kidsexplore.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Guards the three things per-app language needs below Android 13.
 *
 * Above API 33 the framework owns the store and this is all redundant. Below
 * it — which is API 26 to 32, the only reason `androidx.appcompat` is a
 * dependency at all — `AppCompatDelegate` keeps its own store, and only if the
 * app opts in by declaring `AppLocalesMetadataHolderService` with
 * `autoStoreLocales=true`, runs an `AppCompatActivity`, and themes it from
 * `Theme.AppCompat`.
 *
 * Remove any one of those and the app still compiles, every other test still
 * passes, and every user on 26–32 silently loses their language on the next
 * cold start. Nothing else in the build notices, which is why these three
 * assertions exist.
 */
@RunWith(AndroidJUnit4::class)
class PerAppLanguageSetupTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theLocaleStorageServiceIsDeclaredAndOptedIn() {
        @Suppress("DEPRECATION")
        val services = context.packageManager
            .getPackageInfo(
                context.packageName,
                PackageManager.GET_SERVICES or
                    PackageManager.GET_META_DATA or
                    PackageManager.MATCH_DISABLED_COMPONENTS,
            )
            .services
            .orEmpty()

        val holder = services.firstOrNull {
            it.name == "androidx.appcompat.app.AppLocalesMetadataHolderService"
        }
        assertTrue(
            "AppLocalesMetadataHolderService is not declared; the language " +
                "choice will not survive a cold start below API 33",
            holder != null,
        )
        // aapt2 compiles android:value="true" to a typed boolean, which is
        // also how AppCompat reads it back (isAutoStorageOptedIn calls
        // metaData.getBoolean) — getString would return null here.
        assertTrue(
            "autoStoreLocales must be true for AppCompat to persist the locale",
            holder!!.metaData?.getBoolean("autoStoreLocales") == true,
        )
    }

    @Test
    fun mainActivityIsAnAppCompatActivity() {
        // setApplicationLocales applies through a live AppCompatDelegate; a bare
        // ComponentActivity would apply nothing below API 33.
        assertTrue(
            "MainActivity must extend AppCompatActivity",
            AppCompatActivity::class.java.isAssignableFrom(MainActivity::class.java),
        )
    }

    @Test
    fun theAppThemeDescendsFromAppCompat() {
        // AppCompatActivity throws at startup under a non-AppCompat theme, so
        // this would be a crash rather than a silent failure — but it is the
        // third leg of the same setup and belongs beside the other two.
        val theme = context.resources.newTheme()
        theme.applyStyle(R.style.Theme_KidsExplore, true)
        val attrs = intArrayOf(androidx.appcompat.R.attr.colorPrimary)
        val typed = theme.obtainStyledAttributes(attrs)
        try {
            assertTrue(
                "Theme.KidsExplore must descend from Theme.AppCompat",
                typed.hasValue(0),
            )
        } finally {
            typed.recycle()
        }
    }
}
