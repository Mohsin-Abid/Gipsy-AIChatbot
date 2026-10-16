package com.askylix.app.ui.profile

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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
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
import com.askylix.app.R
import com.askylix.app.data.remote.QuotaDto
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.ThemeMode
import com.askylix.app.ui.components.AppSwitch
import com.askylix.app.ui.components.ConfirmDialog
import com.askylix.app.ui.components.ConfirmTone
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.main.GlyphTile
import com.askylix.app.ui.main.ListPanel
import com.askylix.app.ui.main.SectionOverline
import com.askylix.app.ui.main.TabBottomInset
import com.askylix.app.ui.main.TabHero
import com.askylix.app.ui.main.UpgradeDiamondButton
import com.askylix.app.ui.main.TabSidePadding
import com.askylix.app.ui.main.PanelDivider
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.OnHero
import com.askylix.app.ui.theme.TallScreenPreviews
import com.askylix.app.ui.theme.SystemBarIcons
import com.askylix.app.ui.theme.appColors

private val CardShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(20.dp)
private val ShadowTint = Color(0xFF080C28)
private val RowIconInset = 70.dp
private val SegmentTrackShape = RoundedCornerShape(15.dp)
private val SegmentShape = RoundedCornerShape(12.dp)


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
    onEditName: () -> Unit,
    onEditLevel: () -> Unit,
    onEditSubjects: () -> Unit,
    onEditGoals: () -> Unit,
    onOpenLanguage: () -> Unit,
    onSeeProPlans: () -> Unit,
    onLogOut: () -> Unit,
    modifier: Modifier = Modifier,
    showUpgrade: Boolean = false,
    /** False in the browse mode "Skip for now" leads to: there is no account to log out of, so it offers to sign in. */
    signedIn: Boolean = true,
    onSignIn: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    /** The daily question allowance as the server last said it. The usage line shows only when it is known. */
    quota: QuotaDto? = null,
) {
    val colors = MaterialTheme.appColors
    var confirmingLogOut by rememberSaveable { mutableStateOf(false) }
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
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

        if (confirmingDelete) {
            DeleteAccountConfirmation(
                onConfirm = {
                    confirmingDelete = false
                    onDeleteAccount()
                },
                onDismiss = { confirmingDelete = false },
            )
        }

        if (confirmingLogOut) {
            LogOutConfirmation(
                onConfirm = {
                    confirmingLogOut = false
                    onLogOut()
                },
                onDismiss = { confirmingLogOut = false },
            )
        }

        ProfileHero(
            profile = profile,
            onEditName = onEditName,
            showUpgrade = showUpgrade,
            onSeeProPlans = onSeeProPlans,
        )

        Box(
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(horizontal = TabSidePadding),
        ) {
            UpgradeCard(onSeeProPlans = onSeeProPlans, quota = quota)
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
                // One row per screen it opens, so a tap lands on exactly the answer it names.
                NavigationRow(
                    icon = AppIcons.School,
                    label = stringResource(R.string.profile_level_grade),
                    value = levelAndGrade(profile),
                    onClick = onEditLevel,
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Book,
                    label = stringResource(R.string.profile_subjects),
                    value = stringResource(R.string.profile_subjects_value, profile.subjects.size),
                    onClick = onEditSubjects,
                )
                PanelDivider(inset = RowIconInset)
                NavigationRow(
                    icon = AppIcons.Target,
                    label = stringResource(R.string.profile_daily_goal),
                    value = stringResource(profile.dailyTime.labelRes),
                    onClick = onEditGoals,
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
                if (signedIn) LogOutButton(onClick = { confirmingLogOut = true }) else SignInButton(onClick = onSignIn)
                // Google Play requires any app that makes accounts to let people delete them.
                if (signedIn) DeleteAccountLink(onClick = { confirmingDelete = true })
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

/** The quiet way into deleting the account: plain red text, below Log out, so it is findable and not tempting. */
@Composable
private fun DeleteAccountLink(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Text(
        text = stringResource(R.string.profile_delete_account),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = colors.danger,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Dimens.spaceMd),
    )
}

/**
 * Asks before deleting the account. It says what happens and for how long, and that a Google Play
 * subscription is not cancelled by it, because that is the surprise that costs people money.
 */
@Composable
private fun DeleteAccountConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        icon = AppIcons.Trash,
        title = stringResource(R.string.profile_delete_title),
        body = stringResource(R.string.profile_delete_body),
        stayLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(R.string.profile_delete_confirm),
        tone = ConfirmTone.Destructive,
        onStay = onDismiss,
        onConfirm = onConfirm,
    )
}

