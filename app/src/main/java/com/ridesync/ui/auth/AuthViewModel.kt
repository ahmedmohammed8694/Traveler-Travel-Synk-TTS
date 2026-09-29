package com.ridesync.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ridesync.data.model.AuthRepository
import com.ridesync.data.model.AuthUser
import com.ridesync.data.model.PrivacySettings
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.AuthRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    LOGIN,
    SIGNUP
}

sealed interface AuthState {
    data object Idle : AuthState
    data object Authenticating : AuthState
    data class ProfileSetupRequired(val user: AuthUser) : AuthState
    data class Authenticated(val userProfile: UserProfile) : AuthState
    data class Error(val message: String) : AuthState
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    private val _screenMode = MutableStateFlow(AuthScreenMode.LOGIN)
    val screenMode: StateFlow<AuthScreenMode> = _screenMode.asStateFlow()

    init {
        checkExistingSession()
    }

    fun setScreenMode(mode: AuthScreenMode) {
        _screenMode.value = mode
        if (_uiState.value is AuthState.Error) {
            _uiState.value = AuthState.Idle
        }
    }

    fun checkExistingSession() {
        val user = repository.currentUser
        if (user != null) {
            loadUserProfile(user)
        } else {
            _uiState.value = AuthState.Idle
        }
    }

