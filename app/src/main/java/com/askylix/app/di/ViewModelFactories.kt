package com.askylix.app.di

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.askylix.app.AITutorApplication
import com.askylix.app.ui.chat.ChatViewModel
import com.askylix.app.ui.history.HistoryViewModel
import com.askylix.app.ui.home.HomeViewModel
import com.askylix.app.ui.settings.SettingsViewModel
import com.askylix.app.ui.signin.AuthViewModel

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
        initializer {
            SettingsViewModel(
                preferences = container().preferences,
                account = container().account,
                auth = container().auth,
                messages = container().messages,
            )
        }
    }

    val auth = viewModelFactory {
        initializer {
            AuthViewModel(
                auth = container().auth,
                account = container().account,
                preferences = container().preferences,
            )
        }
    }

    val home = viewModelFactory {
        initializer {
            HomeViewModel(
                chats = container().chats,
                account = container().account,
                session = container().session,
                preferences = container().preferences,
                auth = container().auth,
            )
        }
    }

    val history = viewModelFactory {
        initializer {
            HistoryViewModel(
                chats = container().chats,
                session = container().session,
                auth = container().auth,
            )
        }
    }

    val chat = viewModelFactory {
        initializer {
            ChatViewModel(
                savedStateHandle = createSavedStateHandle(),
                chats = container().chats,
                auth = container().auth,
                preferences = container().preferences,
                attachments = container().attachments,
                downloader = container().downloader,
                voice = container().voice,
                speaker = container().speaker,
                messages = container().messages,
            )
        }
    }
}
