package com.ridesync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.ridesync.ui.MainContainerScreen
import com.ridesync.ui.auth.*
import com.ridesync.ui.profile.UserProfileScreen

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sanitizeIncomingIntent(intent)
        setContent {
            com.ridesync.ui.theme.RideSyncTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = com.ridesync.ui.theme.HudColors.ObsidianCanvas
                ) {
                    val authState by authViewModel.uiState.collectAsState()
                    val screenMode by authViewModel.screenMode.collectAsState()
                    val passwordResetStatus by authViewModel.passwordResetStatus.collectAsState()

                    when (val state = authState) {
                        is AuthState.Idle, is AuthState.Authenticating, is AuthState.Error -> {
                            val errorMessage = (state as? AuthState.Error)?.message
                            val isLoading = state is AuthState.Authenticating

                            when (screenMode) {
                                AuthScreenMode.LOGIN -> {
                                    LoginScreen(
                                        onEmailSignInClick = { email, pass ->
                                            authViewModel.signInWithEmail(email, pass)
                                        },
                                        onGoogleSignInClick = { idToken ->
                                            authViewModel.onGoogleIdTokenReceived(idToken)
                                        },
                                        onNavigateToSignUp = {
                                            authViewModel.setScreenMode(AuthScreenMode.SIGNUP)
                                        },
                                        onForgotPasswordClick = { email ->
                                            authViewModel.sendPasswordResetEmail(email)
                                        },
                                        onGuestSignInClick = {
                                            authViewModel.signInAnonymously()
                                        },
                                        onAuthError = { message ->
                                            authViewModel.setError(message)
                                        },
                                        isLoading = isLoading,
                                        errorMessage = errorMessage,
                                        passwordResetStatusMessage = passwordResetStatus
                                    )
                                }

                                AuthScreenMode.SIGNUP -> {
                                    SignUpScreen(
                                        onSignUpClick = { name, email, pass, confirmPass ->
                                            authViewModel.signUpWithEmail(email, pass, confirmPass, name)
                                        },
                                        onGoogleSignUpClick = { idToken ->
                                            authViewModel.onGoogleIdTokenReceived(idToken)
                                        },
                                        onNavigateToLogin = {
                                            authViewModel.setScreenMode(AuthScreenMode.LOGIN)
                                        },
                                        onAuthError = { message ->
                                            authViewModel.setError(message)
                                        },
                                        isLoading = isLoading,
                                        errorMessage = errorMessage
                                    )
                                }
                            }
                        }

                        is AuthState.ProfileSetupRequired -> {
                            ProfileSetupScreen(
                                initialDisplayName = state.user.displayName.ifBlank { "Rider" },
                                onSaveProfile = { vehicle, tank, shareLoc, phone ->
                                    authViewModel.saveUserProfile(vehicle, tank, shareLoc, phone)
                                },
                                onSaveFullProfile = { dob, initVeh, shareLoc, phone ->
                                    authViewModel.saveFullUserProfile(dob, initVeh, shareLoc, phone)
                                },
                                isLoading = false
                            )
                        }

                        is AuthState.Authenticated -> {
                            MainContainerScreen(
                                userProfile = state.userProfile,
                                onSaveUserProfile = { updatedProfile ->
                                    authViewModel.updateFullUserProfile(updatedProfile)
                                },
                                onSignOut = {
                                    authViewModel.signOut()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        sanitizeIncomingIntent(intent)
    }

    private fun sanitizeIncomingIntent(intent: Intent?) {
        if (intent == null) return
        try {
            // Check for deep link join code
            val data = intent.data
            if (data != null) {
                val code = if (data.scheme == "ridesync") {
                    data.getQueryParameter("code")
                } else if (data.host == "ridesync.app") {
                    data.lastPathSegment
                } else null

                if (!code.isNullOrBlank()) {
                    Log.i("MainActivity", "Deep link received with Lobby Code: $code")
                    com.ridesync.data.repository.TripRepository.getAllTrips().firstOrNull { 
                        it.lobbyCode.equals(code, ignoreCase = true) || it.tripId.contains(code, ignoreCase = true) 
                    }?.let { matchingTrip ->
                        com.ridesync.data.repository.TripRepository.setOngoingTrip(matchingTrip.tripId)
                    }
                }
            }

            // Strip any nested un-sanitized Intent extras to prevent Intent Redirection vulnerabilities
            val extras = intent.extras
            if (extras != null) {
                for (key in extras.keySet()) {
                    val value = extras.get(key)
                    if (value is Intent) {
                        Log.w("MainActivity", "Blocked potentially unsafe nested Intent extra: $key")
                        intent.removeExtra(key)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to sanitize incoming intent extras", e)
        }
    }
}

