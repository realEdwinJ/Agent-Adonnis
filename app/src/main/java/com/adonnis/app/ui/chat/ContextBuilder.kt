package com.adonnis.app.ui.chat

import com.adonnis.app.data.repository.DiaryRepository
import com.adonnis.app.data.repository.MemoryRepository
import com.adonnis.app.data.repository.PlanRepository
import com.adonnis.app.data.repository.UserRepository

/**
 * Builds a system-level context string that is injected before every AI call.
 * Gathers user profile, timetable, current plans, diary, and goals
 * so the AI has full situational awareness.
 */
class ContextBuilder(
    private val userRepository: UserRepository,
    private val planRepository: PlanRepository,
    private val diaryRepository: DiaryRepository,
    private val memoryRepository: MemoryRepository? = null
) {

    /**
     * Build the full context string synchronously (fast local reads).
     */
    suspend fun buildContext(): String {
        val sb = StringBuilder()

        // ── Today (real calendar date — never assume a weekday) ──────
        val now = java.util.Calendar.getInstance()
        val dateFmt = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", java.util.Locale.ENGLISH)
        val isoFmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.ENGLISH)
        sb.appendLine("=== TODAY ===")
        sb.appendLine("Today is ${dateFmt.format(now.time)} (${isoFmt.format(now.time)}).")
        sb.appendLine("Use this real date and weekday for anything about plans, schedules, or deadlines.")
        sb.appendLine()

        // ── User Identity ────────────────────────────────────────────
        val user = userRepository.getUser()
        if (user != null) {
            sb.appendLine("=== USER PROFILE ===")
            sb.appendLine("Name: ${user.name}")
            sb.appendLine("Assistant Name: ${user.agentName}")
            sb.appendLine()

            // ── Timetable ────────────────────────────────────────────
            if (!user.timetableRaw.isNullOrBlank()) {
                sb.appendLine("=== TIMETABLE ===")
                sb.appendLine(user.timetableRaw)
                sb.appendLine()
            }

            // ── Goals ────────────────────────────────────────────────
            if (!user.goalsJson.isNullOrBlank()) {
                sb.appendLine("=== TOP GOALS ===")
                try {
                    val json = org.json.JSONArray(user.goalsJson)
                    for (i in 0 until json.length()) {
                        sb.appendLine("  ${i + 1}. ${json.getString(i)}")
                    }
                } catch (_: Exception) {
                    sb.appendLine(user.goalsJson)
                }
                sb.appendLine()
            }

            // ── Sleep ────────────────────────────────────────────────
            if (!user.wakeUpTime.isNullOrBlank() || !user.bedTime.isNullOrBlank()) {
                sb.appendLine("=== SLEEP SCHEDULE ===")
                if (!user.wakeUpTime.isNullOrBlank()) sb.appendLine("Wake up: ${user.wakeUpTime}")
                if (!user.bedTime.isNullOrBlank()) sb.appendLine("Bedtime: ${user.bedTime}")
                if (user.sleepHoursNeeded != null) sb.appendLine("Sleep needed: ${user.sleepHoursNeeded}h")
                sb.appendLine()
            }

            // ── Module Difficulties (1 = hardest) ────────────────────
            if (!user.moduleDifficultiesJson.isNullOrBlank()) {
                sb.appendLine("=== MODULE DIFFICULTY (1 = HARDEST) ===")
                try {
                    val json = org.json.JSONArray(user.moduleDifficultiesJson)
                    for (i in 0 until json.length()) {
                        val mod = json.getJSONObject(i)
                        sb.appendLine("  ${mod.optString("name", "?")}: rank ${mod.optInt("rank", 99)}")
                    }
                } catch (_: Exception) {
                    sb.appendLine(user.moduleDifficultiesJson)
                }
                sb.appendLine()
            }
        }

        // ── Learned Memories ─────────────────────────────────────────
        val memories = memoryRepository?.getRecentMemories(limit = 20).orEmpty()
        if (memories.isNotEmpty()) {
            val userName = user?.name ?: "the user"
            sb.appendLine("=== WHAT I'VE LEARNED ABOUT $userName (accumulated over time) ===")
            memories.forEach { memory ->
                sb.appendLine("  - ${memory.content}")
            }
            sb.appendLine()
        }

        // ── Current Plans ────────────────────────────────────────────
        val plans = planRepository.getAllPlansOnce()
        if (plans.isNotEmpty()) {
            sb.appendLine("=== CURRENT 3-DAY PLAN ===")
            for (plan in plans.sortedBy { it.dayRelative }) {
                val dayLabel = when (plan.dayRelative) {
                    0 -> "Today"
                    1 -> "Tomorrow"
                    2 -> "Day After Tomorrow"
                    else -> "Day ${plan.dayRelative}"
                }
                sb.appendLine("--- $dayLabel (${plan.date}) ---")
                sb.appendLine(plan.planJson)
                sb.appendLine()
            }
        }

        // ── Latest Diary ─────────────────────────────────────────────
        val latestDiary = diaryRepository.getLatestEntry()
        if (latestDiary != null) {
            sb.appendLine("=== LATEST DIARY ENTRY (${latestDiary.date}) ===")
            sb.appendLine(latestDiary.content.take(300))
            if (latestDiary.content.length > 300) sb.appendLine("...")
            sb.appendLine()

            if (!latestDiary.futureEventsJson.isNullOrBlank()) {
                sb.appendLine("=== UPCOMING EVENTS ===")
                try {
                    val events = org.json.JSONArray(latestDiary.futureEventsJson)
                    for (i in 0 until events.length()) {
                        val event = events.getJSONObject(i)
                        sb.appendLine("  - ${event.optString("date", "?")}: ${event.optString("event", "")}")
                    }
                } catch (_: Exception) {
                    sb.appendLine(latestDiary.futureEventsJson)
                }
                sb.appendLine()
            }

            if (!latestDiary.goalsJson.isNullOrBlank()) {
                sb.appendLine("=== RECENT GOALS MENTIONED ===")
                try {
                    val goals = org.json.JSONArray(latestDiary.goalsJson)
                    for (i in 0 until goals.length()) {
                        sb.appendLine("  - ${goals.getString(i)}")
                    }
                } catch (_: Exception) {
                    sb.appendLine(latestDiary.goalsJson)
                }
                sb.appendLine()
            }
        }

        return sb.toString()
    }
}
