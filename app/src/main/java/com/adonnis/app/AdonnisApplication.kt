package com.adonnis.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.adonnis.app.data.local.AppDatabase
import com.adonnis.app.data.preferences.PreferencesManager
import com.adonnis.app.data.repository.*
import com.adonnis.app.reminder.ReminderScheduler

class AdonnisApplication : Application() {

    /** Lazy-initialized database singleton */
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    /** Lazy-initialized preferences manager */
    val preferencesManager: PreferencesManager by lazy { PreferencesManager(this) }

    /** Lazy-initialized reminder scheduler */
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }

    /** Lazy-initialized alarm scheduler */
    val alarmScheduler: com.adonnis.app.alarm.AlarmScheduler by lazy {
        com.adonnis.app.alarm.AlarmScheduler(this)
    }

    // ── Repositories ─────────────────────────────────────────────────

    val userRepository: UserRepository by lazy {
        UserRepository(database.userDao(), preferencesManager)
    }

    val planRepository: PlanRepository by lazy {
        PlanRepository(database.planDao())
    }

    val diaryRepository: DiaryRepository by lazy {
        DiaryRepository(database.diaryEntryDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao(), this)
    }

    val alarmRepository: AlarmRepository by lazy {
        AlarmRepository(database.alarmDao(), alarmScheduler)
    }

    val chatRepository: ChatRepository by lazy {
        ChatRepository(database.chatMessageDao())
    }

    val memoryRepository: MemoryRepository by lazy {
        MemoryRepository(database.memoryDao())
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val reminderChannel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for events, study sessions, and tasks"
            enableVibration(true)
        }

        val diaryChannel = NotificationChannel(
            CHANNEL_DIARY,
            "Daily Diary",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "End-of-day diary prompts and reflections"
        }

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Wake-up alarm notifications"
            enableVibration(true)
            setSound(null, null) // custom alarm sound managed by alarm service
        }

        val insightsChannel = NotificationChannel(
            CHANNEL_INSIGHTS,
            "Insights",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Weekly recaps and AI-generated insights"
        }

        manager.createNotificationChannel(reminderChannel)
        manager.createNotificationChannel(diaryChannel)
        manager.createNotificationChannel(alarmChannel)
        manager.createNotificationChannel(insightsChannel)
    }

    companion object {
        const val CHANNEL_REMINDERS = "adonnis_reminders"
        const val CHANNEL_DIARY = "adonnis_diary"
        const val CHANNEL_ALARM = "adonnis_alarm"
        const val CHANNEL_INSIGHTS = "adonnis_insights"

        @Volatile
        private var instance: AdonnisApplication? = null

        /** Global accessor for the application instance */
        fun get(): AdonnisApplication = instance!!
    }
}
