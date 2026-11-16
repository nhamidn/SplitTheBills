package com.dts.splitthebill

import android.content.Intent
import android.app.ActivityManager
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.isSystemInDarkTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.dts.splitthebill.ui.SplitTheBillsAction
import com.dts.splitthebill.ui.SplitTheBillsApp
import com.dts.splitthebill.ui.SplitTheBillsViewModel
import com.dts.splitthebill.ui.theme.SplitTheBillsTheme
import com.dts.splitthebill.domain.repository.ThemePreference
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val pendingInvitationToken = MutableStateFlow<String?>(null)
    private val viewModel: SplitTheBillsViewModel by viewModels()
    private lateinit var deviceAuthenticator: DeviceAuthenticator
    private var unlocked by mutableStateOf(false)
    private var foreground by mutableStateOf(false)
    private var lockError by mutableStateOf<String?>(null)
    private var lockEnabled = false
    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri == null) {
            viewModel.onAction(SplitTheBillsAction.ExportFinished())
        } else {
            val document = viewModel.state.value.exportDocument
            if (document == null) {
                viewModel.onAction(SplitTheBillsAction.ExportFinished(error = "Export expired. Please try exporting again."))
            } else lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val output = contentResolver.openOutputStream(uri, "wt") ?: throw java.io.IOException("No writable document")
                        output.bufferedWriter(Charsets.UTF_8).use { it.write(document.contents) }
                    }
                    viewModel.onAction(SplitTheBillsAction.ExportFinished(saved = true))
                } catch (_: java.io.IOException) {
                    viewModel.onAction(SplitTheBillsAction.ExportFinished(error = "Could not save the CSV. Choose a writable location and try again."))
                } catch (_: SecurityException) {
                    viewModel.onAction(SplitTheBillsAction.ExportFinished(error = "Access to that document was denied. Try another location."))
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deviceAuthenticator = DeviceAuthenticator(this)
        pendingInvitationToken.value = intent.invitationToken()
        enableEdgeToEdge()
        setContent {
            val state = viewModel.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (state.value.themePreference) {
                ThemePreference.SYSTEM -> systemDark
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }
            LaunchedEffect(darkTheme) {
                val barStyle = SystemBarStyle.auto(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT,
                ) { darkTheme }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            SplitTheBillsTheme(darkTheme = darkTheme) {
                LaunchedEffect(state.value.exportRequested) {
                    if (state.value.exportRequested) state.value.exportDocument?.let { document ->
                        viewModel.onAction(SplitTheBillsAction.ExportPickerOpened)
                        exportLauncher.launch(document.fileName)
                    }
                }
                LaunchedEffect(state.value.preferences.biometricLock) {
                    lockEnabled = state.value.preferences.biometricLock
                    if (lockEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
                val locked = state.value.preferences.biometricLock && !unlocked
                LaunchedEffect(locked, foreground, state.value.preferencesResolved) {
                    if (locked && foreground && state.value.preferencesResolved) unlock()
                }
                val invitationToken = pendingInvitationToken.collectAsStateWithLifecycle()
                LaunchedEffect(invitationToken.value) {
                    invitationToken.value?.let { token ->
                        viewModel.onAction(SplitTheBillsAction.InvitationLinkReceived(token))
                        pendingInvitationToken.value = null
                    }
                }
                LaunchedEffect(state.value.accountDeleted) {
                    if (state.value.accountDeleted) {
                        Toast.makeText(
                            this@MainActivity,
                            "Account deleted and local data cleared",
                            Toast.LENGTH_LONG,
                        ).show()
                        delay(800)
                        val cleared = getSystemService(ActivityManager::class.java)
                            .clearApplicationUserData()
                        if (!cleared) {
                            finishAndRemoveTask()
                            Process.killProcess(Process.myPid())
                        }
                    }
                }
                when {
                    !state.value.preferencesResolved -> Surface(Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) { Text(state.value.error ?: "Loading preferences…") }
                    }
                    locked -> Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Split the Bills is locked", style = MaterialTheme.typography.headlineSmall)
                            lockError?.let { Text(it, modifier = Modifier.padding(vertical = 16.dp)) }
                            Button(onClick = { unlock() }) { Text("Unlock") }
                        }
                    }
                    else -> SplitTheBillsApp(
                        state = state.value,
                        onAction = viewModel::onAction,
                        onGoogleSignIn = { beginGoogleSignIn(viewModel) },
                        onBiometricLockChange = { enabled ->
                            deviceAuthenticator.authenticate(onSuccess = {
                                unlocked = true
                                viewModel.onAction(SplitTheBillsAction.BiometricLockChanged(enabled))
                            }, onFailure = { viewModel.onAction(SplitTheBillsAction.ReportError(it)) })
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingInvitationToken.value = intent.invitationToken()
    }

    override fun onResume() { super.onResume(); foreground = true }
    override fun onPause() { foreground = false; super.onPause() }
    override fun onStop() {
        if (lockEnabled && !isChangingConfigurations) unlocked = false
        super.onStop()
    }

    private fun unlock() {
        deviceAuthenticator.authenticate(onSuccess = { unlocked = true; lockError = null }, onFailure = { lockError = it })
    }

    private fun beginGoogleSignIn(viewModel: SplitTheBillsViewModel) {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            viewModel.onAction(SplitTheBillsAction.GoogleUnavailable)
            return
        }
        lifecycleScope.launch {
            try {
                val googleOption = GetGoogleIdOption.Builder()
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleOption)
                    .build()
                val result = CredentialManager.create(this@MainActivity)
                    .getCredential(this@MainActivity, request)
                val credential = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.onAction(SplitTheBillsAction.GoogleTokenReceived(credential.idToken))
            } catch (_: NoCredentialException) {
                viewModel.onAction(
                    SplitTheBillsAction.GoogleSignInFailed("No Google account is available on this device."),
                )
            } catch (failure: Exception) {
                viewModel.onAction(
                    SplitTheBillsAction.GoogleSignInFailed(
                        failure.message ?: "Google sign-in was cancelled.",
                    ),
                )
            }
        }
    }
}

private fun Intent.invitationToken(): String? = data
    ?.takeIf { it.scheme == "https" && it.path?.startsWith("/invite") == true }
    ?.getQueryParameter("token")
    ?.takeIf(String::isNotBlank)
