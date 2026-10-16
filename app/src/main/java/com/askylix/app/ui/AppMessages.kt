package com.askylix.app.ui

import androidx.annotation.StringRes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** A notice: which string, and whether it needs longer on screen to be read. */
data class AppMessage(@param:StringRes val text: Int, val long: Boolean = false)

/**
 * One-line notices for things that failed after the screen had already moved on, such as a rename the
 * server refused and the app has put back.
 *
 * Shown as a system toast: it needs no layout to be reserved and no motion, which suits an app that
 * has none. A message nobody is collecting is dropped, which is right for something about the moment.
 */
class AppMessages {
    private val _events = MutableSharedFlow<AppMessage>(extraBufferCapacity = 4)
    val events: SharedFlow<AppMessage> = _events.asSharedFlow()

    fun post(@StringRes message: Int, long: Boolean = false) {
        _events.tryEmit(AppMessage(message, long))
    }
}
