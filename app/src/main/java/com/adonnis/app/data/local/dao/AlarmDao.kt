package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    fun getAllAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour ASC, minute ASC")
    suspend fun getAllAlarmsOnce(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE is_enabled = 1 ORDER BY hour ASC, minute ASC")
    fun getEnabledAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getAlarmById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun getAlarmByIdFlow(id: Long): Flow<AlarmEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Query("UPDATE alarms SET is_enabled = :isEnabled WHERE id = :id")
    suspend fun setEnabled(id: Long, isEnabled: Boolean)

    @Query("UPDATE alarms SET math_questions_json = :questions WHERE id = :id")
    suspend fun updateMathQuestions(id: Long, questions: String)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("DELETE FROM alarms")
    suspend fun deleteAll()
}
