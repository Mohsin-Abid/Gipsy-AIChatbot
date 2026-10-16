package com.askylix.app.data.remote

import kotlinx.serialization.json.JsonObject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Every call the app makes to the backend, one function each.
 *
 * Bodies that must say exactly which keys they carry are [JsonObject]s built with [ApiBodies]. The
 * `chat` calls return a [Response] because HTTP 207 is a successful status to Retrofit and means "the
 * question was saved, no answer" to the app, so the status has to be readable.
 */
interface AskylixApi {

    /**
     * The one call that works with no sign-in, because it runs before anyone has signed in. It still
     * carries the token whenever there is one, like every other call.
     */
    @Headers("$OPTIONAL_AUTH_HEADER: true")
    @GET("config")
    suspend fun config(): ConfigDto

    @POST("signup")
    suspend fun signup(@Body body: SignupRequest): MeDto

    @GET("me")
    suspend fun me(): MeDto

    @PATCH("me")
    suspend fun updateMe(@Body body: JsonObject): MeDto

    @DELETE("me")
    suspend fun deleteAccount(): DeleteAccountDto

    @GET("tools")
    suspend fun tools(): ToolsDto

    @GET("dashboard")
    suspend fun dashboard(): DashboardDto

    @POST("chat")
    suspend fun ask(
        @Header("Idempotency-Key") key: String,
        @Body body: JsonObject,
    ): Response<ChatResponse>

    /**
     * A question with a file. The JSON goes as a plain text part named `payload` and the bytes as a
     * part named `file`, as the examples in the backend's collection do.
     */
    @Multipart
    @POST("chat")
    suspend fun askWithFile(
        @Header("Idempotency-Key") key: String,
        @Part("payload") payload: RequestBody,
        @Part file: MultipartBody.Part,
    ): Response<ChatResponse>

    @GET("chats")
    suspend fun chats(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("q") query: String? = null,
        @Query("category") category: String? = null,
    ): ChatPageDto

    @GET("chats/{id}")
    suspend fun openChat(
        @Path("id") id: String,
        @Query("before") before: String? = null,
    ): ChatDetailDto

    @PATCH("chats/{id}")
    suspend fun rename(@Path("id") id: String, @Body body: JsonObject): ChatItemDto

    @DELETE("chats/{id}")
    suspend fun deleteChat(@Path("id") id: String)

    @POST("messages/{id}/feedback")
    suspend fun feedback(@Path("id") id: String, @Body body: JsonObject)

    @POST("billing/verify")
    suspend fun verify(
        @Header("Idempotency-Key") key: String,
        @Body body: JsonObject,
    ): PlanDto
}

/**
 * Signed file links. They are fetched through a second client that has no token: the link already
 * carries its own signature, and a token sent along would go to whatever host the link names.
 */
interface FileApi {
    @Streaming
    @GET
    suspend fun download(@Url signedUrl: String): ResponseBody
}

/**
 * Marks a call that works without a sign-in. It carries the token when there is one; the difference is
 * that being refused is not the end of the session. Removed before the request leaves the phone.
 */
internal const val OPTIONAL_AUTH_HEADER = "X-Auth-Optional"
