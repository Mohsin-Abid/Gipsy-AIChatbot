package com.aitutor.chatbot.app.domain.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.ui.icons.AppIcons

/**
 * The groups the Tools screen files its rows under, in the order it lists them. [chipLabelRes] is
 * the short form for the filter chips, where the full heading would not fit.
 */
enum class ToolCategory(
    @param:StringRes val labelRes: Int,
    @param:StringRes val chipLabelRes: Int,
) {
    Popular(R.string.tools_group_popular, R.string.tools_chip_solve),
    Understand(R.string.tools_group_understand, R.string.tools_chip_understand),
    Write(R.string.tools_group_write, R.string.tools_chip_write),
    Summarize(R.string.tools_group_summarize, R.string.tools_chip_summarize),
}

/**
 * The ten study tools, and everything the three screens that list them need.
 *
 * Each screen wants a different cut of the same set, which is why there are several flags rather
 * than one ordering: Home shows [featured] as two large cards with [gridTools] beneath, Tools
 * promotes [recommended] to its own card and groups the rest by [category], and both use
 * [taglineRes] as the one-line description. [gridLabelRes] carries an explicit line break because
 * Home's four-column grid sets every label on exactly two lines.
 */
enum class StudyTool(
    @param:StringRes val labelRes: Int,
    @param:StringRes val gridLabelRes: Int,
    @param:StringRes val taglineRes: Int,
    val icon: ImageVector,
    val category: ToolCategory? = null,
    /** Shown as a large card on Home. */
    val featured: Boolean = false,
    /** Shown as the single highlighted card at the top of Tools, above the groups. */
    val recommended: Boolean = false,
    val isNew: Boolean = false,
) {
    Chatbot(
        labelRes = R.string.tool_chatbot,
        gridLabelRes = R.string.tool_chatbot_grid,
        taglineRes = R.string.tool_chatbot_tagline,
        icon = AppIcons.Chat,
        category = ToolCategory.Understand,
        featured = true,
    ),
    HomeworkSolver(
        labelRes = R.string.tool_homework,
        gridLabelRes = R.string.tool_homework_grid,
        taglineRes = R.string.tool_homework_tagline,
        icon = AppIcons.Quiz,
        category = ToolCategory.Popular,
        featured = true,
    ),
    MathSolver(
        labelRes = R.string.tool_math,
        gridLabelRes = R.string.tool_math_grid,
        taglineRes = R.string.tool_math_tagline,
        icon = AppIcons.Math,
        category = ToolCategory.Popular,
    ),
    NotesSummarizer(
        labelRes = R.string.tool_notes,
        gridLabelRes = R.string.tool_notes_grid,
        taglineRes = R.string.tool_notes_tagline,
        icon = AppIcons.Document,
        category = ToolCategory.Summarize,
    ),
    AiTutor(
        labelRes = R.string.tool_tutor,
        gridLabelRes = R.string.tool_tutor_grid,
        taglineRes = R.string.tool_tutor_tagline,
        icon = AppIcons.Study,
        recommended = true,
    ),
    GrammarFixer(
        labelRes = R.string.tool_grammar,
        gridLabelRes = R.string.tool_grammar_grid,
        taglineRes = R.string.tool_grammar_tagline,
        icon = AppIcons.Grammar,
        category = ToolCategory.Write,
    ),
    EssayWriter(
        labelRes = R.string.tool_essay,
        gridLabelRes = R.string.tool_essay_grid,
        taglineRes = R.string.tool_essay_tagline,
        icon = AppIcons.Write,
        category = ToolCategory.Write,
    ),
    Paraphrasing(
        labelRes = R.string.tool_paraphrase,
        gridLabelRes = R.string.tool_paraphrase_grid,
        taglineRes = R.string.tool_paraphrase_tagline,
        icon = AppIcons.Paraphrase,
        category = ToolCategory.Write,
        isNew = true,
    ),
    SummaryGenerator(
        labelRes = R.string.tool_summary,
        gridLabelRes = R.string.tool_summary_grid,
        taglineRes = R.string.tool_summary_tagline,
        icon = AppIcons.SummaryLines,
        category = ToolCategory.Summarize,
    ),
    ConceptExplainer(
        labelRes = R.string.tool_concept,
        gridLabelRes = R.string.tool_concept_grid,
        taglineRes = R.string.tool_concept_tagline,
        icon = AppIcons.Idea,
        category = ToolCategory.Understand,
    );

    companion object {
        val featuredTools: List<StudyTool> get() = entries.filter { it.featured }
        val gridTools: List<StudyTool> get() = entries.filterNot { it.featured }
        val recommendedTool: StudyTool get() = entries.first { it.recommended }

        /**
         * The tools filed under [category], for the Tools screen. The recommended tool belongs to
         * no group — it already has its own card there, and listing it twice would overstate the
         * count in a group's badge.
         */
        fun inCategory(category: ToolCategory): List<StudyTool> =
            entries.filter { it.category == category }
    }
}
