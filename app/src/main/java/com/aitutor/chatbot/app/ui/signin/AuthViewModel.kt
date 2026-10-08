package com.aitutor.chatbot.app.ui.signin

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.data.auth.AuthError
import com.aitutor.chatbot.app.data.auth.AuthRepository
import com.aitutor.chatbot.app.data.auth.AuthResult
import com.aitutor.chatbot.app.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the sign-in screen draws. */
data class AuthUiState(
    val inProgress: Boolean = false,
    /** Why the last attempt failed, or null. A cancelled sheet is not a failure and sets nothing. */
    @param:StringRes val errorRes: Int? = null,
)

/**
 * Who is signed in, and the actions that change it.
 *
 * Shared by the sign-in screen, the session check on launch and the log-out button, so it is created
 * once for the whole navigation host rather than per screen.
 */
class AuthViewModel(private val auth: AuthRepository) : ViewModel() {

    /** Starts from what Firebase already saved on the phone, so the first value is never a guess. */
    val user: StateFlow<AuthUser?> = auth.user.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = auth.currentUser,
    )

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    /**
     * Runs the Google account sheet. Returns true once someone is signed in.
     *
     * A suspend function the screen calls from its own scope rather than a launch in [viewModelScope]:
     * the sheet is drawn over an [Activity], and a request that outlived the screen would hold on to
     * it. Leaving the screen cancels the request instead.
     */
    suspend fun signInWithGoogle(activity: Activity): Boolean {
        if (_state.value.inProgress) return false
        _state.value = AuthUiState(inProgress = true)
        return try {
            val result = auth.signInWithGoogle(activity)
            _state.value = AuthUiState(errorRes = (result as? AuthResult.Failed)?.error?.toMessageRes())
            result is AuthResult.Success
        } finally {
            settleIfCancelled()
        }
    }

    /**
     * "Skip for now". Never blocks: if the anonymous account cannot be made (no connection, or the
     * option is off in Firebase) the person still gets into the app, just without an account yet.
     */
    suspend fun continueWithoutAccount() {
        if (_state.value.inProgress) return
        _state.value = AuthUiState(inProgress = true)
        try {
            auth.continueAnonymously()
        } finally {
            _state.value = AuthUiState()
        }
    }

    /**
     * A request cancelled part-way — the person confirmed leaving, or the activity was recreated —
     * never reaches the line that clears [AuthUiState.inProgress]. This ViewModel outlives the
     * screen, so without it the buttons would stay disabled for as long as the app does.
     */
    private fun settleIfCancelled() {
        if (_state.value.inProgress) _state.value = AuthUiState()
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }
}

@StringRes
private fun AuthError.toMessageRes(): Int = when (this) {
    AuthError.NoGoogleAccount -> R.string.signin_error_no_account
    AuthError.NoPlayServices -> R.string.signin_error_no_play
    AuthError.Network -> R.string.signin_error_network
    AuthError.Failed -> R.string.signin_error_generic
}

/** The [Activity] behind a context, which Compose often hands over wrapped. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
