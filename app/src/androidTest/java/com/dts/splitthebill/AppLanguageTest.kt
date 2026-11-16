package com.dts.splitthebill

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test fun appLanguageChangesAndSurvivesActivityRecreation() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            var original = LocaleListCompat.getEmptyLocaleList()
            scenario.onActivity { original = AppCompatDelegate.getApplicationLocales() }
            try {
                for ((tag, title) in listOf("fr" to "Langue", "es" to "Idioma", "en" to "Language")) {
                    scenario.onActivity { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) }
                    InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                    scenario.recreate()
                    scenario.onActivity {
                        assertEquals(tag, it.resources.configuration.locales[0].language)
                        assertEquals(title, it.getString(R.string.language))
                        assertEquals(tag, AppCompatDelegate.getApplicationLocales()[0]?.language)
                    }
                }
            } finally {
                scenario.onActivity { AppCompatDelegate.setApplicationLocales(original) }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            }
        }
    }
}
