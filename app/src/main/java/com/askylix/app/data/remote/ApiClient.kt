package com.askylix.app.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Where the sign-in token comes from. Firebase keeps it fresh; this only asks for it. */
fun interface TokenProvider {
    /** The current ID token, or null when nobody is signed in. [forceRefresh] is for a token the server refused. */
    suspend fun idToken(forceRefresh: Boolean): String?
}

/**
 * The two Retrofit clients: [api] for the backend, [files] for signed file links.
 *
 * The backend's calls carry the sign-in token. A 401 gets one retry with a freshly requested token,
 * because tokens last an hour and a long-open app will meet an expired one now and then. A second
 * refusal is passed through for the caller to treat as the end of the session.
 */
class ApiClient(
    baseUrl: String,
    tokens: TokenProvider,
    base: OkHttpClient = OkHttpClient(),
    private val configure: OkHttpClient.Builder.() -> Unit = {},
) {

    private val apiHttp: OkHttpClient = base.newBuilder()
        .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(READ_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(TimeoutInterceptor())
        .addInterceptor(AuthInterceptor(tokens))
        .authenticator(TokenAuthenticator(tokens))
        .apply(configure)
        .build()

    // No interceptors that add the token, and no authenticator: see FileApi.
    private val fileHttp: OkHttpClient = base.newBuilder()
        .connectTimeout(CONNECT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_SECONDS, TimeUnit.SECONDS)
        .build()

    private val converter = ApiJson.asConverterFactory("application/json".toMediaType())

    val api: AskylixApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(apiHttp)
        .addConverterFactory(converter)
        .build()
        .create(AskylixApi::class.java)

    val files: FileApi = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(fileHttp)
        .addConverterFactory(converter)
        .build()
        .create(FileApi::class.java)

    private companion object {
        const val CONNECT_SECONDS = 15L
        const val READ_SECONDS = 30L
    }
}

/**
 * Gives a question longer to be answered. A model can take a while, so `POST /chat` waits up to
 * 90 seconds where every other call waits 30. The wait is for a *reply*, so a slow answer is not mistaken for a dead connection.
 */
private class TimeoutInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val isChat = request.method == "POST" && request.url.encodedPath.endsWith("/chat")
        return if (isChat) {
            chain.withReadTimeout(CHAT_SECONDS, TimeUnit.SECONDS)
                .withWriteTimeout(CHAT_SECONDS, TimeUnit.SECONDS)
                .proceed(request)
        } else {
            chain.proceed(request)
        }
    }

    private companion object {
        const val CHAT_SECONDS = 90
    }
}

/**
 * Adds `Authorization: Bearer <token>` to every call, with the token Firebase issued when the person
 * signed in with Google.
 *
 * A call marked [OPTIONAL_AUTH_HEADER] works without one: when nobody is signed in, or the token cannot
 * be read just now, it goes out without rather than failing. It is tagged [OptionalAuth] so the rest of
 * the chain can tell that being refused is not the end of anyone's session.
 */
private class AuthInterceptor(private val tokens: TokenProvider) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val optional = original.header(OPTIONAL_AUTH_HEADER) != null
        val request = original.newBuilder()
            .removeHeader(OPTIONAL_AUTH_HEADER)
            .apply { if (optional) tag(OptionalAuth::class.java, OptionalAuth) }
            .build()

        // OkHttp calls this on its own thread, so waiting for the token here blocks nothing the person sees.
        val token = try {
            runBlocking { tokens.idToken(false) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (optional) null else throw error
        }
        return chain.proceed(request.withToken(token))
    }
}

/** Carried on a request as a tag: this call is allowed to go without a sign-in. */
private object OptionalAuth

/** On a 401, asks for a new token once and repeats the call. A second 401 is given up on. */
private class TokenAuthenticator(private val tokens: TokenProvider) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // This response is already the answer to a retried call: the new token was refused too.
        if (response.priorResponse != null) return null
        // A call that works without a sign-in has nothing to renew: a refusal there says nothing about the session.
        if (response.request.tag(OptionalAuth::class.java) != null) return null

        val fresh = try {
            runBlocking { tokens.idToken(true) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } ?: return null

        return response.request.withToken(fresh)
    }
}

private fun Request.withToken(token: String?): Request =
    if (token == null) this else newBuilder().header("Authorization", "Bearer $token").build()
