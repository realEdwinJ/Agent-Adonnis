package com.adonnis.app.ai

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses structured JSON responses from the AI into typed domain objects.
 * Uses safe parsing with try/catch fallbacks.
 */
object ResponseParser {

    // ── Plan Data ────────────────────────────────────────────────────

    data class PlanBlock(
        val time: String,
        val title: String,
        val durationMinutes: Int,
        val note: String = ""
    )

    data class DayPlan(
        val label: String,
        val date: String,
        val blocks: List<PlanBlock>
    )

    data class PlanResult(
        val days: List<DayPlan>
    )

    /**
     * Parse the AI's plan generation response into structured plan data.
     * Attempts JSON parsing first, falls back to regex extraction.
     */
    fun parsePlanResponse(rawResponse: String): Result<PlanResult> {
        // Try to extract JSON from the response (handles markdown-wrapped JSON)
        val jsonStr = extractJson(rawResponse) ?: return Result.failure(
            Exception("No valid JSON found in plan response")
        )

        return try {
            val root = JSONObject(jsonStr)
            val daysArray = root.getJSONArray("days")
            val days = mutableListOf<DayPlan>()

            for (i in 0 until daysArray.length()) {
                val dayObj = daysArray.getJSONObject(i)
                val blocksArray = dayObj.getJSONArray("blocks")
                val blocks = mutableListOf<PlanBlock>()

                for (j in 0 until blocksArray.length()) {
                    val block = blocksArray.getJSONObject(j)
                    blocks.add(
                        PlanBlock(
                            time = block.optString("time", ""),
                            title = block.optString("title", ""),
                            durationMinutes = block.optInt("durationMinutes", 60),
                            note = block.optString("note", "")
                        )
                    )
                }

                days.add(
                    DayPlan(
                        label = dayObj.optString("label", ""),
                        date = dayObj.optString("date", ""),
                        blocks = blocks
                    )
                )
            }

            Result.success(PlanResult(days))
        } catch (e: Exception) {
            // Fallback: try regex to extract time blocks
            Result.failure(e)
        }
    }

    // ── Diary Data ───────────────────────────────────────────────────

    data class DiaryData(
        val content: String,
        val goals: List<String> = emptyList(),
        val futureEvents: List<FutureEvent> = emptyList(),
        val mood: String? = null,
        val sentiment: Float? = null
    )

    data class FutureEvent(
        val date: String,
        val event: String
    )

    /**
     * Parse a diary response — the structured summary that the AI
     * generates silently after a diary conversation.
     */
    fun parseDiaryResponse(rawResponse: String): Result<DiaryData> {
        val jsonStr = extractJson(rawResponse)
            ?: return Result.success(
                DiaryData(content = rawResponse.take(500))
            )

        return try {
            val root = JSONObject(jsonStr)

            val goalsArray = root.optJSONArray("goals")
            val goals = mutableListOf<String>()
            if (goalsArray != null) {
                for (i in 0 until goalsArray.length()) {
                    goals.add(goalsArray.getString(i))
                }
            }

            val eventsArray = root.optJSONArray("futureEvents")
            val events = mutableListOf<FutureEvent>()
            if (eventsArray != null) {
                for (i in 0 until eventsArray.length()) {
                    val event = eventsArray.getJSONObject(i)
                    events.add(
                        FutureEvent(
                            date = event.optString("date", ""),
                            event = event.optString("event", "")
                        )
                    )
                }
            }

            Result.success(
                DiaryData(
                    content = rawResponse.take(1000),
                    goals = goals,
                    futureEvents = events,
                    mood = root.optString("mood", null).takeIf { it.isNotBlank() },
                    sentiment = root.optString("sentiment", null)?.toFloatOrNull()
                )
            )
        } catch (e: Exception) {
            // Fallback: return raw text as content
            Result.success(DiaryData(content = rawResponse.take(500)))
        }
    }

    // ── Math Equations ───────────────────────────────────────────────

    data class MathEquation(
        val question: String,
        val answer: Int
    )

    /**
     * Parse math equation JSON from the AI.
     */
    fun parseMathEquations(rawResponse: String): Result<List<MathEquation>> {
        val jsonStr = extractJson(rawResponse)
            ?: return Result.failure(Exception("No valid JSON found in math response"))

        return try {
            val array = JSONArray(jsonStr)
            val equations = mutableListOf<MathEquation>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                equations.add(
                    MathEquation(
                        question = obj.getString("question"),
                        answer = obj.getInt("answer")
                    )
                )
            }

            if (equations.isEmpty()) {
                Result.failure(Exception("No equations found in response"))
            } else {
                Result.success(equations)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Plan Refinement ──────────────────────────────────────────────

    /**
     * Parse a refined plan (same format as initial plan).
     */
    fun parseRefinedPlan(rawResponse: String): Result<PlanResult> =
        parsePlanResponse(rawResponse)

    // ── Helpers ──────────────────────────────────────────────────────

    /**
     * Extract a JSON object or array from a string that may contain
     * markdown code fences or surrounding text.
     */
    private fun extractJson(text: String): String? {
        // Try finding JSON within ```json ... ``` or ``` ... ``` blocks
        val fenceRegex = Regex("```(?:json)?\\s*([\\s\\S]*?)```")
        val fenceMatch = fenceRegex.find(text)
        if (fenceMatch != null) {
            return fenceMatch.groupValues[1].trim()
        }

        // Try parsing the entire text as JSON
        val trimmed = text.trim()
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return trimmed
        }

        // Try to find first { or [ and last } or ]
        val startBrace = trimmed.indexOfFirst { it == '{' || it == '[' }
        val endBrace = trimmed.indexOfLast { it == '}' || it == ']' }
        if (startBrace >= 0 && endBrace > startBrace) {
            return trimmed.substring(startBrace, endBrace + 1)
        }

        return null
    }
}
