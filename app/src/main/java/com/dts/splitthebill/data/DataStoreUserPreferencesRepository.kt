package com.dts.splitthebill.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.dts.splitthebill.domain.repository.FeaturePreferences
import com.dts.splitthebill.domain.repository.ThemePreference
import com.dts.splitthebill.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {
    // Security preferences must fail closed: a read error must never turn off the app lock.
    private val preferences = dataStore.data
    override val features: Flow<FeaturePreferences> = preferences.map { values ->
        FeaturePreferences(
            defaultCurrency = values[CURRENCY] ?: "EUR",
            simplifyDebts = values[SIMPLIFY] ?: true,
            biometricLock = values[LOCK] ?: false,
            notificationReadAt = values.asMap().entries.mapNotNull { (key, value) ->
                if (key.name.startsWith(READ_PREFIX) && value is Long) key.name.removePrefix(READ_PREFIX) to value else null
            }.toMap(),
        )
    }
    override val theme: Flow<ThemePreference> = dataStore.data
        .catch { failure ->
            if (failure is IOException) emit(emptyPreferences()) else throw failure
        }
        .map { preferences ->
            runCatching { ThemePreference.valueOf(preferences[THEME] ?: ThemePreference.SYSTEM.name) }
                .getOrDefault(ThemePreference.SYSTEM)
        }

    override suspend fun setTheme(value: ThemePreference) {
        dataStore.edit { it[THEME] = value.name }
    }

    override suspend fun setDefaultCurrency(value: String) { dataStore.edit { it[CURRENCY] = value } }
    override suspend fun setSimplifyDebts(value: Boolean) { dataStore.edit { it[SIMPLIFY] = value } }
    override suspend fun setBiometricLock(value: Boolean) { dataStore.edit { it[LOCK] = value } }
    override suspend fun markNotificationsRead(uid: String, throughEpochMillis: Long) {
        val key = longPreferencesKey("$READ_PREFIX$uid")
        dataStore.edit { it[key] = maxOf(it[key] ?: 0, throughEpochMillis) }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val CURRENCY = stringPreferencesKey("default_currency")
        val SIMPLIFY = booleanPreferencesKey("simplify_debts")
        val LOCK = booleanPreferencesKey("biometric_lock")
        const val READ_PREFIX = "notifications_read_at_"
    }
}
