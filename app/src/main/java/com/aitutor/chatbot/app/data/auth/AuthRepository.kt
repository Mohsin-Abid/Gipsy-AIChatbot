package com.aitutor.chatbot.app.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Accounts, on Firebase Authentication.
 *
 * Firebase is where a Google identity becomes an account: it takes the ID token, finds or creates the
 * user, **remembers the login on the phone**, and refreshes its own tokens by itself. That is why
 * there is no token storage or refresh logic anywhere in this app.
 */
class AuthRepository(
    context: Context,
    private val firebase: FirebaseAuth = FirebaseAuth.getInstance(),
    private val google: GoogleCredentialSource = GoogleCredentialSource(context),
) {

    /** Who is signed in right now. Available at once, from what Firebase saved on the phone. */
    val currentUser: AuthUser? get() = firebase.currentUser?.toAuthUser()

    /** Every change of who is signed in, starting with the current state. */
    val user: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth -> trySend(auth.currentUser?.toAuthUser()) }
        firebase.addAuthStateListener(listener)
        awaitClose { firebase.removeAuthStateListener(listener) }
    }

    /**
     * The "Continue with Google" button.
     *
     * Someone who skipped sign-in earlier has an anonymous account. The Google identity is *linked* to
     * it, so they keep the same `uid` and anything already attached to it. If that Google account
     * already exists from another phone, it is used instead.
     */
    suspend fun signInWithGoogle(activity: Activity): AuthResult = try {
        val idToken = google.requestIdToken(activity)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        AuthResult.Success(linkOrSignIn(credential))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        // The raw cause is the only evidence of *why* — a missing fingerprint and a missing account
        // look identical to the person holding the phone.
        Log.w(TAG, "Google sign-in failed", error)
        error.toAuthResult()
    }

    /** "Skip for now": an account with no identity behind it, so every request still has a `uid`. */
    suspend fun continueAnonymously(): AuthResult {
        firebase.currentUser?.let { return AuthResult.Success(it.toAuthUser()) }
        return try {
            AuthResult.Success(firebase.signInAnonymously().await().user!!.toAuthUser())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            Log.w(TAG, "Anonymous sign-in failed", error)
            error.toAuthResult()
        }
    }

    suspend fun signOut() {
        firebase.signOut()
        google.clearState()
    }

    /**
     * The token to send as `Authorization: Bearer …` once the API exists. Firebase caches it and
     * refreshes it before it expires; [forceRefresh] is for a 401, which means it was rejected anyway.
     */
    suspend fun idToken(forceRefresh: Boolean = false): String? =
        firebase.currentUser?.getIdToken(forceRefresh)?.await()?.token

    private suspend fun linkOrSignIn(credential: AuthCredential): AuthUser {
        val current = firebase.currentUser
        if (current != null && current.isAnonymous) {
            try {
                return current.linkWithCredential(credential).await().user!!.toAuthUser()
            } catch (_: FirebaseAuthUserCollisionException) {
                // This Google account already has a Firebase user, so there is nothing to link to.
                // Fall through and sign in as that user.
            }
        }
        return firebase.signInWithCredential(credential).await().user!!.toAuthUser()
    }

    private companion object {
        const val TAG = "AuthRepository"
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
    isAnonymous = isAnonymous,
)
