package com.adonnis.app.ui.navigation

/**
 * All navigation routes in the app.
 */
sealed class NavRoutes(val route: String) {
    data object Onboarding : NavRoutes("onboarding")
    data object Chat : NavRoutes("chat")
    data object Planner : NavRoutes("planner")
    data object Diary : NavRoutes("diary")
    data object Settings : NavRoutes("settings")
    data object Alarm : NavRoutes("alarm")
    data object AlarmChallenge : NavRoutes("alarm_challenge/{alarmId}") {
        fun createRoute(alarmId: Long) = "alarm_challenge/$alarmId"
    }
}
