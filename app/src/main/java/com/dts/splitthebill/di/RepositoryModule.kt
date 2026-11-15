package com.dts.splitthebill.di

import com.dts.splitthebill.data.AndroidNetworkMonitor
import com.dts.splitthebill.data.FirebaseAuthRepository
import com.dts.splitthebill.data.FirebaseGroupsRepository
import com.dts.splitthebill.data.RoomExpenseDraftRepository
import com.dts.splitthebill.data.DataStoreUserPreferencesRepository
import com.dts.splitthebill.domain.repository.AuthRepository
import com.dts.splitthebill.domain.repository.GroupsRepository
import com.dts.splitthebill.domain.repository.ExpenseDraftRepository
import com.dts.splitthebill.domain.repository.NetworkMonitor
import com.dts.splitthebill.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindAuthRepository(implementation: FirebaseAuthRepository): AuthRepository

    @Binds
    abstract fun bindGroupsRepository(implementation: FirebaseGroupsRepository): GroupsRepository

    @Binds
    abstract fun bindExpenseDraftRepository(implementation: RoomExpenseDraftRepository): ExpenseDraftRepository

    @Binds
    abstract fun bindNetworkMonitor(implementation: AndroidNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun bindUserPreferencesRepository(implementation: DataStoreUserPreferencesRepository): UserPreferencesRepository
}
