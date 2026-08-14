package com.adonnis.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.adonnis.app.reminder.ReminderReceiver
import com.adonnis.app.ui.navigation.AdonnisNavGraph
import com.adonnis.app.ui.navigation.NavRoutes
import com.adonnis.app.ui.theme.AdonnisTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+: request notification permission up front so alarms
        // and reminders can actually post notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launcher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* result captured; user can re-enable later in Settings */ }
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val app = application as AdonnisApplication
        val prefs = app.preferencesManager
        val initialOnboardingComplete = prefs.onboardingComplete

        // Read deep link destination from notification intent
        val navigateTo = intent?.getStringExtra(ReminderReceiver.NAVIGATE_TO_KEY)

        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            var onboardingComplete by remember {
                mutableStateOf(initialOnboardingComplete)
            }

            // Handle deep link navigation after the nav graph is created
            LaunchedEffect(navController) {
                when (navigateTo) {
                    ReminderReceiver.NAVIGATE_TO_CHAT -> {
                        // Chat is the default destination, no action needed
                    }
                    ReminderReceiver.NAVIGATE_TO_DIARY -> {
                        navController.navigate(NavRoutes.Diary.route) {
                            launchSingleTop = true
                        }
                    }
                    ReminderReceiver.NAVIGATE_TO_PLANNER -> {
                        navController.navigate(NavRoutes.Planner.route) {
                            launchSingleTop = true
                        }
                    }
                }
            }

            AdonnisTheme(dynamicColor = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AdonnisNavGraph(
                        navController = navController,
                        onboardingComplete = onboardingComplete,
                        onOnboardingComplete = { onboardingComplete = true }
                    )
                }
            }
        }
    }
}
