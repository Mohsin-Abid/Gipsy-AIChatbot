package com.aitutor.chatbot.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.core.view.WindowCompat

/**
 * The system bar insets, as plain values.
 *
 * The app draws edge to edge, so every screen has to decide for itself what reaches the display's
 * edge and what does not. The rule throughout is: **backgrounds bleed, content insets.** An accent
 * hero paints all the way up under the status bar — cutting it off at the bar would leave a pale
 * stripe across the top of every screen — while the text and buttons inside it are pushed down clear
 * of the clock.
 *
 * Where a single edge needs clearing, screens use Compose's own `statusBarsPadding()` and
 * `navigationBarsPadding()`. These two exist for the other case: content that already computes a
 * padding — a scrolling list's bottom inset, a bar that stacks its own spacing — and needs to add
 * the system bar to it rather than wrap another layout around itself.
 */
object AppInsets {

    /** How far the status bar reaches down over the top of the page. */
    val top: Dp
        @Composable get() = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    /** How far the navigation bar, or the gesture handle, reaches up over the bottom. */
    val bottom: Dp
        @Composable get() = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
}

/**
 * Sets whether the system bar icons draw light or dark.
 *
 * Drawing edge to edge means the clock and the gesture handle sit on top of whatever the screen puts
 * there, and this app puts two very different things there: a full-chroma accent hero on Home, Tools,
 * History, Profile, Premium and the first-run flow, and a plain light surface on Chat, Language and
 * setup. Dark icons over the accent are close to unreadable, so each screen says which it needs.
 *
 * The navigation bar is uniform — the bottom of every screen is the page or a surface — so it simply
 * follows the theme.
 *
 * @param lightStatusBarIcons true where the top of the screen is dark (an accent hero).
 */
@Composable
fun SystemBarIcons(lightStatusBarIcons: Boolean) {
    val view = LocalView.current
    val darkTheme = MaterialTheme.appColors.isDark
    // In a preview there is no Activity to configure, and nothing to correct for.
    if (view.isInEditMode) return
    val window = (view.context as? Activity)?.window ?: return

    SideEffect {
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !lightStatusBarIcons
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}
