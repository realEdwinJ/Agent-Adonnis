package com.adonnis.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Manages app preferences using EncryptedSharedPreferences for sensitive
 * data (API key) and plain SharedPreferences for non-sensitive settings.
 */
class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_FILE_ENCRYPTED,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val plainPrefs: SharedPreferences =
        context.getSharedPreferences(PREFS_FILE_PLAIN, Context.MODE_PRIVATE)

    // ── API Key ──────────────────────────────────────────────────────

    var apiKey: String
        get() = encryptedPrefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = encryptedPrefs.edit().putString(KEY_API_KEY, value).apply()

    // ── OpenRouter Model ─────────────────────────────────────────────

    var openRouterModel: String
        get() = plainPrefs.getString(KEY_OPENROUTER_MODEL, com.adonnis.app.ai.OpenRouterClient.DEFAULT_MODEL)
            ?: com.adonnis.app.ai.OpenRouterClient.DEFAULT_MODEL
        set(value) = plainPrefs.edit().putString(KEY_OPENROUTER_MODEL, value).apply()

    // ── Onboarding ───────────────────────────────────────────────────

    var onboardingComplete: Boolean
        get() = plainPrefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
        set(value) = plainPrefs.edit().putBoolean(KEY_ONBOARDING_COMPLETE, value).apply()

    // ── User Name ────────────────────────────────────────────────────

    var userName: String
        get() = plainPrefs.getString(KEY_USER_NAME, "") ?: ""
        set(value) = plainPrefs.edit().putString(KEY_USER_NAME, value).apply()

    // ── Agent Name ───────────────────────────────────────────────────

    var agentName: String
        get() = plainPrefs.getString(KEY_AGENT_NAME, "Adonnis") ?: "Adonnis"
        set(value) = plainPrefs.edit().putString(KEY_AGENT_NAME, value).apply()

    // ── Last Active Date ─────────────────────────────────────────────

    var lastActiveDate: String
        get() = plainPrefs.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        set(value) = plainPrefs.edit().putString(KEY_LAST_ACTIVE_DATE, value).apply()

    // ── Diary Prompt Time ────────────────────────────────────────────

    var diaryPromptHour: Int
        get() = plainPrefs.getInt(KEY_DIARY_HOUR, 20) // Default 8pm
        set(value) = plainPrefs.edit().putInt(KEY_DIARY_HOUR, value).apply()

    var diaryPromptMinute: Int
        get() = plainPrefs.getInt(KEY_DIARY_MINUTE, 0)
        set(value) = plainPrefs.edit().putInt(KEY_DIARY_MINUTE, value).apply()

    // ── Notification Toggles ─────────────────────────────────────────

    var remindersEnabled: Boolean
        get() = plainPrefs.getBoolean(KEY_REMINDERS_ENABLED, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_REMINDERS_ENABLED, value).apply()

    var diaryPromptsEnabled: Boolean
        get() = plainPrefs.getBoolean(KEY_DIARY_PROMPTS_ENABLED, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_DIARY_PROMPTS_ENABLED, value).apply()

    // ── Dark Mode ────────────────────────────────────────────────────

    var darkMode: Boolean
        get() = plainPrefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = plainPrefs.edit().putBoolean(KEY_DARK_MODE, value).apply()

    // ── Insights Enabled ────────────────────────────────────────────

    var insightsEnabled: Boolean
        get() = plainPrefs.getBoolean(KEY_INSIGHTS_ENABLED, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_INSIGHTS_ENABLED, value).apply()

    // ── Alarm Enabled ───────────────────────────────────────────────

    var alarmNotificationsEnabled: Boolean
        get() = plainPrefs.getBoolean(KEY_ALARM_ENABLED, true)
        set(value) = plainPrefs.edit().putBoolean(KEY_ALARM_ENABLED, value).apply()

    // ── Reset ────────────────────────────────────────────────────────

    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
        plainPrefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_FILE_ENCRYPTED = "adonnis_encrypted_prefs"
        private const val PREFS_FILE_PLAIN = "adonnis_prefs"

        private const val KEY_API_KEY = "openrouter_api_key"
        private const val KEY_OPENROUTER_MODEL = "openrouter_model"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_AGENT_NAME = "agent_name"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"
        private const val KEY_DIARY_HOUR = "diary_prompt_hour"
        private const val KEY_DIARY_MINUTE = "diary_prompt_minute"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_DIARY_PROMPTS_ENABLED = "diary_prompts_enabled"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_INSIGHTS_ENABLED = "insights_enabled"
        private const val KEY_ALARM_ENABLED = "alarm_notifications_enabled"
    }
}
