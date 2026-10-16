package com.askylix.app.data.auth

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
    // Backing out of the sheet is a choice, not a failure. But Google also reports "I could not verify
    // this account" as a cancellation, with "[16] Account reauth failed." as the message: a stale
    // account on the phone, or an app Google has no record of (the package or its signing fingerprint
    // is not registered). Nobody chose that, so it is shown as a failure rather than swallowed.
    is GetCredentialCancellationException ->
        if (message.orEmpty().contains("reauth", ignoreCase = true)) AuthResult.Failed(AuthError.Failed) else AuthResult.Cancelled

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
