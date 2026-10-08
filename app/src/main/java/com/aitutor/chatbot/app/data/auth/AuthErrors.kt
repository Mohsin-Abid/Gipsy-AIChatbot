package com.aitutor.chatbot.app.data.auth

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import java.io.IOException

/**
 * Sorts whatever sign-in threw into the few outcomes a screen cares about.
 *
 * Pure on purpose: it takes an exception and returns a value, so every branch can be tested without a
 * phone, a Google account or a network.
 */
internal fun Throwable.toAuthResult(): AuthResult = when (this) {
    // Backing out of the sheet is a choice, not a failure.
    is GetCredentialCancellationException -> AuthResult.Cancelled

    // Nothing to choose from. Also what a misconfigured build looks like (a fingerprint Firebase has
    // not been told about), which is why the cause is logged where this is called.
    is NoCredentialException -> AuthResult.Failed(AuthError.NoGoogleAccount)

    // The Google side of the phone cannot be reached at all: no Play services, or too old.
    is GetCredentialProviderConfigurationException,
    is GetCredentialUnsupportedException,
    -> AuthResult.Failed(AuthError.NoPlayServices)

    is FirebaseNetworkException, is IOException -> AuthResult.Failed(AuthError.Network)

    else -> AuthResult.Failed(AuthError.Failed)
}
