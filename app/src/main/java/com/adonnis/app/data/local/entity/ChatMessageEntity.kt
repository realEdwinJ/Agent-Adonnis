package com.adonnis.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persisted chat message. One row per message in the conversation history.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["timestamp"])]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** "user", "agent", or "system" */
    @ColumnInfo(name = "role")
    val role: String,

    /** Message body text */
    @ColumnInfo(name = "content")
    val content: String,

    /** "text", "image", or "system" */
    @ColumnInfo(name = "message_type")
    val messageType: String = "text",

    /** Epoch millis */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    /** Image URI if messageType == "image" */
    @ColumnInfo(name = "image_uri")
    val imageUri: String? = null
)
