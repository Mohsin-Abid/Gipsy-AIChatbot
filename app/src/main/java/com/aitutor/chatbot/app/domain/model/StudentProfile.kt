package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.icons.AppIcons

/** Where the student is studying — drives how explanations are pitched. */
enum class StudyLevel(
    @param:StringRes val labelRes: Int,
    @param:StringRes val detailRes: Int,
    val icon: ImageVector,
) {
    School(R.string.setup_level_school, R.string.setup_level_school_detail, AppIcons.School),
    College(R.string.setup_level_college, R.string.setup_level_college_detail, AppIcons.College),
    University(R.string.setup_level_university, R.string.setup_level_university_detail, AppIcons.Study),
}

enum class Grade(@param:StringRes val labelRes: Int) {
    Grades1To5(R.string.grade_1_5),
    Grade6(R.string.grade_6),
    Grade7(R.string.grade_7),
    Grade8(R.string.grade_8),
    Grade9(R.string.grade_9),
    Grade10(R.string.grade_10),
    Grade11(R.string.grade_11),
    Grade12(R.string.grade_12),
}

enum class Subject(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    Math(R.string.subject_math, AppIcons.Math),
    Physics(R.string.subject_physics, AppIcons.Physics),
    Chemistry(R.string.subject_chemistry, AppIcons.Chemistry),
    Biology(R.string.subject_biology, AppIcons.Biology),
    English(R.string.subject_english, AppIcons.EnglishSubject),
    History(R.string.subject_history, AppIcons.History),
    Geography(R.string.subject_geography, AppIcons.Globe),
    ComputerScience(R.string.subject_computer_science, AppIcons.Code),
    Economics(R.string.subject_economics, AppIcons.Economics),
    Languages(R.string.subject_languages, AppIcons.Languages),
    Art(R.string.subject_art, AppIcons.Art),
}

enum class StudyGoal(@param:StringRes val labelRes: Int, val icon: ImageVector) {
    AceExams(R.string.goal_exams, AppIcons.Target),
    FinishHomework(R.string.goal_homework, AppIcons.Quiz),
    UnderstandTopics(R.string.goal_understand, AppIcons.Idea),
    ImproveWriting(R.string.goal_writing, AppIcons.Write),
}

enum class StudyTime(@param:StringRes val labelRes: Int) {
    Minutes15(R.string.study_time_15),
    Minutes30(R.string.study_time_30),
    Hour1(R.string.study_time_60),
    Hours2Plus(R.string.study_time_120),
}

/**
 * What the setup flow collects. It is one immutable value passed between steps, so a step can be
 * skipped without leaving the rest half-written.
 */
data class StudentProfile(
    val name: String = "",
    val level: StudyLevel? = StudyLevel.School,
    val grade: Grade? = Grade.Grade10,
    val subjects: Set<Subject> = emptySet(),
    val goals: Set<StudyGoal> = emptySet(),
    val dailyTime: StudyTime = StudyTime.Hour1,
    val reminderEnabled: Boolean = true,
) {
    companion object {
        /** The design shows two goals chosen; more than that stops being a priority. */
        const val MAX_GOALS = 2
    }
}
