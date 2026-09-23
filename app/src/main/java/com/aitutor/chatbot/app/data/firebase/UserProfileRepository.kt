package com.aitutor.chatbot.app.data.firebase

import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.UserPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The device's entitlement, read from DataStore.
 *
 * `isPremium` used to arrive from a Firestore listener on `users/{uid}`, which a Cloud Function
 * owned exclusively so a tampered client couldn't grant itself premium. With Firestore removed
 * there is no server source any more: the cached flag is all there is, and a rooted device could
 * edit it. Whatever replaces it must keep that same rule — the client may read entitlement, never
 * mint it — which in practice means verifying the Play purchase token server-side before trusting
 * anything.
 */
class UserProfileRepository(
    private val preferencesRepository: UserPreferencesRepository,
) {

    val plan: Flow<UserPlan> = combine(
        preferencesRepository.premiumCached,
        preferencesRepository.questionsUsedToday,
    ) { isPremium, used -> UserPlan(isPremium = isPremium, questionsUsedToday = used) }

    suspend fun recordQuestionAsked() {
        preferencesRepository.incrementQuestionsUsedToday()
    }
}
