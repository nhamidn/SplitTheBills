package com.dts.splitthebill.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
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

    private companion object {
        val THEME = stringPreferencesKey("theme")
    }
}
