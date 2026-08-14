package com.adonnis.app.data.repository

import com.adonnis.app.alarm.AlarmScheduler
import com.adonnis.app.data.local.dao.AlarmDao
import com.adonnis.app.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for wake-up alarm configurations and math challenge data.
 * Every mutation also (re)schedules or cancels the underlying AlarmManager
 * alarm via [AlarmScheduler] — this is what actually makes alarms fire.
 */
class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val scheduler: AlarmScheduler? = null
) {

    /** Flow of all alarms */
    fun getAllAlarms(): Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()

    /** Get all alarms once */
    suspend fun getAllAlarmsOnce(): List<AlarmEntity> = alarmDao.getAllAlarmsOnce()

    /** Flow of only enabled alarms */
    fun getEnabledAlarms(): Flow<List<AlarmEntity>> = alarmDao.getEnabledAlarms()

    /** Get a single alarm by ID */
    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    /** Get a single alarm by ID as Flow */
    fun getAlarmByIdFlow(id: Long): Flow<AlarmEntity?> = alarmDao.getAlarmByIdFlow(id)

    /** Create a new alarm and schedule it. Returns the generated ID. */
    suspend fun createAlarm(alarm: AlarmEntity): Long {
        val id = alarmDao.insert(alarm)
        val saved = alarm.copy(id = id)
        scheduler?.schedule(saved)
        return id
    }

    /** Update an existing alarm and re-schedule it. */
    suspend fun updateAlarm(alarm: AlarmEntity) {
        alarmDao.update(alarm)
        scheduler?.schedule(alarm)
    }

    /** Enable or disable an alarm (re-schedules / cancels). */
    suspend fun setAlarmEnabled(id: Long, enabled: Boolean) {
        val existing = alarmDao.getAlarmById(id) ?: return
        val updated = existing.copy(isEnabled = enabled)
        alarmDao.setEnabled(id, enabled)
        if (enabled) scheduler?.schedule(updated) else scheduler?.cancel(updated)
    }

    /** Cache math questions for an alarm trigger */
    suspend fun cacheMathQuestions(alarmId: Long, questionsJson: String) =
        alarmDao.updateMathQuestions(alarmId, questionsJson)

    /** Delete an alarm and cancel its scheduled trigger. */
    suspend fun deleteAlarm(alarm: AlarmEntity) {
        scheduler?.cancel(alarm)
        alarmDao.delete(alarm)
    }

    /** Delete all alarms and cancel all scheduled triggers. */
    suspend fun deleteAll() {
        val all = alarmDao.getAllAlarmsOnce()
        for (alarm in all) scheduler?.cancel(alarm)
        alarmDao.deleteAll()
    }

    /** Re-schedule all enabled alarms (used after boot). */
    suspend fun rescheduleAll() {
        val all = alarmDao.getAllAlarmsOnce()
        scheduler?.rescheduleAll(all)
    }
}
