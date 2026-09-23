package com.aitutor.chatbot.app.domain.model

/**
 * Shown as tappable chips above the composer while a chat is still empty. A blank text field is the
 * hardest part of asking for help — these give the student a running start in each mode's shape.
 */
fun starterPromptsFor(modeId: ModeId): List<String> = when (modeId) {
    ModeId.AiChatbot -> listOf(
        "Explain this topic simply",
        "Quiz me on what I just studied",
        "Help me plan my revision",
    )
    ModeId.HomeworkSolver -> listOf(
        "Solve this step by step",
        "Check my working",
        "Give me a similar practice question",
    )
    ModeId.MathSolver -> listOf(
        "Solve and show every step",
        "Explain the formula used",
        "Where did I go wrong?",
    )
    ModeId.NotesSummarizer -> listOf(
        "Summarize these notes",
        "Pull out the key definitions",
        "List the formulas only",
    )
    ModeId.AiTutor -> listOf(
        "Teach me this from scratch",
        "Give me practice questions",
        "Test my understanding",
    )
    ModeId.GrammarFixer -> listOf(
        "Fix the grammar in this",
        "Make this sound more formal",
        "Explain my mistakes",
    )
    ModeId.EssayWriter -> listOf(
        "Help me outline this essay",
        "Write a model paragraph",
        "Improve my thesis statement",
    )
    ModeId.Paraphrasing -> listOf(
        "Reword this in my own words",
        "Make this more academic",
        "Simplify this passage",
    )
    ModeId.SummaryGenerator -> listOf(
        "Summarize this in a paragraph",
        "Give me a short summary",
        "Summarize for revision",
    )
    ModeId.ConceptExplainer -> listOf(
        "Explain this like I'm 12",
        "Give me a real-world example",
        "Why does this matter?",
    )
}
