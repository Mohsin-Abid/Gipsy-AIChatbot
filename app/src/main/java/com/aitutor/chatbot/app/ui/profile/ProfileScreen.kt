package com.aitutor.chatbot.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.ThemeMode
import com.aitutor.chatbot.app.ui.components.AppSwitch
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.main.GlyphTile
import com.aitutor.chatbot.app.ui.main.HeroIconButton
import com.aitutor.chatbot.app.ui.main.HeroStatStrip
import com.aitutor.chatbot.app.ui.main.ListPanel
import com.aitutor.chatbot.app.ui.main.SectionOverline
import com.aitutor.chatbot.app.ui.main.TabBottomInset
import com.aitutor.chatbot.app.ui.main.TabHero
import com.aitutor.chatbot.app.ui.main.TabSidePadding
import com.aitutor.chatbot.app.ui.main.PanelDivider
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.TallScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private val CardShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(20.dp)
private val ShadowTint = Color(0xFF080C28)
private val RowIconInset = 70.dp
private val SegmentTrackShape = RoundedCornerShape(15.dp)
private val SegmentShape = RoundedCornerShape(12.dp)

/** Placeholder usage until billing and the data layer exist. */
private const val DAILY_QUESTIONS_USED = 7
private const val DAILY_QUESTIONS_LIMIT = 10

/**
 * The account tab: identity and totals on the hero, the upgrade card overlapping it, then the
 * settings panels. Every preference here is held by the caller, so the screen stays a pure view.
 */
@Composable
fun ProfileScreen(
    profile: StudentProfile,
    language: AppLanguage,
    remindersEnabled: Boolean,
    themeMode: ThemeMode,
    voiceInputEnabled: Boolean,
    onRemindersChange: (Boolean) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onVoiceInputChange: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onOpenLanguage: () -> Unit,
    onSeeProPlans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            // Background first, then the inset: the page colour fills the strip behind the
            // navigation bar, while the scrolling content stops above it rather than sliding under.
            .background(colors.pageTint)
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
    // The accent hero runs under the status bar.
    SystemBarIcons(lightStatusBarIcons = true)

        ProfileHero(profile = profile, onEditProfile = onEditProfile)

        Box(
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(horizontal = TabSidePadding),
        ) {
            UpgradeCard(onSeeProPlans = onSeeProPlans)
        }

        Column(
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(
                    start = TabSidePadding,
                    end = TabSidePadding,
                    top = 28.dp,
                    bottom = TabBottomInset,
                ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            SettingsGroup(title = stringResource(R.string.profile_group_learning)) {
                NavigationRow(
                    icon = AppIcons.Book,
                    label = stringResource(R.string.profile_subjects),
                    value = stringResource(R.string.profile_subjects_value, profile.subjects.size),
                    onClick = onEditProfile,
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Target,
                    label = stringResource(R.string.profile_daily_goal),
                    value = stringResource(profile.dailyTime.labelRes),
                    onClick = onEditProfile,
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Globe,
                    label = stringResource(R.string.profile_language),
                    value = stringResource(language.englishNameRes),
                    onClick = onOpenLanguage,
                )
            }

            SettingsGroup(title = stringResource(R.string.profile_group_preferences)) {
                ToggleRow(
                    icon = AppIcons.Bell,
                    label = stringResource(R.string.profile_reminders),
                    checked = remindersEnabled,
                    onCheckedChange = onRemindersChange,
                )
                PanelDivider(inset = RowIconInset)
                ThemeRow(selected = themeMode, onSelect = onThemeModeChange)
                PanelDivider(inset = RowIconInset)
                ToggleRow(
                    icon = AppIcons.Mic,
                    label = stringResource(R.string.profile_voice_input),
                    checked = voiceInputEnabled,
                    onCheckedChange = onVoiceInputChange,
                )
            }

            SettingsGroup(title = stringResource(R.string.profile_group_account)) {
                NavigationRow(
                    icon = AppIcons.Card,
                    label = stringResource(R.string.profile_subscription),
                    value = stringResource(R.string.profile_plan_free),
                    onClick = onSeeProPlans,
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Shield,
                    label = stringResource(R.string.profile_privacy),
                    onClick = {},
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Help,
                    label = stringResource(R.string.profile_help),
                    onClick = {},
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Star,
                    label = stringResource(R.string.profile_rate),
                    onClick = {},
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
                LogOutButton(onClick = {})
                Text(
                    text = stringResource(R.string.profile_version, "1.0.0"),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ---- Hero ----

@Composable
private fun ProfileHero(
    profile: StudentProfile,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val name = profile.name.ifBlank { stringResource(R.string.profile_default_name) }

    TabHero(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            HeroIconButton(
                icon = AppIcons.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                onClick = onEditProfile,
            )
        }

        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.spaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AvatarWithEditBadge(name = name, onEdit = onEditProfile)
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.66).sp,
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(top = Dimens.spaceMd),
                )
                Text(
                    text = profile.level?.let { stringResource(it.labelRes) }
                        ?: stringResource(R.string.profile_level_unset),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.5.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.onAccentMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
                PlanBadge(modifier = Modifier.padding(top = 10.dp))
            }
        }

        HeroStatStrip(
            stats = listOf(
                stringResource(R.string.profile_stat_questions_value) to
                    stringResource(R.string.profile_stat_questions_label),
                stringResource(R.string.profile_stat_notes_value) to
                    stringResource(R.string.profile_stat_notes_label),
                stringResource(R.string.profile_stat_streak_value) to
                    stringResource(R.string.profile_stat_streak_label),
            ),
            modifier = Modifier.padding(top = 20.dp),
        )
    }
}

/** The avatar carries initials until there is somewhere to store a photo. */
@Composable
private fun AvatarWithEditBadge(name: String, onEdit: () -> Unit) {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.size(88.dp)) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(AppShapes.Pill)
                .background(colors.surface)
                .border(3.dp, Color.White.copy(alpha = 0.55f), AppShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.initials(),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 30.sp,
                    letterSpacing = (-0.6).sp,
                ),
                color = colors.accent,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 2.dp, y = 2.dp)
                .size(32.dp)
                .clip(AppShapes.Pill)
                .background(colors.hero)
                .padding(3.dp)
                .clip(AppShapes.Pill)
                .background(colors.surface)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Pencil,
                contentDescription = stringResource(R.string.cd_edit_profile),
                tint = colors.accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun PlanBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(AppShapes.Pill)
            .background(OnHero.FillSoft)
            .border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
            .padding(start = 9.dp, end = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            imageVector = AppIcons.Spark,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = stringResource(R.string.profile_plan_free_badge),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
        )
    }
}

