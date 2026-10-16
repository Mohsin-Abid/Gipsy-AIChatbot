package com.askylix.app.ui.gate

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.askylix.app.R
import com.askylix.app.ui.components.AppIconMark
import com.askylix.app.ui.icons.AppIcons
import com.askylix.app.ui.theme.AITutorTheme
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.HeroShadow
import com.askylix.app.ui.theme.OnHero
import com.askylix.app.ui.theme.ScreenPreviews
import com.askylix.app.ui.theme.Sunshine
import com.askylix.app.ui.theme.SunshineInk
import com.askylix.app.ui.theme.SystemBarIcons
import com.askylix.app.ui.theme.appColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The screen that stops the app when this version is too old to talk to the server.
 *
 * Stateless: it reports the one button and nothing else. Working out that an update is needed is the
 * gate's job ([gateFor]), and opening the store page is the caller's, so the whole screen renders in a
 * `@Preview` exactly as it does on a device.
 *
 * The artwork is positioned from the display's top edge in the design's 390 x 844 coordinates, as a
 * single composition; the white sheet is anchored to the bottom and takes the height its text needs.
 */
@Composable
fun ForceUpdateScreen(
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.appColors

    // The whole top of the screen is the accent hero, so the clock must be light.
    SystemBarIcons(lightStatusBarIcons = true)

    Box(modifier = modifier.fillMaxSize().background(colors.hero)) {
        UpdateBackdrop()
        UpdateArtwork()

        UpdateBadge(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp),
        )

        UpdateSheet(onUpdate = onUpdate, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ---- Hero: everything drawn ----

/**
 * The glow, the dotted field, the three rings and the progress ring, all centred on the app icon.
 *
 * Drawn rather than laid out as views: the outer ring is wider than the screen and has to bleed past
 * both edges, and the dots fade out towards the corners, which is a per-dot decision.
 */
@Composable
private fun UpdateBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centre = Offset(size.width / 2f, MarkCentreY.toPx())

        val glowCentre = Offset(centre.x, GlowCentreY.toPx())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
                center = glowCentre,
                radius = GlowRadius.toPx(),
            ),
            radius = GlowRadius.toPx(),
            center = glowCentre,
        )

        drawDots()

        val hairline = Stroke(width = 1.dp.toPx())
        drawCircle(
            color = OnHero.Stroke,
            radius = 136.dp.toPx(),
            center = centre,
            // An approximation of CSS `1px dashed` on a circle this size.
            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
        )
        drawCircle(color = OnHero.RingMedium, radius = 186.dp.toPx(), center = centre, style = hairline)
        drawCircle(color = OnHero.RingWhisper, radius = 240.dp.toPx(), center = centre, style = hairline)

        drawProgressRing(centre)
    }
}

/** A grid of small dots that is strongest behind the icon and thins out to nothing towards the edges. */
private fun DrawScope.drawDots() {
    val step = DotSpacing.toPx()
    val radius = DotRadius.toPx()
    val fadeCentre = Offset(size.width / 2f, DotFadeCentreY.toPx())
    val fadeRadiusX = DotFadeRadiusX.toPx()
    val fadeRadiusY = DotFadeRadiusY.toPx()
    val fieldHeight = DotFieldHeight.toPx()

    var y = step / 2f
    while (y < fieldHeight) {
        var x = step / 2f
        while (x < size.width) {
            val dx = (x - fadeCentre.x) / fadeRadiusX
            val dy = (y - fadeCentre.y) / fadeRadiusY
            // Full strength at the centre, none from 70% of the way out.
            val strength = 1f - sqrt(dx * dx + dy * dy) / DotFadeReach
            if (strength > 0f) {
                drawCircle(
                    color = OnHero.Hairline.copy(alpha = OnHero.Hairline.alpha * strength),
                    radius = radius,
                    center = Offset(x, y),
                )
            }
            x += step
        }
        y += step
    }
}

/** A ring about two thirds full around the icon, with a dot where the fill stops: the update "in progress". */
private fun DrawScope.drawProgressRing(centre: Offset) {
    val radius = ProgressRadius.toPx()
    val stroke = Stroke(width = ProgressStroke.toPx(), cap = StrokeCap.Round)
    val topLeft = Offset(centre.x - radius, centre.y - radius)
    val box = Size(radius * 2f, radius * 2f)

    drawArc(color = OnHero.Hairline, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = box, style = stroke)
    val sweep = 360f * ProgressFilled
    drawArc(color = OnHero.Text, startAngle = -90f, sweepAngle = sweep, useCenter = false, topLeft = topLeft, size = box, style = stroke)

    val end = Math.toRadians((-90f + sweep).toDouble())
    val marker = Offset(centre.x + radius * cos(end).toFloat(), centre.y + radius * sin(end).toFloat())
    drawCircle(color = OnHero.Text, radius = 9.dp.toPx(), center = marker)
    drawCircle(color = Sunshine, radius = 4.dp.toPx(), center = marker)
}

// ---- Hero: the things on top ----

