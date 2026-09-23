package com.aitutor.chatbot.app.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.HistoryEdu
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.vector.ImageVector

/** [promptLabel] is what actually reaches Gemini — the UI label is written for students, not models. */
enum class GradeLevel(val label: String, val caption: String, val promptLabel: String) {
    Elementary("Grades 1–5", "Primary school", "elementary school (grades 1-5)"),
    MiddleSchool("Grades 6–8", "Middle school", "middle school (grades 6-8)"),
    HighSchool("Grades 9–12", "High school", "high school (grades 9-12)"),
    College("College", "University level", "college / university level"),
    Other("Something else", "Self-study or other", "a general learner");

    companion object {
        fun fromName(name: String?): GradeLevel? = entries.firstOrNull { it.name == name }
    }
}

enum class Subject(val label: String, val icon: ImageVector) {
    Math("Math", Icons.Outlined.Calculate),
    Physics("Physics", Icons.Outlined.Bolt),
    Chemistry("Chemistry", Icons.Outlined.Science),
    Biology("Biology", Icons.Outlined.Biotech),
    English("English", Icons.AutoMirrored.Outlined.MenuBook),
    History("History", Icons.Outlined.HistoryEdu),
    Geography("Geography", Icons.Outlined.Public),
    ComputerScience("Computer Science", Icons.Outlined.Computer),
    Languages("Languages", Icons.Outlined.Translate);

    companion object {
        fun fromNames(names: Set<String>): Set<Subject> =
            names.mapNotNull { name -> entries.firstOrNull { it.name == name } }.toSet()
    }
}

enum class StudyGoal(val label: String, val detail: String, val promptLabel: String) {
    KeepUp("Keep up with homework", "Daily help getting assignments done", "keeping up with day-to-day homework"),
    ExamPrep("Prepare for exams", "Revision, practice and past questions", "preparing for upcoming exams"),
    Grades("Improve my grades", "Targeted help on weaker topics", "raising their grades in weaker topics"),
    Understand("Understand topics deeply", "Concepts explained until they click", "building deep conceptual understanding");

    companion object {
        fun fromName(name: String?): StudyGoal? = entries.firstOrNull { it.name == name }
    }
}

/**
 * Collected during onboarding, injected into every chat's system instruction so answers land at the
 * student's level. Every field is optional — onboarding is skippable and the tutor still works.
 */
data class StudentProfile(
    val grade: GradeLevel? = null,
    val subjects: Set<Subject> = emptySet(),
    val goal: StudyGoal? = null,
) {
    val isSet: Boolean get() = grade != null || subjects.isNotEmpty() || goal != null

    /** "Grades 9–12 · Math, Physics" — the dashboard chip. */
    val summary: String
        get() = listOfNotNull(
            grade?.label,
            subjects.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.label },
        ).joinToString(" · ").ifBlank { "Set up your learning profile" }

    /** The block handed to Gemini. Empty when nothing was collected, so no fake context is invented. */
    fun toPromptBlock(): String {
        if (!isSet) return ""
        val parts = buildList {
            grade?.let { add("studies at ${it.promptLabel}") }
            subjects.takeIf { it.isNotEmpty() }?.let { add("focuses on ${it.joinToString(", ") { s -> s.label }}") }
            goal?.let { add("is focused on ${it.promptLabel}") }
        }
        return "The student " + parts.joinToString("; ") + "."
    }
}
