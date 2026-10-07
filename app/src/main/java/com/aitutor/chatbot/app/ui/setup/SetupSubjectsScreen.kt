package com.aitutor.chatbot.app.ui.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.appColors

/** Step three: a wrapping field of subject chips, multi-select. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupSubjectsScreen(
    selected: Set<Subject>,
    onToggle: (Subject) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = {},
    editing: Boolean = false,
) {
    val colors = MaterialTheme.appColors
    SetupScaffold(
        step = 3,
        title = stringResource(R.string.setup_subjects_title),
        subtitle = stringResource(R.string.setup_subjects_subtitle),
        ctaText = stringResource(if (editing) R.string.setup_save else R.string.setup_continue),
        editing = editing,
        onBack = onBack,
        onSkip = onSkip,
        onContinue = onContinue,
        modifier = modifier,
        bottomHint = {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.textPrimary)) {
                        append(stringResource(R.string.setup_subjects_selected, selected.size))
                    }
                    // "You can change these later" is a promise for the first run; on the screen where
                    // they are being changed it reads as a non sequitur.
                    if (!editing) {
                        append(" · ")
                        append(stringResource(R.string.setup_subjects_change_later))
                    }
                },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        FlowRow(
            modifier = Modifier.padding(top = Dimens.spaceXxs),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Subject.entries.forEach { subject ->
                OptionChip(
                    label = stringResource(subject.labelRes),
                    icon = subject.icon,
                    selected = subject in selected,
                    onClick = { onToggle(subject) },
                )
            }
        }
    }
}

@ScreenPreviews
@Composable
private fun SetupSubjectsScreenPreview() {
    AITutorTheme {
        SetupSubjectsScreen(
            selected = setOf(Subject.Math, Subject.Chemistry, Subject.Biology, Subject.English),
            onToggle = {},
            onBack = {},
            onSkip = {},
            onContinue = {},
        )
    }
}
