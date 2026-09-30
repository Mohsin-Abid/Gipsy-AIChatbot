package com.aitutor.chatbot.app.data.api

import com.aitutor.chatbot.app.domain.model.AnswerBlock
import com.aitutor.chatbot.app.domain.model.StudentProfile
import com.aitutor.chatbot.app.domain.model.StudyTool

/**
 * What the app sends to get an answer.
 *
 * The profile is included because the tutor is meant to pitch its reply at the student's level, and
 * [history] carries the conversation so far so a follow-up question makes sense on its own.
 */
data class TutorRequest(
    val tool: StudyTool,
    val subject: String,
    val question: String,
    val attachedText: String? = null,
    val history: List<TutorTurn> = emptyList(),
    val profile: StudentProfile = StudentProfile(),
)

/** One earlier turn, flattened to text — the wire format of whatever API is plugged in. */
data class TutorTurn(val fromUser: Boolean, val text: String)

/** A reply, already in the blocks the screens render. */
data class TutorAnswer(val blocks: List<AnswerBlock>)

/**
 * The seam the custom API plugs into.
 *
 * Nothing above this interface knows how answers are fetched, so swapping in an HTTP client later
 * touches one file. It returns [Result] rather than throwing: a failed request is an ordinary
 * outcome the chat screen has to show, not an exception to crash on.
 */
interface TutorApiClient {
    suspend fun requestAnswer(request: TutorRequest): Result<TutorAnswer>
}

/** Raised when a reply is asked for before an API has been configured. */
class TutorApiNotConfiguredException : IllegalStateException(
    "No tutor API is configured. Provide a TutorApiClient implementation in AppContainer."
)

/**
 * The client in use until a real endpoint exists.
 *
 * It fails, deliberately and every time. The alternative — returning canned text — would make the
 * app look like it worked and put words in the tutor's mouth that no model produced, which is worse
 * than an honest error the moment anyone believes one of those answers.
 */
class UnconfiguredTutorApiClient : TutorApiClient {
    override suspend fun requestAnswer(request: TutorRequest): Result<TutorAnswer> =
        Result.failure(TutorApiNotConfiguredException())
}
