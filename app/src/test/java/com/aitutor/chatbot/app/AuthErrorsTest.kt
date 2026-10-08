package com.aitutor.chatbot.app

import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.aitutor.chatbot.app.data.auth.AuthError
import com.aitutor.chatbot.app.data.auth.AuthResult
import com.aitutor.chatbot.app.data.auth.toAuthResult
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * How a sign-in failure is sorted into what a student is told. Every branch matters: the wrong one
 * either scolds someone who simply closed the sheet, or sends them to "add a Google account" when
 * the real problem is their connection.
 */
class AuthErrorsTest {

    @Test
    fun `closing the account sheet is a choice, not a failure`() {
        assertEquals(AuthResult.Cancelled, GetCredentialCancellationException().toAuthResult())
    }

    @Test
    fun `no account to choose from asks for a Google account`() {
        assertEquals(
            AuthResult.Failed(AuthError.NoGoogleAccount),
            NoCredentialException().toAuthResult(),
        )
    }

    @Test
    fun `a phone Google cannot reach at all is reported as missing Play services`() {
        assertEquals(
            AuthResult.Failed(AuthError.NoPlayServices),
            GetCredentialProviderConfigurationException().toAuthResult(),
        )
        assertEquals(
            AuthResult.Failed(AuthError.NoPlayServices),
            GetCredentialUnsupportedException().toAuthResult(),
        )
    }

    @Test
    fun `a dropped connection is reported as a network problem`() {
        assertEquals(AuthResult.Failed(AuthError.Network), IOException("reset").toAuthResult())
        // FirebaseNetworkException is handled by the same branch, but it cannot be built here: its
        // constructor calls Android's TextUtils, which does not exist on a plain JVM. That case is
        // exercised on a phone with the network switched off.
    }

    @Test
    fun `anything unrecognised falls back to a general failure`() {
        assertEquals(
            AuthResult.Failed(AuthError.Failed),
            IllegalStateException("surprise").toAuthResult(),
        )
        // An interrupted sheet can simply be tried again; it is not any of the specific causes above.
        assertEquals(
            AuthResult.Failed(AuthError.Failed),
            GetCredentialInterruptedException().toAuthResult(),
        )
    }
}
