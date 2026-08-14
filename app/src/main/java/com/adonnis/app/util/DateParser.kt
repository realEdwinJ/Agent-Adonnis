package com.adonnis.app.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Parses natural-language date/time phrases into epoch millis.
 * Handles the phrases an AI is likely to use when it mentions a future
 * event: "tomorrow 3pm", "next friday 10:00", "in 2 hours", "2026-08-12",
 * "august 15", "tonight 8pm", etc. Falls back to null when unparseable.
 */
object DateParser {

    /**
     * Parse [text] into epoch millis relative to now.
     * Returns null if no date/time could be understood.
     */
    fun parse(text: String): Long? {
        val input = text.trim().lowercase(Locale.ENGLISH)
        if (input.isEmpty()) return null

        // ── 1. ISO dates: 2026-08-12, 2026-08-12 14:00 ─────────────
        parseIso(input)?.let { return it }

        // ── 2. "in N minutes/hours/days" ────────────────────────────
        parseRelative(input)?.let { return it }

        // ── 3. Relative days + optional time ─────────────────────────
        val calendar = Calendar.getInstance()
        var matchedDay = false

        when {
            input.contains("day after tomorrow") -> { calendar.add(Calendar.DAY_OF_YEAR, 2); matchedDay = true }
            input.contains("tomorrow") || input.contains("tmr") -> { calendar.add(Calendar.DAY_OF_YEAR, 1); matchedDay = true }
            input.contains("tonight") -> {
                calendar.add(Calendar.DAY_OF_YEAR, 0)
                applyTime(calendar, "20:00")
                return truncateToMinute(calendar)
            }
            input.contains("today") -> { matchedDay = true }
            input.contains("this week") -> { matchedDay = true } // keep today
            else -> {
                // Weekday names: "monday", "next monday", "fri"
                val weekday = findWeekday(input)
                if (weekday != null) {
                    val next = nextWeekday(weekday, allowToday = false)
                    calendar.timeInMillis = next
                    matchedDay = true
                }
            }
        }

        if (!matchedDay) return null

        // Optional time: "3pm", "15:30", "3:30 pm"
        val time = extractTime(input)
        if (time != null) {
            applyTime(calendar, time)
        } else {
            // Default to 09:00 if no time given but a day was specified
            applyTime(calendar, "09:00")
        }

        // If the computed time is already in the past, roll forward to the
        // next day for relative phrases ("tomorrow 9am" said at 3pm, "today"
        // with no time after 9am, "monday" when Monday's 9am already passed).
        // Absolute dates (ISO, "august 15") are left alone so the caller can
        // decide whether a past date is still meaningful.
        if (calendar.timeInMillis <= System.currentTimeMillis() &&
            (input.contains("tomorrow") || input.contains("today") ||
             input.contains("day after tomorrow") || findWeekday(input) != null)
        ) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        return truncateToMinute(calendar)
    }

    // ── ISO ──────────────────────────────────────────────────────────

    private fun parseIso(input: String): Long? {
        val isoDate = Regex("(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})").find(input)
        if (isoDate != null) {
            val (y, m, d) = isoDate.destructured
            val cal = Calendar.getInstance()
            cal.clear()
            cal.set(Calendar.YEAR, y.toInt())
            cal.set(Calendar.MONTH, m.toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, d.toInt())
            val time = extractTime(input)
            if (time != null) applyTime(cal, time) else applyTime(cal, "09:00")
            return truncateToMinute(cal)
        }
        // "august 15", "aug 15", "august 15, 2026"
        val monthDay = Regex("([a-z]{3,9})\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,?\\s+(\\d{4}))?", RegexOption.IGNORE_CASE).find(input)
        if (monthDay != null) {
            val monthName = monthDay.groupValues[1].lowercase(Locale.ENGLISH)
            val day = monthDay.groupValues[2].toIntOrNull() ?: return null
            val year = monthDay.groupValues[3].toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
            val month = monthNumber(monthName) ?: return null
            val cal = Calendar.getInstance()
            cal.clear()
            cal.set(year, month - 1, day)
            val time = extractTime(input)
            if (time != null) applyTime(cal, time) else applyTime(cal, "09:00")
            return truncateToMinute(cal)
        }
        return null
    }

