package com.aitutor.chatbot.app.ui.home

import android.text.format.DateUtils
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.HistoryEntry
import com.aitutor.chatbot.app.domain.model.StudyTool
import com.aitutor.chatbot.app.ui.components.Reveal
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.main.TabBottomInset
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Coral
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.OverlineStyle
import com.aitutor.chatbot.app.ui.theme.SampleContent
import com.aitutor.chatbot.app.ui.theme.TallScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private val SidePadding = 20.dp

/**
 * How far through the resumed conversation the ring reads. A real figure needs a notion of lesson
 * progress the tutor does not report yet, so this stays a constant rather than a fabricated number
 * dressed up as one.
 */
private const val CONTINUE_PROGRESS = 0.6f
private val HeroShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
private val CardShape = RoundedCornerShape(24.dp)
private val PanelShape = RoundedCornerShape(22.dp)
private val RowShape = RoundedCornerShape(20.dp)

/**
 * The dashboard. An accent hero carrying the composer, then a "continue" card that overlaps its
 * lower edge, then the tool grid, challenge and recent activity.
 */
@Composable
fun HomeScreen(
    userName: String,
    activity: List<HistoryEntry>,
    onToolClick: (StudyTool) -> Unit,
    onOpenChat: (HistoryEntry) -> Unit,
    onSeeAllTools: () -> Unit,
    onViewHistory: () -> Unit,
    onOpenProfile: () -> Unit,
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

        HomeHero(userName = userName, onOpenProfile = onOpenProfile)

        Reveal(
            delayMillis = Motion.Stagger,
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(horizontal = SidePadding),
        ) {
            ContinueCard(
                recent = activity.firstOrNull(),
                onResume = { entry -> onOpenChat(entry) },
                onStartFirst = { onToolClick(StudyTool.Chatbot) },
            )
        }

        Column(
            modifier = Modifier
                .offset(y = (-48).dp)
                .padding(start = SidePadding, end = SidePadding, top = 28.dp, bottom = TabBottomInset),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            ToolsSection(onToolClick = onToolClick, onSeeAll = onSeeAllTools)
            DailyChallengeCard()
            ActivitySection(
                activity = activity,
                onEntryClick = onOpenChat,
                onViewAll = onViewHistory,
            )
        }
    }
}

// ---- Hero ----

@Composable
private fun HomeHero(userName: String, onOpenProfile: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(HeroShape)
            .background(colors.hero),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width + 50.dp.toPx(), 30.dp.toPx()),
                    radius = 160.dp.toPx(),
                ),
                radius = 160.dp.toPx(),
                center = Offset(size.width + 50.dp.toPx(), 30.dp.toPx()),
            )
            val hairline = Stroke(width = 1.dp.toPx())
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = 180.dp.toPx(),
                center = Offset(30.dp.toPx(), size.height + 30.dp.toPx()),
                style = hairline,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.10f),
                radius = 130.dp.toPx(),
                center = Offset(30.dp.toPx(), size.height + 30.dp.toPx()),
                style = hairline,
            )
        }

        Column(
            // The hero paints under the status bar; its header starts below it.
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = SidePadding, end = SidePadding, top = 24.dp, bottom = 68.dp),
        ) {
            HeroHeader(userName = userName, onOpenProfile = onOpenProfile)

            Reveal(delayMillis = Motion.Stagger, lift = 16.dp, modifier = Modifier.padding(top = 28.dp)) {
                Column {
                    AssistantBadge()
                    Text(
                        text = stringResource(R.string.home_prompt),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        modifier = Modifier.padding(top = Dimens.spaceMd),
                    )
                    AskBar(modifier = Modifier.padding(top = 20.dp))
                }
            }

            SuggestionChips(modifier = Modifier.padding(top = Dimens.spaceLg))
        }
    }
}

@Composable
private fun HeroHeader(userName: String, onOpenProfile: () -> Unit) {
    val colors = MaterialTheme.appColors
    val profileLabel = stringResource(R.string.cd_profile)
    val initials = userName.split(" ").mapNotNull { it.firstOrNull() }.take(2)
        .joinToString("").uppercase().ifBlank { "?" }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
            modifier = Modifier.weight(1f),
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(AppShapes.Pill)
                    .background(Color.White)
                    .border(2.dp, Color.White.copy(alpha = 0.55f), AppShapes.Pill)
                    .clickable(onClick = onOpenProfile)
                    .semantics { contentDescription = profileLabel },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.accent,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = stringResource(R.string.home_greeting_morning),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onAccentMuted,
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineSmall.copy(letterSpacing = (-0.5).sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        NotificationButton()
    }
}

@Composable
private fun NotificationButton() {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(AppShapes.Pill)
                .background(OnHero.FillSoft)
                .border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.Bell,
                contentDescription = stringResource(R.string.cd_notifications),
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-9).dp, y = 8.dp)
                .size(12.dp)
                .clip(AppShapes.Pill)
                .background(colors.hero),
            contentAlignment = Alignment.Center,
        ) {
            Box(modifier = Modifier.size(8.dp).clip(AppShapes.Pill).background(Coral))
        }
    }
}

