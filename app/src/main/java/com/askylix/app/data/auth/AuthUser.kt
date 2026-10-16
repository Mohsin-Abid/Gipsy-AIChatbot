package com.askylix.app.data.auth

/**
 * Who is signed in, without any Firebase type attached.
 *
 * Screens and ViewModels see this and never a `FirebaseUser`, so nothing outside `data/auth` needs to
 * know which service the accounts live in.
 */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
) {
    /**
     * What to call them: the first word of the Google name, or an empty string when there is none.
     *
     * "Alex Morgan" greets as "Alex". Used only to prefill the setup name field, which the person can
     * change; it is a suggestion, never something written without them seeing it.
     */
    fun suggestedFirstName(): String =
        displayName?.trim()?.substringBefore(' ')?.trim().orEmpty()
}

/** Why signing in failed, in the terms a student can act on. */
enum class AuthError {
    /** No Google account on the phone, or sign-in is not set up for this build. */
    NoGoogleAccount,

    /** The phone has no Google Play services, which Google sign-in needs. */
    NoPlayServices,

    Network,
    Failed,
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult

    /** The person closed the account sheet. Not an error, so nothing is shown for it. */
    data object Cancelled : AuthResult

    data class Failed(val error: AuthError) : AuthResult
}
