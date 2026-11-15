package com.dts.splitthebill

import android.content.Intent
import android.app.ActivityManager
import android.os.Bundle
import android.os.Process
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val pendingInvitationToken = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingInvitationToken.value = intent.invitationToken()
        enableEdgeToEdge()
        setContent {
            val viewModel: SplitTheBillsViewModel = hiltViewModel()
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
                SplitTheBillsApp(
                    state = state.value,
                    onAction = viewModel::onAction,
                    onGoogleSignIn = { beginGoogleSignIn(viewModel) },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingInvitationToken.value = intent.invitationToken()
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
