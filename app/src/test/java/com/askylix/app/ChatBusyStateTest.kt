package com.askylix.app

import com.askylix.app.ui.chat.ChatUiState
import com.askylix.app.ui.chat.VoiceUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** What counts as "something in progress" — the only time Back asks before leaving a chat: work that leaving would lose. */
class ChatBusyStateTest {

    @Test
    fun `an idle chat is not busy`() {
        assertFalse(ChatUiState().isBusy)
    }

    @Test
    fun `waiting for an answer is not busy, because it keeps running when the screen is left`() {
        // The request runs outside the screen, so going back cuts nothing short and nothing is asked.
        assertFalse(ChatUiState(sending = true).isBusy)
    }

    @Test
    fun `adding a file is busy`() {
        assertTrue(ChatUiState(attaching = true).isBusy)
    }

    @Test
    fun `a typed draft is not busy, because nothing is running`() {
        assertFalse(ChatUiState(draft = "What is a derivative?").isBusy)
    }

    @Test
    fun `dictation is not busy, because closing its panel is what Back does there`() {
        assertFalse(ChatUiState(voice = VoiceUiState(listening = true)).isBusy)
    }
}
