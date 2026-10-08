package com.aitutor.chatbot.app.navigation

import androidx.annotation.Keep
import com.aitutor.chatbot.app.domain.model.StudyTool

/**
 * What a chat does the moment it opens, for the doors into it that mean more than "open the tool".
 *
 * It travels inside [Route.Chat], so its serializer has to survive minification — hence [Keep].
 */
@Keep
enum class ChatStart {
    /** Just open it. What a tool card does. */
    None,

    /** Put the cursor in the composer and bring the keyboard up — the person came to type. */
    Compose,

    /** Open the scan sheet — the person came to photograph or pick something. */
    Scan,
}

/**
 * Where Home's ask bar and suggestion chips lead: a tool, and how its chat opens.
 *
 * [draft] is a question placed in the composer for the person to send or change — not sent for them,
 * because a mis-tap on a chip should not spend a request.
 */
data class ChatLaunch(
    val tool: StudyTool,
    val start: ChatStart = ChatStart.None,
    val draft: String = "",
)