@Composable
private fun AssistantBadge() {
    Row(
        modifier = Modifier
            .clip(AppShapes.Pill)
            .background(OnHero.Fill)
            .padding(start = Dimens.spaceSm, end = 10.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Icon(
            imageVector = AppIcons.Spark,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = stringResource(R.string.home_assistant_badge),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun AskBar(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, AppShapes.Pill, ambientColor = Color(0xFF080C28), spotColor = Color(0xFF080C28))
            .clip(AppShapes.Pill)
            .background(colors.surface)
            .height(60.dp)
            .padding(start = 18.dp, end = Dimens.spaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = AppIcons.Spark,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.home_ask_hint),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = colors.textTertiary,
            modifier = Modifier.weight(1f),
        )
        CircleAction(
            icon = AppIcons.Mic,
            contentDescription = stringResource(R.string.cd_ask_voice),
            background = colors.accentTint,
            tint = colors.accent,
        )
        CircleAction(
            icon = AppIcons.SendUp,
            contentDescription = stringResource(R.string.cd_send_question),
            background = colors.accent,
            tint = colors.onAccent,
            iconSize = 18.dp,
        )
    }
}

@Composable
private fun CircleAction(
    icon: ImageVector,
    contentDescription: String,
    background: Color,
    tint: Color,
    iconSize: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(AppShapes.Pill)
            .background(background)
            .clickable { },
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun SuggestionChips(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        HeroChip(stringResource(R.string.home_chip_scan), AppIcons.ScanFrame)
        HeroChip(stringResource(R.string.home_chip_newton), null)
        HeroChip(stringResource(R.string.home_chip_quiz), null)
    }
}

@Composable
private fun HeroChip(label: String, icon: ImageVector?) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(AppShapes.Pill)
            .background(OnHero.FillSoft)
            .border(1.dp, OnHero.StrokeSoft, AppShapes.Pill)
            .clickable { }
            .padding(horizontal = Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White,
            maxLines = 1,
        )
    }
}

// ---- Continue learning ----

@Composable
private fun ContinueCard(
    recent: HistoryEntry?,
    onResume: (HistoryEntry) -> Unit,
    onStartFirst: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    // With no conversations yet the slot becomes an invitation rather than disappearing: the hero
    // above overlaps whatever sits here, so removing the card would leave a notch in the layout.
    val tool = recent?.tool ?: StudyTool.Chatbot
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(20.dp, CardShape, ambientColor = colors.accentDeep, spotColor = colors.accentDeep)
            .clip(CardShape)
            .background(colors.surfaceRaised)
            .then(if (colors.isDark) Modifier.border(1.dp, colors.cardBorder, CardShape) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        ProgressMedallion(tool = tool, progress = if (recent == null) 0f else CONTINUE_PROGRESS)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(
                    if (recent == null) R.string.home_start_label else R.string.home_continue_label
                ).uppercase(),
                style = OverlineStyle.copy(letterSpacing = 0.77.sp),
                color = colors.accentText,
            )
            Text(
                text = recent?.title ?: stringResource(R.string.home_start_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = recent?.let { stringResource(it.tool.labelRes) }
                    ?: stringResource(R.string.home_start_detail),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(AppShapes.Pill)
                .background(colors.accent)
                .clickable { recent?.let(onResume) ?: onStartFirst() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (recent == null) AppIcons.Plus else AppIcons.Play,
                contentDescription = stringResource(
                    R.string.cd_resume,
                    recent?.title ?: stringResource(R.string.home_start_title),
                ),
                tint = colors.onAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** A 60dp ring showing lesson progress, with the tool's glyph sitting inside it. */
@Composable
private fun ProgressMedallion(tool: StudyTool, progress: Float) {
    val colors = MaterialTheme.appColors
    Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(60.dp)) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            val inset = 4.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(
                color = colors.accentTintStrong,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
            drawArc(
                color = colors.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = stroke,
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(AppShapes.Pill)
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---- Tools ----

@Composable
private fun ToolsSection(onToolClick: (StudyTool) -> Unit, onSeeAll: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
        SectionHeader(
            title = stringResource(R.string.home_tools_title),
            actionLabel = stringResource(R.string.home_see_all),
            onAction = onSeeAll,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd)) {
            StudyTool.featuredTools.forEachIndexed { index, tool ->
                FeaturedToolCard(
                    tool = tool,
                    deep = index == 1,
                    onClick = { onToolClick(tool) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        ToolGridPanel(onToolClick = onToolClick)
    }
}

@Composable
private fun SectionHeader(title: String, actionLabel: String, onAction: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 19.sp, letterSpacing = (-0.48).sp),
            color = colors.textPrimary,
        )
        Row(
            modifier = Modifier
                .clip(AppShapes.Pill)
                .clickable(onClick = onAction)
                .padding(vertical = 10.dp, horizontal = Dimens.spaceXxs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.accentText,
            )
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun FeaturedToolCard(
    tool: StudyTool,
    deep: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .height(156.dp)
            .shadow(16.dp, CardShape, ambientColor = colors.accentDeep, spotColor = colors.accentDeep)
            .clip(CardShape)
            .background(if (deep) colors.accentDeep else colors.hero)
            .clickable(onClick = onClick),
    ) {
        // The oversized ring bleeding out of the bottom-right corner, clipped by the card.
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.07f),
                radius = 75.dp.toPx() - 9.dp.toPx(),
                center = Offset(size.width + 35.dp.toPx(), size.height + 15.dp.toPx()),
                style = Stroke(width = 18.dp.toPx()),
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(OnHero.Fill)
                        .border(1.dp, OnHero.StrokeSoft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(AppShapes.Pill)
                        .background(OnHero.Fill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.ArrowUpRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(tool.labelRes),
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = (-0.32).sp),
                    color = Color.White,
                )
                Text(
                    text = stringResource(tool.taglineRes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                    color = colors.onAccentMuted,
                    maxLines = 2,
                )
            }
        }
    }
}

@Composable
private fun ToolGridPanel(onToolClick: (StudyTool) -> Unit) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PanelShape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, PanelShape)
            .padding(start = 10.dp, end = 10.dp, top = 18.dp, bottom = Dimens.spaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        StudyTool.gridTools.chunked(4).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
            ) {
                row.forEach { tool ->
                    ToolGridTile(
                        tool = tool,
                        onClick = { onToolClick(tool) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolGridTile(tool: StudyTool, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.accentTint)
                .border(1.dp, colors.accentRing, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = stringResource(tool.gridLabelRes),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 15.sp),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.height(30.dp),
        )
    }
}

// ---- Daily challenge ----

@Composable
private fun DailyChallengeCard() {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(colors.accentTint)
            .border(1.dp, colors.accentRing, CardShape),
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = colors.accentRing.copy(alpha = 0.6f),
                radius = 65.dp.toPx(),
                center = Offset(size.width + 35.dp.toPx(), 15.dp.toPx()),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AppIcons.Target,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_challenge_label).uppercase(),
                    style = OverlineStyle.copy(letterSpacing = 0.77.sp),
                    color = colors.accentText,
                )
                Text(
                    text = stringResource(R.string.home_challenge_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.home_challenge_detail),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
                    color = colors.textSecondary,
                )
            }
            Text(
                text = stringResource(R.string.home_challenge_start),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onAccent,
                modifier = Modifier
                    .clip(AppShapes.Pill)
                    .background(colors.accent)
                    .clickable { }
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            )
        }
    }
}

