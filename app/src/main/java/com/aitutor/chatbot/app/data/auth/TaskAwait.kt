package com.aitutor.chatbot.app.data.auth

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Waits for a Play services [Task] without blocking a thread.
 *
 * Written here rather than pulled in as a library: it is twelve lines, and the library that provides
 * it is one more thing that has to match this project's Kotlin version.
 */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed"))
        }
    }
}
