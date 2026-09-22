package com.aitutor.chatbot.app.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.components.ChalkRow
import com.aitutor.chatbot.app.ui.components.InitialAvatar
import com.aitutor.chatbot.app.ui.components.SectionLabel
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.state.SampleData
import com.aitutor.chatbot.app.ui.state.SettingsPreferences
import com.aitutor.chatbot.app.ui.state.SettingsUiState
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.FontScalePreviews
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onProfileClick: () -> Unit,
    onGradeSubjectsClick: () -> Unit,
    onParentalControlsClick: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        SettingsUiState.Loading -> SettingsLoading(modifier)
        is SettingsUiState.Success -> SettingsContent(
            state = state,
            onProfileClick = onProfileClick,
            onGradeSubjectsClick = onGradeSubjectsClick,
            onParentalControlsClick = onParentalControlsClick,
            onDarkModeChange = onDarkModeChange,
            onNotificationsChange = onNotificationsChange,
            onLanguageClick = onLanguageClick,
            onHelpClick = onHelpClick,
            onAboutClick = onAboutClick,
            onSignOut = onSignOut,
            modifier = modifier
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsUiState.Success,
    onProfileClick: () -> Unit,
    onGradeSubjectsClick: () -> Unit,
    onParentalControlsClick: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = MaterialTheme.spacing
    val hairline = MaterialTheme.colorScheme.outlineVariant
    val haptics = LocalHapticFeedback.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = spacing.lg, vertical = spacing.xl)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = spacing.lg)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onProfileClick
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                InitialAvatar(initial = state.profile.initial, contentDescription = null, size = 44.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = state.profile.gradeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            HorizontalDivider(color = hairline, modifier = Modifier.padding(top = spacing.sm))
        }

        item {
            SectionLabel(text = "Account", modifier = Modifier.padding(top = spacing.lg, bottom = spacing.xs))
            ChalkRow(title = "Grade & Subjects", leadingIcon = Icons.Outlined.School, onClick = onGradeSubjectsClick)
            HorizontalDivider(color = hairline)
            ChalkRow(title = "Parental Controls", leadingIcon = Icons.Outlined.Shield, onClick = onParentalControlsClick)
            HorizontalDivider(color = hairline)
        }

        item {
            SectionLabel(text = "Preferences", modifier = Modifier.padding(top = spacing.lg, bottom = spacing.xs))
            ChalkRow(
                title = "Dark Mode",
                leadingIcon = Icons.Outlined.DarkMode,
                showChevron = false,
                trailing = {
                    Switch(
                        checked = state.preferences.darkMode,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDarkModeChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            )
            HorizontalDivider(color = hairline)
            ChalkRow(
                title = "Notifications",
                leadingIcon = Icons.Outlined.NotificationsNone,
                showChevron = false,
                trailing = {
                    Switch(
                        checked = state.preferences.notifications,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onNotificationsChange(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            )
            HorizontalDivider(color = hairline)
            ChalkRow(
                title = "Language",
                leadingIcon = Icons.Outlined.Language,
                trailingText = state.preferences.language,
                onClick = onLanguageClick
            )
            HorizontalDivider(color = hairline)
        }

        item {
            SectionLabel(text = "Support", modifier = Modifier.padding(top = spacing.lg, bottom = spacing.xs))
            ChalkRow(title = "Help Center", leadingIcon = Icons.AutoMirrored.Outlined.HelpOutline, onClick = onHelpClick)
            HorizontalDivider(color = hairline)
            ChalkRow(title = "About AI Tutor", leadingIcon = Icons.Outlined.Info, onClick = onAboutClick)
            HorizontalDivider(color = hairline)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp)
                    .padding(top = spacing.sm)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onSignOut
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Sign Out",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = "AI Tutor · Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = spacing.xl)
            )
        }
    }
}

@Composable
private fun SettingsLoading(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxSize().padding(horizontal = spacing.lg, vertical = spacing.xl)) {
        repeat(5) { SkeletonRow() }
    }
}

// ---- Previews ----

private val sampleSettingsState = SettingsUiState.Success(
    profile = SampleData.profile,
    preferences = SampleData.preferences
)

@ThemePreviews
@Composable
private fun SettingsScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(sampleSettingsState, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}

@FontScalePreviews
@Composable
private fun SettingsScreenFontScalePreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(sampleSettingsState, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}

@ThemePreviews
@Composable
private fun SettingsScreenLoadingPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(SettingsUiState.Loading, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
    }
}
