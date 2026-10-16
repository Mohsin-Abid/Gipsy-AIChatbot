package com.askylix.app.data.remote

import com.askylix.app.domain.model.AnswerBlock
import com.askylix.app.domain.model.AppLanguage
import com.askylix.app.domain.model.Attachment
import com.askylix.app.domain.model.ChatSummary
import com.askylix.app.domain.model.Grade
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyGoal
import com.askylix.app.domain.model.StudyLevel
import com.askylix.app.domain.model.StudyTime
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.domain.model.Subject
import java.time.Instant

// The words the server uses for the app's enums.
//
// They are written out rather than derived from the Kotlin names wherever deriving would guess:
// `Grades1To5` has to become GRADES_1_5, not GRADES_1_TO_5, and a quiet mismatch here would make the
// server refuse a profile that looks fine on screen. Each list is checked against the documented
// values in WireTest.

private val boundary = Regex("(?<=[a-z0-9])(?=[A-Z])")

/** `ComputerScience` to `COMPUTER_SCIENCE`. Right for every enum without digits in its names. */
private fun String.screamingSnake(): String = replace(boundary, "_").uppercase()

fun StudyTool.wire(): String = name.screamingSnake()
fun StudyLevel.wire(): String = name.screamingSnake()
fun Subject.wire(): String = name.screamingSnake()
fun StudyGoal.wire(): String = name.screamingSnake()

fun Grade.wire(): String = when (this) {
    Grade.Grades1To5 -> "GRADES_1_5"
    Grade.Grade6 -> "GRADE_6"
    Grade.Grade7 -> "GRADE_7"
    Grade.Grade8 -> "GRADE_8"
    Grade.Grade9 -> "GRADE_9"
    Grade.Grade10 -> "GRADE_10"
    Grade.Grade11 -> "GRADE_11"
    Grade.Grade12 -> "GRADE_12"
    Grade.Other -> "OTHER"
}

fun StudyTime.wire(): String = when (this) {
    StudyTime.Minutes15 -> "MINUTES_15"
    StudyTime.Minutes30 -> "MINUTES_30"
    StudyTime.Hour1 -> "HOUR_1"
    StudyTime.Hours2Plus -> "HOURS_2_PLUS"
}

fun String?.toStudyTool(): StudyTool? = StudyTool.entries.firstOrNull { it.wire() == this }
fun String?.toStudyLevel(): StudyLevel? = StudyLevel.entries.firstOrNull { it.wire() == this }
fun String?.toGrade(): Grade? = Grade.entries.firstOrNull { it.wire() == this }
fun String?.toSubject(): Subject? = Subject.entries.firstOrNull { it.wire() == this }
fun String?.toStudyGoal(): StudyGoal? = StudyGoal.entries.firstOrNull { it.wire() == this }
fun String?.toStudyTime(): StudyTime? = StudyTime.entries.firstOrNull { it.wire() == this }
fun String?.toAppLanguage(): AppLanguage? = AppLanguage.entries.firstOrNull { it.tag == this }

// ---- Profile --------------------------------------------------------------------------------------

/** The server's profile as the app holds it. A value this build does not know is dropped, not guessed at. */
fun MeDto.toProfile(): StudentProfile = StudentProfile(
    name = profile.name.orEmpty(),
    level = profile.level.toStudyLevel(),
    grade = profile.grade.toGrade(),
    subjects = profile.subjects.mapNotNull { it.toSubject() }.toSet(),
    goals = profile.goals.mapNotNull { it.toStudyGoal() }.toSet(),
    dailyTime = profile.dailyTime.toStudyTime() ?: StudyTime.Hour1,
    reminderEnabled = settings.remindersEnabled,
)

fun StudentProfile.toSignup(): SignupRequest = SignupRequest(
    name = name.trim().ifEmpty { null },
    level = level?.wire(),
    grade = grade?.wire(),
    subjects = subjects.map { it.wire() },
    goals = goals.map { it.wire() },
    dailyTime = dailyTime.wire(),
    remindersEnabled = reminderEnabled,
)

// ---- Chats ----------------------------------------------------------------------------------------

/** A chat for a list, or null if it names a tool this build does not have. */
fun ChatItemDto.toSummaryOrNull(): ChatSummary? {
    val known = tool.toStudyTool() ?: return null
    return ChatSummary(
        id = chatId,
        tool = known,
        title = title,
        lastMessageAt = lastMessageAt.toEpochMillisOrZero(),
    )
}

fun BlockDto.toBlock(): AnswerBlock =
    if (type == "BULLET" && term != null) AnswerBlock.Bullet(term = term, text = text) else AnswerBlock.Paragraph(text)

fun AttachmentDto.toAttachment(): Attachment = Attachment(
    source = Attachment.sourceFor(mimeType),
    fileName = fileName,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    url = url,
)

fun String?.toRating(): Rating? = when (this) {
    "HELPFUL" -> Rating.Helpful
    "NOT_HELPFUL" -> Rating.NotHelpful
    else -> null
}

fun Rating?.wire(): String = when (this) {
    Rating.Helpful -> "HELPFUL"
    Rating.NotHelpful -> "NOT_HELPFUL"
    null -> "NONE"
}

/** An ISO 8601 UTC time as epoch milliseconds, or 0 when absent or unreadable. */
fun String?.toEpochMillisOrZero(): Long =
    this?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() } ?: 0L
