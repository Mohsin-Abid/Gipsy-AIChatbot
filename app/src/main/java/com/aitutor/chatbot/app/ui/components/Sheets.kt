package com.aitutor.chatbot.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Motion
import com.aitutor.chatbot.app.ui.theme.Scrim
import com.aitutor.chatbot.app.ui.theme.ScrimStrength
import com.aitutor.chatbot.app.ui.theme.appColors

private val SheetShape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
private val MenuShape = RoundedCornerShape(20.dp)

/**
 * The dimmed backdrop behind a sheet or a menu. Tapping it dismisses, and it swallows taps that
 * would otherwise land on the content underneath.
 */
@Composable
fun BoxScope.SheetScrim(
    visible: Boolean,
    onDismiss: () -> Unit,
    strength: Float = ScrimStrength.Sheet,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(Motion.medium()),
        exit = fadeOut(Motion.fast()),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Scrim.copy(alpha = strength))
                // No ripple and no indication: this is a dismiss target, not a button.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
        )
    }
}

/**
 * A bottom sheet: scrim, then a panel that slides up from the bottom edge. Not Material's
 * `ModalBottomSheet`, because the design's own radius, grip and shadow are what make these read as
 * part of the app rather than as a system surface.
 */
@Composable
fun BoxScope.BottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    bottomPadding: Dp = 30.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    SheetScrim(visible = visible, onDismiss = onDismiss)
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(Motion.Slow, easing = Motion.Emphasized),
            initialOffsetY = { it },
        ) + fadeIn(Motion.fast()),
        exit = slideOutVertically(
            animationSpec = tween(Motion.Medium, easing = Motion.Standard),
            targetOffsetY = { it },
        ) + fadeOut(Motion.fast()),
        modifier = modifier.align(Alignment.BottomCenter),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(28.dp, SheetShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(SheetShape)
                .background(MaterialTheme.appColors.surface)
                // The sheet's own surface reaches the bottom edge; its content stops above the
                // navigation bar, so nothing in it lands under the gesture handle.
                .navigationBarsPadding()
                .padding(
                    start = horizontalPadding,
                    end = horizontalPadding,
                    top = 10.dp,
                    bottom = bottomPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SheetGrip()
            content()
        }
    }
}

/** The 40×5 handle every sheet opens with. Decorative — dragging is not wired up yet. */
@Composable
fun SheetGrip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(5.dp)
            .clip(AppShapes.Pill)
            .background(MaterialTheme.appColors.sheetGrip)
    )
}

/**
 * A menu anchored under a header button. It grows from its top-right corner, so it reads as
 * unfolding out of the button that opened it rather than appearing from nowhere.
 */
@Composable
fun BoxScope.AnchoredMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.appColors
    SheetScrim(visible = visible, onDismiss = onDismiss, strength = ScrimStrength.Menu)
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = tween(Motion.Fast, easing = Motion.Emphasized),
            initialScale = 0.9f,
            transformOrigin = TransformOrigin(1f, 0f),
        ) + fadeIn(Motion.fast()),
        exit = scaleOut(
            animationSpec = tween(Motion.Fast),
            targetScale = 0.94f,
            transformOrigin = TransformOrigin(1f, 0f),
        ) + fadeOut(Motion.fast()),
        modifier = modifier.align(Alignment.TopEnd),
    ) {
        Column(
            modifier = Modifier
                // Anchored under the header, which itself starts below the status bar.
                .statusBarsPadding()
                .width(250.dp)
                .shadow(24.dp, MenuShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(MenuShape)
                .background(colors.surfaceRaised)
                .border(1.dp, colors.cardBorder, MenuShape)
                .padding(vertical = 6.dp),
            content = content,
        )
    }
}
