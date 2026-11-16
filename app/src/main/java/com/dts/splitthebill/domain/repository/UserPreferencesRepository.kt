package com.dts.splitthebill.domain.repository

import kotlinx.coroutines.flow.Flow

enum class ThemePreference { SYSTEM, LIGHT, DARK }

data class FeaturePreferences(
    val defaultCurrency: String = "EUR",
    val simplifyDebts: Boolean = true,
    val biometricLock: Boolean = false,
    val notificationReadAt: Map<String, Long> = emptyMap(),
)

interface UserPreferencesRepository {
    val theme: Flow<ThemePreference>
    val features: Flow<FeaturePreferences>
    suspend fun setTheme(value: ThemePreference)
    suspend fun setDefaultCurrency(value: String)
    suspend fun setSimplifyDebts(value: Boolean)
    suspend fun setBiometricLock(value: Boolean)
    suspend fun markNotificationsRead(uid: String, throughEpochMillis: Long)
}
