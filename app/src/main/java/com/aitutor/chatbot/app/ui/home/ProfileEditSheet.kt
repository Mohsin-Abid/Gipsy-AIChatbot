package com.aitutor.chatbot.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.aitutor.chatbot.app.domain.model.GradeLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.Subject
import com.aitutor.chatbot.app.ui.components.SectionLabel
import com.aitutor.chatbot.app.ui.theme.spacing

/**
 * Edits the same profile onboarding collects. Kept as a sheet on the dashboard rather than buried
 * in Settings, because the value of correcting it is highest right where its effects are visible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditSheet(
    profile: StudentProfile,
    onSave: (StudentProfile) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = MaterialTheme.spacing
    var grade by remember { mutableStateOf(profile.grade) }
    var subjects by remember { mutableStateOf(profile.subjects) }
    var goal by remember { mutableStateOf(profile.goal) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl)
                .padding(bottom = spacing.xxl)
        ) {
            Text(
                text = "Your learning profile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Your tutor uses this to pitch every answer at the right level.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = spacing.xs, bottom = spacing.xl)
            )

            SectionLabel("Level")
            ChipFlow(modifier = Modifier.padding(top = spacing.sm)) {
                GradeLevel.entries.forEach { level ->
                    FilterChip(
                        selected = grade == level,
                        onClick = { grade = if (grade == level) null else level },
                        label = { Text(level.label) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            SectionLabel(text = "Subjects", modifier = Modifier.padding(top = spacing.xl))
            ChipFlow(modifier = Modifier.padding(top = spacing.sm)) {
                Subject.entries.forEach { subject ->
                    FilterChip(
                        selected = subject in subjects,
                        onClick = {
                            subjects = if (subject in subjects) subjects - subject else subjects + subject
                        },
                        label = { Text(subject.label) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            SectionLabel(text = "Goal", modifier = Modifier.padding(top = spacing.xl))
            ChipFlow(modifier = Modifier.padding(top = spacing.sm)) {
                StudyGoal.entries.forEach { studyGoal ->
                    FilterChip(
                        selected = goal == studyGoal,
                        onClick = { goal = if (goal == studyGoal) null else studyGoal },
                        label = { Text(studyGoal.label) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
            }

            Button(
                onClick = { onSave(StudentProfile(grade = grade, subjects = subjects, goal = goal)) },
                modifier = Modifier.fillMaxWidth().padding(top = spacing.xxl),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Minimal wrap layout so chips flow onto multiple lines without pulling in a flow-layout API. */
@Composable
private fun ChipFlow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs),
    ) {
        content()
    }
}
