package com.aitutor.chatbot.app.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.components.HeroBadge
import com.aitutor.chatbot.app.ui.components.designCard
import com.aitutor.chatbot.app.ui.icons.AppIcons
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.OnHero
import com.aitutor.chatbot.app.ui.theme.OverlineStyle
import com.aitutor.chatbot.app.ui.theme.appColors

/** Page one: a question and its answer, shown rather than described. */
@Composable
fun AskAnythingArt(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.width(318.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        Text(
            text = stringResource(R.string.onboarding_1_question),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, lineHeight = 21.sp),
            color = OnHero.Text,
            modifier = Modifier
                .align(Alignment.End)
                .widthIn(max = 230.dp)
                .clip(AppShapes.BubbleUser)
                .background(OnHero.Fill)
                .border(1.dp, OnHero.Stroke, AppShapes.BubbleUser)
                .padding(horizontal = 16.dp, vertical = Dimens.spaceMd),
        )

        Column(
            modifier = Modifier
                .align(Alignment.Start)
                .width(290.dp)
                .designCard(shape = AppShapes.BubbleAi)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(AppShapes.LogoSmall)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = AppIcons.Spark,
                        contentDescription = null,
                        tint = colors.onAccent,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.ai_tutor_label),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, lineHeight = 16.sp),
                    color = colors.accentText,
                )
            }
            Text(
                text = stringResource(R.string.onboarding_1_answer),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
                SuggestionChip(stringResource(R.string.onboarding_1_chip_simpler))
                SuggestionChip(stringResource(R.string.onboarding_1_chip_example))
            }
        }

        TypingIndicator(modifier = Modifier.align(Alignment.End))
    }
}

@Composable
private fun SuggestionChip(text: String) {
    val colors = MaterialTheme.appColors
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = colors.accentText,
        modifier = Modifier
            .clip(AppShapes.Pill)
            .background(colors.accentTint)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun TypingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(AppShapes.Pill)
            .background(OnHero.Fill)
            .border(1.dp, OnHero.Stroke, AppShapes.Pill)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceLg),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(0.9f, 0.6f, 0.35f).forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(AppShapes.Pill)
                    .background(Color.White.copy(alpha = alpha))
            )
        }
    }
}

/** Page two: a scanned problem and the worked steps that come back. */
@Composable
fun SolveStepsArt(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier.width(318.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        HeroBadge(
            text = stringResource(R.string.onboarding_2_scanned),
            icon = AppIcons.ScanFrame,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .designCard()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            contentAlignment = Alignment.Center,
        ) {
            ScanCorners(modifier = Modifier.matchParentSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.onboarding_2_problem_label).uppercase(),
                    style = OverlineStyle,
                    color = colors.textTertiary,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.onboarding_2_problem),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = Dimens.spaceXs),
                )
            }
        }

        Column(
            modifier = Modifier.padding(start = Dimens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            SolutionStep(
                marker = { StepNumber("1") },
                text = stringResource(R.string.onboarding_2_step_1),
                result = stringResource(R.string.onboarding_2_step_1_result),
            )
            SolutionStep(
                marker = { StepCheck() },
                text = stringResource(R.string.onboarding_2_step_2),
                result = stringResource(R.string.onboarding_2_step_2_result),
            )
        }
    }
}

/** The four bracket corners that mark the card as a camera capture. */
@Composable
private fun ScanCorners(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.appColors.accent
    Canvas(modifier = modifier) {
        val arm = 22.dp.toPx()
        val elbow = 8.dp.toPx()
        val inset = 10.dp.toPx()
        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val left = inset
        val top = inset
        val right = size.width - inset
        val bottom = size.height - inset

        fun bracket(cornerX: Float, cornerY: Float, dirX: Float, dirY: Float) {
            val path = Path()
            path.moveTo(cornerX, cornerY + dirY * arm)
            path.lineTo(cornerX, cornerY + dirY * elbow)
            path.quadraticTo(cornerX, cornerY, cornerX + dirX * elbow, cornerY)
            path.lineTo(cornerX + dirX * arm, cornerY)
            drawPath(path, color = accent, style = stroke)
        }

        bracket(left, top, 1f, 1f)
        bracket(right, top, -1f, 1f)
        bracket(left, bottom, 1f, -1f)
        bracket(right, bottom, -1f, -1f)
    }
}

@Composable
private fun SolutionStep(
    marker: @Composable () -> Unit,
    text: String,
    result: String,
) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .designCard(shape = AppShapes.CardSmall)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMd),
    ) {
        marker()
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = result,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.accentText,
        )
    }
}

@Composable
private fun StepNumber(label: String) {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(AppShapes.Pill)
            .background(colors.accentTint),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = colors.accentText,
        )
    }
}

@Composable
private fun StepCheck() {
    val colors = MaterialTheme.appColors
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(AppShapes.Pill)
            .background(colors.accent),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = AppIcons.Check,
            contentDescription = null,
            tint = colors.onAccent,
            modifier = Modifier.size(14.dp),
        )
    }
}

/** Page three: the tool grid, with the brand mark holding the centre cell. */
@Composable
fun StudyToolsArt(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        ToolGrid()
        HeroBadge(text = stringResource(R.string.onboarding_3_tools_badge), icon = AppIcons.Spark)
    }
}

@Composable
private fun ToolGrid(modifier: Modifier = Modifier) {
    val cells: List<ImageVector?> = listOf(
        AppIcons.Chat, AppIcons.Quiz, AppIcons.Math,
        AppIcons.Document, null, AppIcons.Grammar,
        AppIcons.Write, AppIcons.Study, AppIcons.Idea,
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
    ) {
        cells.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceLg)) {
                row.forEach { icon -> ToolTile(icon = icon) }
            }
        }
    }
}

@Composable
private fun ToolTile(icon: ImageVector?) {
    val colors = MaterialTheme.appColors
    val isBrand = icon == null
    Box(
        modifier = Modifier
            .size(Dimens.toolTile)
            .clip(AppShapes.Tool)
            .background(if (isBrand) Color.White else OnHero.FillFaint)
            .then(if (isBrand) Modifier else Modifier.border(1.dp, OnHero.StrokeSoft, AppShapes.Tool)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon ?: AppIcons.Logo,
            contentDescription = null,
            tint = if (isBrand) colors.accentText else OnHero.Text,
            modifier = Modifier.size(if (isBrand) 44.dp else 30.dp),
        )
    }
}
