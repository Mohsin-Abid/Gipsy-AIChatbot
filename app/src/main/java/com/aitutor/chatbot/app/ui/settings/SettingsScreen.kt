package com.aitutor.chatbot.app.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.core.ext.cardStyle
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.core.ext.elevatedCardStyle
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.ExplanationLevel
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.ThemeMode
import com.aitutor.chatbot.app.ui.components.GradientHeader
import com.aitutor.chatbot.app.ui.components.SkeletonRow
import com.aitutor.chatbot.app.ui.components.pressScale
import com.aitutor.chatbot.app.ui.home.ProBadge
import com.aitutor.chatbot.app.ui.home.ProfileEditSheet
import com.aitutor.chatbot.app.ui.home.SectionHeader
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.FontScale
import com.aitutor.chatbot.app.ui.theme.PremiumGold
import com.aitutor.chatbot.app.ui.theme.PremiumGoldBright
import com.aitutor.chatbot.app.ui.theme.PremiumInkEnd
import com.aitutor.chatbot.app.ui.theme.PremiumInkStart
import com.aitutor.chatbot.app.ui.theme.ThemePreviews
import com.aitutor.chatbot.app.ui.theme.spacing
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsRoute(
    onLanguageClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onAccountCleared: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsLifecycleAwareState()
    var editingProfile by remember { mutableStateOf<StudentProfile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.accountCleared.collect { onAccountCleared() }
    }

    SettingsScreen(
        state = state,
        onLanguageClick = onLanguageClick,
        onPremiumClick = onPremiumClick,
        onEditProfile = { editingProfile = it },
        onThemeModeSelected = viewModel::onThemeModeSelected,
        onExplanationLevelSelected = viewModel::onExplanationLevelSelected,
        onNotificationsChanged = viewModel::onNotificationsChanged,
        onFontScaleIncrease = viewModel::onFontScaleIncrease,
        onFontScaleDecrease = viewModel::onFontScaleDecrease,
        onFontScaleReset = viewModel::onFontScaleReset,
        onLogOutConfirmed = viewModel::onLogOutConfirmed,
        onDeleteAccountConfirmed = viewModel::onDeleteAccountConfirmed,
    )

    editingProfile?.let { profile ->
        ProfileEditSheet(
            profile = profile,
            onSave = { updated ->
                viewModel.onProfileUpdated(updated)
                editingProfile = null
            },
            onDismiss = { editingProfile = null },
        )
    }
}

@Composable
fun SettingsScreen(
    state: UiState<SettingsData>,
    onLanguageClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onEditProfile: (StudentProfile) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onExplanationLevelSelected: (ExplanationLevel) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onFontScaleIncrease: () -> Unit,
    onFontScaleDecrease: () -> Unit,
    onFontScaleReset: () -> Unit,
    onLogOutConfirmed: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        UiState.Loading, is UiState.Error -> SettingsLoading(modifier)
        is UiState.Success -> SettingsContent(
            data = state.data,
            onLanguageClick = onLanguageClick,
            onPremiumClick = onPremiumClick,
            onEditProfile = onEditProfile,
            onThemeModeSelected = onThemeModeSelected,
            onExplanationLevelSelected = onExplanationLevelSelected,
            onNotificationsChanged = onNotificationsChanged,
            onFontScaleIncrease = onFontScaleIncrease,
            onFontScaleDecrease = onFontScaleDecrease,
            onFontScaleReset = onFontScaleReset,
            onLogOutConfirmed = onLogOutConfirmed,
            onDeleteAccountConfirmed = onDeleteAccountConfirmed,
            modifier = modifier,
        )
    }
}

