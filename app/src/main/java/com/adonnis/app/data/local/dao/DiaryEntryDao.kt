package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryEntryDao {

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT * FROM diary_entries ORDER BY date DESC")
    suspend fun getAllEntriesOnce(): List<DiaryEntryEntity>

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun getEntryByDate(date: String): DiaryEntryEntity?

    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    fun getEntryByDateFlow(date: String): Flow<DiaryEntryEntity?>

    @Query("SELECT * FROM diary_entries ORDER BY date DESC LIMIT 1")
    suspend fun getLatestEntry(): DiaryEntryEntity?

    /** Get entries within a date range (for weekly insights) */
    @Query("SELECT * FROM diary_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
    suspend fun getEntriesInRange(startDate: String, endDate: String): List<DiaryEntryEntity>

    /** Get entries that have mood data */
    @Query("SELECT * FROM diary_entries WHERE mood_emoji IS NOT NULL ORDER BY date DESC")
    fun getEntriesWithMood(): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntryEntity)

    @Update
    suspend fun update(entry: DiaryEntryEntity)

    @Delete
    suspend fun delete(entry: DiaryEntryEntity)

    @Query("DELETE FROM diary_entries")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun count(): Int
}
