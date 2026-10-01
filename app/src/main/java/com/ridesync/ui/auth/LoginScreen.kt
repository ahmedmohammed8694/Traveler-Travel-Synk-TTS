package com.ridesync.ui.auth

import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.ridesync.R
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onEmailSignInClick: (email: String, pass: String) -> Unit,
    onGoogleSignInClick: (String) -> Unit,
    onNavigateToSignUp: () -> Unit,
    onForgotPasswordClick: (email: String) -> Unit = {},
    onGuestSignInClick: () -> Unit = {},
    onAuthError: ((String) -> Unit)? = null,
    isLoading: Boolean,
    errorMessage: String?,
    passwordResetStatusMessage: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val backgroundColor = com.ridesync.ui.theme.HudColors.ObsidianCanvas
    val accentColor = com.ridesync.ui.theme.HudColors.CyanPrimary
    val textFieldBg = com.ridesync.ui.theme.HudColors.ObsidianSurface

    val webClientId = remember(context) {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else "395720155616-qlsvl60qq4c6qti4nkt0lcqji04nkvbt.apps.googleusercontent.com"
        } catch (e: Exception) {
            "395720155616-qlsvl60qq4c6qti4nkt0lcqji04nkvbt.apps.googleusercontent.com"
        }
    }

    val googleSignInClient = remember(context, webClientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                onGoogleSignInClick(idToken)
            } else {
                Log.e("RideSyncAuth", "Google Sign-In returned null idToken")
                onAuthError?.invoke("Google Sign-In failed: no credential returned. Please try again.")
            }
        } catch (e: Exception) {
            val apiException = e as? ApiException
            val statusCode = apiException?.statusCode
            val detail = when (statusCode) {
                10 -> "Configuration error (Code 10: Developer Error)"
                12500 -> "Sign-in configuration error (Code 12500)"
                7 -> "Network error (Code 7)"
                else -> e.localizedMessage ?: "Sign-in was cancelled or failed"
            }
            Log.e("RideSyncAuth", "Google Sign-In intent failed: status=$statusCode, ${e.message}", e)
            onAuthError?.invoke("Google Sign-In: $detail. Please try again.")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        // High-Contrast Rally Instrument Graphic Background Pattern
        com.ridesync.ui.theme.RallyGridGraphicBackground()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // RRS Transparent Logo Header Graphic
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo_badge),
                contentDescription = "Riders Ride Sync (RRS) Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(110.dp)
                    .padding(bottom = 12.dp)
            )

            // App Title
            Text(
                text = "Riders Ride Sync (RRS)",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                letterSpacing = 0.5.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Motorcycle Convoy & Realtime Telemetry",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
            )

            // Error display card
            if (errorMessage != null) {
                Surface(
                    color = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFECACA),
                        modifier = Modifier.padding(14.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Password reset success banner
            if (passwordResetStatusMessage != null) {
                Surface(
                    color = Color(0xFF065F46),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Text(
                        text = passwordResetStatusMessage,
                        color = Color(0xFFA7F3D0),
                        modifier = Modifier.padding(14.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Email Address Input
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = accentColor)
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = textFieldBg,
                    unfocusedContainerColor = textFieldBg,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )
            )

            // Password Input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = accentColor)
                },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = icon, contentDescription = "Toggle password visibility", tint = com.ridesync.ui.theme.HudColors.TextCoolSilver)
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = textFieldBg,
                    unfocusedContainerColor = textFieldBg,
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                    focusedLabelColor = accentColor,
                    unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )
            )

            var showForgotPasswordDialog by remember { mutableStateOf(false) }
            var resetEmailInput by remember { mutableStateOf("") }

            // Forgot Password Link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "Forgot Password?",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        resetEmailInput = email
                        showForgotPasswordDialog = true
                    }
                )
            }

            if (showForgotPasswordDialog) {
                AlertDialog(
                    onDismissRequest = { showForgotPasswordDialog = false },
                    containerColor = textFieldBg,
                    title = {
                        Text("Reset Password", color = com.ridesync.ui.theme.HudColors.TextCrispWhite, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Column {
                            Text(
                                "Enter your registered email address to receive a password reset link:",
                                color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = resetEmailInput,
                                onValueChange = { resetEmailInput = it },
                                label = { Text("Email Address") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                                    focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                                    unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showForgotPasswordDialog = false
                                if (resetEmailInput.isNotBlank()) {
                                    onForgotPasswordClick(resetEmailInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                        ) {
                            Text("Send Link", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showForgotPasswordDialog = false }) {
                            Text("Cancel", color = com.ridesync.ui.theme.HudColors.TextCoolSilver)
                        }
                    }
                )
            }

            // Email/Password Sign In Button (Minimum 60dp height)
            Button(
                onClick = { onEmailSignInClick(email, password) },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                } else {
                    Text("Sign In with Email", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Or Divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = com.ridesync.ui.theme.HudColors.ObsidianBorder)
                Text(
                    text = "OR",
                    fontSize = 13.sp,
                    color = com.ridesync.ui.theme.HudColors.TextMuted,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = com.ridesync.ui.theme.HudColors.ObsidianBorder)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        triggerGoogleSignIn(
                            context = context,
                            webClientId = webClientId,
                            onIdTokenReceived = onGoogleSignInClick,
                            onFallbackNeeded = {
                                googleSignInLauncher.launch(googleSignInClient.signInIntent)
                            },
                            onError = { msg ->
                                onAuthError?.invoke(msg)
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                ),
                border = ButtonDefaults.outlinedButtonBorder(enabled = !isLoading).copy(
                    brush = Brush.horizontalGradient(listOf(com.ridesync.ui.theme.HudColors.ObsidianBorder, com.ridesync.ui.theme.HudColors.CobaltBlue))
                )
            ) {
                Text("Sign in with Google", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = com.ridesync.ui.theme.HudColors.TextCrispWhite)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Switch to Sign Up screen navigation link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Don't have an account? ", color = com.ridesync.ui.theme.HudColors.TextCoolSilver, fontSize = 15.sp)
                Text(
                    text = "Sign Up",
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.clickable(onClick = onNavigateToSignUp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private suspend fun triggerGoogleSignIn(
    context: Context,
    webClientId: String,
    onIdTokenReceived: (String) -> Unit,
    onFallbackNeeded: () -> Unit,
    onError: ((String) -> Unit)? = null
) {
    val credentialManager = CredentialManager.create(context)
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(false)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(request = request, context = context)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            onIdTokenReceived(googleIdTokenCredential.idToken)
        } else if (credential is GoogleIdTokenCredential) {
            onIdTokenReceived(credential.idToken)
        } else {
            onFallbackNeeded()
        }
    } catch (e: GetCredentialException) {
        Log.e("RideSyncAuth", "Credential Manager failed, launching GoogleSignInClient fallback: ${e.message}", e)
        onFallbackNeeded()
    } catch (e: Exception) {
        Log.e("RideSyncAuth", "Google Sign-In failed, launching fallback: ${e.message}", e)
        onFallbackNeeded()
    }
}
