package com.kidsexplore.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/**
 * `res/xml/locales_config.xml` is what Android 13+ reads to build the system's
 * per-app language screen; [AppLocales.SUPPORTED] is what the in-app picker
 * offers. Nothing links the two at compile time, so a language added to one and
 * not the other would silently appear in only one of the two places.
 */
@RunWith(AndroidJUnit4::class)
class LocalesConfigTest {

    @Test
    fun localesConfigMatchesTheInAppPicker() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.xml.locales_config)
        val declared = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                val name = parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                if (name != null) declared.add(name)
            }
        }
        assertEquals(declared.sorted(), AppLocales.SUPPORTED.sorted())
    }
}
