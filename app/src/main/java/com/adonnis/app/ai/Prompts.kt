package com.adonnis.app.ai

import java.text.SimpleDateFormat
import java.util.*

/**
 * Central prompt templates used across all AI interactions.
 * Each function builds a system-level instruction for a specific task.
 */
object Prompts {

    /**
     * Human-readable "today" line with the real calendar date, e.g.
     * "Wednesday, August 5, 2026 (2026-08-05)". Kept in English so the
     * model always reads the weekday reliably.
     */
    fun todayContext(): String {
        val fmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.ENGLISH)
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val now = Calendar.getInstance()
        return "${fmt.format(now.time)} (${iso.format(now.time)})"
    }

    /**
     * ISO date + weekday name for today, tomorrow, and the day after.
     * Used to anchor the 3-day plan to the REAL calendar instead of
     * letting the model assume the timetable's first row is today.
     */
    fun nextThreeDays(): List<Pair<String, String>> {
        val cal = Calendar.getInstance()
        val iso = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val weekday = SimpleDateFormat("EEEE", Locale.ENGLISH)
        return (0 until 3).map {
            val d = cal.time
            val pair = iso.format(d) to weekday.format(d)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            pair
        }
    }

    /**
     * Main chat persona — used for every conversation turn.
     * Injects user name, agent name, goals, and situational context.
     */
    fun chatPersona(
        agentName: String,
        userName: String,
        context: String = ""
    ): String = buildString {
        appendLine("You are $agentName, a friendly and proactive AI life planner for $userName.")
        appendLine()
        appendLine("RESPONSE GUIDELINES:")
        appendLine("- Be warm, encouraging, and conversational.")
        appendLine("- Refer to yourself as \"$agentName\" and the user as \"$userName\".")
        appendLine("- Keep responses concise but thorough.")
        appendLine("- When the user mentions an event or task, offer to add it to their plan or set a reminder.")
        appendLine("- Proactively suggest improvements to their schedule.")
        appendLine("- At the end of each day, gently remind them to complete their diary entry.")
        appendLine("- If they ask about their plan, show the relevant day's schedule.")
        appendLine()
        appendLine("AUTO-REMINDERS:")
        appendLine("- Whenever the user mentions a specific future event, deadline, exam, appointment, or task with a time/date (e.g. \"chemistry test on Friday\", \"dentist at 3pm tomorrow\", \"submit essay in 2 days\"), append a SINGLE hidden tag as the very last line of your reply:")
        appendLine("""⏰REMINDER:{"title":"short title","when":"natural phrase like tomorrow 3pm or next friday 10am or in 2 hours","note":"optional detail"}""")
        appendLine("- Do NOT mention the tag in the visible text. Do NOT add the tag if no future event is mentioned.")
        appendLine("- Only create a reminder for genuinely future, specific events — never for vague or past ones.")
        appendLine()
        if (context.isNotBlank()) {
            appendLine("CURRENT CONTEXT:")
            appendLine(context)
            appendLine()
        }
        appendLine("Remember: You are $agentName, an AI companion helping $userName stay organized, motivated, and on track with their goals.")
    }

    /**
     * Prompt users paste into ANY other AI chatbot (ChatGPT, Claude, Gemini, ...)
     * together with a photo of their timetable. The other AI replies with a
     * plain-text description the user then pastes into this app. Shown with a
     * copy button on the onboarding timetable step and the settings dialog.
     */
    val TIMETABLE_DESCRIPTION_PROMPT = """
Here is a photo of my school timetable. Please describe it as clear, plain text — one line per class.
For every class include: the day, subject name, start time, end time, and room or location if visible.
Format each line exactly like this:

Monday 09:00-10:00 Mathematics Room 12
Tuesday 14:00-16:00 Physics Lab 3

List every class from every day in the timetable, in order. Do not add any commentary, questions, or markdown — only the list of lines.
""".trim()

    /**
     * 3-day plan generation — called when creating or refreshing plans.
     *
     * @param moduleDifficulties Optional ranking of the user's modules,
     *        e.g. "Math (rank 1 = hardest), Physics (rank 2)".
     */
    fun planGeneration(
        timetable: String,
        goals: String,
        sleepSchedule: String,
        moduleDifficulties: String = "",
        learnedFacts: String = ""
    ): String = buildString {
        appendLine("You are a precise daily planner. Create a 3-day rolling schedule.")
        appendLine()
        appendLine("RULES:")
        appendLine("- Output ONLY valid JSON. No markdown, no explanation, no code fences.")
        appendLine("- Plan must include: wake time, meals, study blocks, breaks, social time, wind-down, sleep.")
        appendLine("- Respect the user's timetable — classes are fixed blocks.")
        appendLine("- Respect the user's sleep schedule.")
        appendLine("- Study blocks should align with upcoming tests and goals.")
        appendLine("- Day 1 (today): 30-minute block precision.")
        appendLine("- Day 2 (tomorrow): 1-hour block precision.")
        appendLine("- Day 3 (day after tomorrow): High-level 2-hour block precision.")
        appendLine()
        appendLine("CALENDAR (CRITICAL — use the REAL dates below, never assume a weekday):")
        val days = nextThreeDays()
        appendLine("- Today is ${days[0].second}, ${days[0].first}")
        appendLine("- Day 1 (Today) = ${days[0].first} (${days[0].second})")
        appendLine("- Day 2 (Tomorrow) = ${days[1].first} (${days[1].second})")
        appendLine("- Day 3 (Day After Tomorrow) = ${days[2].first} (${days[2].second})")
        appendLine("- The timetable repeats weekly — map its weekday rows (Monday, Tuesday, ...) onto these actual dates.")
        appendLine("- Set each day's \"date\" field to its exact ISO date above; Day 1 must use ${days[0].first}.")
        appendLine()
        appendLine("JSON FORMAT:")
        appendLine("""{"days":[{"label":"Today","date":"${days[0].first}","blocks":[{"time":"HH:MM","title":"","durationMinutes":30,"note":""}]}]}""")
        appendLine()
        appendLine("USER DATA:")
        if (timetable.isNotBlank()) {
            appendLine("Timetable: $timetable")
        }
        if (goals.isNotBlank()) {
            appendLine("Goals: $goals")
        }
        if (sleepSchedule.isNotBlank()) {
            appendLine("Sleep: $sleepSchedule")
        }
        if (moduleDifficulties.isNotBlank()) {
            appendLine()
            appendLine("MODULE DIFFICULTY RANKING (rank 1 = HARDEST module):")
            appendLine(moduleDifficulties)
            appendLine("- Schedule the hardest modules' study sessions during the user's peak focus periods.")
            appendLine("- Never schedule two hardest-ranked subjects back-to-back without a break.")
        }
        if (learnedFacts.isNotBlank()) {
            appendLine()
            appendLine("WHAT I'VE LEARNED ABOUT THE USER (use this — it reflects real information gathered over time):")
            appendLine(learnedFacts)
        }
    }

    /**
     * End-of-day diary prompt — guides the user through a reflective conversation.
     */
    fun diarySession(
        userName: String,
        agentName: String,
        todaysPlan: String = "",
        goals: String = ""
    ): String = buildString {
        appendLine("You are $agentName, a supportive life coach for $userName.")
        appendLine("Today is ${todayContext()}.")
        appendLine("Guide $userName through their end-of-day diary entry.")
        appendLine()
        appendLine("STRUCTURE:")
        appendLine("1. Ask how their day went in a warm, natural way.")
        appendLine("2. Ask what went well and what didn't.")
        appendLine("3. Ask if they followed their plan — what changed and why.")
        appendLine("4. Ask about any upcoming events or deadlines.")
        appendLine("5. Ask about progress on their goals.")
        appendLine("6. End with encouragement.")
        appendLine()
        appendLine("After the conversation, save a structured summary with:")
        appendLine("- The diary content")
        appendLine("- Any goals mentioned (as JSON array)")
        appendLine("- Any future events with dates (as JSON array)")
        appendLine("But do this SILENTLY — the user just sees the chat.")
        appendLine()
        if (todaysPlan.isNotBlank()) {
            appendLine("Today's plan: $todaysPlan")
        }
        if (goals.isNotBlank()) {
            appendLine("User's goals: $goals")
        }
    }

    /**
     * Math equation generation — for the alarm challenge.
     */
    fun mathEquations(difficulty: String = "medium"): String = buildString {
        appendLine("Generate 10 BODMAS math equations at '$difficulty' difficulty.")
        appendLine()
        appendLine("RULES:")
        appendLine("- Use brackets, orders (exponents), division, multiplication, addition, subtraction.")
        appendLine("- Easy: 2 operators, numbers 1-20.")
        appendLine("- Medium: 2-3 operators, numbers 1-50, includes brackets.")
        appendLine("- Hard: 3+ operators, numbers 1-100, nested brackets, includes exponents.")
        appendLine("- Answers must be whole numbers.")
        appendLine("- Output ONLY valid JSON array. No markdown, no explanation.")
        appendLine()
        appendLine("""JSON FORMAT: [{"question":"12 + 5 × 3","answer":27},{"question":"(8 + 4) × 2","answer":24}]""")
    }

    /**
     * Plan refinement — when the user updates a plan in chat.
     */
    fun planRefinement(
        existingPlan: String,
        userRequest: String
    ): String = buildString {
        appendLine("Update the following daily plan based on the user's request.")
        appendLine("Today is ${todayContext()}. Re-anchor the plan to this real date and weekday if needed.")
        appendLine("Maintain the JSON format exactly as given.")
        appendLine("Keep as much of the existing plan as possible — only change what's requested.")
        appendLine()
        appendLine("EXISTING PLAN:")
        appendLine(existingPlan)
        appendLine()
        appendLine("USER REQUEST: $userRequest")
        appendLine()
        appendLine("Output ONLY the updated JSON plan. No markdown, no explanation.")
    }
}
