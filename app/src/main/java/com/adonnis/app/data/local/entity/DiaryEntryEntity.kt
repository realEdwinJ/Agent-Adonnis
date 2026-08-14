package com.adonnis.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores a single diary/journal entry for a day, including
 * the free-form content, extracted goals, and future events.
 */
@Entity(
    tableName = "diary_entries",
    indices = [Index(value = ["date"], unique = true)]
)
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** ISO date string, e.g. "2026-07-30" */
    @ColumnInfo(name = "date")
    val date: String,

    /** Full diary content — the raw conversation or user's written entry */
    @ColumnInfo(name = "content")
    val content: String,

    /** Short preview/snippet shown in diary list */
    @ColumnInfo(name = "snippet")
    val snippet: String = "",

    /** JSON array of goals mentioned, e.g. ["Study math", "Run 3x"] */
    @ColumnInfo(name = "goals_json")
    val goalsJson: String? = null,

    /** JSON array of future events extracted, e.g. [{"date":"2026-08-05","event":"Math test"}] */
    @ColumnInfo(name = "future_events_json")
    val futureEventsJson: String? = null,

    /** Mood emoji, e.g. "😊", "😐", "😢" */
    @ColumnInfo(name = "mood_emoji")
    val moodEmoji: String? = null,

    /** Sentiment score -1.0 to 1.0 (extracted by AI) */
    @ColumnInfo(name = "sentiment")
    val sentiment: Float? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
