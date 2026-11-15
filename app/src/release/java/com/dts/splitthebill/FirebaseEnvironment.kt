package com.dts.splitthebill

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

internal object FirebaseEnvironment {
    fun initialize(context: Context): FirebaseApp? = FirebaseApp.initializeApp(context)

    fun configure(firebaseApp: FirebaseApp) {
        FirebaseAppCheck.getInstance(firebaseApp).installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance(),
        )
    }
}
