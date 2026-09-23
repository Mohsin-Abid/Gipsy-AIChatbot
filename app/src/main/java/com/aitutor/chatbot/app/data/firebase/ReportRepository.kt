package com.aitutor.chatbot.app.data.firebase

import com.aitutor.chatbot.app.domain.model.ReportReason

/**
 * Message reporting. The transport is gone: reports used to be create-only writes to a top-level
 * `reports` collection in Firestore, which was removed along with the rest of it.
 *
 * Until the custom API exposes an endpoint, [submit] accepts the report and fails. That is the
 * honest behaviour — the alternative is telling a student their report was filed when nothing left
 * the device. The caller already ignores the result, so the reporting UI behaves as it did before.
 */
class ReportRepository {

    suspend fun submit(
        chatId: String,
        messageId: String,
        reason: ReportReason,
        detail: String?,
    ): Result<Unit> = Result.failure(ReportingNotConfiguredException())
}

class ReportingNotConfiguredException : IllegalStateException(
    "Reports can't be sent yet — no reporting endpoint is configured."
)
