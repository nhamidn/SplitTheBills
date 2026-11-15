package com.dts.splitthebill.data

import com.dts.splitthebill.data.remote.awaitResult
import com.dts.splitthebill.domain.model.SignedInUser
import com.dts.splitthebill.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {
    override val currentUser: Flow<SignedInUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            trySend(user?.let { SignedInUser(it.uid, it.email, it.displayName) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).awaitResult()
    }

    override suspend fun createAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password).awaitResult()
    }

    override suspend fun signInWithGoogle(idToken: String) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).awaitResult()
    }

    override suspend fun sendPasswordReset(email: String) {
        auth.sendPasswordResetEmail(email).awaitResult()
    }

    override suspend fun updateDisplayName(displayName: String) {
        val user = checkNotNull(auth.currentUser) { "Sign in again to update your profile." }
        user.updateProfile(
            UserProfileChangeRequest.Builder().setDisplayName(displayName).build(),
        ).awaitResult()
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}
