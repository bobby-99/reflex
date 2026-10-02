package com.reflex.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.reflex.app.ReflexApplication
import com.reflex.app.data.FocusMode
import com.reflex.app.navigation.Screen
import com.reflex.app.ui.components.NavTab
import com.reflex.app.ui.components.ReflexBottomNavBar
import com.reflex.app.ui.components.rememberQuickAddState

import com.reflex.app.viewmodel.CalendarViewModel
import com.reflex.app.viewmodel.TasksViewModel
import dev.chrisbanes.haze.hazeSource

@Composable
fun ReflexMainScreen(
    initialRoutineId: Long = -1L,
    onInitialRoutineHandled: (() -> Unit)? = null,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val activeTimerState by com.reflex.app.service.RoutineTimerService.timerState.collectAsState()
    val activeFocusState by com.reflex.app.service.FocusTimerService.timerState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(initialRoutineId) {
        if (initialRoutineId != -1L) {
            navController.navigate(Screen.RunningTimer.createRoute(initialRoutineId)) {
                popUpTo(Screen.RoutineList.route)
            }
            onInitialRoutineHandled?.invoke()
        }
    }

    androidx.compose.runtime.LaunchedEffect(activeTimerState, activeFocusState) {
        val routineState = activeTimerState
        val focusState = activeFocusState

        if (routineState != null && !routineState.isFinished && !routineState.isCancelled && currentRoute == Screen.RoutineList.route) {
            navController.navigate(Screen.RunningTimer.createRoute(routineState.routineId))
        } else if (focusState != null && !focusState.isFinished && !focusState.isCancelled && (currentRoute == Screen.RoutineList.route || currentRoute == Screen.FocusHome.route)) {
            navController.navigate(Screen.RunningFocus.createRoute(focusState.mode.name))
        }
    }

    // Determine active tab across all 5 navigation destinations
    val currentTab = when (currentRoute) {
        Screen.Calendar.route -> NavTab.CALENDAR
        Screen.Tasks.route -> NavTab.TASKS
        Screen.Habits.route -> NavTab.HABITS
        Screen.FocusHome.route, Screen.FocusAnalytics.route -> NavTab.FOCUS
        else -> NavTab.ROUTINES
    }

    // Quick add floating card state
    var isQuickAddOpen by rememberSaveable { mutableStateOf(false) }
    val quickAddState = rememberQuickAddState()
    var tasksScrollToTopTrigger by remember { androidx.compose.runtime.mutableIntStateOf(0) }

    val showBottomBar = (currentRoute == Screen.RoutineList.route || currentRoute == Screen.Tasks.route || currentRoute == Screen.FocusHome.route || currentRoute == Screen.Calendar.route || currentRoute == Screen.Habits.route || currentRoute == Screen.FocusAnalytics.route)

    val app = LocalContext.current.applicationContext as ReflexApplication
    val context = LocalContext.current
    val hasCompletedOnboarding = remember { com.reflex.app.util.OnboardingManager.hasCompletedOnboarding(context) }
    val initialDestination = if (hasCompletedOnboarding) Screen.RoutineList.route else Screen.Onboarding.route

    val tasksViewModel: TasksViewModel = viewModel(factory = TasksViewModel.Factory(app.repository))
    val calendarViewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory(app.repository))

    // Observe due medium & high priority tasks to show full-screen PriorityTaskAlertDialog
    val allTasks by app.repository.getAllTasks().collectAsState(initial = emptyList())
    var dismissedPriorityTaskIds by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(setOf<Long>()) }
    var taskToEditFromAlert by remember { androidx.compose.runtime.mutableStateOf<com.reflex.app.data.Task?>(null) }

    val pendingPriorityTask = remember(allTasks, dismissedPriorityTaskIds) {
        val now = System.currentTimeMillis()
        allTasks.firstOrNull { task ->
            if (task.isCompleted || (task.priority != com.reflex.app.data.Priority.HIGH && task.priority != com.reflex.app.data.Priority.MEDIUM) || dismissedPriorityTaskIds.contains(task.id)) {
                false
            } else {
                val effectiveTime = when {
                    task.dueTime != null && task.dueTime > now && (task.reminderTime == null || task.reminderTime <= now) -> task.dueTime
                    task.reminderTime != null -> task.reminderTime
                    task.dueTime != null -> task.dueTime
                    else -> null
                }
                effectiveTime != null && effectiveTime <= now
            }
        }
    }

    if (pendingPriorityTask != null) {
        com.reflex.app.ui.components.PriorityTaskAlertDialog(
            task = pendingPriorityTask,
            onComplete = { task ->
                tasksViewModel.toggleTaskComplete(task, context)
                dismissedPriorityTaskIds = dismissedPriorityTaskIds + task.id
            },
            onSnooze = { task, targetMillis ->
                val snoozedTask = task.copy(reminderTime = targetMillis)
                tasksViewModel.updateTask(snoozedTask, context)
                dismissedPriorityTaskIds = dismissedPriorityTaskIds + task.id
            },
            onOpenTask = { task ->
                dismissedPriorityTaskIds = dismissedPriorityTaskIds + task.id
                taskToEditFromAlert = task
            },
            onDismiss = {
                dismissedPriorityTaskIds = dismissedPriorityTaskIds + pendingPriorityTask.id
            }
        )
    }

    if (taskToEditFromAlert != null) {
        com.reflex.app.ui.components.TaskEditSheet(
            task = taskToEditFromAlert!!,
            onDismiss = { taskToEditFromAlert = null },
            onSave = { updated ->
                tasksViewModel.updateTask(updated, context)
                taskToEditFromAlert = null
            },
            onDelete = {
                tasksViewModel.deleteTask(taskToEditFromAlert!!, context)
                taskToEditFromAlert = null
            }
        )
    }

    var showExitConfirmationDialog by remember { mutableStateOf(false) }

    val isTopLevelTab = currentRoute in listOf(
        Screen.RoutineList.route,
        Screen.Calendar.route,
        Screen.Tasks.route,
        Screen.Habits.route,
        Screen.FocusHome.route
    )

    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            showExitConfirmationDialog -> {
                showExitConfirmationDialog = false
            }
            isQuickAddOpen -> {
                isQuickAddOpen = false
            }
            taskToEditFromAlert != null -> {
                taskToEditFromAlert = null
            }
            currentRoute == Screen.RoutineList.route -> {
                showExitConfirmationDialog = true
            }
            isTopLevelTab -> {
                navController.navigate(Screen.RoutineList.route) {
                    popUpTo(Screen.RoutineList.route) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            else -> {
                navController.popBackStack()
            }
        }
    }

    if (showExitConfirmationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExitConfirmationDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(36.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                androidx.compose.material3.Text(
                    text = "Exit Reflex?",
                    fontFamily = com.reflex.app.ui.theme.Lora,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                androidx.compose.material3.Text(
                    text = "Are you sure you want to close Reflex?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                com.reflex.app.ui.components.ReflexButton(
                    text = "Exit",
                    onClick = {
                        showExitConfirmationDialog = false
                        (context as? android.app.Activity)?.finish()
                    },
                    variant = com.reflex.app.ui.components.ReflexButtonVariant.DESTRUCTIVE
                )
            },
            dismissButton = {
                com.reflex.app.ui.components.ReflexButton(
                    text = "Stay",
                    onClick = { showExitConfirmationDialog = false },
                    variant = com.reflex.app.ui.components.ReflexButtonVariant.SECONDARY
                )
            }
        )
    }

    val hazeState = remember { dev.chrisbanes.haze.HazeState() }

    androidx.compose.runtime.CompositionLocalProvider(
        com.reflex.app.ui.components.LocalHazeState provides hazeState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = initialDestination,
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
            ) {
            // Top-Level Tab 1: Routine List
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
                        },
                        onStartRoutine = { routineId ->
                            navController.navigate(Screen.RunningTimer.createRoute(routineId))
                        },
                        onNavigateToTasks = {
                            navController.navigate(Screen.Tasks.route)
                        },
                        onNavigateToHabits = {
                            navController.navigate(Screen.Habits.route)
                        },
                        onNavigateToFocus = {
                            navController.navigate(Screen.FocusHome.route)
                        }
                    )
                }

                // Routine Editor Screen
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
                    val appApplication = context.applicationContext as com.reflex.app.ReflexApplication
                    val viewModel: com.reflex.app.viewmodel.RoutineEditorViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        key = "routine_editor_${routineId}_$templateId",
                        factory = com.reflex.app.viewmodel.RoutineEditorViewModel.Factory(appApplication.repository, routineId, templateId)
                    )

                    RoutineEditorScreen(
                        viewModel = viewModel,
                        routineId = routineId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Top-Level Tab 2: Tasks
                composable(Screen.Tasks.route) {
                    TasksScreen(
                        viewModel = tasksViewModel,
                        isQuickAddOpen = isQuickAddOpen,
                        onQuickAddOpenChange = { isQuickAddOpen = it },
                        quickAddState = quickAddState,
                        onCalendarClick = {
                            navController.navigate(Screen.Calendar.route)
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        },
                        scrollToTopTrigger = tasksScrollToTopTrigger
                    )
                }

                // Top-Level Tab 4: Habits
                composable(Screen.Habits.route) {
                    HabitsScreen(
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }

                // Top-Level Tab 3: Focus Home
                composable(Screen.FocusHome.route) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val appApplication = context.applicationContext as com.reflex.app.ReflexApplication
                    val runningViewModel = androidx.compose.runtime.remember { com.reflex.app.viewmodel.RunningFocusViewModel(appApplication.repository) }

                    FocusHomeScreen(
                        onStartPomodoro = { title, checklistJson, tagId ->
                            runningViewModel.startPomodoro(context, title, checklistJson, tagId)
                            navController.navigate(Screen.RunningFocus.createRoute(FocusMode.CLASSIC_POMODORO.name))
                        },
                        onStartTimedFlow = { targetMin, title, checklistJson, tagId ->
                            runningViewModel.startTimedFlow(context, targetMin, title, checklistJson, tagId)
                            navController.navigate(Screen.RunningFocus.createRoute(FocusMode.FLOW_TIMED.name, targetMin))
                        },
                        onStartOpenFlow = { title, checklistJson, tagId ->
                            runningViewModel.startOpenFlow(context, title, checklistJson, tagId)
                            navController.navigate(Screen.RunningFocus.createRoute(FocusMode.FLOW_OPEN.name))
                        },
                        onSettingsClick = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onAnalyticsClick = {
                            navController.navigate(Screen.FocusAnalytics.route)
                        }
                    )
                }

                // Focus Analytics Screen
                composable(Screen.FocusAnalytics.route) {
                    FocusAnalyticsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Running Focus Screen
                composable(
                    route = Screen.RunningFocus.route,
                    arguments = listOf(
                        navArgument("mode") { type = NavType.StringType },
                        navArgument("targetMin") { type = NavType.IntType; defaultValue = 30 }
                    )
                ) { backStackEntry ->
                    val modeStr = backStackEntry.arguments?.getString("mode") ?: FocusMode.CLASSIC_POMODORO.name
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

                // Focus Summary Screen
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

                // Calendar Screen
                composable(Screen.Calendar.route) {
                    CalendarScreen(
                        viewModel = calendarViewModel,
                        onNavigateToSettings = { navController.navigate(Screen.Settings.createRoute("calendar")) },
                        onNavigateToRoutine = { routineId ->
                            navController.navigate(Screen.RoutineDetail.createRoute(routineId))
                        }
                    )
                }

                // Calendar Settings Screen
                composable(Screen.CalendarSettings.route) {
                    CalendarSettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                // Settings Screen
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
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToHelpGuide = { navController.navigate(Screen.HelpGuide.route) },
                        onReplayOnboarding = { navController.navigate(Screen.Onboarding.createRoute(isReplay = true)) }
                    )
                }

                // Help Guide Screen
                composable(Screen.HelpGuide.route) {
                    HelpGuideScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(Screen.RoutineList.route)
                            }
                        }
                    )
                }

                // First-Launch Onboarding Tour Screen
                composable(
                    route = Screen.Onboarding.route,
                    arguments = listOf(androidx.navigation.navArgument("isReplay") {
                        type = androidx.navigation.NavType.BoolType
                        defaultValue = false
                    })
                ) { backStackEntry ->
                    val isReplay = backStackEntry.arguments?.getBoolean("isReplay") ?: false
                    TourScreen(
                        isReplay = isReplay,
                        onFinishOnboarding = {
                            if (isReplay) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.RoutineList.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToSettings = {
                            if (isReplay) {
                                navController.popBackStack()
                            } else {
                                navController.navigate(Screen.Settings.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // Detail Flows
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
                    val appApplication = context.applicationContext as com.reflex.app.ReflexApplication
                    val viewModel = androidx.compose.runtime.remember { com.reflex.app.viewmodel.RunningTimerViewModel(appApplication.repository) }
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

                // Debug entry: Component Showcase
                composable(Screen.ComponentShowcase.route) {
                    ComponentShowcaseScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            if (showBottomBar) {
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

                ReflexBottomNavBar(
                    selectedTab = currentTab,
                    onTabSelected = { tab ->
                        isQuickAddOpen = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        val targetRoute = when (tab) {
                            NavTab.ROUTINES -> Screen.RoutineList.route
                            NavTab.CALENDAR -> Screen.Calendar.route
                            NavTab.TASKS -> Screen.Tasks.route
                            NavTab.HABITS -> Screen.Habits.route
                            NavTab.FOCUS -> Screen.FocusHome.route
                        }
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(Screen.RoutineList.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    isQuickAddOpen = isQuickAddOpen,
                    hasDraftText = quickAddState.text.isNotBlank(),
                    onCenterActionClick = {
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                        if (currentTab != NavTab.TASKS) {
                            navController.navigate(Screen.Tasks.route) {
                                popUpTo(Screen.RoutineList.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            tasksScrollToTopTrigger++
                        }
                    }
                )
            }
        }
    }
}
