package com.adonnis.app.ui.diary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ai.AIMessage
import com.adonnis.app.ai.OpenRouterClient
import com.adonnis.app.ai.Prompts
import com.adonnis.app.ai.ResponseParser
import com.adonnis.app.data.local.entity.DiaryEntryEntity
import com.adonnis.app.data.local.entity.ReminderEntity
import com.adonnis.app.util.DateParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for the diary system.
 * Manages past entries, live diary sessions (AI-guided), goal tracking,
 * and weekly insight generation.
 */
class DiaryViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AdonnisApplication
    private val diaryRepository = app.diaryRepository
    private val planRepository = app.planRepository
    private val userRepository = app.userRepository
    private var aiClient: OpenRouterClient? = null

    /**
     * Returns an OpenRouter client built from the CURRENT stored API key
     * and model, recreating it when either changes (e.g. updated in Settings).
     * Fixes stale-client issues where diary calls used an old key.
     */
    private fun currentClient(): OpenRouterClient? {
        val apiKey = userRepository.getApiKey()
        if (apiKey.isBlank()) {
            aiClient = null
            return null
        }
        val model = app.preferencesManager.openRouterModel
        if (aiClient == null || aiClient?.apiKey != apiKey || aiClient?.model != model) {
            aiClient = OpenRouterClient(apiKey, model)
        }
        return aiClient
    }

    // ── Entry List ───────────────────────────────────────────────────

    val entries: StateFlow<List<DiaryEntryEntity>> = diaryRepository.getAllEntries()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Diary Session State ──────────────────────────────────────────

    private val _isInSession = MutableStateFlow(false)
    val isInSession: StateFlow<Boolean> = _isInSession.asStateFlow()

    private val _sessionMessages = MutableStateFlow<List<DiarySessionMessage>>(emptyList())
    val sessionMessages: StateFlow<List<DiarySessionMessage>> = _sessionMessages.asStateFlow()

    private val _isSessionLoading = MutableStateFlow(false)
    val isSessionLoading: StateFlow<Boolean> = _isSessionLoading.asStateFlow()

    // ── Goal Tracking ────────────────────────────────────────────────

    private val _allGoals = MutableStateFlow<List<String>>(emptyList())
    val allGoals: StateFlow<List<String>> = _allGoals.asStateFlow()

    // ── Weekly Insights ──────────────────────────────────────────────

    private val _weeklyInsight = MutableStateFlow<String?>(null)
    val weeklyInsight: StateFlow<String?> = _weeklyInsight.asStateFlow()

    private val _isGeneratingInsight = MutableStateFlow(false)
    val isGeneratingInsight: StateFlow<Boolean> = _isGeneratingInsight.asStateFlow()

    // ── Date helpers ─────────────────────────────────────────────────

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayDate: String get() = dateFormat.format(Date())

    val displayDateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

    // ── Init ─────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // Warm up the client (re-validated against current key on each call)
            currentClient()
            loadAllGoals()
        }
    }

    private suspend fun loadAllGoals() {
        val allEntries = diaryRepository.getAllEntriesOnce()
        val goals = mutableSetOf<String>()
        for (entry in allEntries) {
            if (!entry.goalsJson.isNullOrBlank()) {
                try {
                    val json = org.json.JSONArray(entry.goalsJson)
                    for (i in 0 until json.length()) {
                        goals.add(json.getString(i))
                    }
                } catch (_: Exception) { }
            }
        }
        _allGoals.value = goals.toList().sorted()
    }

    // ── Start Diary Session ──────────────────────────────────────────

    fun startNewEntry() {
        val client = currentClient()
        if (client == null) {
            _sessionMessages.value = listOf(
                DiarySessionMessage("assistant", "API key not configured. Please set it in Settings.")
            )
            return
        }

        // Check if entry already exists for today
        viewModelScope.launch {
            if (diaryRepository.hasTodayEntry(todayDate)) {
                _sessionMessages.value = listOf(
                    DiarySessionMessage("assistant", "You already have an entry for today! You can view it in your diary history.")
                )
                _isInSession.value = true
                return@launch
            }

            _isInSession.value = true
            _isSessionLoading.value = true
            _sessionMessages.value = emptyList()

            try {
                // Gather context
                val user = userRepository.getUser()
                val todaysPlan = planRepository.getPlanByDay(0)?.planJson ?: ""
                val goals = user?.goalsJson ?: ""

                val prompt = Prompts.diarySession(
                    userName = user?.name ?: "",
                    agentName = user?.agentName ?: "Adonnis",
                    todaysPlan = todaysPlan,
                    goals = goals
                )

                val result = client.generateContent(
                    systemInstruction = prompt,
                    prompt = "Let's start today's diary entry."
                )

                result.onSuccess { response ->
                    _sessionMessages.value = listOf(
                        DiarySessionMessage("assistant", response)
                    )
                }.onFailure { error ->
                    _sessionMessages.value = listOf(
                        DiarySessionMessage("assistant", "I'm having trouble starting your diary: ${error.message}")
                    )
                }
            } catch (e: Exception) {
                _sessionMessages.value = listOf(
                    DiarySessionMessage("assistant", "Sorry, something went wrong: ${e.message}")
                )
            } finally {
                _isSessionLoading.value = false
            }
        }
    }

    // ── Continue Session ─────────────────────────────────────────────

    fun continueSession(userResponse: String) {
        val client = currentClient() ?: return
        if (userResponse.isBlank()) return

        viewModelScope.launch {
            _isSessionLoading.value = true

            // Build history from current messages (BEFORE adding the new user message)
            val history = _sessionMessages.value
                .mapNotNull { msg ->
                    when (msg.role) {
                        "user" -> AIMessage(role = "user", content = msg.content)
                        "assistant" -> AIMessage(role = "assistant", content = msg.content)
                        else -> null
                    }
                }

            // Add user message to local state AFTER building history
            _sessionMessages.value = _sessionMessages.value + DiarySessionMessage("user", userResponse)

            try {
                val user = userRepository.getUser()
                val todaysPlan = planRepository.getPlanByDay(0)?.planJson ?: ""
                val goals = user?.goalsJson ?: ""

                val prompt = Prompts.diarySession(
                    userName = user?.name ?: "",
                    agentName = user?.agentName ?: "Adonnis",
                    todaysPlan = todaysPlan,
                    goals = goals
                )

                val result = client.sendMessage(
                    systemInstruction = prompt,
                    history = history,
                    userMessage = userResponse
                )

                result.onSuccess { response ->
                    // Check if the response contains a JSON summary (diary complete)
                    val finalMessages = _sessionMessages.value + DiarySessionMessage("assistant", response)
                    _sessionMessages.value = finalMessages

                    // Try to parse and save diary entry
                    val diaryData = ResponseParser.parseDiaryResponse(response)
                    diaryData.onSuccess { data ->
                        if (data.content.isNotBlank()) {
                            saveDiaryEntry(response, data)
                        }
                    }
                }.onFailure { error ->
                    _sessionMessages.value = _sessionMessages.value + DiarySessionMessage(
                        "assistant",
                        "Sorry, I lost my train of thought: ${error.message}"
                    )
                }
            } catch (e: Exception) {
                _sessionMessages.value = _sessionMessages.value + DiarySessionMessage(
                    "assistant", "Something went wrong: ${e.message}"
                )
            } finally {
                _isSessionLoading.value = false
            }
        }
    }

    /**
     * Save a diary entry — called both when the session ends naturally
     * and when we detect a structured summary in the AI's response.
     */
    private suspend fun saveDiaryEntry(rawContent: String, data: ResponseParser.DiaryData) {
        val goalsJson = if (data.goals.isNotEmpty()) {
            org.json.JSONArray(data.goals).toString()
        } else null

        val eventsJson = if (data.futureEvents.isNotEmpty()) {
            org.json.JSONArray(data.futureEvents.map {
                org.json.JSONObject().apply {
                    put("date", it.date)
                    put("event", it.event)
                }
            }).toString()
        } else null

        val entry = DiaryEntryEntity(
            date = todayDate,
            content = rawContent,
            snippet = "",
            goalsJson = goalsJson,
            futureEventsJson = eventsJson,
            moodEmoji = data.mood ?: "😐",
            sentiment = data.sentiment
        )

        diaryRepository.saveEntry(entry)
        loadAllGoals()

        // ── Learn from this entry: auto-create reminders + memories ──
        // Goals the user mentioned become long-term memories.
        for (goal in data.goals) {
            app.memoryRepository.remember("goal", goal, "diary")
        }
        // Future events become scheduled reminders (and memories).
        val remindersEnabled = app.preferencesManager.remindersEnabled
        for (event in data.futureEvents) {
            val triggerAt = DateParser.parse(event.date)
            if (remindersEnabled && triggerAt != null && triggerAt > System.currentTimeMillis()) {
                app.reminderRepository.createReminder(
                    ReminderEntity(
                        title = event.event,
                        description = "From your diary on $todayDate",
                        dateTime = triggerAt,
                        isAutoGenerated = true
                    )
                )
            }
            app.memoryRepository.remember(
                "event",
                "Upcoming: ${event.event} (${event.date})",
                "diary"
            )
        }
    }

    /** End the session manually (user taps "Save & Finish") */
    fun endSession() {
        // If there are messages, save whatever we have
        viewModelScope.launch {
            val msgs = _sessionMessages.value
            if (msgs.size >= 2) {
                val fullContent = msgs.joinToString("\n") { "${it.role}: ${it.content}" }
                val entry = DiaryEntryEntity(
                    date = todayDate,
                    content = fullContent,
                    snippet = fullContent.take(120).replace('\n', ' ')
                )
                diaryRepository.saveEntry(entry)
            }
            _isInSession.value = false
            _sessionMessages.value = emptyList()
        }
    }

    /** Cancel the session without saving */
    fun cancelSession() {
        _isInSession.value = false
        _sessionMessages.value = emptyList()
    }

    // ── Weekly Insights ──────────────────────────────────────────────

    fun generateWeeklyInsight() {
        val client = currentClient() ?: return

        viewModelScope.launch {
            _isGeneratingInsight.value = true

            try {
                val allEntries = diaryRepository.getAllEntriesOnce()
                if (allEntries.isEmpty()) {
                    _weeklyInsight.value = "Start writing diary entries to get weekly insights!"
                    return@launch
                }

                val recentEntries = allEntries.take(7) // Last 7 days
                val summary = recentEntries.joinToString("\n---\n") { entry ->
                    "Date: ${entry.date}\nMood: ${entry.moodEmoji ?: "N/A"}\nContent: ${entry.content.take(300)}"
                }

                val insightPrompt = buildString {
                    appendLine("Analyze these diary entries from the past week and provide insights.")
                    appendLine("Today is ${Prompts.todayContext()} — use the real date when referring to days.")
                    appendLine("Focus on: mood patterns, productivity trends, goal progress, and suggestions for next week.")
                    appendLine("Be concise and encouraging.")
                    appendLine()
                    appendLine("ENTRIES:")
                    appendLine(summary)
                }

                val result = client.generateContent(
                    systemInstruction = "You are a thoughtful life coach analyzing weekly diary entries.",
                    prompt = insightPrompt
                )

                result.onSuccess { response ->
                    _weeklyInsight.value = response
                }.onFailure { error ->
                    _weeklyInsight.value = "Could not generate insights: ${error.message}"
                }
            } catch (e: Exception) {
                _weeklyInsight.value = "Error: ${e.message}"
            } finally {
                _isGeneratingInsight.value = false
            }
        }
    }

    fun dismissInsight() { _weeklyInsight.value = null }

    // ── Entry Selection ──────────────────────────────────────────────

    private val _selectedEntryId = MutableStateFlow<Long?>(null)
    val selectedEntry: StateFlow<DiaryEntryEntity?> = combine(
        _selectedEntryId, entries
    ) { id, list -> list.find { it.id == id } }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    fun selectEntry(id: Long) { _selectedEntryId.value = id }
    fun clearSelection() { _selectedEntryId.value = null }

    // ── Format Helpers ───────────────────────────────────────────────

    fun formatDate(dateStr: String): String {
        return try {
            val date = dateFormat.parse(dateStr)
            if (date != null) displayDateFormat.format(date) else dateStr
        } catch (_: Exception) { dateStr }
    }
}

// ── UI Models ───────────────────────────────────────────────────────────

data class DiarySessionMessage(
    val role: String, // "user" or "assistant"
    val content: String
)
