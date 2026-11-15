package com.dts.splitthebill

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

internal object FirebaseEnvironment {
    fun initialize(context: Context): FirebaseApp? =
        FirebaseApp.initializeApp(context) ?: FirebaseApp.initializeApp(
            context,
            FirebaseOptions.Builder()
                .setApplicationId("1:1234567890:android:split-the-bills-emulator")
                // Format-valid placeholder used only by Firebase SDK client-side validation.
                // It is not a credential and the demo project can only reach emulators.
                .setApiKey("AIzaSy000000000000000000000000000000000")
                .setProjectId("demo-split-the-bills")
                .build(),
        )

    fun configure(firebaseApp: FirebaseApp) {
        // The Emulator Suite has no App Check emulator. Installing a provider here would
        // contact production and block credential-free demo-project callable requests.
        // Release builds install Play Integrity and mutation Functions enforce App Check.
        FirebaseAuth.getInstance(firebaseApp).useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 9099)
        FirebaseFirestore.getInstance(firebaseApp).useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, 8080)
        // Debug callable mutations use the emulator-only HTTP transport. This avoids the
        // Firebase Functions SDK's production Firebase Installations token dependency.
    }
}
