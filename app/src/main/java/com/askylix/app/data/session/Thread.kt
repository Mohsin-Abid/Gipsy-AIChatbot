package com.askylix.app.data.session

import com.askylix.app.data.remote.MessageDto
import com.askylix.app.data.remote.toAttachment
import com.askylix.app.data.remote.toBlock
import com.askylix.app.data.remote.toEpochMillisOrZero
import com.askylix.app.data.remote.toRating
import com.askylix.app.domain.model.ChatMessage
import java.util.Calendar

/** A message with the time it was written, which is what the day separators are worked out from. */
data class StoredMessage(val message: ChatMessage, val createdAt: Long) {
    val id: String
        get() = when (message) {
            is ChatMessage.User -> message.id
            is ChatMessage.Assistant -> message.id
            else -> ""
        }
}

/** A message from the server, or null for a role this build does not know. */
fun MessageDto.toStoredOrNull(): StoredMessage? {
    val at = createdAt.toEpochMillisOrZero()
    return when (role) {
        "USER" -> StoredMessage(
            ChatMessage.User(
                id = messageId,
                text = text.orEmpty(),
                attachment = attachment?.toAttachment(),
            ),
            at,
        )
        "ASSISTANT" -> StoredMessage(
            ChatMessage.Assistant(
                id = messageId,
                blocks = blocks.orEmpty().map { it.toBlock() },
                rating = rating.toRating(),
            ),
            at,
        )
        else -> null
    }
}

/**
 * The thread as the screen draws it: a separator before the first message of each day, and the action
 * row on the newest answer only.
 *
 * The order is the order given. The server puts a question and its answer in the same second, so
 * sorting by time here would shuffle them; its order is the conversation's order.
 */
fun List<StoredMessage>.toThread(): List<ChatMessage> {
    val newestAnswer = indexOfLast { it.message is ChatMessage.Assistant }
    val thread = mutableListOf<ChatMessage>()
    var lastDay: Long? = null

    forEachIndexed { index, stored ->
        val day = stored.createdAt.startOfDay()
        if (day != lastDay) {
            thread += ChatMessage.DayMarker(timestamp = stored.createdAt)
            lastDay = day
        }
        val message = stored.message
        thread += if (message is ChatMessage.Assistant) message.copy(showActions = index == newestAnswer) else message
    }
    return thread
}

/** Midnight local time, which is the boundary a reader means by "a different day". */
internal fun Long.startOfDay(): Long {
    val calendar = Calendar.getInstance().apply { timeInMillis = this@startOfDay }
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
