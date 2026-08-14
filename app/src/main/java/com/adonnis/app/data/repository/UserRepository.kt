package com.adonnis.app.data.repository

import com.adonnis.app.data.local.dao.UserDao
import com.adonnis.app.data.local.entity.UserEntity
import com.adonnis.app.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow

/**
 * Repository for user data. Coordinates between Room (UserEntity)
 * and EncryptedSharedPreferences (API key for quick access).
 */
class UserRepository(
    private val userDao: UserDao,
    private val preferencesManager: PreferencesManager
) {

    /** Get user as a Flow for reactive observation */
    fun getUserFlow(): Flow<UserEntity?> = userDao.getUserFlow()

    /** Get user once */
    suspend fun getUser(): UserEntity? = userDao.getUser()

    /**
     * Make sure the singleton user row (id = 1) exists before any partial
     * UPDATE. Partial updates (updateName/updateTimetable/...) are silent
     * no-ops when the row is missing — e.g. after a destructive DB migration
     * wiped the table while onboarding stayed marked complete.
     */
    private suspend fun ensureUserRow() {
        if (userDao.getUser() == null) {
            userDao.insertOrUpdate(UserEntity(id = 1))
        }
    }

    /** Create or update the user profile */
    suspend fun saveUser(user: UserEntity) {
        userDao.insertOrUpdate(user)
        // Keep preferences in sync
        preferencesManager.userName = user.name
        preferencesManager.agentName = user.agentName
        preferencesManager.apiKey = user.apiKey
    }

    /** Quick name lookup from preferences (no DB query) */
    fun getUserName(): String = preferencesManager.userName

    /** Quick agent name lookup from preferences */
    fun getAgentName(): String = preferencesManager.agentName

    /** Quick API key lookup from encrypted prefs */
    fun getApiKey(): String = preferencesManager.apiKey

    /** Save API key to both encrypted prefs and Room */
    suspend fun saveApiKey(apiKey: String) {
        ensureUserRow()
        preferencesManager.apiKey = apiKey
        userDao.updateApiKey(apiKey)
    }

    /** Save user name to both prefs and Room */
    suspend fun saveName(name: String) {
        ensureUserRow()
        preferencesManager.userName = name
        userDao.updateName(name)
    }

    /** Save agent name to both prefs and Room */
    suspend fun saveAgentName(agentName: String) {
        ensureUserRow()
        preferencesManager.agentName = agentName
        userDao.updateAgentName(agentName)
    }

    /** Save timetable to Room */
    suspend fun saveTimetable(timetable: String) {
        ensureUserRow()
        userDao.updateTimetable(timetable)
    }

    /** Save goals as JSON string */
    suspend fun saveGoals(goalsJson: String) {
        ensureUserRow()
        userDao.updateGoals(goalsJson)
    }

    /** Save module difficulty ratings as JSON string */
    suspend fun saveModuleDifficulties(json: String) {
        ensureUserRow()
        userDao.updateModuleDifficulties(json)
    }

    /** Save sleep preferences */
    suspend fun saveSleepPreferences(
        wakeUpTime: String,
        bedTime: String,
        sleepHours: Int?,
        morningRoutine: String?
    ) {
        ensureUserRow()
        userDao.updateSleepPreferences(wakeUpTime, bedTime, sleepHours, morningRoutine)
    }

    /** Check if onboarding was completed */
    fun isOnboardingComplete(): Boolean = preferencesManager.onboardingComplete

    /** Mark onboarding as complete */
    suspend fun completeOnboarding() {
        preferencesManager.onboardingComplete = true
    }

    /** Reset all user data */
    suspend fun deleteAll() {
        userDao.deleteAll()
        preferencesManager.clearAll()
    }
}
