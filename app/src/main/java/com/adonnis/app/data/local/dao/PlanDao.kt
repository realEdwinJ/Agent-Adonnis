package com.adonnis.app.data.local.dao

import androidx.room.*
import com.adonnis.app.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    @Query("SELECT * FROM plans ORDER BY day_relative ASC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM plans WHERE day_relative = :day ORDER BY generated_at DESC LIMIT 1")
    suspend fun getPlanByDay(day: Int): PlanEntity?

    @Query("SELECT * FROM plans WHERE day_relative = :day ORDER BY generated_at DESC LIMIT 1")
    fun getPlanByDayFlow(day: Int): Flow<PlanEntity?>

    @Query("SELECT * FROM plans WHERE date = :date ORDER BY generated_at DESC LIMIT 1")
    suspend fun getPlanByDate(date: String): PlanEntity?

    @Query("SELECT * FROM plans ORDER BY day_relative ASC")
    suspend fun getAllPlansOnce(): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: PlanEntity)

    @Update
    suspend fun update(plan: PlanEntity)

    @Query("DELETE FROM plans WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM plans")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun count(): Int
}
