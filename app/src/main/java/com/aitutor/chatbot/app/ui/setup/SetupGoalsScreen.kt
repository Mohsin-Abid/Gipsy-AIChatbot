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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.StudyTime
import com.aitutor.chatbot.app.ui.components.AppSwitch
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.appColors

/**
 * Step four: goals, daily time, and the reminder toggle.
 *
 * Goals cap at [StudentProfile.MAX_GOALS] — once two are picked the rest dim rather than
 * disappear, so it reads as a limit instead of a bug.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupGoalsScreen(
    goals: Set<StudyGoal>,
    dailyTime: StudyTime,
    reminderEnabled: Boolean,
    onToggleGoal: (StudyGoal) -> Unit,
    onTimeChange: (StudyTime) -> Unit,
    onReminderChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SetupScaffold(
        step = 4,
        title = stringResource(R.string.setup_goals_title),
        subtitle = stringResource(R.string.setup_goals_subtitle),
        ctaText = stringResource(R.string.setup_continue),
        onBack = onBack,
        onSkip = onSkip,
        onContinue = onContinue,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            StudyGoal.entries.forEach { goal ->
                val selected = goal in goals
                OptionCard(
                    icon = goal.icon,
                    title = stringResource(goal.labelRes),
                    detail = null,
                    selected = selected,
                    enabled = selected || goals.size < StudentProfile.MAX_GOALS,
                    onClick = { onToggleGoal(goal) },
                )
            }
        }

        Column(
            modifier = Modifier.padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FieldLabel(text = stringResource(R.string.setup_daily_time_label))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                StudyTime.entries.forEach { time ->
                    OptionChip(
                        label = stringResource(time.labelRes),
                        selected = time == dailyTime,
                        onClick = { onTimeChange(time) },
                    )
                }
            }
        }

        ReminderRow(
            enabled = reminderEnabled,
            onChange = onReminderChange,
            modifier = Modifier.padding(top = 22.dp),
        )
    }
}

@Composable
private fun ReminderRow(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, shape)
            .clickable { onChange(!enabled) }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Icon(
            imageVector = AppIcons.Bell,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.setup_reminder_label),
            style = MaterialTheme.typography.titleSmall,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        AppSwitch(checked = enabled, onCheckedChange = onChange, width = 46.dp, height = 28.dp)
    }
}

@ScreenPreviews
@Composable
private fun SetupGoalsScreenPreview() {
    AITutorTheme {
        SetupGoalsScreen(
            goals = setOf(StudyGoal.AceExams, StudyGoal.UnderstandTopics),
            dailyTime = StudyTime.Hour1,
            reminderEnabled = true,
            onToggleGoal = {},
            onTimeChange = {},
            onReminderChange = {},
            onBack = {},
            onSkip = {},
            onContinue = {},
        )
    }
}
