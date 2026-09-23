package com.aitutor.chatbot.app.domain.model

data class ModeRecommendation(val mode: Mode, val reason: String)

/**
 * Turns the onboarding answers into the "Recommended for you" rail. Deliberately rule-based rather
 * than a model call — it has to render instantly on a cold dashboard, offline included.
 */
fun recommendationsFor(profile: StudentProfile, limit: Int = 3): List<ModeRecommendation> {
    val byId = modes.associateBy { it.id }
    val picks = linkedMapOf<ModeId, String>()

    fun suggest(id: ModeId, reason: String) {
        if (!picks.containsKey(id)) picks[id] = reason
    }

    profile.subjects.forEach { subject ->
        when (subject) {
            Subject.Math -> suggest(ModeId.MathSolver, "For your ${subject.label} work")
            Subject.Physics, Subject.Chemistry, Subject.Biology ->
                suggest(ModeId.HomeworkSolver, "For your ${subject.label} problems")
            Subject.English -> suggest(ModeId.GrammarFixer, "For your ${subject.label} writing")
            Subject.Languages -> suggest(ModeId.Paraphrasing, "For your ${subject.label} practice")
            Subject.History, Subject.Geography -> suggest(ModeId.NotesSummarizer, "For your ${subject.label} notes")
            Subject.ComputerScience -> suggest(ModeId.ConceptExplainer, "For your ${subject.label} concepts")
        }
    }

    when (profile.goal) {
        StudyGoal.ExamPrep -> {
            suggest(ModeId.NotesSummarizer, "Revision-ready notes for exams")
            suggest(ModeId.ConceptExplainer, "Clear up topics before the exam")
        }
        StudyGoal.KeepUp -> suggest(ModeId.HomeworkSolver, "Stay on top of assignments")
        StudyGoal.Grades -> suggest(ModeId.AiTutor, "Guided practice on weak topics")
        StudyGoal.Understand -> suggest(ModeId.ConceptExplainer, "Explanations until it clicks")
        null -> Unit
    }

    // A sensible default trio when onboarding was skipped entirely.
    suggest(ModeId.HomeworkSolver, "Most popular with students")
    suggest(ModeId.MathSolver, "Step-by-step working, every time")
    suggest(ModeId.ConceptExplainer, "Turn tricky ideas into plain language")

    return picks.entries.take(limit).mapNotNull { (id, reason) ->
        byId[id]?.let { ModeRecommendation(it, reason) }
    }
}
