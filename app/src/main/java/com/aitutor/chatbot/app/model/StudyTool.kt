package com.aitutor.chatbot.app.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.ui.graphics.vector.ImageVector

enum class ToolCategory(val label: String) {
    Chat("Chat"),
    ProblemSolving("Problem Solving"),
    Writing("Writing"),
    StudyAids("Study Aids"),
}

data class StudyTool(
    val id: String,
    val title: String,
    val description: String,
    val category: ToolCategory,
    val icon: ImageVector,
    val isFeatured: Boolean = false
)

val studyTools = listOf(
    StudyTool(
        id = "ai_tutor",
        title = "AI Tutor",
        description = "A personalized, guided session that adapts to how you learn.",
        category = ToolCategory.Chat,
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        isFeatured = true
    ),
    StudyTool(
        id = "chatbot",
        title = "AI Chatbot",
        description = "Ask anything about what you're studying, in plain conversation.",
        category = ToolCategory.Chat,
        icon = Icons.AutoMirrored.Outlined.Chat
    ),
    StudyTool(
        id = "homework_solver",
        title = "Homework Solver",
        description = "Step-by-step help that shows the reasoning, not just the answer.",
        category = ToolCategory.ProblemSolving,
        icon = Icons.AutoMirrored.Outlined.Assignment
    ),
    StudyTool(
        id = "math_solver",
        title = "Math Question Solver",
        description = "Equations and word problems, worked through one step at a time.",
        category = ToolCategory.ProblemSolving,
        icon = Icons.Outlined.Calculate
    ),
    StudyTool(
        id = "essay_writer",
        title = "Essay Writer",
        description = "Draft a structured essay from an outline or a topic.",
        category = ToolCategory.Writing,
        icon = Icons.Outlined.EditNote
    ),
    StudyTool(
        id = "grammar_fixer",
        title = "Grammar Fixer",
        description = "Polish grammar, spelling, and sentence style.",
        category = ToolCategory.Writing,
        icon = Icons.Outlined.Spellcheck
    ),
    StudyTool(
        id = "paraphrasing",
        title = "Paraphrasing",
        description = "Rewrite a passage in your own words, several ways.",
        category = ToolCategory.Writing,
        icon = Icons.Outlined.Shuffle
    ),
    StudyTool(
        id = "notes_summarizer",
        title = "Notes Summarizer",
        description = "Turn a page of notes into the handful of points that matter.",
        category = ToolCategory.StudyAids,
        icon = Icons.Outlined.Description
    ),
    StudyTool(
        id = "summary_generator",
        title = "Summary Generator",
        description = "Condense an article or chapter into a short summary.",
        category = ToolCategory.StudyAids,
        icon = Icons.Outlined.Summarize
    ),
    StudyTool(
        id = "concept_explainer",
        title = "Concept Explainer",
        description = "A plain-language explanation of a tricky idea.",
        category = ToolCategory.StudyAids,
        icon = Icons.Outlined.Lightbulb
    ),
)
