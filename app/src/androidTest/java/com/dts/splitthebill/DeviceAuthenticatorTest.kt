package com.dts.splitthebill

import android.content.Context
import android.view.ContextThemeWrapper
import androidx.appcompat.app.AlertDialog
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test

class DeviceAuthenticatorTest {
    @Test fun appThemeCanInflateTheFingerprintFallbackUsedOnOlderAndroidVersions() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_SplitTheBill)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            // The pinned Biometric library uses this dialog on API 24–27. A platform-only
            // activity theme throws at show(), even though the modern system prompt works.
            val dialog = AlertDialog.Builder(context).setTitle("Unlock Split the Bills").create()
            // Inflate onCreate without needing an activity's window token.
            dialog.create()
        }
    }
}
