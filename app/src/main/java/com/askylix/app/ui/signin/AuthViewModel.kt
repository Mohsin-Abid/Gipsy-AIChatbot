package com.askylix.app.ui.signin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.askylix.app.R
import com.askylix.app.data.auth.AuthError
import com.askylix.app.data.auth.AuthRepository
import com.askylix.app.data.auth.AuthResult
import com.askylix.app.data.auth.AuthUser
import com.askylix.app.data.prefs.UserPreferencesRepository
import com.askylix.app.data.remote.ApiError
import com.askylix.app.data.repository.AccountRepository
import com.askylix.app.data.repository.SignInDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the sign-in screen draws. */
data class AuthUiState(
    val inProgress: Boolean = false,
    /** Why the last attempt failed, or null. A cancelled sheet is not a failure and sets nothing. */
    @param:StringRes val errorRes: Int? = null,
)

/** Where a sign-in ended up. */
sealed interface SignInResult {
    /** Signed in and set up: the account's profile is on the phone. */
    data object Home : SignInResult

    /** Signed in to a new account that has no answers yet: the wizard comes next. */
    data object Setup : SignInResult

    /** The sheet was dismissed, or the attempt failed. [AuthUiState.errorRes] says which. */
    data object NotSignedIn : SignInResult
}

/**
 * Who is signed in, and the actions that change it.
 *
 * Shared by the sign-in screen, the "sign in to continue" prompt, the session check on launch and the
 * log-out button, so it is created once for the whole navigation host rather than per screen.
 */
class AuthViewModel(
    private val auth: AuthRepository,
    private val account: AccountRepository,
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    /** Starts from what Firebase already saved on the phone, so the first value is never a guess. */
    val user: StateFlow<AuthUser?> = auth.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = auth.currentUser,
    )

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /**
     * Runs the Google account sheet, then finds out what the account needs.
     *
     * A suspend function the screen calls from its own scope rather than a launch in [viewModelScope]:
     * the sheet is drawn over an [Activity], and a request that outlived the screen would hold on to
     * it. Leaving the screen cancels the request instead.
     *
     * [alwaysUseSavedAnswers] is for signing in from inside the app after skipping: the answers
     * already on the phone set up a new account without asking for them again. From the sign-in screen
     * they are used only if setup was really done on this phone.
     */
    suspend fun signIn(activity: Activity, alwaysUseSavedAnswers: Boolean): SignInResult {
        if (_state.value.inProgress) return SignInResult.NotSignedIn
        _state.value = AuthUiState(inProgress = true)
        return try {
            val google = auth.signInWithGoogle(activity)
            if (google !is AuthResult.Success) {
                _state.value = AuthUiState(errorRes = (google as? AuthResult.Failed)?.error?.toMessageRes())
                return SignInResult.NotSignedIn
            }

            val settings = preferences.settings.first()
            val answers = settings.profile.takeIf { alwaysUseSavedAnswers || (settings.firstRunComplete && it.hasAnswers) }
            val destination = account.resolveAfterSignIn(answers, settings.language).getOrElse { failure ->
                // Google said yes but the app's own server could not be reached or refused the account.
                // Nobody should be left signed in to something that cannot work, so it is undone.
                account.signOut()
                _state.value = AuthUiState(errorRes = failure.toMessageRes())
                return SignInResult.NotSignedIn
            }

            _state.value = AuthUiState()
            when (destination) {
                SignInDestination.Home -> SignInResult.Home
                SignInDestination.Setup -> SignInResult.Setup
            }
        } finally {
            settleIfCancelled()
        }
    }

    fun signOut() {
        viewModelScope.launch { account.signOut() }
    }

    /** Deletes the account on the server and clears this phone. The result says whether a Play subscription is still running. */
    suspend fun deleteAccount() = account.deleteAccount()

    /**
     * A request cancelled part-way — the person confirmed leaving, or the activity was recreated —
     * never reaches the line that clears [AuthUiState.inProgress]. This ViewModel outlives the
     * screen, so without it the buttons would stay disabled for as long as the app does.
     */
    private fun settleIfCancelled() {
        if (_state.value.inProgress) _state.value = AuthUiState()
    }
}

@StringRes
private fun AuthError.toMessageRes(): Int = when (this) {
    AuthError.NoGoogleAccount -> R.string.signin_error_no_account
    AuthError.NoPlayServices -> R.string.signin_error_no_play
    AuthError.Network -> R.string.signin_error_network
    AuthError.Failed -> R.string.signin_error_generic
}

@StringRes
private fun Throwable.toMessageRes(): Int =
    if ((this as? ApiError)?.isConnectionProblem == true) R.string.signin_error_network else R.string.signin_error_server

/** The [Activity] behind a context, which Compose often hands over wrapped. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