    // ── Relative ─────────────────────────────────────────────────────

    private fun parseRelative(input: String): Long? {
        val match = Regex("in\\s+(\\d+)\\s*(minute|min|mins|hour|hr|hours|hrs|day|days|week|weeks)").find(input)
        if (match == null) return null
        val amount = match.groupValues[1].toIntOrNull() ?: return null
        val unit = match.groupValues[2]
        val millis = when {
            unit.startsWith("min") -> amount * 60_000L
            unit.startsWith("h") -> amount * 3_600_000L
            unit.startsWith("d") -> amount * 86_400_000L
            unit.startsWith("w") -> amount * 604_800_000L
            else -> return null
        }
        return System.currentTimeMillis() + millis
    }

    // ── Weekday ──────────────────────────────────────────────────────

    private val weekdayNames = mapOf(
        "sunday" to Calendar.SUNDAY, "sun" to Calendar.SUNDAY,
        "monday" to Calendar.MONDAY, "mon" to Calendar.MONDAY,
        "tuesday" to Calendar.TUESDAY, "tue" to Calendar.TUESDAY, "tues" to Calendar.TUESDAY,
        "wednesday" to Calendar.WEDNESDAY, "wed" to Calendar.WEDNESDAY,
        "thursday" to Calendar.THURSDAY, "thu" to Calendar.THURSDAY, "thur" to Calendar.THURSDAY, "thurs" to Calendar.THURSDAY,
        "friday" to Calendar.FRIDAY, "fri" to Calendar.FRIDAY,
        "saturday" to Calendar.SATURDAY, "sat" to Calendar.SATURDAY
    )

    private fun findWeekday(input: String): Int? {
        for ((name, value) in weekdayNames) {
            // Match "next monday", "on monday", "monday", "mon"
            if (Regex("(^|\\s)(next\\s+|on\\s+|this\\s+)?$name(\\s|$|,)").containsMatchIn(input)) {
                return value
            }
        }
        return null
    }

    private fun nextWeekday(target: Int, allowToday: Boolean): Long {
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_WEEK)
        var delta = (target - today + 7) % 7
        if (delta == 0 && !allowToday) delta = 7
        cal.add(Calendar.DAY_OF_YEAR, delta)
        return cal.timeInMillis
    }

    // ── Time extraction ──────────────────────────────────────────────

    private fun extractTime(input: String): String? {
        // "15:30", "3:30pm", "3 pm", "3pm", "12am"
        val hhmm = Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)?").find(input)
        if (hhmm != null) {
            val h = hhmm.groupValues[1].toInt()
            val m = hhmm.groupValues[2].toInt()
            val ampm = hhmm.groupValues[3]
            return normalizeHour(h, m, ampm)
        }
        val hh = Regex("(^|\\s)(\\d{1,2})\\s*(am|pm)").find(input)
        if (hh != null) {
            val h = hh.groupValues[2].toInt()
            val ampm = hh.groupValues[3]
            return normalizeHour(h, 0, ampm)
        }
        return null
    }

    private fun normalizeHour(hour: Int, minute: Int, ampm: String): String {
        var h = hour
        when (ampm) {
            "pm" -> if (h < 12) h += 12
            "am" -> if (h == 12) h = 0
        }
        return String.format("%02d:%02d", h, minute)
    }

    private fun applyTime(cal: Calendar, time: String) {
        val parts = time.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, m)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun truncateToMinute(cal: Calendar): Long {
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun monthNumber(name: String): Int? = when (name) {
        "jan", "january" -> 1
        "feb", "february" -> 2
        "mar", "march" -> 3
        "apr", "april" -> 4
        "may" -> 5
        "jun", "june" -> 6
        "jul", "july" -> 7
        "aug", "august" -> 8
        "sep", "sept", "september" -> 9
        "oct", "october" -> 10
        "nov", "november" -> 11
        "dec", "december" -> 12
        else -> null
    }

    /** Convenience: parse into a human-readable "EEE, MMM d · h:mm a" string, or the raw input. */
    fun describe(epochMillis: Long): String {
        val fmt = SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault())
        return fmt.format(Date(epochMillis))
    }
}
