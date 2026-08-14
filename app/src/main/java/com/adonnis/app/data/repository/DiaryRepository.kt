package com.adonnis.app.data.repository

import com.adonnis.app.data.local.dao.DiaryEntryDao
import com.adonnis.app.data.local.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * Repository for diary/journal entries.
 */
class DiaryRepository(
    private val diaryEntryDao: DiaryEntryDao
) {

    /** Flow of all diary entries, newest first */
    fun getAllEntries(): Flow<List<DiaryEntryEntity>> = diaryEntryDao.getAllEntries()

    /** Get all entries once */
    suspend fun getAllEntriesOnce(): List<DiaryEntryEntity> = diaryEntryDao.getAllEntriesOnce()

    /** Get entry for a specific date */
    suspend fun getEntryByDate(date: String): DiaryEntryEntity? = diaryEntryDao.getEntryByDate(date)

    /** Get entry for a specific date as Flow */
    fun getEntryByDateFlow(date: String): Flow<DiaryEntryEntity?> = diaryEntryDao.getEntryByDateFlow(date)

    /** Get the most recent diary entry */
    suspend fun getLatestEntry(): DiaryEntryEntity? = diaryEntryDao.getLatestEntry()

    /** Get entries within a date range (for weekly insights) */
    suspend fun getEntriesInRange(startDate: String, endDate: String): List<DiaryEntryEntity> =
        diaryEntryDao.getEntriesInRange(startDate, endDate)

    /** Flow of entries that have mood data (for mood tracking) */
    fun getEntriesWithMood(): Flow<List<DiaryEntryEntity>> = diaryEntryDao.getEntriesWithMood()

    /** Get this week's entries (past 7 days) */
    suspend fun getThisWeeksEntries(): List<DiaryEntryEntity> {
        val cal = Calendar.getInstance()
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = dateFormat.format(cal.time)
        return diaryEntryDao.getEntriesInRange(startDate, endDate)
    }

    /** Save a new diary entry, auto-generating snippet if not provided */
    suspend fun saveEntry(entry: DiaryEntryEntity) {
        val snippet = if (entry.snippet.isBlank()) {
            entry.content.take(120).replace('\n', ' ') + if (entry.content.length > 120) "..." else ""
        } else {
            entry.snippet
        }
        diaryEntryDao.insert(entry.copy(snippet = snippet))
    }

    /** Update an existing diary entry, auto-generating snippet if blank */
    suspend fun updateEntry(entry: DiaryEntryEntity) {
        val snippet = if (entry.snippet.isBlank()) {
            entry.content.take(120).replace('\n', ' ') + if (entry.content.length > 120) "..." else ""
        } else {
            entry.snippet
        }
        diaryEntryDao.update(entry.copy(snippet = snippet))
    }

    /** Check if a diary entry exists for today */
    suspend fun hasTodayEntry(date: String): Boolean {
        return diaryEntryDao.getEntryByDate(date) != null
    }

    /** Delete all diary entries */
    suspend fun deleteAll() {
        diaryEntryDao.deleteAll()
    }

    /** Count total entries */
    suspend fun count(): Int = diaryEntryDao.count()
}
