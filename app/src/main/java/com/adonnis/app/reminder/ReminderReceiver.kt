package com.adonnis.app.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.MainActivity

/**
 * Receives alarm intents from [ReminderScheduler] and displays
 * a notification with deep-link actions (Done, Snooze, Open Chat).
 *
 * Also handles "Done" and "Snooze" actions from notification buttons.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ReminderScheduler.ACTION_REMINDER_TRIGGERED -> handleTriggered(context, intent)
            ReminderScheduler.ACTION_REMINDER_DONE -> handleDone(context, intent)
            ReminderScheduler.ACTION_REMINDER_SNOOZE -> handleSnooze(context, intent)
        }
    }

    private fun handleTriggered(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0L)
        val title = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_TITLE) ?: "Reminder"
        val description = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DESC) ?: ""
        val isAuto = intent.getBooleanExtra(ReminderScheduler.EXTRA_REMINDER_IS_AUTO, false)

        val notificationId = NOTIFICATION_ID_BASE + (reminderId % 10000).toInt()

        // Deep link: open MainActivity with navigation extras
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
            putExtra(NAVIGATE_TO_KEY, NAVIGATE_TO_CHAT)
        }

        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Done action
        val doneIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_REMINDER_DONE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 10000).toInt(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action (5 minutes)
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderScheduler.ACTION_REMINDER_SNOOZE
            putExtra(ReminderScheduler.EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId + 20000).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, AdonnisApplication.CHANNEL_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isAuto) "🤖 $title" else title)
            .setContentText(description.ifBlank { "Tap to view in chat" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_input_add, "Done", donePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 5m", snoozePendingIntent)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    private fun handleDone(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0L)
        val app = AdonnisApplication.get()
        kotlinx.coroutines.runBlocking {
            app.reminderRepository.markCompleted(reminderId)
            app.reminderScheduler.cancelById(reminderId)
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_BASE + (reminderId % 10000).toInt())
    }

    private fun handleSnooze(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0L)
        val app = AdonnisApplication.get()
        kotlinx.coroutines.runBlocking {
            val reminder = app.reminderRepository.getReminderById(reminderId)
            if (reminder != null && !reminder.isCompleted) {
                val snoozedReminder = reminder.copy(
                    dateTime = System.currentTimeMillis() + 5 * 60 * 1000
                )
                app.reminderRepository.updateReminder(snoozedReminder)
                app.reminderScheduler.schedule(snoozedReminder)
            }
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_BASE + (reminderId % 10000).toInt())
    }

    companion object {
        const val NOTIFICATION_ID_BASE = 1000
        const val NAVIGATE_TO_KEY = "navigate_to"
        const val NAVIGATE_TO_CHAT = "chat"
        const val NAVIGATE_TO_DIARY = "diary"
        const val NAVIGATE_TO_PLANNER = "planner"
    }
}