    fun onGoogleIdTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.signInWithGoogleIdToken(idToken).collect { result ->
                result.fold(
                    onSuccess = { authUser ->
                        loadUserProfile(authUser)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            throwable.localizedMessage ?: "Google Authentication failed"
                        )
                    }
                )
            }
        }
    }

    fun signInAnonymously() {
        val demoProfile = UserProfile(
            userId = "demo_rider_101",
            displayName = "Lead Rider (Demo)",
            email = "demo@ridesync.app",
            vehicleModel = "BMW R1250GS Adventure"
        )
        _uiState.value = AuthState.Authenticated(demoProfile)
    }

    fun signUpWithEmail(
        email: String,
        password: String,
        confirmPassword: String,
        displayName: String
    ) {
        val trimmedEmail = email.trim()
        val trimmedName = displayName.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = AuthState.Error("Please enter your full name.")
            return
        }

        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthState.Error("Please enter a valid email address.")
            return
        }

        if (password != confirmPassword) {
            _uiState.value = AuthState.Error("Passwords do not match.")
            return
        }

        val passwordValidationReason = getPasswordValidationErrorMessage(password)
        if (passwordValidationReason != null) {
            _uiState.value = AuthState.Error(passwordValidationReason)
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.signUpWithEmail(trimmedEmail, password, trimmedName).collect { result ->
                result.fold(
                    onSuccess = { authUser ->
                        loadUserProfile(authUser)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            throwable.localizedMessage ?: "Sign up failed. Please check your connection."
                        )
                    }
                )
            }
        }
    }

    fun signInWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()

        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthState.Error("Please enter a valid email address.")
            return
        }

        if (password.isEmpty()) {
            _uiState.value = AuthState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.signInWithEmail(trimmedEmail, password).collect { result ->
                result.fold(
                    onSuccess = { authUser ->
                        loadUserProfile(authUser)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            throwable.localizedMessage ?: "Invalid email or password."
                        )
                    }
                )
            }
        }
    }

    private val _passwordResetStatus = MutableStateFlow<String?>(null)
    val passwordResetStatus: StateFlow<String?> = _passwordResetStatus.asStateFlow()

    fun sendPasswordResetEmail(email: String) {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthState.Error("Please enter a valid email address.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.sendPasswordResetEmail(trimmedEmail).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthState.Idle
                        _passwordResetStatus.value = "Password reset link sent to $trimmedEmail. Check your inbox!"
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            throwable.localizedMessage ?: "Failed to send password reset email."
                        )
                    }
                )
            }
        }
    }

    fun clearPasswordResetStatus() {
        _passwordResetStatus.value = null
    }

    private fun loadUserProfile(authUser: AuthUser) {
        viewModelScope.launch {
            repository.fetchUserProfile(authUser.uid).collect { result ->
                result.fold(
                    onSuccess = { profile ->
                        if (profile != null && profile.vehicleModel.isNotBlank()) {
                            _uiState.value = AuthState.Authenticated(profile)
                        } else {
                            _uiState.value = AuthState.ProfileSetupRequired(authUser)
                        }
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.ProfileSetupRequired(authUser)
                    }
                )
            }
        }
    }

    fun saveUserProfile(
        vehicleModel: String,
        tankCapacityLiters: Double,
        shareLocationWithGroup: Boolean,
        emergencyContactPhone: String
    ) {
        val currentState = _uiState.value
        val authUser = when (currentState) {
            is AuthState.ProfileSetupRequired -> currentState.user
            is AuthState.Authenticated -> repository.currentUser
            else -> repository.currentUser
        } ?: run {
            _uiState.value = AuthState.Error("No authenticated session found")
            return
        }

        val newProfile = UserProfile(
            userId = authUser.uid,
            displayName = authUser.displayName.ifBlank { "Rider" },
            email = authUser.email,
            photoUrl = authUser.photoUrl,
            vehicleModel = vehicleModel.trim(),
            tankCapacityLiters = tankCapacityLiters,
            privacySettings = PrivacySettings(
                shareLocationWithGroup = shareLocationWithGroup,
                emergencyContactPhone = emergencyContactPhone.trim()
            )
        )

        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.saveUserProfile(newProfile).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthState.Authenticated(newProfile)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            "Failed to save profile: ${throwable.localizedMessage}"
                        )
                    }
                )
            }
        }
    }

    fun saveFullUserProfile(
        dateOfBirth: String,
        initialVehicle: com.ridesync.data.model.Vehicle,
        shareLocationWithGroup: Boolean,
        emergencyContactPhone: String
    ) {
        val currentState = _uiState.value
        val authUser = when (currentState) {
            is AuthState.ProfileSetupRequired -> currentState.user
            is AuthState.Authenticated -> repository.currentUser
            else -> repository.currentUser
        } ?: run {
            _uiState.value = AuthState.Error("No authenticated session found")
            return
        }

        val activeVeh = initialVehicle.copy(isActive = true)
        val newProfile = UserProfile(
            userId = authUser.uid,
            displayName = authUser.displayName.ifBlank { "Rider" },
            email = authUser.email,
            dateOfBirth = dateOfBirth.trim(),
            photoUrl = authUser.photoUrl,
            vehicleModel = activeVeh.fullDisplayName,
            tankCapacityLiters = activeVeh.fuelTankCapacity,
            vehicles = listOf(activeVeh),
            activeVehicleId = activeVeh.id,
            privacySettings = PrivacySettings(
                shareLocationWithGroup = shareLocationWithGroup,
                emergencyContactPhone = emergencyContactPhone.trim()
            )
        )

        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.saveUserProfile(newProfile).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthState.Authenticated(newProfile)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            "Failed to save profile: ${throwable.localizedMessage}"
                        )
                    }
                )
            }
        }
    }

    fun updateFullUserProfile(updatedProfile: UserProfile) {
        viewModelScope.launch {
            _uiState.value = AuthState.Authenticating
            repository.saveUserProfile(updatedProfile).collect { result ->
                result.fold(
                    onSuccess = {
                        _uiState.value = AuthState.Authenticated(updatedProfile)
                    },
                    onFailure = { throwable ->
                        _uiState.value = AuthState.Error(
                            "Failed to update profile: ${throwable.localizedMessage}"
                        )
                    }
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthState.Error) {
            _uiState.value = AuthState.Idle
        }
    }

    fun setError(message: String) {
        _uiState.value = AuthState.Error(message)
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = AuthState.Idle
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
        return emailRegex.matches(email)
    }

    private fun getPasswordValidationErrorMessage(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters long."
        if (!password.any { it.isUpperCase() }) return "Password must contain at least one uppercase letter."
        if (!password.any { it.isLowerCase() }) return "Password must contain at least one lowercase letter."
        if (!password.any { it.isDigit() }) return "Password must contain at least one number."
        return null
    }
}
