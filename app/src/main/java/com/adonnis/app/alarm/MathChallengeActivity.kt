package com.adonnis.app.alarm

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.adonnis.app.AdonnisApplication
import com.adonnis.app.ui.alarm.AlarmChallengeScreen

/**
 * Full-screen activity launched by the alarm.
 * Turns screen on, bypasses lock screen, loads math equations,
 * and manages the challenge state (progress, snoozes, dismissal).
 */
class MathChallengeActivity : ComponentActivity() {

    private lateinit var app: AdonnisApplication
    private lateinit var challengeState: ChallengeState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        app = application as AdonnisApplication
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
        challengeState = ChallengeState(alarmId, app)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        setContent {
            val equations by challengeState.equations.collectAsState()
            val currentIndex by challengeState.currentIndex.collectAsState()
            val solvedCount by challengeState.solvedCount.collectAsState()
            val totalEquations by challengeState.totalEquations.collectAsState()
            val snoozeCount by challengeState.snoozeCount.collectAsState()
            val isComplete by challengeState.isComplete.collectAsState()
            val isLoading by challengeState.isLoading.collectAsState()

            Surface(modifier = Modifier.fillMaxSize()) {
                AlarmChallengeScreen(
                    alarmId = alarmId,
                    equations = equations,
                    currentIndex = currentIndex,
                    solvedCount = solvedCount,
                    totalEquations = totalEquations,
                    snoozeCount = snoozeCount,
                    isComplete = isComplete,
                    isLoading = isLoading,
                    onAnswerSubmitted = { answer -> challengeState.submitAnswer(answer) },
                    onSnooze = { challengeState.snooze(this) },
                    onDismiss = {
                        challengeState.onMorningGreeting()
                        // Stop the foreground service
                        stopService(Intent(this, AlarmForegroundService::class.java).apply {
                            action = AlarmForegroundService.ACTION_STOP_ALARM
                        })
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        challengeState.cleanup()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, 0L)
        if (alarmId != challengeState.alarmId) {
            challengeState.reset(alarmId)
        }
    }
}
