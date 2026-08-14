package com.adonnis.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules active alarms and reminders after device reboot.
 * Delegates to [AlarmScheduler] and the reminder scheduler so there is
 * a single source of truth for scheduling logic.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as com.adonnis.app.AdonnisApplication

        kotlinx.coroutines.runBlocking {
            // Reschedule wake-up alarms
            app.alarmRepository.rescheduleAll()

            // Reschedule active reminders
            app.reminderRepository.rescheduleAll()
        }
    }
}
