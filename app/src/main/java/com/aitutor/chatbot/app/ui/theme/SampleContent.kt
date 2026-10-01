package com.aitutor.chatbot.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.aitutor.chatbot.app.R
import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.ChatMessage
import com.aitutor.chatbot.app.domain.model.HistoryEntry
import com.aitutor.chatbot.app.domain.model.HistoryGroup
import com.aitutor.chatbot.app.domain.model.ScanSource
import com.aitutor.chatbot.app.domain.model.Attachment
import com.aitutor.chatbot.app.domain.model.StudyTool
import java.util.concurrent.TimeUnit

/**
 * The content the artboards show, for previews only.
 *
 * It exists so Android Studio renders every screen as the design draws it — populated, not empty.
 * Nothing here is ever written to the database: a running app starts with no conversations, and
 * seeding these would put words in the tutor's mouth that no model produced.
 *
 * These read string resources, so they are composables rather than plain values.
 */
object SampleContent {

    @Composable
    @ReadOnlyComposable
    fun conversation(): List<ChatMessage> = listOf(
        ChatMessage.DayMarker(System.currentTimeMillis()),
        ChatMessage.User(id = 1, text = stringResource(R.string.chat_sample_q1)),
        ChatMessage.Assistant(id = 2, blocks = answerBlocks(), showActions = true),
        ChatMessage.User(
            id = 3,
            text = stringResource(R.string.chat_sample_q2),
            attachment = Attachment(
                source = ScanSource.Camera,
                fileName = "cell-cycle-notes.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 486_000,
                localPath = "",
            ),
        ),
    )

    @Composable
    @ReadOnlyComposable
    fun answerBlocks(): List<AnswerBlock> = listOf(
        AnswerBlock.Paragraph(stringResource(R.string.chat_sample_a1_intro)),
        AnswerBlock.Bullet(
            term = stringResource(R.string.chat_sample_a1_term1),
            text = stringResource(R.string.chat_sample_a1_text1),
        ),
        AnswerBlock.Bullet(
            term = stringResource(R.string.chat_sample_a1_term2),
            text = stringResource(R.string.chat_sample_a1_text2),
        ),
        AnswerBlock.Paragraph(stringResource(R.string.chat_sample_a1_outro)),
    )

    @Composable
    @ReadOnlyComposable
    fun history(): List<HistoryEntry> {
        val now = System.currentTimeMillis()
        fun hoursAgo(hours: Long) = now - TimeUnit.HOURS.toMillis(hours)
        return listOf(
            entry(1, R.string.history_1_title, R.string.history_1_detail, StudyTool.HomeworkSolver, hoursAgo(1), HistoryGroup.Today),
            entry(2, R.string.history_2_title, R.string.history_2_detail, StudyTool.NotesSummarizer, hoursAgo(2), HistoryGroup.Today),
            entry(3, R.string.history_3_title, R.string.history_3_detail, StudyTool.Chatbot, hoursAgo(4), HistoryGroup.Today),
            entry(4, R.string.history_4_title, R.string.history_4_detail, StudyTool.ConceptExplainer, hoursAgo(30), HistoryGroup.Yesterday),
            entry(5, R.string.history_5_title, R.string.history_5_detail, StudyTool.MathSolver, hoursAgo(34), HistoryGroup.Yesterday),
            entry(6, R.string.history_6_title, R.string.history_6_detail, StudyTool.GrammarFixer, hoursAgo(80), HistoryGroup.EarlierThisWeek),
            entry(7, R.string.history_7_title, R.string.history_7_detail, StudyTool.EssayWriter, hoursAgo(82), HistoryGroup.EarlierThisWeek),
        )
    }

    @Composable
    @ReadOnlyComposable
    private fun entry(
        id: Long,
        titleRes: Int,
        detailRes: Int,
        tool: StudyTool,
        updatedAt: Long,
        group: HistoryGroup,
    ) = HistoryEntry(
        chatId = id,
        title = stringResource(titleRes),
        detail = stringResource(detailRes),
        tool = tool,
        updatedAt = updatedAt,
        group = group,
    )
}