@Composable
private fun SettingsContent(
    data: SettingsData,
    onLanguageClick: () -> Unit,
    onPremiumClick: () -> Unit,
    onEditProfile: (StudentProfile) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onExplanationLevelSelected: (ExplanationLevel) -> Unit,
    onNotificationsChanged: (Boolean) -> Unit,
    onFontScaleIncrease: () -> Unit,
    onFontScaleDecrease: () -> Unit,
    onFontScaleReset: () -> Unit,
    onLogOutConfirmed: () -> Unit,
    onDeleteAccountConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val darkTheme = isSystemInDarkTheme()
    val sidePadding = Modifier.padding(horizontal = spacing.lg)
    var showLogOutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "header") {
            GradientHeader(
                title = "Settings",
                subtitle = if (data.isPremium) "Premium member" else "Free plan",
                darkTheme = darkTheme,
            )
        }

        item(key = "plan") {
            PlanCard(
                isPremium = data.isPremium,
                onClick = onPremiumClick,
                modifier = sidePadding,
            )
        }

        item(key = "profile") {
            LearningProfileCard(
                profile = data.profile,
                onClick = { onEditProfile(data.profile) },
                modifier = sidePadding,
            )
        }

        item(key = "appearance-header") {
            SectionHeader(title = "Appearance", modifier = sidePadding.padding(top = spacing.sm))
        }
        item(key = "theme") {
            SettingsCard(modifier = sidePadding) {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = spacing.md)
                )
                ThemePicker(selected = data.themeMode, onSelected = onThemeModeSelected)
            }
        }
        item(key = "font") {
            SettingsCard(modifier = sidePadding) {
                FontSizeControl(
                    fontScale = data.fontScale,
                    onIncrease = onFontScaleIncrease,
                    onDecrease = onFontScaleDecrease,
                    onReset = onFontScaleReset,
                )
            }
        }

        item(key = "study-header") {
            SectionHeader(title = "Study preferences", modifier = sidePadding.padding(top = spacing.sm))
        }
        item(key = "language") {
            SettingsCard(modifier = sidePadding, contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.Language,
                    title = "Language",
                    subtitle = "Answers come back in this language",
                    trailingText = data.language.englishLabel,
                    onClick = onLanguageClick,
                )
            }
        }
        item(key = "explanation") {
            SettingsCard(modifier = sidePadding) {
                Text(
                    text = "Explanation level",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "How much detail your tutor goes into by default",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.xxs, bottom = spacing.md)
                )
                OptionPills(
                    options = ExplanationLevel.entries.map { it to it.label },
                    selected = data.explanationLevel,
                    onSelected = onExplanationLevelSelected,
                )
            }
        }

        item(key = "voice-header") {
            SectionHeader(title = "Voice & notifications", modifier = sidePadding.padding(top = spacing.sm))
        }
        item(key = "voice") {
            SettingsCard(modifier = sidePadding, contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.Outlined.RecordVoiceOver,
                    title = "Text-to-speech voice",
                    subtitle = "Choose a voice per language — coming soon",
                    enabled = false,
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "Push notifications",
                    subtitle = "Study reminders and streak nudges",
                    trailing = {
                        Switch(
                            checked = data.notificationsEnabled,
                            onCheckedChange = onNotificationsChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    },
                )
            }
        }

        item(key = "support-header") {
            SectionHeader(title = "Support & legal", modifier = sidePadding.padding(top = spacing.sm))
        }
        item(key = "support") {
            SettingsCard(modifier = sidePadding, contentPadding = PaddingValues(0.dp)) {
                SettingsRow(icon = Icons.AutoMirrored.Outlined.HelpOutline, title = "Help center", onClick = {})
                RowDivider()
                SettingsRow(icon = Icons.Outlined.Description, title = "Privacy policy", onClick = {})
                RowDivider()
                SettingsRow(icon = Icons.Outlined.Gavel, title = "Terms of service", onClick = {})
            }
        }

        item(key = "account-header") {
            SectionHeader(title = "Account", modifier = sidePadding.padding(top = spacing.sm))
        }
        item(key = "account") {
            SettingsCard(modifier = sidePadding, contentPadding = PaddingValues(0.dp)) {
                SettingsRow(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    title = "Log out",
                    subtitle = "Start a fresh session on this device",
                    onClick = { showLogOutDialog = true },
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.DeleteForever,
                    title = "Delete account",
                    subtitle = "Permanently removes your chats and preferences",
                    destructive = true,
                    onClick = { showDeleteDialog = true },
                )
            }
        }

        item(key = "version") {
            Text(
                text = "AI Tutor · Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = spacing.lg)
            )
        }
    }

    if (showLogOutDialog) {
        ConfirmDialog(
            title = "Log out?",
            message = "This clears your current session and starts fresh. Your device keeps its saved preferences.",
            confirmLabel = "Log Out",
            onConfirm = { showLogOutDialog = false; onLogOutConfirmed() },
            onDismiss = { showLogOutDialog = false },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = "Delete account?",
            message = "This permanently deletes your chats and preferences on this device. This can't be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = { showDeleteDialog = false; onDeleteAccountConfirmed() },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun PlanCard(isPremium: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .elevatedCardStyle()
            .clip(AppShapes.CardLarge)
            .background(Brush.linearGradient(listOf(PremiumInkStart, PremiumInkEnd)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(spacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(PremiumGoldBright.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = PremiumGoldBright,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPremium) "Premium active" else "Go Premium",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
                Text(
                    text = if (isPremium) {
                        "Every tool unlocked, no daily limit."
                    } else {
                        "Unlimited questions and all 10 study tools."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                )
            }
            if (isPremium) {
                ProBadge(dark = true)
            } else {
                Surface(shape = CircleShape, color = PremiumGoldBright) {
                    Text(
                        text = "Upgrade",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = PremiumInkStart,
                        modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs)
                    )
                }
            }
        }
    }
}

/** Mirrors the dashboard's profile chip — same data, same edit sheet, more room to show it. */
@Composable
private fun LearningProfileCard(
    profile: StudentProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .cardStyle()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .padding(spacing.lg)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Learning profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = profile.grade?.label ?: "Tap to set your level",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }

        if (profile.subjects.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                profile.subjects.forEach { subject ->
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Row(
                            modifier = Modifier.padding(horizontal = spacing.sm, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = subject.icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = subject.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Visual theme swatches instead of a text toggle — you pick what you can see. */
@Composable
private fun ThemePicker(selected: ThemeMode, onSelected: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        ThemeMode.entries.forEach { mode ->
            val isSelected = mode == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(AppShapes.Card)
                    .clickable(role = Role.RadioButton) { onSelected(mode) }
                    .padding(vertical = spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(swatchBrush(mode))
                        .then(
                            if (isSelected) {
                                Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = spacing.xs)
                )
            }
        }
    }
}

private fun swatchBrush(mode: ThemeMode): Brush = when (mode) {
    ThemeMode.Light -> Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF1F1F1)))
    ThemeMode.Dark -> Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF121212)))
    ThemeMode.System -> Brush.linearGradient(listOf(Color(0xFFFFFFFF), Color(0xFF121212)))
}

