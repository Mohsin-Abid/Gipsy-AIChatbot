package com.aitutor.chatbot.app.ui.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.R
import kotlinx.coroutines.delay
import com.aitutor.chatbot.app.ui.components.LogoTile
import com.aitutor.chatbot.app.ui.theme.AITutorTheme
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Dimens
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.ScreenPreviews
import com.aitutor.chatbot.app.ui.theme.SystemBarIcons
import com.aitutor.chatbot.app.ui.theme.appColors

private const val LOADING_PROGRESS = 0.45f
private const val SPLASH_DURATION_MS = 1900L

/**
 * First frame of the app. Light mode floods the page with the accent and cuts the mark out in
 * white; dark mode inverts that, because a full-chroma page at night is glare rather than brand.
 */
@Composable
fun SplashScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    ready: Boolean = true,
) {
    val colors = MaterialTheme.appColors
    val onDark = colors.isDark

    // The whole page is the accent (or near-black in dark), so the clock must be light.
    SystemBarIcons(lightStatusBarIcons = true)

    var started by remember { mutableStateOf(false) }
    var minimumElapsed by remember { mutableStateOf(false) }

    // The entrance runs once, on its own clock, so a slow first read cannot restart it.
    LaunchedEffect(Unit) {
        started = true
        delay(SPLASH_DURATION_MS)
        minimumElapsed = true
    }

    // Leaving needs both: the animation finished, and the stored settings arrived — where this goes
    // next depends on whether setup was ever completed, so guessing would land on the wrong screen.
    LaunchedEffect(minimumElapsed, ready) {
        if (minimumElapsed && ready) onFinished()
    }

    // The mark settles in once. A looping pulse would read as "waiting", not "arriving".
    val markScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.84f,
        animationSpec = Motion.emphasized(durationMillis = 700),
        label = "markScale",
    )
    val markAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = Motion.emphasized(durationMillis = 520),
        label = "markAlpha",
    )
    val loaded by animateFloatAsState(
        targetValue = if (started) LOADING_PROGRESS else 0f,
        animationSpec = Motion.emphasized(durationMillis = 1200, delayMillis = 320),
        label = "loadingProgress",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (onDark) colors.background else colors.accent),
    ) {
        SplashBackdrop()

        // Not inset: the mark and the rings behind it are positioned from the display's top edge
        // as one composition, and 252dp already clears any status bar.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 252.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LogoTile(
                background = if (onDark) colors.accent else Color.White,
                iconTint = if (onDark) Color.White else colors.accent,
                size = Dimens.logoLarge,
                shape = AppShapes.LogoLarge,
                iconSize = 60.dp,
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                modifier = Modifier.padding(top = 30.dp),
            )
            Text(
                text = stringResource(R.string.splash_tagline),
                // 16sp regular — the ramp's 16sp slot is bold, and this line is deliberately not.
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp),
                color = colors.onAccentMuted,
                modifier = Modifier.padding(top = Dimens.spaceSm),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceLg),
        ) {
            LoadingBar(onDark = onDark, progress = loaded)
            Text(
                text = stringResource(R.string.splash_loading),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onAccentMuted,
            )
        }
    }
}

/** Concentric hairline rings around the mark, plus the soft glow behind it. */
@Composable
private fun SplashBackdrop() {
    val colors = MaterialTheme.appColors
    val onDark = colors.isDark
    val ringBase = if (onDark) colors.accent else Color.White

    // The rings breathe very slowly — enough to feel alive, too slow to notice as animation.
    val transition = rememberInfiniteTransition(label = "ringBreath")
    val breath by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = Motion.Standard),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringBreathScale",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centre = Offset(size.width / 2f, 372.dp.toPx())
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    ringBase.copy(alpha = if (onDark) 0.45f else 0.20f),
                    Color.Transparent,
                ),
                center = Offset(size.width / 2f, size.height * 0.44f),
                radius = size.height * 0.55f,
            ),
            radius = size.height * 0.55f,
            center = Offset(size.width / 2f, size.height * 0.44f),
        )
        val hairline = Stroke(width = 1.dp.toPx())
        val alphas = if (onDark) listOf(0.25f, 0.16f, 0.09f) else listOf(0.14f, 0.09f, 0.06f)
        listOf(150.dp, 220.dp, 295.dp).forEachIndexed { index, radius ->
            drawCircle(
                color = ringBase.copy(alpha = alphas[index]),
                radius = radius.toPx() * breath,
                center = centre,
                style = hairline,
            )
        }
    }
}

@Composable
private fun LoadingBar(onDark: Boolean, progress: Float) {
    val colors = MaterialTheme.appColors
    val label = stringResource(R.string.cd_loading)
    Box(
        modifier = Modifier
            .semantics { contentDescription = label }
            .width(140.dp)
            .height(4.dp)
            .clip(AppShapes.Pill)
            .background(Color.White.copy(alpha = if (onDark) 0.10f else 0.22f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceAtLeast(0.001f))
                .height(4.dp)
                .clip(AppShapes.Pill)
                .background(if (onDark) colors.accentText else Color.White),
        )
    }
}

@ScreenPreviews
@Composable
private fun SplashScreenPreview() {
    AITutorTheme {
        SplashScreen(onFinished = {})
    }
}
