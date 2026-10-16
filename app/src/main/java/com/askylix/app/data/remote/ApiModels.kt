package com.askylix.app.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The JSON the backend sends, as the app reads it.
 *
 * Reading is forgiving on purpose: unknown fields are skipped so the server can add them without
 * breaking an installed app, and a null where a default exists falls back to the default. The server
 * sends `"term": null` on a paragraph, for instance, which a strict reader would refuse.
 */
internal val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

// ---- App content -----------------------------------------------------------------------------

@Serializable
data class ConfigDto(
    val minAppVersion: String = "",
    val latestAppVersion: String = "",
    val maintenance: MaintenanceDto = MaintenanceDto(),
    val urls: UrlsDto = UrlsDto(),
    val freeDailyLimit: Int? = null,
    val tools: List<ToolFlagDto> = emptyList(),
)

@Serializable
data class MaintenanceDto(val active: Boolean = false, val message: String? = null)

@Serializable
data class UrlsDto(
    val terms: String? = null,
    val privacy: String? = null,
    val support: String? = null,
    val accountDeletion: String? = null,
)

/** What can change about a tool without a new app: whether it is on, and whether it wears a NEW badge. */
@Serializable
data class ToolFlagDto(
    val tool: String,
    val enabled: Boolean = true,
    val isNew: Boolean = false,
    val category: String? = null,
)

@Serializable
data class ToolsDto(val tools: List<ToolFlagDto> = emptyList())

// ---- Account ---------------------------------------------------------------------------------

@Serializable
data class ProfileDto(
    val name: String? = null,
    val level: String? = null,
    val grade: String? = null,
    val subjects: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val dailyTime: String? = null,
)

@Serializable
data class SettingsDto(
    val language: String = "en",
    val remindersEnabled: Boolean = false,
    val themeMode: String = "SYSTEM",
    val voiceInputEnabled: Boolean = true,
)

@Serializable
data class PlanDto(
    val plan: String = "FREE",
    val productId: String? = null,
    val expiresAt: String? = null,
    val autoRenewing: Boolean? = null,
) {
    val isPro: Boolean get() = plan == "PRO"
}

@Serializable
data class QuotaDto(
    val unlimited: Boolean = false,
    val limit: Int? = null,
    val remaining: Int? = null,
    val resetsAt: String? = null,
)

@Serializable
data class MeDto(
    val userId: String = "",
    val isAnonymous: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val setupCompleted: Boolean = false,
    val profile: ProfileDto = ProfileDto(),
    val settings: SettingsDto = SettingsDto(),
    val plan: PlanDto = PlanDto(),
    val quota: QuotaDto = QuotaDto(),
)

/** The setup wizard's answers, sent once after sign-in. Calling `/signup` again updates, so it is safe to retry. */
@Serializable
data class SignupRequest(
    val name: String? = null,
    val level: String? = null,
    val grade: String? = null,
    val subjects: List<String> = emptyList(),
    val goals: List<String> = emptyList(),
    val dailyTime: String? = null,
    val remindersEnabled: Boolean = false,
)

@Serializable
data class DeleteAccountDto(val deleted: Boolean = true, val activeSubscriptionRemains: Boolean = false)

// ---- Home ------------------------------------------------------------------------------------

@Serializable
data class DashboardDto(
    val user: DashboardUserDto = DashboardUserDto(),
    val quota: QuotaDto = QuotaDto(),
    val tools: List<ToolFlagDto> = emptyList(),
    val recentChats: List<ChatItemDto> = emptyList(),
)

@Serializable
data class DashboardUserDto(
    val name: String? = null,
    val photoUrl: String? = null,
    val plan: String = "FREE",
    val setupCompleted: Boolean = false,
)

// ---- Chat ------------------------------------------------------------------------------------

@Serializable
data class ChatItemDto(
    val chatId: String,
    val tool: String,
    val title: String = "",
    val lastMessageAt: String? = null,
)

@Serializable
data class ChatPageDto(val items: List<ChatItemDto> = emptyList(), val nextCursor: String? = null)

@Serializable
data class BlockDto(val type: String = "PARAGRAPH", val term: String? = null, val text: String = "")

@Serializable
data class AttachmentDto(
    val fileName: String,
    val mimeType: String = "",
    val sizeBytes: Long = 0,
    /** A signed link that stops working after about ten minutes. Never stored. */
    val url: String? = null,
)

@Serializable
data class MessageDto(
    val messageId: String,
    val role: String,
    val createdAt: String? = null,
    val text: String? = null,
    val attachment: AttachmentDto? = null,
    val blocks: List<BlockDto>? = null,
    val rating: String? = null,
)

@Serializable
data class ChatDetailDto(
    val chat: ChatItemDto,
    val messages: List<MessageDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class SentMessageDto(val messageId: String, val text: String? = null)

@Serializable
data class AnswerDto(val messageId: String, val blocks: List<BlockDto> = emptyList())

/**
 * The reply to `POST /chat`. It is one type for two outcomes: with an answer (HTTP 200), or without
 * one (HTTP 207), where the question was saved but nothing could be produced and [error] says why.
 */
@Serializable
data class ChatResponse(
    val chatId: String? = null,
    val title: String? = null,
    val userMessage: SentMessageDto? = null,
    val answer: AnswerDto? = null,
    val quota: QuotaDto? = null,
    val error: ErrorBodyDto? = null,
)

// ---- Billing ---------------------------------------------------------------------------------

@Serializable
data class PurchaseDto(val productId: String, val purchaseToken: String)

// ---- Errors ----------------------------------------------------------------------------------

@Serializable
data class ErrorBodyDto(
    val code: String = "",
    val message: String = "",
    val details: JsonElement? = null,
)

@Serializable
data class ErrorEnvelope(val error: ErrorBodyDto = ErrorBodyDto(), val requestId: String? = null)

// ---- Request bodies built by hand ------------------------------------------------------------
//
// These are JSON objects rather than classes because what matters is which keys are present: a PATCH
// must carry only what changed, and a chat request must carry an explicit `"chatId": null` for a new
// chat. A data class cannot say "this field, even though it is null" and "that one, not at all".

object ApiBodies {

    fun chat(
        tool: String,
        chatId: String?,
        message: String,
        regenerateMessageId: String? = null,
    ): JsonObject = buildJsonObject {
        put("tool", tool)
        put("chatId", chatId)
        put("message", message)
        if (regenerateMessageId != null) put("regenerateMessageId", regenerateMessageId)
    }

    fun rename(title: String): JsonObject = buildJsonObject { put("title", title) }

    fun rating(rating: String): JsonObject = buildJsonObject { put("rating", rating) }

    fun verify(purchases: List<PurchaseDto>): JsonObject =
        ApiJson.encodeToJsonElement(
            kotlinx.serialization.builtins.ListSerializer(PurchaseDto.serializer()),
            purchases,
        ).let { list -> buildJsonObject { put("purchases", list) } }

    /** A `PATCH /me` body: only the sections given, so nothing else on the account is touched. */
    fun me(
        profile: JsonObject? = null,
        settings: JsonObject? = null,
        setupCompleted: Boolean? = null,
    ): JsonObject = buildJsonObject {
        if (profile != null) put("profile", profile)
        if (settings != null) put("settings", settings)
        if (setupCompleted != null) put("setupCompleted", setupCompleted)
    }
}
