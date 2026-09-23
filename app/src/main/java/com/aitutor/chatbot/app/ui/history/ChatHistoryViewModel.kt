package com.aitutor.chatbot.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitutor.chatbot.app.core.state.UiState
import com.aitutor.chatbot.app.data.chat.ChatRepository
import com.aitutor.chatbot.app.domain.model.ChatSummary
import com.aitutor.chatbot.app.domain.model.ModeCategory
import com.aitutor.chatbot.app.domain.model.modes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the History screen renders, derived from the one chat list the repository exposes. */
data class ChatHistoryData(
    val chats: List<ChatSummary>,
    val totalChats: Int,
    val chatsThisWeek: Int,
    val availableCategories: List<ModeCategory>,
)

class ChatHistoryViewModel(private val chatRepository: ChatRepository) : ViewModel() {

    private val query = MutableStateFlow("")
    val searchQuery: StateFlow<String> = query

    private val category = MutableStateFlow<ModeCategory?>(null)
    val selectedCategory: StateFlow<ModeCategory?> = category

    val uiState: StateFlow<UiState<ChatHistoryData>> =
        combine(chatRepository.allChats(), query, category) { chats, searchText, selected ->
            val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
            val filtered = chats
                .filter { searchText.isBlank() || it.title.contains(searchText, ignoreCase = true) }
                .filter { selected == null || it.categoryOrNull() == selected }

            ChatHistoryData(
                chats = filtered,
                totalChats = chats.size,
                chatsThisWeek = chats.count { it.timestampMillis >= weekAgo },
                // Only offer filters the student's own history can actually satisfy.
                availableCategories = ModeCategory.entries.filter { candidate ->
                    chats.any { it.categoryOrNull() == candidate }
                },
            )
        }
            .map<ChatHistoryData, UiState<ChatHistoryData>> { UiState.Success(it) }
            .catch { emit(UiState.Error(it.message ?: "Couldn't load your chats.", it)) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    init {
        chatRepository.startSync()
    }

    fun onQueryChange(newQuery: String) {
        query.value = newQuery
    }

    fun onCategorySelected(newCategory: ModeCategory?) {
        category.value = if (category.value == newCategory) null else newCategory
    }

    fun onDeleteChat(chatId: String) {
        viewModelScope.launch { chatRepository.deleteChat(chatId) }
    }
}

private fun ChatSummary.categoryOrNull(): ModeCategory? =
    modes.firstOrNull { it.id == modeId }?.category
