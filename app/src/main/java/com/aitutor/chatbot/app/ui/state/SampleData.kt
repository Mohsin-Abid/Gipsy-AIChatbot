package com.aitutor.chatbot.app.ui.state

import com.aitutor.chatbot.app.model.ChatEntry
import com.aitutor.chatbot.app.model.StudentProfile
import com.aitutor.chatbot.app.model.ToolCategory
import com.aitutor.chatbot.app.model.chatHistory
import com.aitutor.chatbot.app.model.studyTools

/** Realistic sample data shared by previews and the app's initial state. */
object SampleData {

    val profile = StudentProfile(name = "Aria Khan", gradeLabel = "Grade 7", initial = "A")

    const val Greeting = "Good afternoon, Aria"

    val quickTools = listOf(
        studyTools.first { it.id == "ai_tutor" },
        studyTools.first { it.id == "math_solver" },
        studyTools.first { it.id == "essay_writer" },
        studyTools.first { it.id == "concept_explainer" },
    )

    val history: List<ChatEntry> = chatHistory
    val recentHistory: List<ChatEntry> = chatHistory.take(3)

    val preferences = SettingsPreferences(
        darkMode = false,
        notifications = true,
        language = "English"
    )

    /** A deliberately long entry, for the "long text" preview scenario. */
    val longChatEntry = ChatEntry(
        id = "long",
        title = "Can you explain the difference between mitosis and meiosis in detail, with an example for each stage",
        toolLabel = "Concept Explainer",
        category = ToolCategory.StudyAids,
        time = "3:41 PM",
        dateGroup = "Today"
    )
}
