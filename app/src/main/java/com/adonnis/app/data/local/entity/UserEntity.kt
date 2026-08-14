package com.adonnis.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the user's profile information including API key, name,
 * assistant name, timetable, goals, and sleep preferences.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = false)
    val id: Int = 1, // Singleton — always ID 1

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "agent_name")
    val agentName: String = "Adonnis",

    @ColumnInfo(name = "api_key")
    val apiKey: String = "",

    @ColumnInfo(name = "timetable_raw")
    val timetableRaw: String? = null,

    /** JSON array of goal strings, e.g. ["Get an A in Math", "Learn guitar"] */
    @ColumnInfo(name = "goals_json")
    val goalsJson: String? = null,

    /** JSON array of subject/class names */
    @ColumnInfo(name = "subjects_json")
    val subjectsJson: String? = null,

    /**
     * JSON array of module difficulty ratings, e.g.
     * [{"name":"Advanced Calculus","rank":1},{"name":"English","rank":2}]
     * where rank 1 = HARDEST module, rank 2 = second hardest, etc.
     */
    @ColumnInfo(name = "module_difficulties_json")
    val moduleDifficultiesJson: String? = null,

    @ColumnInfo(name = "wake_up_time")
    val wakeUpTime: String? = null,

    @ColumnInfo(name = "bed_time")
    val bedTime: String? = null,

    @ColumnInfo(name = "sleep_hours_needed")
    val sleepHoursNeeded: Int? = null,

    @ColumnInfo(name = "morning_routine")
    val morningRoutine: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
