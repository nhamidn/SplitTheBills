package com.dts.splitthebill.di

import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReleaseFirebaseModule {
    @Provides
    @Singleton
    fun provideFunctions(firebaseApp: FirebaseApp): FirebaseFunctions =
        FirebaseFunctions.getInstance(firebaseApp, "europe-west1")
}
