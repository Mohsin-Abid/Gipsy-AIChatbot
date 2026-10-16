package com.askylix.app.data.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.askylix.app.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * Asks the phone for a Google account, and returns the ID token Google signs for it.
 *
 * This is the **Credential Manager** half of sign-in: the system draws the account sheet and hands
 * back what was picked. It knows nothing about Firebase; turning the token into an account is
 * [AuthRepository]'s job.
 *
 * `GetSignInWithGoogleOption` is the explicit "Sign in with Google" button flow, which is what the
 * sign-in screen has. The quieter option for returning users is not used: Firebase remembers the
 * login on its own, so there is never a returning user to recognise.
 */
class GoogleCredentialSource(context: Context) {

    private val appContext = context.applicationContext
    private val manager by lazy { CredentialManager.create(appContext) }

    /**
     * Shows the account sheet and returns the chosen account's ID token.
     *
     * Needs an [Activity], not the application context: the sheet is drawn over a window, and an
     * application context has none to draw over. Throws a `GetCredentialException` for a cancelled
     * sheet, no account, or no Play services; [toAuthResult] sorts those.
     */
    suspend fun requestIdToken(activity: Activity): String {
        // Generated from google-services.json by the Google Services plugin: the web client Firebase
        // created when Google sign-in was switched on. It is the audience the token is issued for.
        val webClientId = appContext.getString(R.string.default_web_client_id)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()

        val credential = manager.getCredential(activity, request).credential
        check(
            credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) { "Unexpected credential type: ${credential.type}" }

        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }

    /**
     * Forgets which account was used, so the next sign-in shows the chooser rather than silently
     * picking the same one. Best effort: failing to clear must never stop a sign-out.
     */
    suspend fun clearState() {
        runCatching { manager.clearCredentialState(ClearCredentialStateRequest()) }
    }
}
