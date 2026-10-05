package com.reflex.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.reflex.app.ui.screens.*

@Composable
fun ReflexNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.RoutineList.route
    ) {
        composable(Screen.RoutineList.route) {
            RoutineListScreen(
                onRoutineClick = { routineId ->
                    navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                },
                onEditRoutine = { routineId ->
                    navController.navigate(Screen.RoutineEditor.createRoute(routineId))
                },
                onCreateRoutine = {
                    navController.navigate(Screen.RoutineEditor.createRoute(0L))
                },
                onSelectTemplate = { template ->
                    navController.navigate(Screen.RoutineEditor.createRoute(0L, template.id))
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.createRoute(0L))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onShowcaseClick = {
                    navController.navigate(Screen.ComponentShowcase.route)
                }
            )
        }

        composable(
            route = Screen.RoutineEditor.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType; defaultValue = 0L },
                navArgument("templateId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            val templateId = backStackEntry.arguments?.getString("templateId")
            val context = androidx.compose.ui.platform.LocalContext.current
            val app = context.applicationContext as com.reflex.app.ReflexApplication
            val viewModel: com.reflex.app.viewmodel.RoutineEditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                key = "routine_editor_${routineId}_$templateId",
                factory = com.reflex.app.viewmodel.RoutineEditorViewModel.Factory(app.repository, routineId, templateId)
            )

            RoutineEditorScreen(
                viewModel = viewModel,
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RoutineDetail.route,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            RoutineDetailScreen(
                routineId = routineId,
                onStartRoutine = {
                    navController.navigate(Screen.RunningTimer.createRoute(routineId))
                },
                onHistoryClick = {
                    navController.navigate(Screen.History.createRoute(routineId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RunningTimer.route,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType })
        ) { backStackEntry ->
            val context = androidx.compose.ui.platform.LocalContext.current
            val app = context.applicationContext as com.reflex.app.ReflexApplication
            val viewModel = androidx.compose.runtime.remember { com.reflex.app.viewmodel.RunningTimerViewModel(app.repository) }
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L

            RunningTimerScreen(
                viewModel = viewModel,
                routineId = routineId,
                onComplete = { logId ->
                    navController.navigate(Screen.CompletionSummary.createRoute(routineId, logId)) {
                        popUpTo(Screen.RoutineList.route)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.CompletionSummary.route,
            arguments = listOf(
                navArgument("routineId") { type = NavType.LongType },
                navArgument("logId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            val logId = backStackEntry.arguments?.getLong("logId") ?: 0L
            CompletionSummaryScreen(
                routineId = routineId,
                logId = logId,
                onDone = {
                    navController.popBackStack(Screen.RoutineList.route, inclusive = false)
                }
            )
        }

        composable(
            route = Screen.History.route,
            arguments = listOf(navArgument("routineId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val routineId = backStackEntry.arguments?.getLong("routineId") ?: 0L
            HistoryScreen(
                routineId = routineId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Settings.route,
            arguments = listOf(
                navArgument("subScreen") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val subScreen = backStackEntry.arguments?.getString("subScreen")
            SettingsScreen(
                initialSubScreen = subScreen,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Focus / Pomodoro Routes
        composable(Screen.FocusHome.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val appApplication = context.applicationContext as com.reflex.app.ReflexApplication
            val runningViewModel = androidx.compose.runtime.remember { com.reflex.app.viewmodel.RunningFocusViewModel(appApplication.repository) }

            FocusHomeScreen(
                onStartPomodoro = { title, checklistJson, tagId ->
                    runningViewModel.startPomodoro(context, title, checklistJson, tagId)
                    navController.navigate(Screen.RunningFocus.createRoute(com.reflex.app.data.FocusMode.CLASSIC_POMODORO.name))
                },
                onStartTimedFlow = { targetMin, title, checklistJson, tagId ->
                    runningViewModel.startTimedFlow(context, targetMin, title, checklistJson, tagId)
                    navController.navigate(Screen.RunningFocus.createRoute(com.reflex.app.data.FocusMode.FLOW_TIMED.name, targetMin))
                },
                onStartOpenFlow = { title, checklistJson, tagId ->
                    runningViewModel.startOpenFlow(context, title, checklistJson, tagId)
                    navController.navigate(Screen.RunningFocus.createRoute(com.reflex.app.data.FocusMode.FLOW_OPEN.name))
                },
                onSettingsClick = {
                    navController.navigate(Screen.Settings.route)
                },
                onAnalyticsClick = {
                    navController.navigate(Screen.FocusAnalytics.route)
                }
            )
        }

        composable(Screen.FocusAnalytics.route) {
            FocusAnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RunningFocus.route,
            arguments = listOf(
                navArgument("mode") { type = NavType.StringType },
                navArgument("targetMin") { type = NavType.IntType; defaultValue = 30 }
            )
        ) { backStackEntry ->
            val modeStr = backStackEntry.arguments?.getString("mode") ?: com.reflex.app.data.FocusMode.CLASSIC_POMODORO.name
            val targetMin = backStackEntry.arguments?.getInt("targetMin") ?: 30

            RunningFocusScreen(
                modeStr = modeStr,
                targetMin = targetMin,
                onCompleteSession = { sessionId ->
                    navController.navigate(Screen.FocusSummary.createRoute(sessionId)) {
                        popUpTo(Screen.FocusHome.route) { inclusive = false }
                    }
                },
                onCancel = {
                    val popped = navController.popBackStack(Screen.FocusHome.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.FocusHome.route)
                    }
                }
            )
        }

        composable(
            route = Screen.FocusSummary.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
            FocusSummaryScreen(
                sessionId = sessionId,
                onDone = {
                    com.reflex.app.service.FocusTimerService.clearActiveState()
                    val popped = navController.popBackStack(Screen.FocusHome.route, inclusive = false)
                    if (!popped) {
                        navController.navigate(Screen.FocusHome.route) {
                            popUpTo(Screen.FocusHome.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        // Dev-only: Component Showcase
        composable(Screen.ComponentShowcase.route) {
            ComponentShowcaseScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
