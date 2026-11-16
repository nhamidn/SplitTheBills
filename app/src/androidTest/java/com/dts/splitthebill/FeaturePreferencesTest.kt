package com.dts.splitthebill

import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.dts.splitthebill.data.DataStoreUserPreferencesRepository
import com.dts.splitthebill.domain.repository.ThemePreference
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FeaturePreferencesTest {
    @Test fun currencyDebtLockAndReadStateSurviveReopeningTheDataStore() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = context.preferencesDataStoreFile("feature-test-${System.nanoTime()}")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun repository(file: File) = DataStoreUserPreferencesRepository(PreferenceDataStoreFactory.create(scope = scope) { file })
        try {
            val original = repository(file)
            original.setDefaultCurrency("KWD")
            original.setSimplifyDebts(false)
            original.setBiometricLock(true)
            original.setTheme(ThemePreference.DARK)
            original.markNotificationsRead("alice", 2000)
            original.markNotificationsRead("alice", 1000)
            original.markNotificationsRead("bob", 3000)
            scope.coroutineContext[Job]!!.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val restored = repository(file)
            val features = restored.features.first()
            assertEquals("KWD", features.defaultCurrency)
            assertFalse(features.simplifyDebts)
            assertTrue(features.biometricLock)
            assertEquals(mapOf("alice" to 2000L, "bob" to 3000L), features.notificationReadAt)
            assertEquals(ThemePreference.DARK, restored.theme.first())
        } finally {
            scope.coroutineContext[Job]!!.cancelAndJoin()
            file.delete()
        }
    }
}
