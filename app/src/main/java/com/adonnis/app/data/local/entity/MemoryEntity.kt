package com.adonnis.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single "learned" fact about the user, extracted automatically from
 * diary entries and chat conversations. Accumulated over time and injected
 * into AI prompts so the assistant plans better as it learns.
 *
 * @param category One of: "goal", "event", "preference", "fact", "deadline"
 * @param content  The learned fact, e.g. "User has a Chemistry test on 2026-08-12"
 * @param source   Where it came from: "diary" or "chat"
 */
@Entity(
    tableName = "memory_items",
    indices = [Index(value = ["created_at"])]
)
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "category")
    val category: String = "fact",

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "source")
    val source: String = "chat",

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