@Composable
private fun <T> OptionPills(
    options: List<Pair<T, String>>,
    selected: T,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                onClick = { onSelected(value) },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = spacing.sm, horizontal = spacing.xs)
                )
            }
        }
    }
}

@Composable
private fun FontSizeControl(
    fontScale: Float,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Font size",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Controls text size across the whole app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onReset) {
                Text("Reset", style = MaterialTheme.typography.labelLarge)
            }
        }

        Surface(
            shape = AppShapes.Card,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(top = spacing.sm),
        ) {
            Text(
                text = "The quick brown fox jumps over the lazy dog.",
                style = MaterialTheme.typography.bodyLarge.let {
                    it.copy(fontSize = it.fontSize * fontScale, lineHeight = it.lineHeight * fontScale)
                },
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(spacing.md)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            StepperButton(
                icon = Icons.Filled.Remove,
                contentDescription = "Decrease font size",
                enabled = fontScale > FontScale.MIN,
                onClick = onDecrease,
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val fraction = ((fontScale - FontScale.MIN) / (FontScale.MAX - FontScale.MIN)).coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
            StepperButton(
                icon = Icons.Filled.Add,
                contentDescription = "Increase font size",
                enabled = fontScale < FontScale.MAX,
                onClick = onIncrease,
            )
            Text(
                text = "${(fontScale * 100).toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(48.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(36.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        enabled = enabled,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(MaterialTheme.spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().cardStyle().padding(contentPadding),
        content = content,
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val spacing = MaterialTheme.spacing
    val contentAlpha = if (enabled) 1f else 0.55f
    val titleColor = when {
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
    }
    val iconTint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconTint.copy(alpha = 0.10f * contentAlpha)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.copy(alpha = contentAlpha),
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
        if (onClick != null && trailing == null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RowDivider() {
    androidx.compose.material3.HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(start = MaterialTheme.spacing.lg + 36.dp + MaterialTheme.spacing.md)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    contentColor = if (destructive) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
                )
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SettingsLoading(modifier: Modifier = Modifier) {
    val spacing = MaterialTheme.spacing
    // No gradient header in this state, so the skeletons have to clear the status bar themselves.
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = spacing.lg, vertical = spacing.xl)
    ) {
        repeat(5) { SkeletonRow() }
    }
}

@ThemePreviews
@Composable
private fun SettingsScreenPreview() {
    AITutorTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                state = UiState.Success(
                    SettingsData(
                        language = AppLanguage.English,
                        themeMode = ThemeMode.System,
                        explanationLevel = ExplanationLevel.Standard,
                        fontScale = 1f,
                        notificationsEnabled = true,
                        isPremium = true,
                        profile = StudentProfile(),
                    )
                ),
                onLanguageClick = {},
                onPremiumClick = {},
                onEditProfile = {},
                onThemeModeSelected = {},
                onExplanationLevelSelected = {},
                onNotificationsChanged = {},
                onFontScaleIncrease = {},
                onFontScaleDecrease = {},
                onFontScaleReset = {},
                onLogOutConfirmed = {},
                onDeleteAccountConfirmed = {},
            )
        }
    }
}
