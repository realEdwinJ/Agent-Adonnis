package com.adonnis.app.ui.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ai.OpenRouterClient
import com.adonnis.app.data.local.entity.UserEntity
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * ViewModel for the multi-step onboarding flow.
 * Manages all form state, validation, API key testing, and persistence.
 */
class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AdonnisApplication
    val userRepository = app.userRepository

    // ── Step Navigation ──────────────────────────────────────────────

    var currentStep by mutableIntStateOf(0)
    val totalSteps = 5

    fun nextStep() {
        if (currentStep < totalSteps - 1) currentStep++
    }

    fun previousStep() {
        if (currentStep > 0) currentStep--
    }

    // ── Step 0: API Key (OpenRouter) ─────────────────────────────────

    var apiKey by mutableStateOf("")
    var apiKeyError by mutableStateOf<String?>(null)
    var isTestingKey by mutableStateOf(false)
    var apiKeyValid by mutableStateOf(false)

    fun testApiKey() {
        if (apiKey.isBlank()) {
            apiKeyError = "Please enter an API key"
            return
        }

        isTestingKey = true
        apiKeyError = null
        apiKeyValid = false

        viewModelScope.launch {
            try {
                val (success, errorDetail) = OpenRouterClient.validateKey(apiKey.trim())
                if (success) {
                    apiKeyValid = true
                    apiKeyError = null
                } else {
                    apiKeyError = errorDetail
                    apiKeyValid = false
                }
            } finally {
                isTestingKey = false
            }
        }
    }

    // ── Step 1: Names ────────────────────────────────────────────────

    var userName by mutableStateOf("")
    var userNameError by mutableStateOf<String?>(null)
    var agentName by mutableStateOf("Adonnis")
    var agentNameError by mutableStateOf<String?>(null)

    // ── Step 2: Timetable (paste AI-generated description) ───────────

    /** The plain-text description of the timetable, produced by another AI
     *  (user pastes it here). */
    var timetableText by mutableStateOf("")

    // ── Module Difficulty Ratings (1 = hardest) ──────────────────────

    data class ModuleDifficultyUi(
        val name: String = "",
        val rankText: String = "" // numeric; 1 = hardest
    )

    var moduleDifficulties by mutableStateOf<List<ModuleDifficultyUi>>(emptyList())

    fun addModuleDifficulty() {
        moduleDifficulties = moduleDifficulties + ModuleDifficultyUi()
    }

    fun removeModuleDifficulty(index: Int) {
        moduleDifficulties = moduleDifficulties.filterIndexed { i, _ -> i != index }
    }

    fun updateModuleDifficultyName(index: Int, name: String) {
        moduleDifficulties = moduleDifficulties.toMutableList().also {
            it[index] = it[index].copy(name = name)
        }
    }

    fun updateModuleDifficultyRank(index: Int, rankText: String) {
        // Only allow digits
        val filtered = rankText.filter { it.isDigit() }.take(3)
        moduleDifficulties = moduleDifficulties.toMutableList().also {
            it[index] = it[index].copy(rankText = filtered)
        }
    }

    /** Build the JSON for storage: [{"name":"Math","rank":1},...] sorted by rank.
     *  Rows without a name are dropped; blank rank falls back to list position. */
    fun getModuleDifficultiesJson(): String {
        val entries = moduleDifficulties.mapIndexed { index, item ->
            val name = item.name.trim()
            if (name.isBlank()) null
            else JSONObject().apply {
                put("name", name)
                put("rank", item.rankText.toIntOrNull() ?: (index + 1))
            }
        }.filterNotNull().sortedBy { it.optInt("rank", 999) }
        return JSONArray(entries).toString()
    }

    // ── Step 3: Goals & Subjects & Sleep ─────────────────────────────

    var goal1 by mutableStateOf("")
    var goal2 by mutableStateOf("")
    var goal3 by mutableStateOf("")
    var subjects by mutableStateOf("")
    var wakeUpTime by mutableStateOf("")
    var bedTime by mutableStateOf("")
    var sleepHours by mutableStateOf("")
    var morningRoutine by mutableStateOf("")

    // ── Step Validation ──────────────────────────────────────────────

    fun isStepValid(): Boolean = when (currentStep) {
        0 -> apiKeyValid
        1 -> userName.isNotBlank() && agentName.isNotBlank()
        2 -> timetableText.isNotBlank()
        3 -> goal1.isNotBlank() || goal2.isNotBlank() || goal3.isNotBlank()
        4 -> true // Confirmation — always valid
        else -> false
    }

    fun stepHasContent(): Boolean = when (currentStep) {
        0 -> apiKey.isNotBlank()
        1 -> userName.isNotBlank()
        2 -> timetableText.isNotBlank()
        3 -> goal1.isNotBlank() || goal2.isNotBlank() || goal3.isNotBlank()
        else -> true
    }

    fun stepError(): String? = when (currentStep) {
        0 -> if (!apiKeyValid && apiKey.isNotBlank()) "Test your API key first" else null
        1 -> when {
            userName.isBlank() -> "Enter your name"
            agentName.isBlank() -> "Name your assistant"
            else -> null
        }
        2 -> if (timetableText.isBlank()) "Paste your timetable description below" else null
        3 -> if (goal1.isBlank() && goal2.isBlank() && goal3.isBlank()) "Set at least one goal" else null
        else -> null
    }

    // ── Summary Data ─────────────────────────────────────────────────

    fun getGoalsList(): List<String> = listOfNotNull(
        goal1.takeIf { it.isNotBlank() },
        goal2.takeIf { it.isNotBlank() },
        goal3.takeIf { it.isNotBlank() }
    )

    // ── Save & Complete ──────────────────────────────────────────────

    fun saveAndComplete() {
        viewModelScope.launch {
            val goalsJson = JSONArray().apply {
                getGoalsList().forEach { put(it) }
            }.toString()

            val subjectsJson = JSONArray().apply {
                subjects.split(",").map { it.trim() }
                    .filter { it.isNotBlank() }
                    .forEach { put(it) }
            }.toString()

            val user = UserEntity(
                id = 1,
                name = userName.trim(),
                agentName = agentName.trim(),
                apiKey = apiKey.trim(),
                timetableRaw = timetableText.trim(),
                goalsJson = goalsJson,
                subjectsJson = subjectsJson,
                moduleDifficultiesJson = getModuleDifficultiesJson(),
                wakeUpTime = wakeUpTime.takeIf { it.isNotBlank() },
                bedTime = bedTime.takeIf { it.isNotBlank() },
                sleepHoursNeeded = sleepHours.toIntOrNull(),
                morningRoutine = morningRoutine.takeIf { it.isNotBlank() },
                updatedAt = System.currentTimeMillis()
            )

            userRepository.saveUser(user)
            userRepository.completeOnboarding()
        }
    }
}
