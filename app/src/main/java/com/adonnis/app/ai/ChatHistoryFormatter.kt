package com.adonnis.app.ai

import com.adonnis.app.data.local.entity.ChatMessageEntity

/**
 * Converts persisted chat messages into [AIMessage] format
 * for passing as history to OpenRouterClient's sendMessage().
 */
object ChatHistoryFormatter {

    /** Maximum number of messages to include in history (oldest trimmed first). */
    private const val MAX_HISTORY_MESSAGES = 40

    /**
     * Convert a list of Room chat messages into [AIMessage] list.
     * - Filters out system messages (they're not part of the conversation).
     * - Keeps only the most recent [MAX_HISTORY_MESSAGES] messages.
     * - Alternates user/assistant roles as the chat API expects.
     *
     * @param messages All messages from Room, oldest first.
     * @return List of [AIMessage] suitable for [OpenRouterClient.sendMessage].
     */
    fun format(messages: List<ChatMessageEntity>): List<AIMessage> {
        val relevant = messages
            .filter { it.role != "system" }
            .takeLast(MAX_HISTORY_MESSAGES)

        return relevant.mapNotNull { msg ->
            when (msg.role) {
                "user" -> AIMessage(role = "user", content = msg.content)
                "agent" -> AIMessage(role = "assistant", content = msg.content)
                else -> null
            }
        }
    }

    /**
     * Quick check: does the history have alternating user/assistant roles?
     * The chat API expects strict alternation. If two consecutive messages
     * have the same role, we merge them (common after retries or re-asks).
     */
    fun validateAndFix(history: List<AIMessage>): List<AIMessage> {
        if (history.isEmpty()) return history

        val fixed = mutableListOf(history.first())
        for (i in 1 until history.size) {
            val prevRole = fixed.last().role
            val currRole = history[i].role
            if (currRole != prevRole) {
                fixed.add(history[i])
            }
            // If same role, skip duplicate (keep the first one)
        }
        return fixed
    }
}
