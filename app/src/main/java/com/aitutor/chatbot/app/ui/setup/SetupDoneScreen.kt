package com.aitutor.chatbot.app.ui.setup

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.ui.components.PrimaryCta
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppInsets
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.bottomSafePadding
import com.aitutor.chatbot.app.ui.theme.appColors
import com.aitutor.chatbot.app.domain.model.Grade
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.StudyLevel
import com.aitutor.chatbot.app.domain.model.StudyTime
import com.aitutor.chatbot.app.domain.model.Subject

private val SummaryShape = RoundedCornerShape(24.dp)
private const val REMINDER_TIME = "7:00 PM"

/**
 * The payoff screen: an accent hero with a big tick, and the collected profile reflected straight
 * back on a card that overlaps the hero's lower edge.
 */
@Composable
fun SetupDoneScreen(
    profile: StudentProfile,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.pageTint),
    ) {
    // The accent hero runs under the status bar.
    SystemBarIcons(lightStatusBarIcons = true)

        DoneHero(name = profile.name)

        Box(
            modifier = Modifier
                .offset(y = (-36).dp)
                .padding(horizontal = SetupSidePadding),
        ) {
            ProfileSummary(profile = profile)
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .bottomSafePadding()
                .padding(start = SetupSidePadding, end = SetupSidePadding, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
        ) {
            PrimaryCta(text = stringResource(R.string.setup_done_cta), onClick = onStart)
            Text(
                text = stringResource(R.string.setup_done_edit_hint),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DoneHero(name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Taller by the status bar, so the panel reaches the display's top edge without the
            // artwork inside it being squeezed.
            .height(400.dp + AppInsets.top)
            .clip(AppShapes.HeroPanel)
            .background(colors.hero),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centre = Offset(size.width / 2f, 178.dp.toPx())
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(size.width / 2f, size.height * 0.44f),
                    radius = size.height * 0.55f,
                ),
                radius = size.height * 0.55f,
                center = Offset(size.width / 2f, size.height * 0.44f),
            )
            val hairline = Stroke(width = 1.dp.toPx())
            listOf(100.dp to 0.18f, 150.dp to 0.11f, 205.dp to 0.07f).forEach { (radius, alpha) ->
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = radius.toPx(),
                    center = centre,
                    style = hairline,
                )
            }
        }

        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 118.dp, start = Dimens.spaceXxl, end = Dimens.spaceXxl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(AppShapes.Pill)
                        .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            Box {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(
                            R.string.setup_done_title,
                            name.ifBlank { stringResource(R.string.setup_name_greeting_fallback) },
                        ),
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 26.dp),
                    )
                    Text(
                        text = stringResource(R.string.setup_done_subtitle),
                        style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 21.sp),
                        color = colors.onAccentMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = Dimens.spaceXs),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSummary(profile: StudentProfile, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    val none = stringResource(R.string.setup_done_none)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(SummaryShape)
            .background(colors.surface)
            .border(1.dp, colors.fieldBorder, SummaryShape)
            .padding(horizontal = 18.dp, vertical = Dimens.spaceSm),
    ) {
        val level = profile.level?.let { stringResource(it.labelRes) }
        val grade = profile.grade?.let { stringResource(it.labelRes) }
        val rows = listOf(
            Triple(
                AppIcons.School,
                stringResource(R.string.setup_done_level),
                listOfNotNull(level, grade).joinToString(" · ").ifBlank { none },
            ),
            Triple(
                AppIcons.Book,
                stringResource(R.string.setup_done_subjects),
                profile.subjects.map { stringResource(it.labelRes) }.joinToString(", ").ifBlank { none },
            ),
            Triple(
                AppIcons.Target,
                stringResource(R.string.setup_done_goals),
                profile.goals.map { stringResource(it.labelRes) }.joinToString(" · ").ifBlank { none },
            ),
            Triple(
                AppIcons.Clock,
                stringResource(R.string.setup_done_plan),
                stringResource(
                    R.string.setup_done_plan_value,
                    stringResource(profile.dailyTime.labelRes),
                    REMINDER_TIME,
                ),
            ),
        )

        rows.forEachIndexed { index, (icon, label, value) ->
            SummaryRow(icon = icon, label = label, value = value)
            if (index != rows.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(start = 58.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.fieldBorder)
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(icon: ImageVector, label: String, value: String) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(OptionTileShape)
                .background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@ScreenPreviews
@Composable
private fun SetupDoneScreenPreview() {
    AITutorTheme {
        SetupDoneScreen(
            profile = StudentProfile(
                name = "Alex",
                level = StudyLevel.School,
                grade = Grade.Grade10,
                subjects = setOf(Subject.Math, Subject.Chemistry, Subject.Biology, Subject.English),
                goals = setOf(StudyGoal.AceExams, StudyGoal.UnderstandTopics),
                dailyTime = StudyTime.Hour1,
            ),
            onStart = {},
        )
    }
}
