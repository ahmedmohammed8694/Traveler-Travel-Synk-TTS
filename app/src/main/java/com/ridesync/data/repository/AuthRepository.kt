package com.ridesync.data.model

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: AuthUser?
    
    fun signInWithGoogleIdToken(idToken: String): Flow<Result<AuthUser>>
    fun signUpWithEmail(email: String, password: String, displayName: String): Flow<Result<AuthUser>>
    fun signInWithEmail(email: String, password: String): Flow<Result<AuthUser>>
    fun sendPasswordResetEmail(email: String): Flow<Result<Unit>>
    fun fetchUserProfile(userId: String): Flow<Result<UserProfile?>>
    fun saveUserProfile(userProfile: UserProfile): Flow<Result<Unit>>
    fun signOut()
}
