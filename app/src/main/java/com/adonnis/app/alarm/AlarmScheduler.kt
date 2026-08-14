package com.adonnis.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.adonnis.app.data.local.entity.AlarmEntity
import org.json.JSONArray
import java.util.Calendar

/**
 * Shared scheduling logic for wake-up alarms. Used by [AlarmRepository]
 * (on create/update/toggle/delete), [BootReceiver] (after reboot), and
 * [ChallengeState] (snooze). Exact-alarm aware: falls back to an inexact
 * [AlarmManager.set] when the app lacks the exact-alarm permission so the
 * alarm still fires (just possibly a few minutes late) instead of crashing.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /** Schedule the next occurrence of this alarm (per its days-of-week). */
    fun schedule(alarm: AlarmEntity) {
        if (!alarm.isEnabled) {
            cancel(alarm)
            return
        }
        val alarmManager = alarmManager ?: return

        val pendingIntent = pendingIntent(alarm)
        val triggerAt = nextTrigger(alarm) ?: return

        // Cancel any existing alarm first so re-scheduling doesn't stack.
        alarmManager.cancel(pendingIntent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }

    /** Cancel a scheduled alarm. */
    fun cancel(alarm: AlarmEntity) {
        val alarmManager = alarmManager ?: return
        val pendingIntent = pendingIntent(alarm)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /** Cancel a scheduled alarm by its ID. */
    fun cancelById(alarmId: Long) {
        val alarmManager = alarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    /**
     * Schedule a one-shot alarm (e.g. snooze) that fires [delayMillis] from
     * now for the given [alarmId]. Exact-alarm aware like [schedule].
     */
    fun scheduleSnooze(alarmId: Long, delayMillis: Long) {
        val alarmManager = alarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = System.currentTimeMillis() + delayMillis

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    /** Reschedule all enabled alarms (used after boot). */
    fun rescheduleAll(alarms: List<AlarmEntity>) {
        for (alarm in alarms) {
            if (alarm.isEnabled) schedule(alarm) else cancel(alarm)
        }
    }

    private fun pendingIntent(alarm: AlarmEntity): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.id)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Compute the next trigger time (epoch millis) for this alarm based on
     * its days-of-week, or null if the days list is empty/unparseable.
     */
    private fun nextTrigger(alarm: AlarmEntity): Long? {
        val daysOfWeek = try {
            val arr = JSONArray(alarm.daysOfWeekJson)
            (0 until arr.length()).map { arr.getInt(it) }
        } catch (_: Exception) {
            return null
        }
        if (daysOfWeek.isEmpty()) return null

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, alarm.hour)
        calendar.set(Calendar.MINUTE, alarm.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val today = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0
        val now = System.currentTimeMillis()

        if (daysOfWeek.contains(today) && calendar.timeInMillis > now) {
            return calendar.timeInMillis
        }
        // Find the next valid day within the next 7 days.
        for (daysFromNow in 1..7) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
            if (daysOfWeek.contains(dayOfWeek)) {
                return calendar.timeInMillis
            }
        }
        return null
    }
}
