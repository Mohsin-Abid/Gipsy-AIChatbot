package com.askylix.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.askylix.app.R
import com.askylix.app.ui.components.ConfirmTone
import com.askylix.app.ui.components.DialogCard
import com.askylix.app.ui.components.DialogTextAction
import com.askylix.app.ui.components.DialogWindow
import com.askylix.app.ui.components.PrimaryCta
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.appColors

/**
 * Asks for a new name for the chat.
 *
 * Opens with the current name selected and the keyboard up, so the usual case — replace it — is
 * just typing. Save is off while the name would be empty. [onSave] gets the text as typed; the
 * ViewModel tidies it, so the rules for a title live in one place.
 */
@Composable
fun RenameChatDialog(
    initialTitle: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    DialogWindow(onDismiss = onDismiss) {
        RenameChatCard(initialTitle = initialTitle, onSave = onSave, onDismiss = onDismiss)
    }
}

@Composable
internal fun RenameChatCard(
    initialTitle: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    focusOnOpen: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    // Saveable: turning the phone mid-edit must not put the old name back.
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialTitle, selection = TextRange(0, initialTitle.length)))
    }
    val canSave = value.text.toTitle().isNotEmpty()
    val save = { if (canSave) onSave(value.text) }

    DialogCard(
        icon = AppIcons.Pencil,
        title = stringResource(R.string.chat_rename_title),
        tone = ConfirmTone.Calm,
    ) {
        Spacer(Modifier.height(Dimens.spaceXl))
        TitleField(
            value = value,
            onValueChange = { value = it.copy(text = it.text.take(TITLE_MAX_LENGTH)) },
            onDone = save,
            focusOnOpen = focusOnOpen,
        )
        Spacer(Modifier.height(Dimens.spaceXxl))
        PrimaryCta(
            text = stringResource(R.string.chat_rename_save),
            onClick = save,
            trailingArrow = false,
            enabled = canSave,
        )
        DialogTextAction(
            label = stringResource(R.string.action_cancel),
            color = colors.textSecondary,
            onClick = onDismiss,
        )
    }
}

@Composable
private fun TitleField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onDone: () -> Unit,
    focusOnOpen: Boolean,
) {
    val colors = MaterialTheme.appColors
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val description = stringResource(R.string.chat_rename_field)

    if (focusOnOpen) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .clip(AppShapes.CardSmall)
            .background(colors.surface)
            // The resting outline is the one made visible on a white field in the light theme.
            .border(2.dp, if (focused) colors.accent else colors.inputBorder, AppShapes.CardSmall)
            .padding(horizontal = Dimens.spaceXl),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = description },
        )
    }
}

private val FieldHeight = 56.dp

// ---- Previews ----

@Preview(name = "Rename · light", showBackground = true)
@Composable
private fun RenameChatCardPreview() {
    AITutorTheme(darkTheme = false) {
        Box(Modifier.padding(Dimens.screenPadding)) {
            RenameChatCard(
                initialTitle = "What is a derivative?",
                onSave = {},
                onDismiss = {},
                focusOnOpen = false,
            )
        }
    }
}

@Preview(name = "Rename · dark", showBackground = true, backgroundColor = 0xFF0B0E1A)
@Composable
private fun RenameChatCardDarkPreview() {
    AITutorTheme(darkTheme = true) {
        Box(Modifier.padding(Dimens.screenPadding)) {
            RenameChatCard(
                initialTitle = "What is a derivative?",
                onSave = {},
                onDismiss = {},
                focusOnOpen = false,
            )
        }
    }
}
