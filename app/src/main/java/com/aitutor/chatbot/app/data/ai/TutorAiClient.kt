package com.aitutor.chatbot.app.data.ai

import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.BASE_TUTOR_INSTRUCTION
import com.aitutor.chatbot.app.domain.model.ExplanationLevel
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.StudentProfile

/**
 * The single seam between the chat pipeline and whichever model actually answers. Everything above
 * this — Room, Firestore sync, the chat UI, regenerate/expand — is provider-agnostic and stays
 * untouched when the backing service changes.
 *
 * Implementations are stateless per call: the full history arrives on every request rather than a
 * live session being held open, because Room and Firestore already own the durable history. That
 * survives process death for free.
 */
interface TutorAiClient {

    suspend fun reply(
        mode: Mode,
        explanationLevel: ExplanationLevel,
        language: AppLanguage,
        profile: StudentProfile,
        history: List<Message>,
    ): Result<String>
}

/**
 * Composes the per-chat system instruction from the base tutor rules, what onboarding learned about
 * the student, and the selected mode. Kept out of any one implementation because it's prompt
 * composition, not transport — a custom HTTP backend needs exactly this same string.
 */
fun buildSystemInstruction(
    mode: Mode,
    explanationLevel: ExplanationLevel,
    language: AppLanguage,
    profile: StudentProfile,
): String = buildString {
    append(BASE_TUTOR_INSTRUCTION.trim())
    append("\n\nStudent profile — ")
    val profileBlock = profile.toPromptBlock()
    if (profileBlock.isNotBlank()) {
        append(profileBlock)
        append(" ")
    }
    append("Explanation level: ${explanationLevel.label}. Preferred language: ${language.englishLabel}.")
    append("\n\nMode instructions: ")
    append(mode.instruction)
}

/** Surfaced straight to the student, so it's worded for them rather than for a log. */
class AiNotConfiguredException : IllegalStateException(
    "No AI service is connected yet, so answers can't be generated."
)

/**
 * Stands in until a real provider is wired up. Failing is the honest behaviour: [ChatRepository]
 * appends nothing on failure, so the student's own message stays put and the chat shows an inline
 * retry instead of a fabricated reply.
 */
class NotConfiguredAiClient : TutorAiClient {

    override suspend fun reply(
        mode: Mode,
        explanationLevel: ExplanationLevel,
        language: AppLanguage,
        profile: StudentProfile,
        history: List<Message>,
    ): Result<String> = Result.failure(AiNotConfiguredException())
}
