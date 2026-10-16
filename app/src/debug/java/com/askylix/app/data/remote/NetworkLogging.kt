package com.askylix.app.data.remote

import android.util.Base64
import android.util.Log
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject

/**
 * Debug builds log each call's method, address, status and headers, and say why a call failed.
 *
 * Never the bodies of calls that worked: they hold the student's questions, the answers and signed
 * file links, and the token header is hidden for the same reason. A refused call is different: its
 * body is the server's error (code, message, request id), which names the problem and holds none of
 * that. The release build has an empty twin of this file.
 */
internal fun OkHttpClient.Builder.addDebugLogging() {
    addInterceptor(
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            redactHeader("Authorization")
        }
    )
    addInterceptor(FailureLogger())
}

/** Logs what the server said when it refused a call, so "401" can be told apart from *why* it was a 401. */
private class FailureLogger : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (!response.isSuccessful) {
            val error = runCatching { JSONObject(response.peekBody(MAX_BODY_BYTES).string()) }.getOrNull()
            val detail = error?.optJSONObject("error")
            val text = buildString {
                append(response.request.method).append(' ').append(response.request.url.encodedPath)
                append(" -> ").append(response.code)
                append(" code=").append(detail?.optString("code"))
                append(" message=").append(detail?.optString("message"))
                append(" requestId=").append(error?.optString("requestId"))
                if (response.code == 401) append(" | token ").append(response.request.header("Authorization").describeToken())
            }
            Log.w(TAG, text)
        }
        return response
    }

    private companion object {
        const val TAG = "ApiFailure"
        const val MAX_BODY_BYTES = 4096L
    }
}

/**
 * The public facts about a sign-in token — who issued it, for which project, how, and when it ends —
 * so a refusal can be compared with what the server expects. Never the token itself, and none of the
 * personal claims (name, e-mail, picture).
 */
private fun String?.describeToken(): String {
    val jwt = this?.removePrefix("Bearer ")?.split('.')
    if (jwt == null || jwt.size != 3) return "absent or not a JWT"
    return runCatching {
        val claims = JSONObject(String(Base64.decode(jwt[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)))
        val provider = claims.optJSONObject("firebase")?.optString("sign_in_provider")
        val secondsLeft = claims.optLong("exp") - System.currentTimeMillis() / 1000
        "iss=${claims.optString("iss")} aud=${claims.optString("aud")} provider=$provider expiresInSeconds=$secondsLeft"
    }.getOrElse { "unreadable" }
}
