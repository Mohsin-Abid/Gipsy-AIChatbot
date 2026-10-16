package com.askylix.app.data.remote

import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Why a call failed, as the app tells failures apart.
 *
 * The first block is the server's own `error.code`. The second is what the phone adds when no
 * answer came back at all, because a connection that dropped has no code to read.
 */
enum class ApiErrorCode {
    Unauthenticated,
    TokenExpired,
    InvalidIdToken,
    ValidationFailed,
    UnknownTool,
    AccountRequired,
    PaymentVerificationFailed,
    NotFound,
    FileTooLarge,
    UnsupportedFileType,
    FileUnreadable,
    QuotaExceeded,
    RateLimited,
    UpstreamUnavailable,
    Internal,

    NoConnection,
    Timeout,
    BadResponse,
    Unknown;

    companion object {
        fun fromWire(code: String): ApiErrorCode? = when (code) {
            "UNAUTHENTICATED" -> Unauthenticated
            "TOKEN_EXPIRED" -> TokenExpired
            "INVALID_ID_TOKEN" -> InvalidIdToken
            "VALIDATION_FAILED" -> ValidationFailed
            "UNKNOWN_TOOL" -> UnknownTool
            "ACCOUNT_REQUIRED" -> AccountRequired
            "PAYMENT_VERIFICATION_FAILED" -> PaymentVerificationFailed
            "NOT_FOUND" -> NotFound
            "FILE_TOO_LARGE" -> FileTooLarge
            "UNSUPPORTED_FILE_TYPE" -> UnsupportedFileType
            "FILE_UNREADABLE" -> FileUnreadable
            "QUOTA_EXCEEDED" -> QuotaExceeded
            "RATE_LIMITED" -> RateLimited
            "UPSTREAM_UNAVAILABLE" -> UpstreamUnavailable
            "INTERNAL" -> Internal
            else -> null
        }

        /** The best guess from the HTTP status alone, for a reply with no readable error body. */
        fun fromStatus(status: Int): ApiErrorCode = when (status) {
            401 -> Unauthenticated
            402 -> PaymentVerificationFailed
            403 -> AccountRequired
            404 -> NotFound
            413 -> FileTooLarge
            415 -> UnsupportedFileType
            422 -> FileUnreadable
            429 -> RateLimited
            400 -> ValidationFailed
            503 -> UpstreamUnavailable
            in 500..599 -> Internal
            else -> Unknown
        }
    }
}

/**
 * A failed call. The only exception type the repositories throw, so a screen has one thing to catch
 * and one `code` to switch on, and never reads [message].
 */
class ApiError(
    val code: ApiErrorCode,
    /** The HTTP status, or 0 when no reply arrived. */
    val status: Int = 0,
    val requestId: String? = null,
    /** From the `Retry-After` header on a 429, in seconds. */
    val retryAfterSeconds: Long? = null,
    cause: Throwable? = null,
) : Exception("${code.name} (HTTP $status)", cause) {

    /** True when the session itself is over: a fresh token was tried and still refused. */
    val isUnauthorized: Boolean get() = status == 401

    /** True when nothing reached the server, so trying again later is reasonable. */
    val isConnectionProblem: Boolean
        get() = code == ApiErrorCode.NoConnection || code == ApiErrorCode.Timeout
}

/** Turns whatever a call threw into an [ApiError]. */
internal fun Throwable.toApiError(): ApiError = when (this) {
    is ApiError -> this
    is HttpException -> response()?.toApiError() ?: ApiError(ApiErrorCode.Unknown, code(), cause = this)
    is SocketTimeoutException -> ApiError(ApiErrorCode.Timeout, cause = this)
    is IOException -> ApiError(ApiErrorCode.NoConnection, cause = this)
    is SerializationException -> ApiError(ApiErrorCode.BadResponse, cause = this)
    else -> ApiError(ApiErrorCode.Unknown, cause = this)
}

/** The error a non-2xx reply describes. Reads the body once, so call it only on a failure. */
internal fun Response<*>.toApiError(): ApiError {
    val envelope = runCatching {
        errorBody()?.string()?.let { ApiJson.decodeFromString(ErrorEnvelope.serializer(), it) }
    }.getOrNull()

    return ApiError(
        code = envelope?.error?.code?.let(ApiErrorCode::fromWire) ?: ApiErrorCode.fromStatus(code()),
        status = code(),
        requestId = envelope?.requestId,
        retryAfterSeconds = headers()["Retry-After"]?.toLongOrNull(),
    )
}
