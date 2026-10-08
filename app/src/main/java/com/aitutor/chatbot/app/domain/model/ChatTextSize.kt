package com.aitutor.chatbot.app.domain.model

/**
 * How large the text of a conversation is drawn, as a multiple of the normal size.
 *
 * Four steps rather than a free slider: the menu has room for a pair of buttons, and a step is
 * something a person can find their way back to. It scales the messages only — the header, the
 * composer and the buttons keep their size, since those are laid out in fixed heights.
 */
enum class ChatTextSize(val scale: Float) {
    Small(0.88f),
    Default(1f),
    Large(1.15f),
    Largest(1.3f);

    /** One step down, or null at the smallest — which is what greys the "A−" button out. */
    val smaller: ChatTextSize? get() = entries.getOrNull(ordinal - 1)

    /** One step up, or null at the largest. */
    val larger: ChatTextSize? get() = entries.getOrNull(ordinal + 1)

    companion object {
        /** Stored by name, so an unreadable value falls back to the normal size. */
        fun fromNameOrDefault(name: String?): ChatTextSize =
            entries.firstOrNull { it.name == name } ?: Default
    }
}
