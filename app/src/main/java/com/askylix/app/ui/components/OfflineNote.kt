package com.askylix.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.padding
import com.askylix.app.R
import com.askylix.app.ui.theme.AppShapes
import com.askylix.app.ui.theme.Dimens
import com.askylix.app.ui.theme.appColors

/**
 * "No connection", as a small pill. What is already on screen stays; this only says why a refresh or a
 * question will not go through. It takes no layout of its own when the phone is online.
 */
@Composable
fun OfflineNote(online: Boolean, modifier: Modifier = Modifier) {
    if (online) return
    val colors = MaterialTheme.appColors
    Text(
        text = stringResource(R.string.offline_note),
        style = MaterialTheme.typography.labelLarge,
        color = colors.onAccent,
        modifier = modifier
            .clip(AppShapes.Pill)
            .background(colors.accentDeep)
            .padding(horizontal = Dimens.spaceLg, vertical = Dimens.spaceSm),
    )
}
