package com.aitutor.chatbot.app.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitutor.chatbot.app.core.ext.collectAsLifecycleAwareState
import com.aitutor.chatbot.app.ui.components.AppLogoMark
import com.aitutor.chatbot.app.ui.components.ambientGlowBrush
import com.aitutor.chatbot.app.ui.components.heroBrush
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashRoute(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val destination by viewModel.destination.collectAsLifecycleAwareState()

    LaunchedEffect(destination) {
        destination?.let(onNavigate)
    }

    SplashScreen()
}

/**
 * A single, settled entrance rather than a looping pulse: the mark eases in, then the wordmark
 * follows a beat later. The only sustained motion is the hairline at the bottom, which is there to
 * say "working", not to decorate.
 */
@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val markAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "splashMarkAlpha",
    )
    val markScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.84f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "splashMarkScale",
    )
    val wordAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(550, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "splashWordAlpha",
    )
    val wordLift by animateFloatAsState(
        targetValue = if (started) 0f else 16f,
        animationSpec = tween(700, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "splashWordLift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Box(modifier = Modifier.fillMaxSize().background(ambientGlowBrush()))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AppLogoMark(
                size = 92.dp,
                modifier = Modifier.graphicsLayer {
                    alpha = markAlpha
                    scaleX = markScale
                    scaleY = markScale
                },
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = wordAlpha
                    translationY = wordLift.dp.toPx()
                },
            ) {
                Text(
                    text = "AI Tutor",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 28.dp),
                )
                Text(
                    text = "YOUR AI STUDY COMPANION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 2.4.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        LoadingHairline(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .graphicsLayer { alpha = wordAlpha }
        )
    }
}

/** A 28dp segment sliding inside a 92dp track — the quietest possible "still working" signal. */
@Composable
private fun LoadingHairline(modifier: Modifier = Modifier) {
    val trackWidth = 92.dp
    val segmentWidth = 28.dp
    val transition = rememberInfiniteTransition(label = "splashHairline")
    val position by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing, delayMillis = 120),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splashHairlinePosition",
    )
    val shimmer by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "splashHairlineAlpha",
    )

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .offset(x = (trackWidth - segmentWidth) * position)
                .width(segmentWidth)
                .height(3.dp)
                .clip(CircleShape)
                .graphicsLayer { alpha = shimmer }
                .background(heroBrush())
        )
    }
}