// ---- Recent activity ----

@Composable
private fun ActivitySection(
    activity: List<HistoryEntry>,
    onEntryClick: (HistoryEntry) -> Unit,
    onViewAll: () -> Unit,
) {
    // Nothing to summarise yet, so the whole section stands down rather than showing an empty
    // heading — History already explains what will appear here.
    if (activity.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
        SectionHeader(
            title = stringResource(R.string.home_activity_title),
            actionLabel = stringResource(R.string.home_view_all),
            onAction = onViewAll,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            activity.forEach { entry ->
                ActivityRow(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun ActivityRow(entry: HistoryEntry, onClick: () -> Unit) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, RowShape)
            .clickable(onClick = onClick)
            .padding(Dimens.spaceLg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = entry.tool.icon,
                contentDescription = null,
                tint = colors.onAccent,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = entry.detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                Text(
                    text = stringResource(entry.tool.labelRes),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    color = colors.accentText,
                    modifier = Modifier
                        .clip(AppShapes.Pill)
                        .background(colors.accentTint)
                        .padding(horizontal = Dimens.spaceSm, vertical = 3.dp),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXxs),
                ) {
                    Icon(
                        imageVector = AppIcons.Clock,
                        contentDescription = null,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        text = relativeTime(entry.updatedAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
                        color = colors.textTertiary,
                    )
                }
            }
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

/** Platform-formatted and already localized, so no relative-time strings are hand-written. */
@Composable
private fun relativeTime(timestamp: Long): String = DateUtils.getRelativeTimeSpanString(
    timestamp,
    System.currentTimeMillis(),
    DateUtils.MINUTE_IN_MILLIS,
).toString()

@TallScreenPreviews
@Composable
private fun HomeScreenPreview() {
    AITutorTheme {
        HomeScreen(
            userName = "Alex Morgan",
            activity = SampleContent.history().take(3),
            onToolClick = {},
            onOpenChat = {},
            onSeeAllTools = {},
            onViewHistory = {},
            onOpenProfile = {},
        )
    }
}

@TallScreenPreviews
@Composable
private fun HomeScreenEmptyPreview() {
    AITutorTheme {
        HomeScreen(
            userName = "Alex Morgan",
            activity = emptyList(),
            onToolClick = {},
            onOpenChat = {},
            onSeeAllTools = {},
            onViewHistory = {},
            onOpenProfile = {},
        )
    }
}
