package com.aitutor.chatbot.app.domain.model

/**
 * TEMPORARY TESTING SWITCH — set to false (or delete this constant and its single use in
 * UserPreferencesRepository.premiumCached) when the real premium logic lands.
 *
 * While true, every entitlement check passes: no paywall sheets, no locked tools, no daily question
 * cap. Nothing else about the premium plumbing is removed, so turning this off restores the full
 * gating exactly as it was.
 */
const val FORCE_PREMIUM_UNLOCK = true

/**
 * What this device is entitled to. [isPremium] mirrors `users/{uid}.isPremium` in Firestore, which
 * only a Cloud Function may write after verifying a Play Billing purchase — the client never sets it.
 */
data class UserPlan(
    // Defaults to the testing switch so the placeholder value used before the entitlement flow
    // emits doesn't briefly gate things; reverts to `false` on its own once the switch is off.
    val isPremium: Boolean = FORCE_PREMIUM_UNLOCK,
    val questionsUsedToday: Int = 0,
) {
    val freeQuestionsRemaining: Int
        get() = (FREE_DAILY_QUESTIONS - questionsUsedToday).coerceAtLeast(0)

    val canAskQuestion: Boolean
        get() = isPremium || freeQuestionsRemaining > 0

    fun canOpen(mode: Mode): Boolean = isPremium || !mode.isPremium

    companion object {
        const val FREE_DAILY_QUESTIONS = 10
    }
}

/** A single line item in the paywall's free-vs-premium comparison. */
data class PremiumBenefit(val title: String, val detail: String)

val premiumBenefits = listOf(
    PremiumBenefit("Unlimited questions", "No daily cap — ask as much as you need"),
    PremiumBenefit("All 10 study tools", "AI Tutor, Essay Writer, Paraphrasing and more"),
    PremiumBenefit("Photo & document solving", "Snap homework or upload a PDF"),
    PremiumBenefit("Voice answers", "Listen to explanations in your language"),
    PremiumBenefit("Priority responses", "Faster answers, even at peak times"),
)
