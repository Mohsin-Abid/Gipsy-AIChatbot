package com.aitutor.chatbot.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitutor.chatbot.app.ui.theme.AppShapes
import com.aitutor.chatbot.app.ui.theme.Scrim
import com.aitutor.chatbot.app.ui.theme.ScrimStrength
import com.aitutor.chatbot.app.ui.theme.bottomSafePadding
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
    if (visible) {
        Box(
            modifier = Modifier
                .matchParentSize()
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
    // A sheet wants the bottom half of the screen, which is exactly what an open keyboard is
    // holding. Padding alone would only lift the sheet into a space too short for it, so opening
    // one puts the keyboard away and takes the room back.
    DismissKeyboard(whenVisible = visible)

    SheetScrim(visible = visible, onDismiss = onDismiss)
    if (visible) {
        Column(
            modifier = modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .shadow(28.dp, SheetShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(SheetShape)
                .background(MaterialTheme.appColors.surface)
                // The sheet's surface reaches the bottom edge; its content stops above whatever
                // obstructs it — the gesture handle, or the keyboard if one is still open.
                .bottomSafePadding()
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

/**
 * Hides the keyboard whenever [whenVisible] becomes true.
 *
 * Shared by the sheets and the voice panel: all of them replace the composer as the thing being
 * typed into, so leaving its keyboard up would cover what just opened.
 */
@Composable
fun DismissKeyboard(whenVisible: Boolean) {
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(whenVisible) { if (whenVisible) keyboard?.hide() }
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
    if (visible) {
        Column(
            modifier = modifier
                .align(Alignment.TopEnd)
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
