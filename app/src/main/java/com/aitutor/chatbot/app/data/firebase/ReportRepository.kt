package com.aitutor.chatbot.app.data.firebase

import com.aitutor.chatbot.app.domain.model.ReportReason
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Writes to the top-level `reports` collection — create-only by security rule (no read/update/
 * delete from the client). Not nested under `users/{uid}` since moderators need to query across
 * all users' reports.
 */
class ReportRepository(
    private val firestore: FirebaseFirestore,
    private val authGateway: FirebaseAuthGateway,
) {
    suspend fun submit(chatId: String, messageId: String, reason: ReportReason, detail: String?): Result<Unit> =
        runCatching {
            val report = hashMapOf(
                "uid" to authGateway.currentUid,
                "chatId" to chatId,
                "messageId" to messageId,
                "reason" to reason.name,
                "detail" to detail?.takeIf { it.isNotBlank() },
                "createdAt" to System.currentTimeMillis(),
            )
            firestore.collection("reports").add(report).await()
            Unit
        }
}
