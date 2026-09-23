package com.aitutor.chatbot.app.data.firebase

import com.aitutor.chatbot.app.data.local.UserPreferencesRepository
import com.aitutor.chatbot.app.domain.model.UserPlan
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The device's entitlement. `isPremium` is read-only here by design: it lives on `users/{uid}` in
 * Firestore and only a Cloud Function that has verified a Play Billing purchase may write it, so a
 * tampered client can't grant itself premium. Reads are cached in DataStore for offline/cold start.
 */
class UserProfileRepository(
    private val firestore: FirebaseFirestore,
    private val authGateway: FirebaseAuthGateway,
    private val preferencesRepository: UserPreferencesRepository,
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var syncStarted = false

    val plan: Flow<UserPlan> = combine(
        preferencesRepository.premiumCached,
        preferencesRepository.questionsUsedToday,
    ) { isPremium, used -> UserPlan(isPremium = isPremium, questionsUsedToday = used) }

    fun startSync() {
        if (syncStarted) return
        syncStarted = true
        repoScope.launch {
            val uid = authGateway.uid.first() ?: return@launch
            firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
                val isPremium = snapshot?.getBoolean("isPremium") ?: return@addSnapshotListener
                repoScope.launch { preferencesRepository.setPremiumCached(isPremium) }
            }
        }
    }

    suspend fun recordQuestionAsked() {
        preferencesRepository.incrementQuestionsUsedToday()
    }
}
