package com.aitutor.chatbot.app.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.Grade
import com.aitutor.chatbot.app.domain.model.StudyLevel
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.appColors

/** Step two: study level, the grade within it, and an optional exam board. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupLevelScreen(
    level: StudyLevel?,
    grade: Grade?,
    onLevelChange: (StudyLevel) -> Unit,
    onGradeChange: (Grade) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SetupScaffold(
        step = 2,
        title = stringResource(R.string.setup_level_title),
        subtitle = stringResource(R.string.setup_level_subtitle),
        ctaText = stringResource(R.string.setup_continue),
        onBack = onBack,
        onSkip = onSkip,
        onContinue = onContinue,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudyLevel.entries.forEach { entry ->
                OptionCard(
                    icon = entry.icon,
                    title = stringResource(entry.labelRes),
                    detail = stringResource(entry.detailRes),
                    selected = entry == level,
                    onClick = { onLevelChange(entry) },
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FieldLabel(text = stringResource(R.string.setup_grade_label))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                Grade.entries.forEach { entry ->
                    OptionChip(
                        label = stringResource(entry.labelRes),
                        selected = entry == grade,
                        onClick = { onGradeChange(entry) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FieldLabel(text = stringResource(R.string.setup_board_label), optional = true)
            BoardSelector()
        }
    }
}

/** Placeholder for the board picker — the design shows the closed state only. */
@Composable
private fun BoardSelector(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.accentRing, shape)
            .clickable { }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Icon(
            imageVector = AppIcons.Book,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = stringResource(R.string.setup_board_placeholder),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = AppIcons.ChevronDown,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@ScreenPreviews
@Composable
private fun SetupLevelScreenPreview() {
    AITutorTheme {
        SetupLevelScreen(
            level = StudyLevel.School,
            grade = Grade.Grade10,
            onLevelChange = {},
            onGradeChange = {},
            onBack = {},
            onSkip = {},
            onContinue = {},
        )
    }
}
