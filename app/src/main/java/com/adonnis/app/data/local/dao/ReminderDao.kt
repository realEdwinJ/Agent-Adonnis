package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY date_time ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders ORDER BY date_time ASC")
    suspend fun getAllRemindersOnce(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE is_completed = 0 ORDER BY date_time ASC")
    fun getActiveReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE is_completed = 0 AND date_time BETWEEN :start AND :end ORDER BY date_time ASC")
    suspend fun getRemindersInRange(start: Long, end: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query("UPDATE reminders SET is_completed = 1 WHERE id = :id")
    suspend fun markCompleted(id: Long)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("DELETE FROM reminders")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM reminders WHERE is_completed = 0")
    suspend fun countActive(): Int
}
