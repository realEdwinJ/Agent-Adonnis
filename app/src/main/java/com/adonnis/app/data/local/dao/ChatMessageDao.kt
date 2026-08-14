package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    /** Flow of all messages ordered by timestamp (oldest first) */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    /** One-shot query of all messages (for AI chat history export) */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessagesOnce(): List<ChatMessageEntity>

    /** Last N messages for pagination, oldest first */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC LIMIT :limit")
    fun getRecentMessages(limit: Int = 50): Flow<List<ChatMessageEntity>>

    /** Get messages after a certain ID (for incremental loading) */
    @Query("SELECT * FROM chat_messages WHERE id > :afterId ORDER BY timestamp ASC")
    suspend fun getMessagesAfter(afterId: Long): List<ChatMessageEntity>

    /** Insert a message and return its generated ID */
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    /** Insert multiple messages at once */
    @Insert
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    /** Delete a specific message */
    @Delete
    suspend fun delete(message: ChatMessageEntity)

    /** Delete all messages (clear chat) */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()

    /** Count total messages */
    @Query("SELECT COUNT(*) FROM chat_messages")
    suspend fun count(): Int

    /** Get the most recent message (for welcome message check) */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMessage(): ChatMessageEntity?
}
