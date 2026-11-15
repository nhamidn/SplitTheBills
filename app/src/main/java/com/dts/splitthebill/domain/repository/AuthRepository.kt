package com.dts.splitthebill.domain.repository

import com.dts.splitthebill.domain.model.SignedInUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<SignedInUser?>
    suspend fun signIn(email: String, password: String)
    suspend fun createAccount(email: String, password: String)
    suspend fun signInWithGoogle(idToken: String)
    suspend fun sendPasswordReset(email: String)
    suspend fun updateDisplayName(displayName: String)
    suspend fun signOut()
}
