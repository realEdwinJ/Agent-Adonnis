package com.adonnis.app.alarm

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.MainActivity

/**
 * Foreground service that keeps the CPU awake during an active alarm.
 * Shows a persistent notification until the math challenge is completed or dismissed.
 * Can be stopped externally via ACTION_STOP_ALARM.
 */
class AlarmForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Check if this is a stop command
        if (intent?.action == ACTION_STOP_ALARM) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L) ?: 0L

        val notification = createNotification(alarmId)
        startForeground(NOTIFICATION_ID, notification)

        // Launch the math challenge activity
        val challengeIntent = Intent(this, MathChallengeActivity::class.java)
        challengeIntent.putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        challengeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(challengeIntent)

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Adonnis:AlarmWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minute timeout
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    private fun createNotification(alarmId: Long): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, AdonnisApplication.CHANNEL_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Alarm")
            .setContentText("Solve the math challenge to dismiss!")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 2001
        const val ACTION_STOP_ALARM = "com.adonnis.app.ACTION_STOP_ALARM"

        /** Convenience: create a stop intent for this service */
        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmForegroundService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
    }
}
