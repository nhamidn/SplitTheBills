package com.dts.splitthebill

import android.app.Activity
import android.app.KeyguardManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** The preference changes only after a successful system authentication, including when disabling. */
internal class DeviceAuthenticator(private val activity: FragmentActivity) {
    private var success: (() -> Unit)? = null
    private var failure: ((String) -> Unit)? = null
    private val credentials = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) succeeded() else failed("Authentication cancelled. The app remains locked.")
    }
    private val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = succeeded()
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = failed(errString.toString())
    })

    @Suppress("DEPRECATION")
    fun authenticate(onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        if (success != null) return
        success = onSuccess
        failure = onFailure
        val allowed = if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK
        if (BiometricManager.from(activity).canAuthenticate(allowed) == BiometricManager.BIOMETRIC_SUCCESS) {
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Split the Bills")
                .setSubtitle("Confirm your identity on this device")
                .setAllowedAuthenticators(allowed)
            if (Build.VERSION.SDK_INT < 30) info.setNegativeButtonText("Cancel")
            prompt.authenticate(info.build())
        } else if (Build.VERSION.SDK_INT < 30 && activity.getSystemService(KeyguardManager::class.java).isDeviceSecure) {
            val intent = activity.getSystemService(KeyguardManager::class.java)
                .createConfirmDeviceCredentialIntent("Unlock Split the Bills", "Confirm your device screen lock")
            if (intent != null) credentials.launch(intent) else failed("Device authentication is unavailable.")
        } else {
            failed("Set up a device screen lock or supported fingerprint in Android settings first.")
        }
    }

    private fun succeeded() {
        val callback = success
        success = null
        failure = null
        callback?.invoke()
    }

    private fun failed(message: String) {
        val callback = failure
        success = null
        failure = null
        callback?.invoke(message)
    }
}
