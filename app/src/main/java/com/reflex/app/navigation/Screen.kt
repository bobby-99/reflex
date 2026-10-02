package com.reflex.app.navigation

sealed class Screen(val route: String) {
    data object RoutineList : Screen("routine_list")
    data object Tasks : Screen("tasks")
    data object Calendar : Screen("calendar")
    data object Habits : Screen("habits")
    data object CalendarSettings : Screen("calendar_settings")
    data object Settings : Screen("settings?subScreen={subScreen}") {
        fun createRoute(subScreen: String? = null) =
            if (!subScreen.isNullOrBlank()) "settings?subScreen=$subScreen" else "settings"
    }
    data object RoutineEditor : Screen("routine_editor/{routineId}?templateId={templateId}") {
        fun createRoute(routineId: Long = 0L, templateId: String? = null) =
            if (templateId != null) "routine_editor/$routineId?templateId=$templateId" else "routine_editor/$routineId"
    }
    data object RoutineDetail : Screen("routine_detail/{routineId}") {
        fun createRoute(routineId: Long) = "routine_detail/$routineId"
    }
    data object RunningTimer : Screen("running_timer/{routineId}") {
        fun createRoute(routineId: Long) = "running_timer/$routineId"
    }
    data object CompletionSummary : Screen("completion_summary/{routineId}/{logId}") {
        fun createRoute(routineId: Long, logId: Long) = "completion_summary/$routineId/$logId"
    }
    data object History : Screen("history?routineId={routineId}") {
        fun createRoute(routineId: Long = 0L) = "history?routineId=$routineId"
    }

    // Focus / Pomodoro Screens
    data object FocusHome : Screen("focus_home")
    data object RunningFocus : Screen("running_focus/{mode}/{targetMin}") {
        fun createRoute(mode: String, targetMin: Int = 30) = "running_focus/$mode/$targetMin"
    }
    data object FocusSummary : Screen("focus_summary/{sessionId}") {
        fun createRoute(sessionId: Long) = "focus_summary/$sessionId"
    }
    data object FocusAnalytics : Screen("focus_analytics")

    // Help & Onboarding Screens
    data object HelpGuide : Screen("help_guide")
    data object Onboarding : Screen("onboarding?isReplay={isReplay}") {
        fun createRoute(isReplay: Boolean = false) = "onboarding?isReplay=$isReplay"
    }

    // Dev-only debug entry
    data object ComponentShowcase : Screen("component_showcase")
}
