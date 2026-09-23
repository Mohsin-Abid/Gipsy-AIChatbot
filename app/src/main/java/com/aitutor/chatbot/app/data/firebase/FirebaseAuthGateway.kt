package com.aitutor.chatbot.app.data.firebase

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Anonymous Auth. There is no login/signup UI anywhere in the app — every device
 * silently gets a persistent Firebase UID on first launch, used as the owner key for every
 * Firestore path (`users/{uid}/...`).
 */
class FirebaseAuthGateway(private val auth: FirebaseAuth) {

    val uid: Flow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    val currentUid: String? get() = auth.currentUser?.uid

    /** Blocks (suspends) until a Firebase UID exists for this device, signing in anonymously if needed. */
    suspend fun ensureSignedIn(): Result<String> = runCatching {
        val existing = auth.currentUser
        val uid = existing?.uid ?: auth.signInAnonymously().await().user?.uid
        uid ?: error("Anonymous sign-in returned no user")
    }

    suspend fun deleteAccount(): Result<Unit> = runCatching {
        auth.currentUser?.delete()?.await()
        Unit
    }

    fun signOut() = auth.signOut()
}
