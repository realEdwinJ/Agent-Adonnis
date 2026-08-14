package com.adonnis.app.alarm

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager

/**
 * Receives alarm broadcasts from AlarmManager.
 * Acquires wake lock, starts foreground service, and launches MathChallengeActivity.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, 0L)

        // Re-arm the NEXT occurrence so recurring wake-up alarms keep firing
        // daily/weekly. Alarms are scheduled one-shot; without this re-arm a
        // "Weekdays 6:30" alarm would fire exactly once and never again.
        try {
            val app = context.applicationContext as com.adonnis.app.AdonnisApplication
            kotlinx.coroutines.runBlocking {
                val alarm = app.alarmRepository.getAlarmById(alarmId)
                if (alarm != null && alarm.isEnabled) {
                    app.alarmScheduler.schedule(alarm)
                }
            }
        } catch (_: Exception) {
            // Re-arming is best-effort; the alarm still fires this time.
        }

        // Acquire wake lock to ensure CPU stays on
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Adonnis:AlarmReceiverWakeLock"
        )
        wakeLock.acquire(5000) // 5 second timeout, service will acquire its own

        // Start foreground service (persistent notification + wake lock)
        val serviceIntent = Intent(context, AlarmForegroundService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Also launch the activity directly (as fallback / for immediate display)
        val challengeIntent = Intent(context, MathChallengeActivity::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(challengeIntent)

        // Clean up wake lock after a short delay (service maintains its own)
        wakeLock.release()
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val ACTION_ALARM = "com.adonnis.app.ACTION_ALARM"
    }
}
