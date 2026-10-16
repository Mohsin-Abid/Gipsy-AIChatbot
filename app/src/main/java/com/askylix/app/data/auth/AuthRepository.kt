package com.askylix.app.data.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import com.askylix.app.data.remote.TokenProvider
import com.google.firebase.auth.FirebaseAuth
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
) : TokenProvider {

    init {
        // An earlier build signed people in anonymously when they skipped. Skipping is a browse mode
        // now, with no account, so a leftover anonymous session is ended rather than carried on.
        if (firebase.currentUser?.isAnonymous == true) firebase.signOut()
    }

    /** Who is signed in right now. Available at once, from what Firebase saved on the phone. */
    val currentUser: AuthUser? get() = firebase.currentUser?.takeUnless { it.isAnonymous }?.toAuthUser()

    /** Every change of who is signed in, starting with the current state. */
    val user: Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser?.takeUnless { it.isAnonymous }?.toAuthUser())
        }
        firebase.addAuthStateListener(listener)
        awaitClose { firebase.removeAuthStateListener(listener) }
    }

    /** The "Continue with Google" button. */
    suspend fun signInWithGoogle(activity: Activity): AuthResult = try {
        val idToken = google.requestIdToken(activity)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        AuthResult.Success(firebase.signInWithCredential(credential).await().user!!.toAuthUser())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        // The raw cause is the only evidence of *why* — a missing fingerprint and a missing account
        // look identical to the person holding the phone.
        Log.w(TAG, "Google sign-in failed", error)
        error.toAuthResult()
    }

    suspend fun signOut() {
        firebase.signOut()
        google.clearState()
    }

    /**
     * The token the API is called with. Firebase caches it and refreshes it before it expires;
     * [forceRefresh] is for a call the server refused, which means it was rejected anyway.
     */
    override suspend fun idToken(forceRefresh: Boolean): String? =
        firebase.currentUser?.getIdToken(forceRefresh)?.await()?.token

    private companion object {
        const val TAG = "AuthRepository"
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(
    uid = uid,
    email = email,
    displayName = displayName,
    photoUrl = photoUrl?.toString(),
)
