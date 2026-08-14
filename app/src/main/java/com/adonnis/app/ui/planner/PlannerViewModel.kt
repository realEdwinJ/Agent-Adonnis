package com.adonnis.app.ui.planner

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ai.OpenRouterApiException
import com.adonnis.app.ai.OpenRouterClient
import com.adonnis.app.ai.Prompts
import com.adonnis.app.ai.ResponseParser
import com.adonnis.app.data.local.entity.PlanEntity
import com.adonnis.app.data.local.entity.ReminderEntity
import com.adonnis.app.util.DateParser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for the 3-day rolling planner.
 * Handles plan generation via the AI, day selection, and block color coding.
 */
class PlannerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as AdonnisApplication
    private val planRepository = app.planRepository
    private val userRepository = app.userRepository

    /** AI client initialized lazily from stored API key + model */
    private var aiClient: OpenRouterClient? = null

    /**
     * Returns an OpenRouter client built from the CURRENT stored API key
     * and model, recreating it when either changes (e.g. updated in Settings).
     * Fixes stale-client issues where plan generation used an old key.
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

    // ── Day Selection ────────────────────────────────────────────────

    private val _selectedDay = MutableStateFlow(0)
    val selectedDay: StateFlow<Int> = _selectedDay.asStateFlow()

    fun selectDay(day: Int) { _selectedDay.value = day }

    // ── Plans from Room ──────────────────────────────────────────────

    val plans: StateFlow<List<PlanEntity>> = planRepository.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // ── Loading State ────────────────────────────────────────────────

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()

    // ── Current Day's Blocks (derived from plans + selectedDay) ──────

    private val _currentBlocks = MutableStateFlow<List<TimeBlockUi>>(emptyList())
    val currentBlocks: StateFlow<List<TimeBlockUi>> = _currentBlocks.asStateFlow()

    // ── Day Stats ────────────────────────────────────────────────────

    private val _dayStats = MutableStateFlow(DayStats())
    val dayStats: StateFlow<DayStats> = _dayStats.asStateFlow()

    // ── Detail Labels ────────────────────────────────────────────────

    val dayLabels = listOf("Today", "Tomorrow", "Day After Tomorrow")
    val detailLabels = listOf("Detailed", "Moderate", "Overview")

    // ── Init ─────────────────────────────────────────────────────────

    init {
        viewModelScope.launch {
            // Warm up the client (re-validated against current key on each call)
            currentClient()

            // Observe plans + selected day → compute current blocks & stats
            combine(plans, _selectedDay) { planList, day ->
                val plan = planList.find { it.dayRelative == day }
                if (plan != null) {
                    val blocks = parseBlocksFromPlan(plan)
                    _currentBlocks.value = blocks
                    _dayStats.value = computeStats(blocks)
                } else {
                    _currentBlocks.value = emptyList()
                    _dayStats.value = DayStats()
                }
            }.launchIn(viewModelScope)

            // Auto-generate plans if none exist, or if the "Today" plan is
            // stale (generated on a previous day — it would otherwise be
            // labeled "Today" while holding yesterday's date).
            if (planRepository.count() == 0 || isDayZeroPlanStale()) {
                generatePlans()
            }
        }
    }

    /**
     * True when the day-0 ("Today") plan's stored date isn't the real today.
     */
    private suspend fun isDayZeroPlanStale(): Boolean {
        val plan = planRepository.getPlanByDay(0) ?: return true
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Calendar.getInstance().time)
        return plan.date != today
    }

    // ── Generate Plans ───────────────────────────────────────────────

    fun generatePlans() {
        val client = currentClient()
        if (client == null) {
            _generationError.value = "API key not configured. Set it in Settings."
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationError.value = null

            try {
                // Gather user data for context
                val user = userRepository.getUser()
                val timetable = user?.timetableRaw ?: ""
                val goals = user?.goalsJson ?: ""
                val sleep = listOfNotNull(
                    user?.wakeUpTime?.let { "Wake: $it" },
                    user?.bedTime?.let { "Bed: $it" },
                    user?.sleepHoursNeeded?.let { "Need: ${it}h" }
                ).joinToString(", ")
                val moduleDifficulties = formatModuleDifficulties(user?.moduleDifficultiesJson)
                val learnedFacts = app.memoryRepository.getRecentMemories(limit = 15)
                    .joinToString("\n") { "- ${it.content}" }

                val prompt = Prompts.planGeneration(
                    timetable = timetable,
                    goals = goals,
                    sleepSchedule = sleep,
                    moduleDifficulties = moduleDifficulties,
                    learnedFacts = learnedFacts
                )

                // Retry with backoff like chat does — a single transient
                // provider error shouldn't kill the whole plan generation.
                val result = client.retryWithBackoff(maxRetries = 2) {
                    client.generateContent(
                        systemInstruction = "You are a precise daily planner. Output ONLY valid JSON.",
                        prompt = prompt,
                        jsonMode = true,
                        maxTokens = 8192 // 3 days of blocks needs room; 4096 was truncating
                    )
                }

                result.onSuccess { json ->
                    val parsed = ResponseParser.parsePlanResponse(json)
                    parsed.onSuccess { planResult ->
                        if (planResult.days.isEmpty()) {
                            _generationError.value = "The AI returned an empty plan. Tap retry."
                        } else {
                            saveParsedPlans(planResult)
                            schedulePlanReminders(planResult)
                            _generationError.value = null
                        }
                    }.onFailure { parseError ->
                        _generationError.value = "Could not parse the plan response. Tap retry. (${parseError.message?.take(80)})"
                    }
                }.onFailure { error ->
                    _generationError.value = friendlyError(error)
                }
            } catch (e: Exception) {
                _generationError.value = friendlyError(e)
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Auto-create reminders for today's and tomorrow's study/class/exam
     * blocks so the user actually gets notified about their schedule.
     * Deduplicates against existing reminders so regenerating a plan
     * never stacks duplicate alarms.
     */
    private suspend fun schedulePlanReminders(planResult: ResponseParser.PlanResult) {
        if (!app.preferencesManager.remindersEnabled) return
        val existing = app.reminderRepository.getAllRemindersOnce()
        val now = System.currentTimeMillis()

        for ((index, dayPlan) in planResult.days.withIndex()) {
            if (index > 1) break // today + tomorrow only
            if (dayPlan.date.isBlank()) continue

            for (block in dayPlan.blocks) {
                val lower = block.title.lowercase()
                val isStudy = listOf(
                    "study", "math", "english", "science", "history", "class",
                    "lecture", "homework", "assignment", "exam", "test", "revision"
                ).any { lower.contains(it) }
                if (!isStudy || block.time.isBlank()) continue

                // Build "YYYY-MM-DD HH:MM" and parse to epoch millis
                val dateTime = DateParser.parse("${dayPlan.date} ${block.time}") ?: continue
                if (dateTime <= now) continue

                // Dedup: skip if an identical auto-reminder already exists
                val alreadyScheduled = existing.any {
                    it.isAutoGenerated &&
                    it.sourcePlanDate == dayPlan.date &&
                    it.title.equals(block.title, ignoreCase = true) &&
                    kotlin.math.abs(it.dateTime - dateTime) < 60_000L
                }
                if (alreadyScheduled) continue

                app.reminderRepository.createReminder(
                    ReminderEntity(
                        title = block.title,
                        description = block.note.ifBlank { "From your 3-day plan" },
                        dateTime = dateTime,
                        isAutoGenerated = true,
                        sourcePlanDate = dayPlan.date
                    )
                )
            }
        }
    }

    /**
     * Convert the stored module-difficulty JSON into a readable ranked list.
     * rank 1 = hardest.
     */
    private fun formatModuleDifficulties(json: String?): String {
        if (json.isNullOrBlank()) return ""
        return try {
            val arr = org.json.JSONArray(json)
            val items = mutableListOf<Pair<String, Int>>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name", "")
                val rank = obj.optInt("rank", 99)
                if (name.isNotBlank()) items.add(name to rank)
            }
            items.sortedBy { it.second }.joinToString(", ") { "${it.first} (rank ${it.second})" }
        } catch (_: Exception) {
            json
        }
    }

    /** Map API/network errors to human-friendly messages. */
    private fun friendlyError(error: Throwable): String = when {
        error is OpenRouterApiException && error.statusCode in 401..403 ->
            "Your API key seems invalid. Update it in Settings."
        error is OpenRouterApiException && error.statusCode == 402 ->
            "No OpenRouter credits left. Top up at openrouter.ai."
        error is OpenRouterApiException && error.statusCode == 404 ->
            "The selected model doesn't exist. Pick a valid one in Settings."
        error is OpenRouterApiException && error.statusCode == 429 ->
            "Rate limited. Wait a moment and tap retry."
        error is OpenRouterApiException && error.statusCode in 500..599 ->
            "The AI provider is overloaded. Tap retry in a moment."
        error.message?.contains("API key not configured", ignoreCase = true) == true ->
            "Set your API key in Settings first."
        else -> error.message?.take(150) ?: "Something went wrong. Tap retry."
    }

    /**
     * Save the parsed plan results to Room as PlanEntity rows.
     */
    private suspend fun saveParsedPlans(planResult: ResponseParser.PlanResult) {
        planRepository.deleteAll()

        // Always use the REAL calendar dates (today, tomorrow, day after) —
        // never trust the AI's guess, which used to anchor plans to Monday
        // because the timetable's first row is Monday.
        val cal = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for ((index, dayPlan) in planResult.days.withIndex()) {
            if (index > 2) break // Only 3 days max

            val date = dateFormat.format(cal.time)

            val detailLevel = when (index) {
                0 -> "detailed"
                1 -> "moderate"
                2 -> "overview"
                else -> "moderate"
            }

            // Convert parsed blocks back to JSON for storage
            val planJson = blocksToJson(dayPlan.blocks)

            val entity = PlanEntity(
                date = date,
                dayRelative = index,
                planJson = planJson,
                detailLevel = detailLevel
            )
            planRepository.savePlan(entity)

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    // ── Block Parsing ────────────────────────────────────────────────

    private fun parseBlocksFromPlan(plan: PlanEntity): List<TimeBlockUi> {
        val parsed = ResponseParser.parsePlanResponse(plan.planJson)
        return parsed.getOrNull()?.days?.firstOrNull()?.blocks?.map { block ->
            TimeBlockUi(
                timeRange = formatTimeRange(block.time, block.durationMinutes),
                title = block.title,
                durationMinutes = block.durationMinutes,
                note = block.note.takeIf { it.isNotBlank() },
                color = categorizeBlockColor(block.title)
            )
        } ?: emptyList()
    }

    private fun formatTimeRange(startTime: String, durationMin: Int): String {
        if (startTime.isBlank()) return ""
        val parts = startTime.split(":")
        if (parts.size < 2) return startTime
        val startHour = parts[0].toIntOrNull() ?: return startTime
        val startMin = parts[1].toIntOrNull() ?: 0

        val endTotalMin = startHour * 60 + startMin + durationMin
        val endHour = endTotalMin / 60
        val endMin = endTotalMin % 60

        return String.format("%02d:%02d - %02d:%02d", startHour, startMin, endHour, endMin)
    }

    private fun blocksToJson(blocks: List<ResponseParser.PlanBlock>): String {
        val sb = StringBuilder("{\"days\":[{\"label\":\"\",\"date\":\"\",\"blocks\":[")
        blocks.forEachIndexed { i, block ->
            if (i > 0) sb.append(",")
            sb.append("""{"time":"${block.time}","title":"${escapeJson(block.title)}","durationMinutes":${block.durationMinutes},"note":"${escapeJson(block.note)}"}""")
        }
        sb.append("]}]}")
        return sb.toString()
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    // ── Color Coding ─────────────────────────────────────────────────

    /**
     * Categorize a block title into a color for the timeline.
     */
    private fun categorizeBlockColor(title: String): Color {
        val lower = title.lowercase()
        return when {
            lower.contains("wake") || lower.contains("morning") -> Color(0xFF4A90D9)  // Blue
            lower.contains("breakfast") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("meal") || lower.contains("eat") -> Color(0xFFE67E22)  // Orange
            lower.contains("study") || lower.contains("math") || lower.contains("english") || lower.contains("science") || lower.contains("history") || lower.contains("class") || lower.contains("lecture") || lower.contains("homework") || lower.contains("assignment") || lower.contains("exam") || lower.contains("test") || lower.contains("revision") -> Color(0xFF3498DB)  // Blue
            lower.contains("break") || lower.contains("rest") || lower.contains("free") || lower.contains("relax") -> Color(0xFF2ECC71)  // Green
            lower.contains("gym") || lower.contains("exercise") || lower.contains("run") || lower.contains("workout") || lower.contains("sport") || lower.contains("walk") || lower.contains("yoga") -> Color(0xFFE67E22)  // Orange
            lower.contains("social") || lower.contains("friends") || lower.contains("hang") || lower.contains("call") || lower.contains("family") -> Color(0xFF9B59B6)  // Purple
            lower.contains("sleep") || lower.contains("wind") || lower.contains("bed") || lower.contains("night") -> Color(0xFF8E44AD)  // Deep Purple
            lower.contains("read") || lower.contains("meditate") || lower.contains("journal") -> Color(0xFF1ABC9C)  // Teal
            lower.contains("travel") || lower.contains("commute") || lower.contains("drive") -> Color(0xFF95A5A6)  // Gray
            else -> Color.Unspecified
        }
    }

    // ── Statistics ───────────────────────────────────────────────────

    data class DayStats(
        val totalMinutes: Int = 0,
        val studyMinutes: Int = 0,
        val breakMinutes: Int = 0,
        val mealMinutes: Int = 0,
        val socialMinutes: Int = 0,
        val exerciseMinutes: Int = 0,
        val sleepMinutes: Int = 0
    )

    private fun computeStats(blocks: List<TimeBlockUi>): DayStats {
        var total = 0
        var study = 0
        var breaks = 0
        var meals = 0
        var social = 0
        var exercise = 0
        var sleep = 0

        for (block in blocks) {
            total += block.durationMinutes
            val lower = block.title.lowercase()
            when {
                lower.contains("study") || lower.contains("class") || lower.contains("lecture") ||
                        lower.contains("homework") || lower.contains("exam") || lower.contains("revision") -> study += block.durationMinutes
                lower.contains("break") || lower.contains("rest") || lower.contains("free") || lower.contains("relax") -> breaks += block.durationMinutes
                lower.contains("breakfast") || lower.contains("lunch") || lower.contains("dinner") || lower.contains("meal") || lower.contains("eat") -> meals += block.durationMinutes
                lower.contains("social") || lower.contains("friends") || lower.contains("hang") || lower.contains("family") -> social += block.durationMinutes
                lower.contains("gym") || lower.contains("exercise") || lower.contains("run") || lower.contains("workout") || lower.contains("sport") -> exercise += block.durationMinutes
                lower.contains("sleep") || lower.contains("wind") || lower.contains("bed") || lower.contains("night") -> sleep += block.durationMinutes
            }
        }

        return DayStats(
            totalMinutes = total,
            studyMinutes = study,
            breakMinutes = breaks,
            mealMinutes = meals,
            socialMinutes = social,
            exerciseMinutes = exercise,
            sleepMinutes = sleep
        )
    }

    // ── Format Helpers ────────────────────────────────────────────────

    fun formatHours(minutes: Int): String = when {
        minutes == 0 -> "—"
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }
}

// ── UI Data Models ──────────────────────────────────────────────────────

data class TimeBlockUi(
    val timeRange: String,
    val title: String,
    val durationMinutes: Int,
    val note: String? = null,
    val color: Color = Color.Unspecified
)
