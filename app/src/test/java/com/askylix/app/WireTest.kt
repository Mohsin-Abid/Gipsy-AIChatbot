package com.askylix.app

import com.askylix.app.data.remote.MeDto
import com.askylix.app.data.remote.ProfileDto
import com.askylix.app.data.remote.SettingsDto
import com.askylix.app.data.remote.toGrade
import com.askylix.app.data.remote.toProfile
import com.askylix.app.data.remote.toRating
import com.askylix.app.data.remote.toSignup
import com.askylix.app.data.remote.toStudyGoal
import com.askylix.app.data.remote.toStudyLevel
import com.askylix.app.data.remote.toStudyTime
import com.askylix.app.data.remote.toStudyTool
import com.askylix.app.data.remote.toSubject
import com.askylix.app.data.remote.wire
import com.askylix.app.data.repository.filledProfileFields
import com.askylix.app.data.repository.profilePatch
import com.askylix.app.domain.model.Grade
import com.askylix.app.domain.model.Rating
import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyGoal
import com.askylix.app.domain.model.StudyLevel
import com.askylix.app.domain.model.StudyTime
import com.askylix.app.domain.model.StudyTool
import com.askylix.app.domain.model.Subject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The words the server and the app use for the same things. Each list below is copied from the
 * backend's own documentation, so a rename on either side fails here instead of as a rejected profile.
 */
class WireTest {

    @Test
    fun `tools use the documented words`() {
        assertEquals(
            listOf(
                "CHATBOT", "HOMEWORK_SOLVER", "MATH_SOLVER", "NOTES_SUMMARIZER", "AI_TUTOR",
                "GRAMMAR_FIXER", "ESSAY_WRITER", "PARAPHRASING", "SUMMARY_GENERATOR", "CONCEPT_EXPLAINER",
            ).sorted(),
            StudyTool.entries.map { it.wire() }.sorted(),
        )
    }

    @Test
    fun `levels, subjects, goals, grades and times use the documented words`() {
        assertEquals(listOf("COLLEGE", "SCHOOL", "UNIVERSITY"), StudyLevel.entries.map { it.wire() }.sorted())
        assertEquals(
            listOf(
                "ART", "BIOLOGY", "CHEMISTRY", "COMPUTER_SCIENCE", "ECONOMICS", "ENGLISH",
                "GEOGRAPHY", "HISTORY", "LANGUAGES", "MATH", "OTHER", "PHYSICS",
            ),
            Subject.entries.map { it.wire() }.sorted(),
        )
        assertEquals(
            listOf("ACE_EXAMS", "FINISH_HOMEWORK", "IMPROVE_WRITING", "UNDERSTAND_TOPICS"),
            StudyGoal.entries.map { it.wire() }.sorted(),
        )
        assertEquals(
            listOf("GRADES_1_5", "GRADE_6", "GRADE_7", "GRADE_8", "GRADE_9", "GRADE_10", "GRADE_11", "GRADE_12", "OTHER").sorted(),
            Grade.entries.map { it.wire() }.sorted(),
        )
        assertEquals(
            listOf("HOURS_2_PLUS", "HOUR_1", "MINUTES_15", "MINUTES_30"),
            StudyTime.entries.map { it.wire() }.sorted(),
        )
    }

    @Test
    fun `the awkward ones are what the server expects, not what a rule would guess`() {
        assertEquals("GRADES_1_5", Grade.Grades1To5.wire())
        assertEquals("GRADE_10", Grade.Grade10.wire())
        assertEquals("COMPUTER_SCIENCE", Subject.ComputerScience.wire())
        assertEquals("HOURS_2_PLUS", StudyTime.Hours2Plus.wire())
        assertEquals("AI_TUTOR", StudyTool.AiTutor.wire())
    }

    @Test
    fun `every value reads back as itself`() {
        assertTrue(StudyTool.entries.all { it.wire().toStudyTool() == it })
        assertTrue(StudyLevel.entries.all { it.wire().toStudyLevel() == it })
        assertTrue(Subject.entries.all { it.wire().toSubject() == it })
        assertTrue(StudyGoal.entries.all { it.wire().toStudyGoal() == it })
        assertTrue(Grade.entries.all { it.wire().toGrade() == it })
        assertTrue(StudyTime.entries.all { it.wire().toStudyTime() == it })
    }

    @Test
    fun `a word this build does not know is dropped, not guessed at`() {
        assertNull("QUANTUM_PHYSICS".toSubject())
        assertNull("TIME_TRAVEL".toStudyTool())
        assertNull(null.toGrade())
    }

