package com.dts.splitthebill.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemePreference { SYSTEM, LIGHT, DARK }

interface UserPreferencesRepository {
    val theme: Flow<ThemePreference>
    suspend fun setTheme(value: ThemePreference)
}
