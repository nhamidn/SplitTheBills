package com.dts.splitthebill

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SplitTheBillsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val firebaseApp = FirebaseEnvironment.initialize(this)
        if (firebaseApp == null) {
            Log.i(TAG, "Firebase configuration absent; running the credential-free UI scaffold")
            return
        }
        FirebaseEnvironment.configure(firebaseApp)
    }

    private companion object {
        const val TAG = "SplitTheBills"
    }
}