    @Test
    fun `ratings`() {
        assertEquals("HELPFUL", Rating.Helpful.wire())
        assertEquals("NOT_HELPFUL", Rating.NotHelpful.wire())
        assertEquals("NONE", (null as Rating?).wire())
        assertEquals(Rating.Helpful, "HELPFUL".toRating())
        assertNull("NONE".toRating())
        assertNull(null.toRating())
    }

    // ---- Profile ----

    @Test
    fun `the server's profile becomes the app's, with the device's reminder setting`() {
        val me = MeDto(
            setupCompleted = true,
            profile = ProfileDto(
                name = "Sara", level = "SCHOOL", grade = "GRADE_8",
                subjects = listOf("MATH", "PHYSICS", "NOT_A_SUBJECT"),
                goals = listOf("ACE_EXAMS"), dailyTime = "MINUTES_30",
            ),
            settings = SettingsDto(remindersEnabled = true),
        )

        val profile = me.toProfile()

        assertEquals("Sara", profile.name)
        assertEquals(StudyLevel.School, profile.level)
        assertEquals(Grade.Grade8, profile.grade)
        assertEquals(setOf(Subject.Math, Subject.Physics), profile.subjects)
        assertEquals(setOf(StudyGoal.AceExams), profile.goals)
        assertEquals(StudyTime.Minutes30, profile.dailyTime)
        assertTrue(profile.reminderEnabled)
    }

    @Test
    fun `an empty server profile gives an empty one, with a sensible time`() {
        val profile = MeDto().toProfile()

        assertEquals("", profile.name)
        assertNull(profile.level)
        assertTrue(profile.subjects.isEmpty())
        assertEquals(StudyTime.Hour1, profile.dailyTime)
    }

    @Test
    fun `signup carries every answer in the server's words`() {
        val signup = StudentProfile(
            name = " Sara ", level = StudyLevel.School, grade = Grade.Grade8,
            subjects = setOf(Subject.Math), goals = setOf(StudyGoal.AceExams, StudyGoal.UnderstandTopics),
            dailyTime = StudyTime.Minutes30, reminderEnabled = true,
        ).toSignup()

        assertEquals("Sara", signup.name)
        assertEquals("SCHOOL", signup.level)
        assertEquals("GRADE_8", signup.grade)
        assertEquals(listOf("MATH"), signup.subjects)
        assertEquals(setOf("ACE_EXAMS", "UNDERSTAND_TOPICS"), signup.goals.toSet())
        assertEquals("MINUTES_30", signup.dailyTime)
        assertTrue(signup.remindersEnabled)
    }

    @Test
    fun `an empty name is sent as no name`() {
        assertNull(StudentProfile(name = "  ").toSignup().name)
    }

    // ---- Patches ----

    private val base = StudentProfile(
        name = "Alex", level = StudyLevel.School, grade = Grade.Grade10,
        subjects = setOf(Subject.Math), goals = setOf(StudyGoal.AceExams), dailyTime = StudyTime.Hour1,
    )

    @Test
    fun `nothing changed means nothing to send`() {
        assertNull(profilePatch(base, base))
    }

    @Test
    fun `only the fields that changed are sent`() {
        val patch = profilePatch(base, base.copy(name = "Sam"))!!

        assertEquals(setOf("name"), patch.keys)
        assertEquals(JsonPrimitive("Sam"), patch["name"])
    }

    @Test
    fun `a reminder change is the device's own business and is not sent`() {
        assertNull(profilePatch(base, base.copy(reminderEnabled = !base.reminderEnabled)))
    }

    @Test
    fun `a field cleared to nothing is sent as an explicit null`() {
        // Leaving the key out would mean "unchanged", so a grade taken away must say so.
        val patch = profilePatch(base, base.copy(level = StudyLevel.College, grade = null))!!

        assertEquals(JsonPrimitive("COLLEGE"), patch["level"])
        assertEquals(JsonNull, patch["grade"])
    }

    @Test
    fun `lists are sent whole`() {
        val patch = profilePatch(base, base.copy(subjects = setOf(Subject.Math, Subject.Art)))!!

        assertEquals(setOf("subjects"), patch.keys)
        assertEquals("""["MATH","ART"]""", patch["subjects"].toString().replace(" ", ""))
    }

    @Test
    fun `the fallback profile carries only what was filled in`() {
        val fields = filledProfileFields(StudentProfile(name = "", level = null, grade = null))

        assertEquals(setOf("dailyTime"), fields.keys)
    }
}