// ---- Upgrade ----

/**
 * The upsell. It shows the free allowance running down, which is the honest version of a paywall —
 * and note that nothing here grants anything: entitlement is only ever read, never minted on the
 * client. A Pro plan becomes real once a Play Billing purchase token has been verified server-side.
 */
@Composable
private fun UpgradeCard(onSeeProPlans: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, CardShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(CardShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.cardBorder, CardShape)
            .padding(Dimens.spaceXl),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg + 2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        ) {
            GlyphTile(icon = AppIcons.Crown, filled = true)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_upgrade_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.profile_upgrade_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.profile_daily_questions),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    color = colors.textSecondary,
                )
                Text(
                    text = stringResource(
                        R.string.profile_daily_questions_value,
                        DAILY_QUESTIONS_USED,
                        DAILY_QUESTIONS_LIMIT,
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = colors.textPrimary,
                )
            }
            UsageBar(fraction = DAILY_QUESTIONS_USED.toFloat() / DAILY_QUESTIONS_LIMIT)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(AppShapes.Pill)
                .background(colors.accent)
                .clickable(onClick = onSeeProPlans),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.profile_see_plans),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onAccent,
            )
            Icon(
                imageVector = AppIcons.ArrowRight,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier
                    .padding(start = Dimens.spaceSm)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun UsageBar(fraction: Float) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(AppShapes.Pill)
            .background(colors.accentTint),
    ) {
        Box(
            modifier = Modifier
                .weight(fraction.coerceIn(0.01f, 1f))
                .fillMaxHeight()
                .clip(AppShapes.Pill)
                .background(colors.accent)
        )
        // The remainder has to be a real sibling, or the filled part would stretch to full width.
        Spacer(modifier = Modifier.weight((1f - fraction).coerceAtLeast(0.001f)))
    }
}

// ---- Settings rows ----

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionOverline(text = title)
        ListPanel(content = content)
    }
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    value: String? = null,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable(onClick = onClick)
            .padding(start = Dimens.spaceLg + 2.dp, end = Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        GlyphTile(icon = icon, size = 40.dp, cornerRadius = 13.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color = colors.textTertiary,
                maxLines = 1,
            )
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = Dimens.spaceLg + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        GlyphTile(icon = icon, size = 40.dp, cornerRadius = 13.dp)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AppSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/**
 * Theme, as three choices rather than a switch.
 *
 * The control sits under its own label instead of beside it: three legible options do not fit in
 * what is left of a 62dp row, and shrinking them to icons alone would make "System" a guess.
 */
@Composable
private fun ThemeRow(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.spaceLg + 2.dp, end = Dimens.spaceLg + 2.dp, top = Dimens.spaceMd, bottom = Dimens.spaceLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        ) {
            GlyphTile(icon = AppIcons.Contrast, size = 40.dp, cornerRadius = 13.dp)
            Text(
                text = stringResource(R.string.profile_theme),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        ThemeSegmentedControl(selected = selected, onSelect = onSelect)
    }
}

@Composable
private fun ThemeSegmentedControl(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SegmentTrackShape)
            .background(colors.accentTint)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        ThemeMode.entries.forEach { mode ->
            ThemeSegment(
                mode = mode,
                selected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ThemeSegment(
    mode: ThemeMode,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val background = if (selected) colors.surfaceRaised else Color.Transparent
    val foreground = if (selected) colors.accentText else colors.textSecondary
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(SegmentShape)
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = mode.icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(mode.labelRes),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            color = foreground,
            maxLines = 1,
        )
    }
}

@Composable
private fun LogOutButton(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(10.dp, RowShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(RowShape)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, RowShape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.dangerTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Logout,
                contentDescription = null,
                tint = colors.danger,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_log_out),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.danger,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** Up to two initials, so "Alex Morgan" reads "AM" and a single name still gives one letter. */
private fun String.initials(): String =
    trim().split(" ").filter { it.isNotBlank() }.take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

@TallScreenPreviews
@Composable
private fun ProfileScreenPreview() {
    AITutorTheme {
        ProfileScreen(
            profile = StudentProfile(name = "Alex Morgan"),
            language = AppLanguage.English,
            remindersEnabled = true,
            themeMode = ThemeMode.System,
            voiceInputEnabled = true,
            onRemindersChange = {},
            onThemeModeChange = {},
            onVoiceInputChange = {},
            onEditProfile = {},
            onOpenLanguage = {},
            onSeeProPlans = {},
        )
    }
}
