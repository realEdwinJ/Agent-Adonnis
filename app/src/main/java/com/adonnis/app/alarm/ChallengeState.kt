package com.adonnis.app.alarm

import android.app.Activity
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ai.MathEquationEngine
import com.adonnis.app.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Manages the math challenge alarm state machine.
 * Handles equation generation, answer validation, snooze logic,
 * completion celebration, and morning greeting in chat.
 */
class ChallengeState(val alarmId: Long, private val app: AdonnisApplication) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var aiClient = app.userRepository.getApiKey().takeIf { it.isNotBlank() }
        ?.let { com.adonnis.app.ai.OpenRouterClient(it, app.preferencesManager.openRouterModel) }

    // ── State ────────────────────────────────────────────────────────

    private val _equations = MutableStateFlow<List<MathEquationEngine.Equation>>(emptyList())
    val equations: StateFlow<List<MathEquationEngine.Equation>> = _equations

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _solvedCount = MutableStateFlow(0)
    val solvedCount: StateFlow<Int> = _solvedCount

    private val _totalEquations = MutableStateFlow(10)
    val totalEquations: StateFlow<Int> = _totalEquations

    private val _snoozeCount = MutableStateFlow(0)
    val snoozeCount: StateFlow<Int> = _snoozeCount

    private val _isComplete = MutableStateFlow(false)
    val isComplete: StateFlow<Boolean> = _isComplete

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Track wrong attempts per question for logging */
    private var wrongAttempts = 0

    init {
        loadEquations()
    }

    // ── Load Equations ───────────────────────────────────────────────

    private fun loadEquations() {
        scope.launch {
            _isLoading.value = true
            try {
                // Try to get difficulty from stored alarm
                val alarm = app.alarmRepository.getAlarmById(alarmId)
                val difficulty = alarm?.mathDifficulty ?: "medium"

                val eqs = MathEquationEngine.generate(difficulty, aiClient)
                _equations.value = eqs
                _totalEquations.value = eqs.size
            } catch (_: Exception) {
                // Fallback: use default equations
                _equations.value = MathEquationEngine.generate("medium")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── Submit Answer ─────────────────────────────────────────────────

    fun submitAnswer(answerText: String) {
        val answer = answerText.trim().toIntOrNull() ?: return
        val eqs = _equations.value
        val idx = _currentIndex.value

        if (idx >= eqs.size) return
        if (answer == eqs[idx].answer) {
            // Correct!
            val newSolved = _solvedCount.value + 1
            _solvedCount.value = newSolved

            if (newSolved >= _totalEquations.value) {
                _isComplete.value = true
            } else {
                _currentIndex.value = idx + 1
            }
        } else {
            wrongAttempts++
        }
    }

    // ── Snooze ───────────────────────────────────────────────────────

    fun snooze(activity: Activity) {
        val snoozed = _snoozeCount.value
        if (snoozed >= MAX_SNOOZES) return // No more snoozes allowed

        _snoozeCount.value = snoozed + 1

        // Re-fire the alarm 5 minutes from now via the shared scheduler,
        // which is exact-alarm-permission aware (avoids SecurityException
        // on Android 12+ when exact alarms aren't granted).
        app.alarmScheduler.scheduleSnooze(alarmId, SNOOZE_MS)

        activity.finish()
    }

    // ── Morning Greeting ─────────────────────────────────────────────

    fun onMorningGreeting() {
        scope.launch {
            try {
                val name = app.userRepository.getUserName()
                val agent = app.userRepository.getAgentName()
                val greeting = if (name.isNotBlank()) {
                    "Good morning, $name! 🌅 You solved ${_solvedCount.value} equations in " +
                            "${_totalEquations.value} attempts. Ready to tackle today? " +
                            "Check your plan or let's set some goals!"
                } else {
                    "Good morning! 🌅 You're awake and ready to go!"
                }

                app.chatRepository.saveMessage(
                    ChatMessageEntity(role = "agent", content = greeting)
                )
            } catch (_: Exception) {
                // Greeting is best-effort
            }
        }
    }

    // ── Reset / Cleanup ──────────────────────────────────────────────

    fun reset(newAlarmId: Long) {
        _equations.value = emptyList()
        _currentIndex.value = 0
        _solvedCount.value = 0
        _totalEquations.value = 10
        _snoozeCount.value = 0
        _isComplete.value = false
        wrongAttempts = 0
        loadEquations()
    }

    fun cleanup() {
        // Cancel any pending work
    }

    companion object {
        const val MAX_SNOOZES = 3
        const val SNOOZE_MS = 5 * 60 * 1000L
    }
}
