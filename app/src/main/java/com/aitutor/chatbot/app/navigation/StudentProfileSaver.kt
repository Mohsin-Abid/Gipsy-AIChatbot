package com.aitutor.chatbot.app.navigation

import androidx.compose.runtime.saveable.Saver
import com.aitutor.chatbot.app.domain.model.Grade
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyGoal
import com.aitutor.chatbot.app.domain.model.StudyLevel
import com.aitutor.chatbot.app.domain.model.StudyTime
import com.aitutor.chatbot.app.domain.model.Subject

/**
 * Keeps the half-filled setup profile across process death. Enum names are stored rather than
 * ordinals, so reordering an enum later can't silently change someone's saved answers.
 */
val StudentProfileSaver: Saver<StudentProfile, List<String>> = Saver(
    save = { profile ->
        listOf(
            profile.name,
            profile.level?.name.orEmpty(),
            profile.grade?.name.orEmpty(),
            profile.subjects.joinToString(",") { it.name },
            profile.goals.joinToString(",") { it.name },
            profile.dailyTime.name,
            profile.reminderEnabled.toString(),
        )
    },
    restore = { saved ->
        StudentProfile(
            name = saved[0],
            level = saved[1].toEnumOrNull(StudyLevel.entries),
            grade = saved[2].toEnumOrNull(Grade.entries),
            subjects = saved[3].toEnumSet(Subject.entries),
            goals = saved[4].toEnumSet(StudyGoal.entries),
            dailyTime = saved[5].toEnumOrNull(StudyTime.entries) ?: StudyTime.Hour1,
            reminderEnabled = saved[6].toBooleanStrictOrNull() ?: true,
        )
    },
)

private fun <T : Enum<T>> String.toEnumOrNull(values: List<T>): T? =
    values.firstOrNull { it.name == this }

private fun <T : Enum<T>> String.toEnumSet(values: List<T>): Set<T> =
    if (isBlank()) emptySet() else split(",").mapNotNull { it.toEnumOrNull(values) }.toSet()
