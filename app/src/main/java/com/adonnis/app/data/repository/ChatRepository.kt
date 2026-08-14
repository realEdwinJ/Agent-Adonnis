package com.adonnis.app.data.repository

import com.adonnis.app.data.local.dao.ChatMessageDao
import com.adonnis.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for chat message history operations.
 */
class ChatRepository(
    private val chatMessageDao: ChatMessageDao
) {

    /** Flow of all messages, oldest first */
    fun getAllMessages(): Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    /** One-shot query of all messages (for AI chat history) */
    suspend fun getAllMessagesOnce(): List<ChatMessageEntity> =
        chatMessageDao.getAllMessagesOnce()

    /** Flow of the most recent N messages */
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getRecentMessages(limit)

    /** Get messages after a given ID (for pagination / incremental load) */
    suspend fun getMessagesAfter(afterId: Long): List<ChatMessageEntity> =
        chatMessageDao.getMessagesAfter(afterId)

    /** Save a new message. Returns the generated ID. */
    suspend fun saveMessage(message: ChatMessageEntity): Long =
        chatMessageDao.insert(message)

    /** Save a user text message (convenience) */
    suspend fun saveUserMessage(content: String): Long =
        chatMessageDao.insert(
            ChatMessageEntity(role = "user", content = content, messageType = "text")
        )

    /** Save an agent text message (convenience) */
    suspend fun saveAgentMessage(content: String): Long =
        chatMessageDao.insert(
            ChatMessageEntity(role = "agent", content = content, messageType = "text")
        )

    /** Save a system message (context, errors, etc.) */
    suspend fun saveSystemMessage(content: String): Long =
        chatMessageDao.insert(
            ChatMessageEntity(role = "system", content = content, messageType = "system")
        )

    /** Delete all messages (clear chat) */
    suspend fun deleteAll() = chatMessageDao.deleteAll()

    /** Check if there are any messages */
    suspend fun count(): Int = chatMessageDao.count()

    /** Get the most recent message */
    suspend fun getLatestMessage(): ChatMessageEntity? = chatMessageDao.getLatestMessage()
}