/**
 * Asks before signing out. The log-out button is large, red and at the very bottom of a long scroll,
 * which is exactly where a thumb lands by accident, and signing in again is a whole round trip.
 */
@Composable
private fun LogOutConfirmation(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        icon = AppIcons.Logout,
        title = stringResource(R.string.profile_logout_title),
        body = stringResource(R.string.profile_logout_body),
        stayLabel = stringResource(R.string.action_cancel),
        confirmLabel = stringResource(R.string.profile_log_out),
        tone = ConfirmTone.Destructive,
        onStay = onDismiss,
        onConfirm = onConfirm,
    )
}

// ---- Hero ----

@Composable
private fun ProfileHero(
    profile: StudentProfile,
    onEditName: () -> Unit,
    showUpgrade: Boolean,
    onSeeProPlans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    val name = profile.name.ifBlank { stringResource(R.string.profile_default_name) }

    // The upgrade card below overlaps this panel by 48dp, so the bottom padding is that overlap plus
    // the room that keeps the plan badge from touching the card.
    TabHero(modifier = modifier, bottomPadding = 76.dp) {
        Row(
            // As tall as the diamond, whether or not it is there. Without a fixed height the row is only
            // as tall as its title for a subscriber, and the avatar below sits higher for them than for
            // everyone else.
            modifier = Modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            if (showUpgrade) UpgradeDiamondButton(onClick = onSeeProPlans)
        }

        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.spaceMd),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // The picture and the name are one tap target: both stand for the same thing, and the
                // only thing about them that can be changed is the name. There is no badge to say so —
                // the screen it opens is the explanation.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(AppShapes.CardSmall)
                        .clickable(onClickLabel = stringResource(R.string.cd_edit_name), onClick = onEditName)
                        .padding(horizontal = 24.dp),
                ) {
                    Avatar(name = name)
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
                }
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
    }
}

/**
 * The avatar carries initials until there is somewhere to store a photo.
 *
 * It is not a button on its own: there is no photo to change. Tapping it opens the name editor
 * because [ProfileHero] makes the picture and the name one target.
 */
@Composable
private fun Avatar(name: String) {
    val colors = MaterialTheme.appColors
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
private fun UpgradeCard(onSeeProPlans: () -> Unit, quota: QuotaDto?, modifier: Modifier = Modifier) {
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

        // Only a number the server has given: signed out, or before it has answered, there is nothing true to show.
        val limit = quota?.limit
        val remaining = quota?.remaining
        if (quota != null && !quota.unlimited && limit != null && limit > 0 && remaining != null) {
            val used = (limit - remaining).coerceIn(0, limit)
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
                            used,
                            limit,
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = colors.textPrimary,
                    )
                }
                UsageBar(fraction = used.toFloat() / limit)
            }
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

@Composable
private fun SignInButton(onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(10.dp, RowShape, ambientColor = ShadowTint, spotColor = ShadowTint)
            .clip(RowShape)
            .background(colors.surface)
            .border(1.dp, colors.cardBorder, RowShape)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Person,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(17.dp),
            )
        }
        Text(
            text = stringResource(R.string.profile_sign_in),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = colors.accentText,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}

/** Up to two initials, so "Alex Morgan" reads "AM" and a single name still gives one letter. */
/** "School · Grade 10", or whichever half is set, or "Not set" if neither is. */
@Composable
private fun levelAndGrade(profile: StudentProfile): String {
    val level = profile.level?.let { stringResource(it.labelRes) }
    val grade = profile.grade?.let { stringResource(it.labelRes) }
    return when {
        level != null && grade != null -> stringResource(R.string.profile_level_grade_value, level, grade)
        else -> level ?: grade ?: stringResource(R.string.profile_not_set)
    }
}

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
            onEditName = {},
            onEditLevel = {},
            onEditSubjects = {},
            onEditGoals = {},
            onOpenLanguage = {},
            onSeeProPlans = {},
            onLogOut = {},
            showUpgrade = true,
        )
    }
}
