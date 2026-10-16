package com.askylix.app

import com.askylix.app.domain.model.StudentProfile
import com.askylix.app.domain.model.StudyGoal
import com.askylix.app.domain.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rules for picking subjects and goals, shared by first-run setup and the edit screens. */
class StudentProfileTest {

    @Test
    fun `toggling a subject adds it, and toggling it again takes it off`() {
        val added = StudentProfile().toggled(Subject.Math)
        assertTrue(Subject.Math in added.subjects)

        val removed = added.toggled(Subject.Math)
        assertFalse(Subject.Math in removed.subjects)
    }

    @Test
    fun `toggling one subject leaves the others alone`() {
        val profile = StudentProfile(subjects = setOf(Subject.Math, Subject.Other))
        assertEquals(setOf(Subject.Math), profile.toggled(Subject.Other).subjects)
    }

    @Test
    fun `toggling a subject changes nothing else about the profile`() {
        // An edit screen saves only the fields it owns; if the toggle touched the name or the daily
        // time, saving subjects would quietly undo changes made elsewhere.
        val profile = StudentProfile(name = "Alex", reminderEnabled = false)
        val toggled = profile.toggled(Subject.Physics)

        assertEquals("Alex", toggled.name)
        assertEquals(profile.level, toggled.level)
        assertEquals(profile.dailyTime, toggled.dailyTime)
        assertFalse(toggled.reminderEnabled)
    }

    @Test
    fun `goals are capped at the maximum`() {
        val full = StudentProfile(goals = setOf(StudyGoal.AceExams, StudyGoal.FinishHomework))
        assertEquals(StudentProfile.MAX_GOALS, full.goals.size)

        // A third pick is ignored rather than silently replacing one already made.
        val attempted = full.toggled(StudyGoal.ImproveWriting)
        assertEquals(full.goals, attempted.goals)
    }

    @Test
    fun `at the cap a chosen goal can still be removed`() {
        val full = StudentProfile(goals = setOf(StudyGoal.AceExams, StudyGoal.FinishHomework))
        val trimmed = full.toggled(StudyGoal.AceExams)

        assertEquals(setOf(StudyGoal.FinishHomework), trimmed.goals)
        // ...which makes room for another.
        assertTrue(StudyGoal.ImproveWriting in trimmed.toggled(StudyGoal.ImproveWriting).goals)
    }

    @Test
    fun `a goal can be added below the cap`() {
        assertEquals(
            setOf(StudyGoal.UnderstandTopics),
            StudentProfile().toggled(StudyGoal.UnderstandTopics).goals,
        )
    }
}
