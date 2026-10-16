package com.askylix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.askylix.app.R
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.appColors

/** How loudly the badge above a [ConfirmDialog] speaks: a pause for thought, or a warning. */
enum class ConfirmTone { Calm, Destructive }

/**
 * Asks before doing something the person may not be able to take back.
 *
 * The safe answer is the filled button and sits first, so a thumb that lands on it by accident
 * loses nothing; the consequential one is plain text in the danger colour beneath it. Backing out
 * of the dialog, or tapping outside it, is the same as the safe answer. The buttons stack rather
 * than share a row so that a long translation never has to squeeze either of them.
 */
@Composable
fun ConfirmDialog(
    icon: ImageVector,
    title: String,
    body: String,
    stayLabel: String,
    confirmLabel: String,
    onStay: () -> Unit,
    onConfirm: () -> Unit,
    tone: ConfirmTone = ConfirmTone.Calm,
) {
    DialogWindow(onDismiss = onStay) {
        ConfirmCard(
            icon = icon,
            title = title,
            body = body,
            stayLabel = stayLabel,
            confirmLabel = confirmLabel,
            tone = tone,
            onStay = onStay,
            onConfirm = onConfirm,
        )
    }
}

/**
 * The window every dialog in the app opens in. Full width so the card can be centred and capped by
 * itself, not by the platform's idea of how wide a dialog is.
 */
@Composable
internal fun DialogWindow(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenPadding),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

/** The card itself, apart from the window it opens in, so a preview can show it. */
@Composable
internal fun ConfirmCard(
    icon: ImageVector,
    title: String,
    body: String,
    stayLabel: String,
    confirmLabel: String,
    tone: ConfirmTone,
    onStay: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    DialogCard(icon = icon, title = title, tone = tone, modifier = modifier) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spaceSm),
        )
        Spacer(Modifier.height(Dimens.spaceXxl))
        PrimaryCta(text = stayLabel, onClick = onStay, trailingArrow = false)
        DialogTextAction(label = confirmLabel, color = colors.danger, onClick = onConfirm)
    }
}

/**
 * The surface, badge and title shared by every dialog; [content] is what sits beneath the title.
 * The badge is the icon the dialog is about, tinted by [tone].
 */
@Composable
internal fun DialogCard(
    icon: ImageVector,
    title: String,
    tone: ConfirmTone,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    val destructive = tone == ConfirmTone.Destructive

    Column(
        modifier = modifier
            .widthIn(max = CardMaxWidth)
            .fillMaxWidth()
            .clip(AppShapes.CardLarge)
            .background(colors.surfaceRaised)
            .padding(Dimens.spaceXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(BadgeSize)
                .clip(CircleShape)
                .background(if (destructive) colors.dangerTint else colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (destructive) colors.danger else colors.accentText,
                modifier = Modifier.size(26.dp),
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Dimens.spaceXl),
        )
        content()
    }
}

/** The plain-text action under a dialog's filled button: full width and 48dp tall, so easy to hit. */
@Composable
internal fun DialogTextAction(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(top = Dimens.spaceXxs)
            .fillMaxWidth()
            .height(ActionHeight)
            .clip(AppShapes.Button)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

private val CardMaxWidth = 360.dp
private val BadgeSize = 56.dp
private val ActionHeight = 48.dp

// ---- Previews ----

@Preview(name = "Calm · light", showBackground = true)
@Composable
private fun ConfirmCardCalmPreview() {
    AITutorTheme(darkTheme = false) {
        Box(Modifier.padding(Dimens.screenPadding)) {
            ConfirmCard(
                icon = AppIcons.Clock,
                title = stringResource(R.string.leave_title),
                body = stringResource(R.string.leave_attach_body),
                stayLabel = stringResource(R.string.leave_stay),
                confirmLabel = stringResource(R.string.leave_confirm),
                tone = ConfirmTone.Calm,
                onStay = {},
                onConfirm = {},
            )
        }
    }
}

@Preview(name = "Destructive · dark", showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun ConfirmCardDestructivePreview() {
    AITutorTheme(darkTheme = true) {
        Box(Modifier.padding(Dimens.screenPadding), contentAlignment = Alignment.Center) {
            ConfirmCard(
                icon = AppIcons.Logout,
                title = stringResource(R.string.profile_logout_title),
                body = stringResource(R.string.profile_logout_body),
                stayLabel = stringResource(R.string.action_cancel),
                confirmLabel = stringResource(R.string.profile_log_out),
                tone = ConfirmTone.Destructive,
                onStay = {},
                onConfirm = {},
            )
        }
    }
}
