package com.askylix.app.ui.signin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Shows [SignInPrompt] for [reason] while it is non-null, and signs in right there when asked.
 *
 * It is one place for every screen that needs an account — subscribing, and the History tab — so
 * each of them only says *why* it needs one. A successful sign-in calls [onSignedIn] and closes the
 * prompt; the screen underneath is untouched, and notices the new account by itself.
 */
@Composable
fun SignInGate(
    reason: SignInReason?,
    authViewModel: AuthViewModel,
    onDismiss: () -> Unit,
    onSignedIn: () -> Unit = onDismiss,
) {
    if (reason == null) return
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    SignInPrompt(
        reason = reason,
        inProgress = authState.inProgress,
        errorRes = authState.errorRes,
        onContinue = {
            context.findActivity()?.let { host ->
                scope.launch {
                    // The answers saved while skipping set a new account up without asking for them again.
                    if (authViewModel.signIn(host, alwaysUseSavedAnswers = true) != SignInResult.NotSignedIn) onSignedIn()
                }
            }
        },
        onDismiss = onDismiss,
    )
}
