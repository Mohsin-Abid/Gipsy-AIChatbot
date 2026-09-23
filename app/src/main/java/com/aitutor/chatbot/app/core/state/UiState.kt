package com.aitutor.chatbot.app.core.state

/**
 * The one Loading/Success/Error shape every ViewModel exposes. Screens branch
 * on it with the extension functions below instead of each screen defining
 * its own sealed state hierarchy.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>
}

inline fun <T> UiState<T>.onLoading(action: () -> Unit): UiState<T> {
    if (this is UiState.Loading) action()
    return this
}

inline fun <T> UiState<T>.onSuccess(action: (T) -> Unit): UiState<T> {
    if (this is UiState.Success) action(data)
    return this
}

inline fun <T> UiState<T>.onError(action: (String, Throwable?) -> Unit): UiState<T> {
    if (this is UiState.Error) action(message, throwable)
    return this
}

inline fun <T, R> UiState<T>.map(transform: (T) -> R): UiState<R> = when (this) {
    is UiState.Loading -> UiState.Loading
    is UiState.Success -> UiState.Success(transform(data))
    is UiState.Error -> this
}

inline fun <T, R> UiState<T>.fold(
    onLoading: () -> R,
    onSuccess: (T) -> R,
    onError: (String, Throwable?) -> R
): R = when (this) {
    is UiState.Loading -> onLoading()
    is UiState.Success -> onSuccess(data)
    is UiState.Error -> onError(message, throwable)
}

val <T> UiState<T>.dataOrNull: T?
    get() = (this as? UiState.Success)?.data
