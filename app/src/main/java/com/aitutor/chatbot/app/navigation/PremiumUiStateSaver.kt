package com.aitutor.chatbot.app.navigation

import androidx.compose.runtime.saveable.Saver
import com.aitutor.chatbot.app.domain.model.ProPlan
import com.aitutor.chatbot.app.ui.premium.PremiumUiState

/**
 * Keeps the chosen plan across a configuration change.
 *
 * Only the selection is saved — the prices come from Play on every open and must not be restored
 * from a stale bundle, or a rotation could show yesterday's price next to today's purchase. The plan
 * is stored by **name** rather than ordinal, for the same reason every other enum here is.
 */
val PremiumUiStateSaver: Saver<PremiumUiState, String> = Saver(
    save = { it.selected.name },
    restore = { name ->
        PremiumUiState(
            selected = ProPlan.entries.firstOrNull { it.name == name }
                ?: ProPlan.entries.first { it.bestValue }
        )
    },
)
