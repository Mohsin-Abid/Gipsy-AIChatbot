package com.aitutor.chatbot.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aitutor.chatbot.app.AITutorApplication
import com.aitutor.chatbot.app.ui.chat.ChatViewModel
import com.aitutor.chatbot.app.ui.history.HistoryViewModel
import com.aitutor.chatbot.app.ui.home.HomeViewModel
import com.aitutor.chatbot.app.ui.settings.SettingsViewModel

/** The container, reached from the `Application` the ViewModel was created under. */
private fun CreationExtras.container(): AppContainer =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as AITutorApplication).container

/**
 * One factory for every ViewModel in the app.
 *
 * Each takes its dependencies as constructor arguments and knows nothing about Android's
 * ViewModelProvider, so each is constructible in a test with fakes and no framework at all.
 */
object AppViewModelFactory {
    val settings = viewModelFactory {
        initializer { SettingsViewModel(container().preferences) }
    }

    val home = viewModelFactory {
        initializer { HomeViewModel(container().chats, container().preferences) }
    }

    val history = viewModelFactory {
        initializer { HistoryViewModel(container().chats) }
    }

    val chat = viewModelFactory {
        initializer {
            ChatViewModel(
                savedStateHandle = createSavedStateHandle(),
                chats = container().chats,
                preferences = container().preferences,
                attachments = container().attachments,
            )
        }
    }
}
