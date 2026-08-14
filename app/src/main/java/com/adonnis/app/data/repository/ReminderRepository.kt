package com.adonnis.app.data.repository

import android.content.Context
import com.adonnis.app.data.local.dao.ReminderDao
import com.adonnis.app.data.local.entity.ReminderEntity
import com.adonnis.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/**
 * Repository for reminders — both user-created and AI-auto-generated.
 * Automatically schedules/cancels AlarmManager alarms when reminders change.
 */
class ReminderRepository(
    private val reminderDao: ReminderDao,
    private val context: Context? = null
) {
    private val scheduler by lazy { context?.let { ReminderScheduler(it) } }

    /** Flow of all reminders, ordered by date */
    fun getAllReminders(): Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    /** Get all reminders once */
    suspend fun getAllRemindersOnce(): List<ReminderEntity> = reminderDao.getAllRemindersOnce()

    /** Flow of only active (non-completed) reminders */
    fun getActiveReminders(): Flow<List<ReminderEntity>> = reminderDao.getActiveReminders()

    /** Get reminders within a time range (for scheduling) */
    suspend fun getRemindersInRange(start: Long, end: Long): List<ReminderEntity> =
        reminderDao.getRemindersInRange(start, end)

    /** Get a single reminder by ID */
    suspend fun getReminderById(id: Long): ReminderEntity? = reminderDao.getReminderById(id)

    /** Create a new reminder. Auto-schedules the alarm. Returns the generated ID. */
    suspend fun createReminder(reminder: ReminderEntity): Long {
        val id = reminderDao.insert(reminder)
        val saved = reminder.copy(id = id)
        scheduler?.schedule(saved)
        return id
    }

    /** Update an existing reminder. Re-schedules the alarm. */
    suspend fun updateReminder(reminder: ReminderEntity) {
        reminderDao.update(reminder)
        if (reminder.isCompleted) {
            scheduler?.cancel(reminder)
        } else {
            scheduler?.schedule(reminder)
        }
    }

    /** Mark a reminder as completed. Cancels the scheduled alarm. */
    suspend fun markCompleted(id: Long) {
        reminderDao.markCompleted(id)
        scheduler?.cancelById(id)
    }

    /** Delete a reminder. Cancels the scheduled alarm. */
    suspend fun deleteReminder(reminder: ReminderEntity) {
        scheduler?.cancel(reminder)
        reminderDao.delete(reminder)
    }

    /** Delete all reminders. Cancels all scheduled alarms. */
    suspend fun deleteAll() {
        val all = reminderDao.getAllRemindersOnce()
        for (reminder in all) {
            scheduler?.cancel(reminder)
        }
        reminderDao.deleteAll()
    }

    /** Count active reminders */
    suspend fun countActive(): Int = reminderDao.countActive()

    /** Reschedule all active reminders (for use after boot) */
    suspend fun rescheduleAll() {
        val all = reminderDao.getAllRemindersOnce()
        scheduler?.rescheduleAll(all)
    }
}
