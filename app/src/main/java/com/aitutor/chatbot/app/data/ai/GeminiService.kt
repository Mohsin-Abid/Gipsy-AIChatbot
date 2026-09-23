package com.aitutor.chatbot.app.data.ai

import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.aitutor.chatbot.app.domain.model.AppLanguage
import com.aitutor.chatbot.app.domain.model.BASE_TUTOR_INSTRUCTION
import com.aitutor.chatbot.app.domain.model.ExplanationLevel
import com.aitutor.chatbot.app.domain.model.Message
import com.aitutor.chatbot.app.domain.model.MessageRole
import com.aitutor.chatbot.app.domain.model.Mode
import com.aitutor.chatbot.app.domain.model.StudentProfile

private const val MODEL_NAME = "gemini-2.5-flash"

/**
 * Wraps Firebase AI Logic (Gemini). Stateless per call: the full message history is passed as
 * `contents` on every request rather than keeping a live `Chat` object around, since Room/Firestore
 * already own the durable history — this survives process death for free.
 */
class GeminiService {

    private val firebaseAi by lazy { Firebase.ai(backend = GenerativeBackend.googleAI()) }

    private fun modelFor(
        mode: Mode,
        explanationLevel: ExplanationLevel,
        language: AppLanguage,
        profile: StudentProfile,
    ): GenerativeModel =
        firebaseAi.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content { text(buildSystemInstruction(mode, explanationLevel, language, profile)) },
        )

    suspend fun reply(
        mode: Mode,
        explanationLevel: ExplanationLevel,
        language: AppLanguage,
        profile: StudentProfile,
        history: List<Message>,
    ): Result<String> = runCatching {
        require(history.isNotEmpty()) { "Cannot reply with no history" }
        val model = modelFor(mode, explanationLevel, language, profile)
        val contents = history.map { message ->
            content(role = if (message.role == MessageRole.User) "user" else "model") {
                text(message.text)
            }
        }
        val response = model.generateContent(contents)
        response.text?.takeIf { it.isNotBlank() } ?: error("Gemini returned an empty response.")
    }

    private fun buildSystemInstruction(
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
}
