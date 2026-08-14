package com.adonnis.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ai.OpenRouterClient
import com.adonnis.app.data.local.entity.ReminderEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel for the Settings screen — manages all configurable preferences,
 * dialogs, API key testing, AI model selection, and data reset.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AdonnisApplication
    private val userRepository = app.userRepository
    private val preferencesManager = app.preferencesManager

    // ── User Data ───────────────────────────────────────────────────

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _agentName = MutableStateFlow("Adonnis")
    val agentName: StateFlow<String> = _agentName.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _timetable = MutableStateFlow("")
    val timetable: StateFlow<String> = _timetable.asStateFlow()

    private val _goals = MutableStateFlow(listOf<String>())
    val goals: StateFlow<List<String>> = _goals.asStateFlow()

    private val _wakeUpTime = MutableStateFlow("")
    val wakeUpTime: StateFlow<String> = _wakeUpTime.asStateFlow()

    private val _bedTime = MutableStateFlow("")
    val bedTime: StateFlow<String> = _bedTime.asStateFlow()

    private val _sleepHours = MutableStateFlow("")
    val sleepHours: StateFlow<String> = _sleepHours.asStateFlow()

    private val _morningRoutine = MutableStateFlow("")
    val morningRoutine: StateFlow<String> = _morningRoutine.asStateFlow()

    // ── AI Model ────────────────────────────────────────────────────

    private val _model = MutableStateFlow(OpenRouterClient.DEFAULT_MODEL)
    val model: StateFlow<String> = _model.asStateFlow()

    // ── Module Difficulties (1 = hardest) ───────────────────────────

    data class ModuleDifficultyItem(val name: String = "", val rankText: String = "")

    private val _moduleDifficulties = MutableStateFlow<List<ModuleDifficultyItem>>(emptyList())
    val moduleDifficulties: StateFlow<List<ModuleDifficultyItem>> = _moduleDifficulties.asStateFlow()

    // ── Notification Toggles ─────────────────────────────────────────

    private val _remindersEnabled = MutableStateFlow(true)
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _diaryPromptsEnabled = MutableStateFlow(true)
    val diaryPromptsEnabled: StateFlow<Boolean> = _diaryPromptsEnabled.asStateFlow()

    private val _insightsEnabled = MutableStateFlow(true)
    val insightsEnabled: StateFlow<Boolean> = _insightsEnabled.asStateFlow()

    private val _alarmNotificationsEnabled = MutableStateFlow(true)
    val alarmNotificationsEnabled: StateFlow<Boolean> = _alarmNotificationsEnabled.asStateFlow()

    // ── Dark Mode ────────────────────────────────────────────────────

    private val _darkMode = MutableStateFlow(false)
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    // ── Dialog States ────────────────────────────────────────────────

    private val _showNameDialog = MutableStateFlow(false)
    val showNameDialog: StateFlow<Boolean> = _showNameDialog.asStateFlow()

    private val _showAgentDialog = MutableStateFlow(false)
    val showAgentDialog: StateFlow<Boolean> = _showAgentDialog.asStateFlow()

    private val _showApiKeyDialog = MutableStateFlow(false)
    val showApiKeyDialog: StateFlow<Boolean> = _showApiKeyDialog.asStateFlow()

    private val _showTimetableDialog = MutableStateFlow(false)
    val showTimetableDialog: StateFlow<Boolean> = _showTimetableDialog.asStateFlow()

    private val _showGoalsDialog = MutableStateFlow(false)
    val showGoalsDialog: StateFlow<Boolean> = _showGoalsDialog.asStateFlow()

    private val _showSleepDialog = MutableStateFlow(false)
    val showSleepDialog: StateFlow<Boolean> = _showSleepDialog.asStateFlow()

    private val _showModelDialog = MutableStateFlow(false)
    val showModelDialog: StateFlow<Boolean> = _showModelDialog.asStateFlow()

    private val _showModuleDifficultyDialog = MutableStateFlow(false)
    val showModuleDifficultyDialog: StateFlow<Boolean> = _showModuleDifficultyDialog.asStateFlow()

    private val _showResetDialog = MutableStateFlow(false)
    val showResetDialog: StateFlow<Boolean> = _showResetDialog.asStateFlow()

    // ── Reminders Management ────────────────────────────────────────

    private val _showRemindersDialog = MutableStateFlow(false)
    val showRemindersDialog: StateFlow<Boolean> = _showRemindersDialog.asStateFlow()

    private val _reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val reminders: StateFlow<List<ReminderEntity>> = _reminders.asStateFlow()

    /** Open the reminders manager, loading active (future) reminders. */
    fun showRemindersDialog() {
        viewModelScope.launch {
            val all = app.reminderRepository.getAllRemindersOnce()
            _reminders.value = all
                .filter { !it.isCompleted && it.dateTime > System.currentTimeMillis() }
                .sortedBy { it.dateTime }
            _showRemindersDialog.value = true
        }
    }

    fun hideRemindersDialog() { _showRemindersDialog.value = false }

    fun markReminderDone(id: Long) {
        viewModelScope.launch {
            app.reminderRepository.markCompleted(id)
            _reminders.value = _reminders.value.filter { it.id != id }
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch {
            app.reminderRepository.deleteReminder(reminder)
            _reminders.value = _reminders.value.filter { it.id != reminder.id }
        }
    }

    /** Format a reminder's trigger time for display. */
    fun formatReminderTime(dateTime: Long): String =
        com.adonnis.app.util.DateParser.describe(dateTime)

    // ── API Key Testing ──────────────────────────────────────────────

    private val _isTestingKey = MutableStateFlow(false)
    val isTestingKey: StateFlow<Boolean> = _isTestingKey.asStateFlow()

    private val _apiKeyTestResult = MutableStateFlow<ApiKeyTestResult?>(null)
    val apiKeyTestResult: StateFlow<ApiKeyTestResult?> = _apiKeyTestResult.asStateFlow()

    data class ApiKeyTestResult(val success: Boolean, val message: String)

    // ── Event for navigation ─────────────────────────────────────────

    private val _navigateToOnboarding = MutableStateFlow(false)
    val navigateToOnboarding: StateFlow<Boolean> = _navigateToOnboarding.asStateFlow()

    // ── Init ─────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            loadAllSettings()
        }
    }

    private suspend fun loadAllSettings() {
        _userName.value = userRepository.getUserName()
        _agentName.value = userRepository.getAgentName()
        _apiKey.value = userRepository.getApiKey()
        _model.value = preferencesManager.openRouterModel
        _darkMode.value = preferencesManager.darkMode
        _remindersEnabled.value = preferencesManager.remindersEnabled
        _diaryPromptsEnabled.value = preferencesManager.diaryPromptsEnabled
        _insightsEnabled.value = preferencesManager.insightsEnabled
        _alarmNotificationsEnabled.value = preferencesManager.alarmNotificationsEnabled

        val user = userRepository.getUser()
        if (user != null) {
            _timetable.value = user.timetableRaw ?: ""
            _wakeUpTime.value = user.wakeUpTime ?: ""
            _bedTime.value = user.bedTime ?: ""
            _sleepHours.value = (user.sleepHoursNeeded?.toString()) ?: ""
            _morningRoutine.value = user.morningRoutine ?: ""

            // Parse goals
            if (!user.goalsJson.isNullOrBlank()) {
                try {
                    val json = JSONArray(user.goalsJson)
                    val list = mutableListOf<String>()
                    for (i in 0 until json.length()) {
                        list.add(json.getString(i))
                    }
                    _goals.value = list
                } catch (_: Exception) {
                    _goals.value = emptyList()
                }
            }

            // Parse module difficulties
            _moduleDifficulties.value = parseModuleDifficulties(user.moduleDifficultiesJson)
        }
    }

    private fun parseModuleDifficulties(json: String?): List<ModuleDifficultyItem> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val items = mutableListOf<ModuleDifficultyItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                items.add(
                    ModuleDifficultyItem(
                        name = obj.optString("name", ""),
                        rankText = obj.optInt("rank", 0).toString()
                    )
                )
            }
            items.sortedBy { it.rankText.toIntOrNull() ?: 999 }
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── Dialog Controls ──────────────────────────────────────────────

    fun showNameDialog() { _showNameDialog.value = true }
    fun hideNameDialog() { _showNameDialog.value = false }

    fun showAgentDialog() { _showAgentDialog.value = true }
    fun hideAgentDialog() { _showAgentDialog.value = false }

    fun showApiKeyDialog() { _showApiKeyDialog.value = true }
    fun hideApiKeyDialog() { _showApiKeyDialog.value = false }

    fun showTimetableDialog() { _showTimetableDialog.value = true }
    fun hideTimetableDialog() { _showTimetableDialog.value = false }

    fun showGoalsDialog() { _showGoalsDialog.value = true }
    fun hideGoalsDialog() { _showGoalsDialog.value = false }

    fun showSleepDialog() { _showSleepDialog.value = true }
    fun hideSleepDialog() { _showSleepDialog.value = false }

    fun showModelDialog() { _showModelDialog.value = true }
    fun hideModelDialog() { _showModelDialog.value = false }

    fun showModuleDifficultyDialog() { _showModuleDifficultyDialog.value = true }
    fun hideModuleDifficultyDialog() { _showModuleDifficultyDialog.value = false }

    fun showResetDialog() { _showResetDialog.value = true }
    fun hideResetDialog() { _showResetDialog.value = false }

    // ── Save Functions ───────────────────────────────────────────────

    fun saveName(newName: String) {
        viewModelScope.launch {
            userRepository.saveName(newName)
            _userName.value = newName
            _showNameDialog.value = false
        }
    }

    fun saveAgentName(newName: String) {
        viewModelScope.launch {
            userRepository.saveAgentName(newName)
            _agentName.value = newName
            _showAgentDialog.value = false
        }
    }

    fun saveTimetable(newTimetable: String) {
        viewModelScope.launch {
            userRepository.saveTimetable(newTimetable)
            _timetable.value = newTimetable
            _showTimetableDialog.value = false
        }
    }

    fun saveGoals(newGoals: List<String>) {
        viewModelScope.launch {
            val json = JSONArray().apply {
                newGoals.forEach { put(it) }
            }.toString()
            userRepository.saveGoals(json)
            _goals.value = newGoals
            _showGoalsDialog.value = false
        }
    }

    fun saveSleepPreferences(
        wakeUp: String,
        bed: String,
        hours: String,
        routine: String
    ) {
        viewModelScope.launch {
            userRepository.saveSleepPreferences(
                wakeUpTime = wakeUp,
                bedTime = bed,
                sleepHours = hours.toIntOrNull(),
                morningRoutine = routine.takeIf { it.isNotBlank() }
            )
            _wakeUpTime.value = wakeUp
            _bedTime.value = bed
            _sleepHours.value = hours
            _morningRoutine.value = routine
            _showSleepDialog.value = false
        }
    }

    // ── AI Model Management ──────────────────────────────────────────

    fun saveModel(newModel: String) {
        viewModelScope.launch {
            preferencesManager.openRouterModel = newModel.trim()
            _model.value = newModel.trim()
            _showModelDialog.value = false
        }
    }

    // ── Module Difficulty Management ─────────────────────────────────

    fun addModuleDifficulty() {
        _moduleDifficulties.value = _moduleDifficulties.value + ModuleDifficultyItem()
    }

    fun removeModuleDifficulty(index: Int) {
        _moduleDifficulties.value = _moduleDifficulties.value.filterIndexed { i, _ -> i != index }
    }

    fun updateModuleDifficulty(index: Int, name: String, rankText: String) {
        val filtered = rankText.filter { it.isDigit() }.take(3)
        _moduleDifficulties.value = _moduleDifficulties.value.toMutableList().also {
            it[index] = it[index].copy(name = name, rankText = filtered)
        }
    }

    fun saveModuleDifficulties() {
        viewModelScope.launch {
            val entries = _moduleDifficulties.value.mapIndexed { index, item ->
                val name = item.name.trim()
                if (name.isBlank()) null
                else JSONObject().apply {
                    put("name", name)
                    put("rank", item.rankText.toIntOrNull() ?: (index + 1))
                }
            }.filterNotNull().sortedBy { it.optInt("rank", 999) }
            val json = JSONArray(entries).toString()
            userRepository.saveModuleDifficulties(json)
            _moduleDifficulties.value = entries.map {
                ModuleDifficultyItem(name = it.optString("name", ""), rankText = it.optInt("rank", 0).toString())
            }
            _showModuleDifficultyDialog.value = false
        }
    }

    // ── API Key Management ───────────────────────────────────────────

    private var pendingApiKey: String = ""

    fun setPendingApiKey(key: String) {
        pendingApiKey = key
        _apiKeyTestResult.value = null
    }

    fun testApiKey() {
        if (pendingApiKey.isBlank()) {
            _apiKeyTestResult.value = ApiKeyTestResult(false, "Please enter an API key")
            return
        }

        _isTestingKey.value = true
        _apiKeyTestResult.value = null

        viewModelScope.launch {
            try {
                val (success, message) = OpenRouterClient.validateKey(pendingApiKey.trim())
                if (success) {
                    _apiKeyTestResult.value = ApiKeyTestResult(true, "API key is valid! ✓")
                } else {
                    _apiKeyTestResult.value = ApiKeyTestResult(false, message)
                }
            } finally {
                _isTestingKey.value = false
            }
        }
    }

    fun saveApiKey() {
        if (apiKeyTestResult.value?.success != true) return

        viewModelScope.launch {
            userRepository.saveApiKey(pendingApiKey.trim())
            _apiKey.value = pendingApiKey
            _showApiKeyDialog.value = false
            _apiKeyTestResult.value = null
        }
    }

    // ── Notification Toggles ─────────────────────────────────────────

    fun setRemindersEnabled(enabled: Boolean) {
        preferencesManager.remindersEnabled = enabled
        _remindersEnabled.value = enabled
    }

    fun setDiaryPromptsEnabled(enabled: Boolean) {
        preferencesManager.diaryPromptsEnabled = enabled
        _diaryPromptsEnabled.value = enabled
    }

    fun setInsightsEnabled(enabled: Boolean) {
        preferencesManager.insightsEnabled = enabled
        _insightsEnabled.value = enabled
    }

    fun setAlarmNotificationsEnabled(enabled: Boolean) {
        preferencesManager.alarmNotificationsEnabled = enabled
        _alarmNotificationsEnabled.value = enabled
    }

    // ── Dark Mode ────────────────────────────────────────────────────

    fun toggleDarkMode() {
        val newValue = !_darkMode.value
        preferencesManager.darkMode = newValue
        _darkMode.value = newValue
    }

    // ── Reset All Data ───────────────────────────────────────────────

    fun resetAllData() {
        viewModelScope.launch {
            // Clear everything the reset dialog promises — including the
            // AI's learned memories, reminders, and alarms.
            userRepository.deleteAll()
            app.memoryRepository.deleteAll()
            app.reminderRepository.deleteAll()
            app.alarmRepository.deleteAll()
            app.planRepository.deleteAll()
            app.diaryRepository.deleteAll()
            app.chatRepository.deleteAll()
            _showResetDialog.value = false
            _navigateToOnboarding.value = true
        }
    }
}
