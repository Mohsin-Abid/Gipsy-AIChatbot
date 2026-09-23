package com.aitutor.chatbot.app.domain.model

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitutor.chatbot.app.ui.theme.CategoryChatTint
import com.aitutor.chatbot.app.ui.theme.CategoryProblemSolvingTint
import com.aitutor.chatbot.app.ui.theme.CategoryStudyAidsTint
import com.aitutor.chatbot.app.ui.theme.CategoryWritingTint

enum class ModeCategory(val label: String, val tint: Color) {
    Chat("Chat", CategoryChatTint),
    ProblemSolving("Problem Solving", CategoryProblemSolvingTint),
    Writing("Writing", CategoryWritingTint),
    StudyAids("Study Aids", CategoryStudyAidsTint),
}

enum class ModeId {
    AiChatbot, HomeworkSolver, MathSolver, NotesSummarizer, AiTutor,
    GrammarFixer, EssayWriter, Paraphrasing, SummaryGenerator, ConceptExplainer
}

data class Mode(
    val id: ModeId,
    val title: String,
    val description: String,
    val category: ModeCategory,
    val icon: ImageVector,
    val inputPlaceholder: String,
    /** Mode-specific slice of the Gemini system instruction; combined with [BASE_TUTOR_INSTRUCTION] in Phase 2. */
    val instruction: String,
    val usesStructuredOutput: Boolean = false,
    /** Premium-only modes are visible to everyone but open the paywall instead of a chat. */
    val isPremium: Boolean = false,
)

/**
 * Combined with a student-profile context block and each [Mode.instruction] to form the
 * per-chat systemInstruction sent to Gemini (Firebase AI Logic) — wired starting Phase 2.
 */
const val BASE_TUTOR_INSTRUCTION = """
You are a patient, encouraging study tutor. Teach through explanation and guided steps rather
than only giving answers. Match your language and complexity to the student's grade level.
Always respond in the same language and script the student used. Never fabricate facts,
citations, or sources — say when you're unsure. Do not produce a finished, submittable
assignment (essay, report, homework) on the student's behalf; guide them to write it themselves.
"""

val modes: List<Mode> = listOf(
    Mode(
        id = ModeId.AiChatbot,
        title = "AI Chatbot",
        description = "Ask anything about what you're studying, in plain conversation.",
        category = ModeCategory.Chat,
        icon = Icons.AutoMirrored.Outlined.Chat,
        inputPlaceholder = "Ask me anything…",
        instruction = "Hold a casual, unstructured study conversation. If the student's message " +
            "looks like a homework-style question with a specific right answer, offer to switch " +
            "to Homework Solver mode rather than solving it here."
    ),
    Mode(
        id = ModeId.HomeworkSolver,
        title = "Homework Solver",
        description = "Step-by-step help that shows the reasoning, not just the answer.",
        category = ModeCategory.ProblemSolving,
        icon = Icons.AutoMirrored.Outlined.Assignment,
        inputPlaceholder = "Paste or describe the homework question…",
        instruction = "Solve as numbered steps, with the reasoning for each step, not just the " +
            "operation. After the solution, add one unsolved practice question of similar " +
            "difficulty for the student to try themselves."
    ),
    Mode(
        id = ModeId.MathSolver,
        title = "Math Question Solver",
        description = "Equations and word problems, worked through one step at a time.",
        category = ModeCategory.ProblemSolving,
        icon = Icons.Outlined.Calculate,
        inputPlaceholder = "Type a math question or equation…",
        instruction = "Structure the response as: Given / Find, then Method, then numbered " +
            "step-by-step working, then a boxed final answer, then a one-line sanity check of " +
            "the result.",
        usesStructuredOutput = true
    ),
    Mode(
        id = ModeId.NotesSummarizer,
        title = "Notes Summarizer",
        description = "Turn a page of notes into the handful of points that matter.",
        category = ModeCategory.StudyAids,
        icon = Icons.Outlined.Description,
        inputPlaceholder = "Paste your notes…",
        instruction = "Condense into short bullet notes. Separate out definitions and formulas " +
            "into their own clearly labelled sub-sections rather than mixing them into prose.",
        isPremium = true
    ),
    Mode(
        id = ModeId.AiTutor,
        title = "AI Tutor",
        description = "A personalized, guided session that adapts to how you learn.",
        category = ModeCategory.Chat,
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        inputPlaceholder = "What are you working on?",
        instruction = "Run a sequential explain → example → practice → check loop. Never solve " +
            "the student's original question directly — guide them to the answer through that loop.",
        isPremium = true
    ),
    Mode(
        id = ModeId.GrammarFixer,
        title = "Grammar Fixer",
        description = "Polish grammar, spelling, and sentence style.",
        category = ModeCategory.Writing,
        icon = Icons.Outlined.Spellcheck,
        inputPlaceholder = "Paste the text to correct…",
        instruction = "Return the corrected text in full, followed by a list of each change made " +
            "and a short reason for it."
    ),
    Mode(
        id = ModeId.EssayWriter,
        title = "Essay Writer",
        description = "Draft a structured essay from an outline or a topic.",
        category = ModeCategory.Writing,
        icon = Icons.Outlined.EditNote,
        inputPlaceholder = "What's the essay topic or prompt?",
        instruction = "Provide an outline and exactly one fully-written model paragraph — never a " +
            "complete essay. Coach the student through writing the remaining paragraphs themselves.",
        isPremium = true
    ),
    Mode(
        id = ModeId.Paraphrasing,
        title = "Paraphrasing",
        description = "Rewrite a passage in your own words, several ways.",
        category = ModeCategory.Writing,
        icon = Icons.Outlined.Shuffle,
        inputPlaceholder = "Paste the text to paraphrase…",
        instruction = "Reword the passage at roughly the same length in the tone the student " +
            "requests (default: neutral/academic). This is a rewrite, not a summary — preserve " +
            "every point made in the original.",
        isPremium = true
    ),
    Mode(
        id = ModeId.SummaryGenerator,
        title = "Summary Generator",
        description = "Condense an article or chapter into a short summary.",
        category = ModeCategory.StudyAids,
        icon = Icons.Outlined.Summarize,
        inputPlaceholder = "Paste the article or chapter…",
        instruction = "Write a plain-paragraph summary at the student's requested length " +
            "(default: medium) — prose, not bullet-point notes formatting.",
        isPremium = true
    ),
    Mode(
        id = ModeId.ConceptExplainer,
        title = "Concept Explainer",
        description = "A plain-language explanation of a tricky idea.",
        category = ModeCategory.StudyAids,
        icon = Icons.Outlined.Lightbulb,
        inputPlaceholder = "What concept do you want explained?",
        instruction = "Give a short definition, then an analogy, then a real-world example, then " +
            "one line on why it matters/where it's used."
    ),
)