/** The app icon with its "up" badge, the three claims, and the sparkles. Placed from the top edge. */
@Composable
private fun UpdateArtwork(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Box(modifier = modifier.fillMaxSize()) {
        AppIconMark(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = MarkCentreY - Dimens.logoLarge / 2)
                .shadow(24.dp, AppShapes.AppIconLarge, ambientColor = HeroShadow, spotColor = HeroShadow),
        )

        // The badge's ring is the hero's own colour, so it reads as a cut-out of the icon rather than a sticker.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = 57.dp, y = 314.dp)
                .size(46.dp)
                .shadow(10.dp, AppShapes.Pill, ambientColor = HeroShadow, spotColor = HeroShadow)
                .clip(AppShapes.Pill)
                .background(OnHero.Text)
                .border(4.dp, colors.hero, AppShapes.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = AppIcons.ArrowUp,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
        }

        Sparkle(Sunshine, 26.dp, Modifier.align(Alignment.TopEnd).offset(x = (-60).dp, y = 368.dp))
        Sparkle(OnHero.Text, 18.dp, Modifier.align(Alignment.TopStart).offset(x = 64.dp, y = 276.dp))
        Sparkle(OnHero.Text.copy(alpha = 0.7f), 12.dp, Modifier.align(Alignment.TopEnd).offset(x = (-48).dp, y = 118.dp))

        // Each claim is anchored to the edge it sits nearest, so a wider screen pushes them apart
        // rather than stranding them inland.
        Claim(AppIcons.Bolt, R.string.gate_update_chip_faster, Modifier.align(Alignment.TopStart).offset(x = 20.dp, y = 118.dp))
        Claim(AppIcons.ScanText, R.string.gate_update_chip_scanning, Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = 160.dp))
        Claim(AppIcons.ShieldCheck, R.string.gate_update_chip_security, Modifier.align(Alignment.TopStart).offset(x = 28.dp, y = 390.dp))
    }
}

@Composable
private fun Sparkle(tint: Color, size: Dp, modifier: Modifier = Modifier) {
    Icon(
        imageVector = AppIcons.Sparkle,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size),
    )
}

/** One of the three claims floating over the hero. */
@Composable
private fun Claim(icon: ImageVector, @StringRes text: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(AppShapes.Pill)
            .background(OnHero.FillSoft)
            .border(1.dp, OnHero.Stroke, AppShapes.Pill)
            .padding(start = 5.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(AppShapes.Pill).background(OnHero.Text),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
        }
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = OnHero.Text,
            maxLines = 1,
        )
    }
}

/** "New update available", at the top. */
@Composable
private fun UpdateBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(32.dp)
            .clip(AppShapes.Pill)
            .background(OnHero.Backing)
            .border(1.dp, OnHero.Hairline, AppShapes.Pill)
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
    ) {
        Box(
            modifier = Modifier.size(22.dp).clip(AppShapes.Pill).background(Sunshine),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = AppIcons.ArrowUp, contentDescription = null, tint = SunshineInk, modifier = Modifier.size(13.dp))
        }
        Text(
            text = stringResource(R.string.gate_update_badge),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = OnHero.Text,
        )
    }
}

// ---- The sheet ----

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateSheet(onUpdate: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShapes.SheetTop)
            .background(colors.surface)
            .navigationBarsPadding()
            .padding(start = Dimens.screenPadding, top = 30.dp, end = Dimens.screenPadding, bottom = Dimens.spaceXxl),
    ) {
        Text(
            text = stringResource(R.string.gate_update_title),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.95).sp),
            color = colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.gate_update_body, stringResource(R.string.app_name)),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 10.dp),
        )

        FlowRow(
            modifier = Modifier.padding(top = Dimens.spaceXl),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceSm),
        ) {
            Reassurance(R.string.gate_update_point_free)
            Reassurance(R.string.gate_update_point_quick)
            Reassurance(R.string.gate_update_point_saved)
        }

        Spacer(modifier = Modifier.height(ButtonGap))

        UpdateButton(onClick = onUpdate)

        Text(
            text = stringResource(R.string.gate_update_store_hint),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp),
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceMd),
        )
    }
}

/** A tick and a few words: what the person is spared by updating. */
@Composable
private fun Reassurance(@StringRes text: Int, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
    ) {
        Box(
            modifier = Modifier.size(18.dp).clip(AppShapes.Pill).background(colors.accentTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = AppIcons.Check, contentDescription = null, tint = colors.accentText, modifier = Modifier.size(11.dp))
        }
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
            color = colors.textPrimary,
            maxLines = 1,
        )
    }
}

/** The one action. The glow is the accent itself, as on the Premium button. */
@Composable
private fun UpdateButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.ctaHeight)
            .shadow(14.dp, AppShapes.Button, ambientColor = colors.accent, spotColor = colors.accent)
            .clip(AppShapes.Button)
            .background(colors.accent)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = AppIcons.Download, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.gate_update_action),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onAccent,
        )
    }
}

// ---- Geometry, from the design's 390 x 844 frame ----

/** Where the icon, the rings and the progress ring share a centre. */
private val MarkCentreY = 284.dp

private val GlowCentreY = 270.dp
private val GlowRadius = 280.dp

private val DotSpacing = 22.dp
private val DotRadius = 1.4.dp
private val DotFieldHeight = 556.dp
private val DotFadeCentreY = 189.dp
private val DotFadeRadiusX = 276.dp
private val DotFadeRadiusY = 519.dp
private const val DotFadeReach = 0.7f

private val ProgressRadius = 82.dp
private val ProgressStroke = 7.dp
private const val ProgressFilled = 0.68f

/** The space between the reassurances and the button: what makes the sheet the design's 328dp tall. */
private val ButtonGap = 44.dp

@ScreenPreviews
@Composable
private fun ForceUpdateScreenPreview() {
    AITutorTheme {
        ForceUpdateScreen(onUpdate = {})
    }
}
