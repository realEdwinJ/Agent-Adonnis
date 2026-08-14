package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Query("SELECT * FROM users WHERE id = 1")
    suspend fun getUser(): UserEntity?

    @Query("SELECT * FROM users WHERE id = 1")
    fun getUserFlow(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: UserEntity)

    @Query("UPDATE users SET name = :name, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateName(name: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET agent_name = :agentName, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateAgentName(agentName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET api_key = :apiKey, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateApiKey(apiKey: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET timetable_raw = :timetable, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateTimetable(timetable: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET goals_json = :goalsJson, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateGoals(goalsJson: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE users SET module_difficulties_json = :json, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateModuleDifficulties(json: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE users SET 
            wake_up_time = :wakeUpTime, 
            bed_time = :bedTime, 
            sleep_hours_needed = :sleepHours,
            morning_routine = :morningRoutine,
            updated_at = :updatedAt 
        WHERE id = 1
    """)
    suspend fun updateSleepPreferences(
        wakeUpTime: String,
        bedTime: String,
        sleepHours: Int?,
        morningRoutine: String?,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
