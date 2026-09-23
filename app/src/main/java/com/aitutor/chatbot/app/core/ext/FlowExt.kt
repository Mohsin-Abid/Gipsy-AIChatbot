package com.aitutor.chatbot.app.core.ext

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Collects a [StateFlow] only while the composition is at least STARTED. */
@Composable
fun <T> StateFlow<T>.collectAsLifecycleAwareState(): State<T> = collectAsStateWithLifecycle()

/** Collects a cold [Flow] (e.g. a DataStore preference) lifecycle-aware, seeded with [initial]. */
@Composable
fun <T> Flow<T>.collectAsLifecycleAwareState(initial: T): State<T> = collectAsStateWithLifecycle(initialValue = initial)
