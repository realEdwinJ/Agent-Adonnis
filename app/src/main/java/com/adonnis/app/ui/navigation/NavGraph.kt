package com.adonnis.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.adonnis.app.ui.alarm.AlarmChallengeScreen
import com.adonnis.app.ui.alarm.AlarmScreen
import com.adonnis.app.ui.chat.ChatScreen
import com.adonnis.app.ui.diary.DiaryScreen
import com.adonnis.app.ui.onboarding.OnboardingScreen
import com.adonnis.app.ui.planner.PlannerScreen
import com.adonnis.app.ui.settings.SettingsScreen

/**
 * Sets up the full navigation graph for the app.
 * Shows onboarding if first launch, otherwise goes to chat.
 * The 5 main screens (Chat/Plan/Diary/Alarm/Settings) share a
 * TikTok/Instagram-style bottom navigation bar.
 *
 * @param onOnboardingComplete Called when onboarding finishes — parent should
 *        update the [onboardingComplete] state so the NavGraph recomposes
 *        with the correct start destination on future re-creations.
 */
@Composable
fun AdonnisNavGraph(
    navController: NavHostController,
    onboardingComplete: Boolean,
    onOnboardingComplete: () -> Unit = {},
    startDestination: String = if (onboardingComplete) NavRoutes.Chat.route else NavRoutes.Onboarding.route
) {
    // Switch between the 5 main tabs, preserving each tab's state.
    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(NavRoutes.Chat.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(NavRoutes.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    onOnboardingComplete()
                    navController.navigate(NavRoutes.Chat.route) {
                        popUpTo(NavRoutes.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.Chat.route) {
            ChatScreen(
                bottomBar = { MainBottomBar(NavRoutes.Chat.route, ::navigateToTab) }
            )
        }

        composable(NavRoutes.Planner.route) {
            PlannerScreen(
                onBack = { navController.popBackStack() },
                bottomBar = { MainBottomBar(NavRoutes.Planner.route, ::navigateToTab) }
            )
        }

        composable(NavRoutes.Diary.route) {
            DiaryScreen(
                onBack = { navController.popBackStack() },
                bottomBar = { MainBottomBar(NavRoutes.Diary.route, ::navigateToTab) }
            )
        }

        composable(NavRoutes.Alarm.route) {
            AlarmScreen(
                onBack = { navController.popBackStack() },
                onAlarmTriggered = { alarmId ->
                    navController.navigate(NavRoutes.AlarmChallenge.createRoute(alarmId))
                },
                bottomBar = { MainBottomBar(NavRoutes.Alarm.route, ::navigateToTab) }
            )
        }

        composable(
            route = NavRoutes.AlarmChallenge.route,
            arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
        ) { backStackEntry ->
            val alarmId = backStackEntry.arguments?.getLong("alarmId") ?: 0L
            AlarmChallengeScreen(
                alarmId = alarmId,
                onDismiss = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onResetOnboarding = {
                    navController.navigate(NavRoutes.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                bottomBar = { MainBottomBar(NavRoutes.Settings.route, ::navigateToTab) }
            )
        }
    }
}
