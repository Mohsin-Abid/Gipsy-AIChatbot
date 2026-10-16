package com.askylix.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.askylix.app.R
import com.askylix.app.ui.icons.AppIcons

/**
 * Asks before leaving a screen that is in the middle of something.
 *
 * While [busy] the system Back button, and whatever the screen wires to the returned function (its
 * own back arrow), open a [ConfirmDialog] instead of leaving; once [busy] is false both leave at
 * once, and Back is left to the system. [bodyRes] says what would be lost, because "are you sure?"
 * on its own teaches people to tap through.
 *
 * The dialog closes by itself if the work finishes while it is open: there is nothing left to lose,
 * and the person is better off seeing the result than being asked about it.
 *
 * Call it before any other Back handling on the screen. Of the handlers that are enabled, the one
 * registered last is asked first, so a sheet or panel registered after this one closes before the
 * screen is considered to be leaving.
 *
 * @return the function a back arrow should call.
 */
@Composable
fun rememberLeaveGuard(
    busy: Boolean,
    @StringRes bodyRes: Int,
    onLeave: () -> Unit,
): () -> Unit {
    var asking by rememberSaveable { mutableStateOf(false) }
    val leave by rememberUpdatedState(onLeave)

    // A later piece of work must not open a dialog nobody asked for.
    LaunchedEffect(busy) { if (!busy) asking = false }

    BackHandler(enabled = busy) { asking = true }

    if (asking && busy) {
        ConfirmDialog(
            icon = AppIcons.Clock,
            title = stringResource(R.string.leave_title),
            body = stringResource(bodyRes),
            stayLabel = stringResource(R.string.leave_stay),
            confirmLabel = stringResource(R.string.leave_confirm),
            onStay = { asking = false },
            onConfirm = {
                asking = false
                leave()
            },
        )
    }

    return remember(busy) { { if (busy) asking = true else leave() } }
}
