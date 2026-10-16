package com.askylix.app.ui.signin

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.askylix.app.R
import com.askylix.app.ui.components.ConfirmTone
import com.askylix.app.ui.components.DialogCard
import com.askylix.app.ui.components.DialogTextAction
import com.askylix.app.ui.components.DialogWindow
import com.askylix.app.ui.components.PrimaryCta
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.appColors

/** What the person was about to do when the prompt appeared, which is what it explains. */
enum class SignInReason(@param:StringRes val bodyRes: Int) {
    AskQuestion(R.string.signin_prompt_send_body),
    Subscribe(R.string.signin_prompt_buy_body),
    ViewHistory(R.string.signin_prompt_history_body),
}

/**
 * "Sign in to continue", shown when someone who skipped sign-in tries to ask a question or to subscribe.
 *
 * Skipping is a browse mode with no account, and both of those need one: a question is answered for an
 * account, and a subscription belongs to one. The prompt does not navigate anywhere. Signing in happens
 * in place, and afterwards the person is exactly where they were, with the question still typed and
 * the plan still chosen. Nothing is sent or bought for them; they tap again.
 */
@Composable
fun SignInPrompt(
    reason: SignInReason,
    inProgress: Boolean,
    @StringRes errorRes: Int?,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    DialogWindow(onDismiss = onDismiss) {
        SignInPromptCard(reason, inProgress, errorRes, onContinue, onDismiss)
    }
}

@Composable
internal fun SignInPromptCard(
    reason: SignInReason,
    inProgress: Boolean,
    @StringRes errorRes: Int?,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.appColors
    DialogCard(
        icon = AppIcons.Person,
        title = stringResource(R.string.signin_prompt_title),
        tone = ConfirmTone.Calm,
    ) {
        Text(
            text = stringResource(reason.bodyRes),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spaceSm),
        )
        if (errorRes != null) {
            Text(
                text = stringResource(errorRes),
                style = MaterialTheme.typography.bodySmall,
                color = colors.danger,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = Dimens.spaceMd),
            )
        }
        Spacer(Modifier.height(Dimens.spaceXxl))
        PrimaryCta(
            text = stringResource(if (inProgress) R.string.signin_in_progress else R.string.signin_prompt_continue),
            onClick = onContinue,
            trailingArrow = false,
            enabled = !inProgress,
        )
        DialogTextAction(
            label = stringResource(R.string.signin_prompt_not_now),
            color = colors.textSecondary,
            onClick = onDismiss,
        )
    }
}

@Preview(name = "Sign in prompt · light", showBackground = true)
@Composable
private fun SignInPromptPreview() {
    AITutorTheme(darkTheme = false) {
        androidx.compose.foundation.layout.Box(Modifier.padding(Dimens.screenPadding)) {
            SignInPromptCard(SignInReason.AskQuestion, inProgress = false, errorRes = null, onContinue = {}, onDismiss = {})
        }
    }
}

@Preview(name = "Sign in prompt · dark, with error", showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun SignInPromptErrorPreview() {
    AITutorTheme(darkTheme = true) {
        androidx.compose.foundation.layout.Box(Modifier.padding(Dimens.screenPadding)) {
            SignInPromptCard(SignInReason.Subscribe, inProgress = false, errorRes = R.string.signin_error_network, onContinue = {}, onDismiss = {})
        }
    }
}
