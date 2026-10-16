package com.askylix.app.data.remote

import kotlinx.coroutines.CancellationException
import retrofit2.Response

/** How a question ended, when the call itself got a reply. */
sealed interface ChatOutcome {
    /** HTTP 200: the question was saved and answered. */
    data class Answered(val response: ChatResponse) : ChatOutcome

    /** HTTP 207: the question was saved but no answer could be produced. No quota was used. */
    data class Unanswered(val response: ChatResponse) : ChatOutcome
}

/**
 * The single door every API call goes through.
 *
 * It does three jobs so that nothing above it has to. Whatever a call throws becomes an [ApiError]; a
 * 401 that survived the token retry is reported to [onUnauthorized] — the one signal that the session is
 * over, whichever screen happened to make the call; and when the server says "too many requests" every
 * call waits out the delay it asked for rather than adding to the pile.
 */
class ApiCaller(
    private val onUnauthorized: () -> Unit = {},
    private val now: () -> Long = System::currentTimeMillis,
) {
    @Volatile private var pausedUntil = 0L

    /**
     * [endsSessionOn401] is false for a call that works without a sign-in: a refusal there is reported
     * to the caller like any error, but it does not say the session is over.
     */
    suspend fun <T> call(endsSessionOn401: Boolean = true, block: suspend () -> T): T = try {
        failIfPaused()
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        throw failure.toApiError().also { report(it, endsSessionOn401) }
    }

    /**
     * A `POST /chat`. Retrofit hands back the raw [Response] so the status can tell 200 from 207;
     * anything outside 2xx becomes an [ApiError] here, as it would for any other call.
     */
    suspend fun chat(block: suspend () -> Response<ChatResponse>): ChatOutcome = call {
        val response = block()
        if (!response.isSuccessful) throw response.toApiError()

        val body = response.body() ?: throw ApiError(ApiErrorCode.BadResponse, response.code())
        if (response.code() == 207) ChatOutcome.Unanswered(body) else ChatOutcome.Answered(body)
    }

    /** While a delay the server asked for is running, a call fails at once with the time that is left. */
    private fun failIfPaused() {
        val left = pausedUntil - now()
        if (left > 0) throw ApiError(ApiErrorCode.RateLimited, status = 429, retryAfterSeconds = (left + 999) / 1000)
    }

    private fun report(error: ApiError, endsSessionOn401: Boolean) {
        if (error.isUnauthorized && endsSessionOn401) onUnauthorized()
        // QUOTA_EXCEEDED shares the status but is not a delay: it means the day's questions are used up.
        if (error.code == ApiErrorCode.RateLimited && error.status == 429) {
            val seconds = (error.retryAfterSeconds ?: DEFAULT_PAUSE_SECONDS).coerceIn(1, MAX_PAUSE_SECONDS)
            pausedUntil = maxOf(pausedUntil, now() + seconds * 1000)
        }
    }

    private companion object {
        /** Used when the server does not say how long. */
        const val DEFAULT_PAUSE_SECONDS = 5L

        /** A cap, so a mistaken header cannot leave the app unable to call the server for an hour. */
        const val MAX_PAUSE_SECONDS = 60L
    }
}
