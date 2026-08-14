package com.adonnis.app.data.repository

import com.adonnis.app.data.local.dao.PlanDao
import com.adonnis.app.data.local.entity.PlanEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

/**
 * Repository for the 3-day rolling plans.
 * Manages plan generation, retrieval, and daily shifts.
 */
class PlanRepository(
    private val planDao: PlanDao
) {

    /** Flow of all plans, ordered by day */
    fun getAllPlans(): Flow<List<PlanEntity>> = planDao.getAllPlans()

    /** Get a specific day's plan as Flow */
    fun getPlanByDayFlow(day: Int): Flow<PlanEntity?> = planDao.getPlanByDayFlow(day)

    /** Get a specific day's plan once */
    suspend fun getPlanByDay(day: Int): PlanEntity? = planDao.getPlanByDay(day)

    /** Get plan by date string once */
    suspend fun getPlanByDate(date: String): PlanEntity? = planDao.getPlanByDate(date)

    /** Get all plans once (non-reactive) */
    suspend fun getAllPlansOnce(): List<PlanEntity> = planDao.getAllPlansOnce()

    /** Save a new plan (replaces existing plan for that day) */
    suspend fun savePlan(plan: PlanEntity) {
        planDao.insert(plan)
    }

    /** Update an existing plan */
    suspend fun updatePlan(plan: PlanEntity) {
        planDao.update(plan)
    }

    /** Delete a plan by date */
    suspend fun deletePlanByDate(date: String) {
        planDao.deleteByDate(date)
    }

    /**
     * Roll plans forward by one day.
     * Clears all existing plans, re-inserts with shifted day values,
     * and sets today's date for the former tomorrow.
     */
    suspend fun shiftPlans() {
        val plans = planDao.getAllPlansOnce()
        planDao.deleteAll()

        for (plan in plans) {
            val newDay = plan.dayRelative - 1
            if (newDay >= 0) {
                planDao.insert(
                    plan.copy(
                        dayRelative = newDay,
                        date = getDateString(newDay),
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /** Delete all plans */
    suspend fun deleteAll() {
        planDao.deleteAll()
    }

    /** Count plans */
    suspend fun count(): Int = planDao.count()

    /** Get ISO date string for days from now (0 = today) */
    private fun getDateString(daysFromNow: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, daysFromNow)
        return String.format(
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }
}
