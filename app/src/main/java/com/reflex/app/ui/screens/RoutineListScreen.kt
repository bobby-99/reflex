package com.reflex.app.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reflex.app.ReflexApplication
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.Habit
import com.reflex.app.data.HabitKind
import com.reflex.app.data.Priority
import com.reflex.app.data.RoutineTemplate
import com.reflex.app.data.RoutineWithSteps
import com.reflex.app.data.StepType
import com.reflex.app.data.Task
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.SettingsIcon
import com.reflex.app.ui.components.TemplatePickerModal
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.goldColor
import com.reflex.app.ui.theme.onCopper
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.routineGreen
import com.reflex.app.util.StreakCalculator
import com.reflex.app.util.UserProfileRepository
import com.reflex.app.viewmodel.RoutineListViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineListScreen(
    onRoutineClick: (Long) -> Unit,
    onEditRoutine: (Long) -> Unit,
    onCreateRoutine: () -> Unit,
    onSelectTemplate: (RoutineTemplate) -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onShowcaseClick: () -> Unit = {},
    onStartRoutine: (Long) -> Unit = onRoutineClick,
    onNavigateToTasks: () -> Unit = {},
    onNavigateToHabits: () -> Unit = {},
    onNavigateToFocus: () -> Unit = {}
) {
    SetStatusBarAppearance()

    val context = LocalContext.current
    val app = context.applicationContext as ReflexApplication
    val scope = rememberCoroutineScope()
    val viewModel: RoutineListViewModel = viewModel(factory = RoutineListViewModel.Factory(app.repository))
    val state by viewModel.uiState.collectAsState()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    // Real data repositories
    val profile by UserProfileRepository.profile.collectAsState()
    val allTasks by app.repository.getAllTasks().collectAsState(initial = emptyList())
    val allHabits by app.repository.getAllHabits().collectAsState(initial = emptyList())
    val allLogs by app.repository.getAllLogs().collectAsState(initial = emptyList())
    val focusSessions by app.repository.getAllFocusSessions().collectAsState(initial = emptyList())

    // Date & Time anchors
    val today = remember { LocalDate.now() }
    val todayEpochDay = remember(today) { today.toEpochDay() }
    val todayZone = remember { ZoneId.systemDefault() }
    val todayStartMs = remember(today) { today.atStartOfDay(todayZone).toInstant().toEpochMilli() }
    val todayEndMs = remember(today) { today.plusDays(1).atStartOfDay(todayZone).toInstant().toEpochMilli() }
    val nowTime = remember { LocalTime.now() }

    // Habit logs for today
    val todayHabitLogs by app.repository.getLogsForDay(todayEpochDay).collectAsState(initial = emptyList())
    val habitLogMap = remember(todayHabitLogs) { todayHabitLogs.associate { it.habitId to it.value } }

    // 1. Header Calculations
    val greeting = remember(nowTime) {
        val hour = nowTime.hour
        when {
            hour < 12 -> "Good morning"
            hour < 17 -> "Good afternoon"
            else -> "Good evening"
        }
    }
    val dateText = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    val displayName = profile.name.trim().ifEmpty { "Friend" }

    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDeleteRoutine by remember { mutableStateOf<RoutineWithSteps?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            pendingDeleteRoutine?.let { routine ->
                viewModel.deleteRoutine(routine)
            }
        }
    }

    // 2. Routine calculations
    val activeRoutines = state.activeRoutines
    val displayedRoutines = remember(activeRoutines, pendingDeleteRoutine) {
        if (pendingDeleteRoutine == null) activeRoutines else activeRoutines.filter { it.routine.id != pendingDeleteRoutine?.routine?.id }
    }
    val todayCompletedRoutineIds = remember(allLogs, todayStartMs, todayEndMs) {
        allLogs.filter { it.dateCompleted in todayStartMs until todayEndMs && it.isCompleted }
            .map { it.routineId }
            .toSet()
    }
    val doneRoutinesCount = remember(activeRoutines, todayCompletedRoutineIds) {
        activeRoutines.count { it.routine.id in todayCompletedRoutineIds }
    }
    val totalRoutinesCount = activeRoutines.size

    // Next routine to run today
    val nextRoutine = remember(activeRoutines, todayCompletedRoutineIds, nowTime) {
        val incomplete = activeRoutines.filter { it.routine.id !in todayCompletedRoutineIds }
        if (incomplete.isEmpty()) null
        else {
            incomplete.minByOrNull { rws ->
                rws.routine.reminderTime?.let { timeStr ->
                    val parts = timeStr.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val remMins = h * 60 + m
                    val nowMins = nowTime.hour * 60 + nowTime.minute
                    if (remMins >= nowMins) remMins - nowMins else (remMins + 1440) - nowMins
                } ?: 720
            }
        }
    }

    // 3. Tasks calculations
    val todayTasks = remember(allTasks, todayStartMs, todayEndMs) {
        allTasks.filter { task ->
            task.dueDate == null || (task.dueDate in todayStartMs until todayEndMs)
        }
    }
    val doneTasksCount = remember(todayTasks) { todayTasks.count { it.isCompleted } }
    val totalTasksCount = todayTasks.size
    val tasksLeftCount = totalTasksCount - doneTasksCount

    // 4. Habits calculations
    val doneHabitsCount = remember(allHabits, habitLogMap) {
        allHabits.count { it.isCompleted(habitLogMap[it.id]) }
    }
    val totalHabitsCount = allHabits.size
    val habitsToGoCount = (totalHabitsCount - doneHabitsCount).coerceAtLeast(0)

    // 5. Overall combined percentage
    val totalProgressItems = (totalRoutinesCount + totalTasksCount + totalHabitsCount).coerceAtLeast(1)
    val doneProgressItems = doneRoutinesCount + doneTasksCount + doneHabitsCount
    val combinedPercent = ((doneProgressItems.toFloat() / totalProgressItems.toFloat()) * 100f).roundToInt()

    // 6. Focus session time today
    val todayFocusSessions = remember(focusSessions, todayStartMs, todayEndMs) {
        focusSessions.filter { it.startTime in todayStartMs until todayEndMs }
    }
    val totalFocusSeconds = remember(todayFocusSessions) { todayFocusSessions.sumOf { it.actualDurationSeconds } }
    val focusFormatted = remember(totalFocusSeconds) {
        val h = totalFocusSeconds / 3600
        val m = (totalFocusSeconds % 3600) / 60
        if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    // 7. Streak calculation
    val streakStats = remember(allLogs) { StreakCalculator.calculateStreaks(allLogs) }

    val scrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .reflexStatusBarPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 140.dp)
        ) {
        // --- 1. Header (reflex-home.html .hd) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CopperPrimary,
                    fontSize = 15.sp
                )
                Text(
                    text = displayName,
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    letterSpacing = 0.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // History icon button (44dp visual, 48dp touch target, clock-arrow)
                com.reflex.app.ui.components.TopBarIconButton(
                    iconName = "hist",
                    contentDescriptionText = "Routines history",
                    onClick = { showHistorySheet = true }
                )

                // Settings gear button (44dp visual, 48dp touch target)
                com.reflex.app.ui.components.SettingsTopBarButton(
                    onClick = onSettingsClick
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 2. Overview Card (Concentric 3 Rings) ---
        HomeOverviewCard(
            combinedPercent = combinedPercent,
            routinesDone = doneRoutinesCount,
            routinesTotal = totalRoutinesCount,
            tasksDone = doneTasksCount,
            tasksTotal = totalTasksCount,
            habitsDone = doneHabitsCount,
            habitsTotal = totalHabitsCount
        )

        // --- 3. Up Next Card (.next-c) ---
        Spacer(modifier = Modifier.height(12.dp))
        if (nextRoutine != null) {
            val estimatedMins = remember(nextRoutine) {
                val totalSecs = nextRoutine.steps.sumOf { st ->
                    when (st.stepType) {
                        StepType.TIMED -> st.durationSeconds ?: 60
                        StepType.REPEAT_COUNT -> (st.targetCount ?: 3) * 12 + (st.restDurationSeconds ?: 15) * 2
                        StepType.CHECK_OFF -> 15
                    }
                }
                (totalSecs / 60).coerceAtLeast(1)
            }
            val timeLabel = nextRoutine.routine.reminderTime ?: "Today"
            val minsUntil = remember(nextRoutine, nowTime) {
                nextRoutine.routine.reminderTime?.let { timeStr ->
                    val parts = timeStr.split(":")
                    val h = parts.getOrNull(0)?.toIntOrNull() ?: 12
                    val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                    val diff = (h * 60 + m) - (nowTime.hour * 60 + nowTime.minute)
                    if (diff > 0) diff else null
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeCard)
                    .background(CopperPrimary)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (minsUntil != null) "Up next · in $minsUntil min · $timeLabel" else "Up next · $timeLabel",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onCopper.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = nextRoutine.routine.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 25.sp,
                            color = MaterialTheme.colorScheme.onCopper
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$estimatedMins min",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onCopper.copy(alpha = 0.85f)
                        )
                    }

                    // Start Pill Button (48dp height)
                    Box(
                        modifier = Modifier
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onCopper)
                            .clickable { onStartRoutine(nextRoutine.routine.id) }
                            .padding(horizontal = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SettingsIcon(name = "play", tint = CopperPrimary, size = 18.dp)
                            Text(
                                text = "Start",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = CopperPrimary
                            )
                        }
                    }
                }
            }
        } else {
            // All caught up empty state card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(ReflexTokens.ShapeIconTile)
                            .background(MaterialTheme.colorScheme.routineGreen.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.routineGreen, size = 22.dp)
                    }
                    Column {
                        Text(
                            text = "All routines complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Nice and steady. You're done for today.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 4. Focus & Streak Side-by-Side Cards (.g2) ---
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Focus today card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .clip(ReflexTokens.ShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                    .clickable { onNavigateToFocus() }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingsIcon(name = "timer", tint = CopperPrimary, size = 16.dp)
                        Text(
                            text = "Focus today",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = focusFormatted,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${todayFocusSessions.size} sessions today",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Mini chip pill
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(CopperContainer)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start 25:00",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = CopperPrimary
                        )
                    }
                }
            }

            // Streak card
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(140.dp)
                    .clip(ReflexTokens.ShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                    .clickable { showHistorySheet = true }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SettingsIcon(name = "fire", tint = CopperPrimary, size = 16.dp)
                        Text(
                            text = "Routine streak",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column {
                        Text(
                            text = "${streakStats.currentStreak} days",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Best streak: ${streakStats.bestStreak} days",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }

        // --- 5. Tasks Today Section (.tl) ---
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Tasks today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$tasksLeftCount left",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = CopperPrimary,
                modifier = Modifier
                    .clip(ReflexTokens.ShapeChip)
                    .clickable { onNavigateToTasks() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ReflexTokens.ShapeCard)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            if (todayTasks.isEmpty()) {
                Text(
                    text = "No tasks for today. Tap 'See all' to add tasks.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column {
                    todayTasks.take(5).forEachIndexed { idx, task ->
                        HomeTaskRow(
                            task = task,
                            isLast = idx == (todayTasks.take(5).size - 1),
                            onToggle = {
                                scope.launch {
                                    val updated = task.copy(
                                        isCompleted = !task.isCompleted,
                                        completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
                                    )
                                    app.repository.updateTask(updated)
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- 6. Habits Section (.hs) ---
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Habits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$habitsToGoCount to go today",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "See all",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = CopperPrimary,
                modifier = Modifier
                    .clip(ReflexTokens.ShapeChip)
                    .clickable { onNavigateToHabits() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        if (allHabits.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                    .padding(20.dp)
            ) {
                Text(
                    text = "No habits tracked yet. Set up habits to build your daily reflex.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(allHabits, key = { it.id }) { habit ->
                    val logVal = habitLogMap[habit.id] ?: 0.0
                    val isDone = habit.isCompleted(logVal)
                    val fraction = habit.progressFraction(logVal)

                    HomeHabitCard(
                        habit = habit,
                        value = logVal,
                        isDone = isDone,
                        fraction = fraction,
                        onClick = {
                            scope.launch {
                                val nextVal = if (habit.habitKind == HabitKind.CHECK_OFF) {
                                    if (isDone) 0.0 else 1.0
                                } else {
                                    habit.nextStepValue(logVal, 1)
                                }
                                app.repository.setHabitLogValue(habit.id, todayEpochDay, nextVal)
                            }
                        }
                    )
                }
            }
        }

        // --- 7. Routines Section (.band & .rl) ---
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Routines",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$doneRoutinesCount of $totalRoutinesCount done",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Header actions: Presets & + New routine
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Presets chip button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                        .clickable { showTemplatePicker = true }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Presets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // + New routine pill button
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable { showTemplatePicker = true }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ New routine",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Routine Timeline Band Card (.band)
        HomeTimelineBandCard(
            routines = activeRoutines,
            completedRoutineIds = todayCompletedRoutineIds,
            nextRoutineId = nextRoutine?.routine?.id,
            nowTime = nowTime
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Routines List Card (.rl)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ReflexTokens.ShapeCard)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            if (displayedRoutines.isEmpty()) {
                Text(
                    text = "No routines yet. Tap '+ New routine' to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Column {
                    displayedRoutines.forEachIndexed { idx, rws ->
                        val isDone = rws.routine.id in todayCompletedRoutineIds
                        val isNext = rws.routine.id == nextRoutine?.routine?.id

                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onEditRoutine(rws.routine.id)
                                        false
                                    }
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        pendingDeleteRoutine?.let { prev ->
                                            if (prev.routine.id != rws.routine.id) {
                                                viewModel.deleteRoutine(prev)
                                            }
                                        }
                                        pendingDeleteRoutine = rws
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Routine deleted",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (result == SnackbarResult.ActionPerformed) {
                                                if (pendingDeleteRoutine?.routine?.id == rws.routine.id) {
                                                    pendingDeleteRoutine = null
                                                }
                                            } else {
                                                if (pendingDeleteRoutine?.routine?.id == rws.routine.id) {
                                                    viewModel.deleteRoutine(rws)
                                                    pendingDeleteRoutine = null
                                                }
                                            }
                                        }
                                        false
                                    }
                                    SwipeToDismissBoxValue.Settled -> false
                                }
                            },
                            positionalThreshold = { totalDistance -> totalDistance * 0.35f }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            enableDismissFromStartToEnd = true,
                            enableDismissFromEndToStart = true,
                            backgroundContent = {
                                val direction = dismissState.dismissDirection
                                val isStartToEnd = direction == SwipeToDismissBoxValue.StartToEnd
                                val isEndToStart = direction == SwipeToDismissBoxValue.EndToStart

                                val bgColor = when {
                                    isStartToEnd -> CopperContainer
                                    isEndToStart -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }
                                val icon = when {
                                    isStartToEnd -> Icons.Default.Edit
                                    isEndToStart -> Icons.Default.Delete
                                    else -> null
                                }
                                val alignment = if (isStartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                val iconTint = if (isStartToEnd) CopperPrimary else Color.White

                                if (icon != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(bgColor)
                                            .padding(horizontal = 18.dp),
                                        contentAlignment = alignment
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = if (isStartToEnd) "Edit routine" else "Delete routine",
                                            tint = iconTint,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                HomeRoutineRow(
                                    routineWithSteps = rws,
                                    isDone = isDone,
                                    isNext = isNext,
                                    isLast = idx == displayedRoutines.size - 1,
                                    onClick = { onRoutineClick(rws.routine.id) },
                                    onStart = { onStartRoutine(rws.routine.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 96.dp)
            .padding(horizontal = 16.dp)
    )
}

    // --- 8. History Bottom Sheet ---
    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            scrimColor = MaterialTheme.colorScheme.scrim,
            shape = ReflexTokens.ShapeSheet,
            dragHandle = null
        ) {
            HomeHistorySheetContent(
                logs = allLogs,
                routines = activeRoutines,
                onDismiss = { showHistorySheet = false }
            )
        }
    }

    // Template Picker Modal
    if (showTemplatePicker) {
        TemplatePickerModal(
            onDismiss = { showTemplatePicker = false },
            onSelectTemplate = { tpl ->
                showTemplatePicker = false
                onSelectTemplate(tpl)
            },
            onStartFromScratch = {
                showTemplatePicker = false
                onCreateRoutine()
            }
        )
    }
}

/**
 * Concentric 3-ring progress card reproducing reflex-home.html .ov
 */
@Composable
private fun HomeOverviewCard(
    combinedPercent: Int,
    routinesDone: Int,
    routinesTotal: Int,
    tasksDone: Int,
    tasksTotal: Int,
    habitsDone: Int,
    habitsTotal: Int
) {
    val routineGreen = MaterialTheme.colorScheme.routineGreen
    val taskCopper = CopperPrimary
    val habitGold = MaterialTheme.colorScheme.goldColor
    val trackColor = MaterialTheme.colorScheme.secondaryContainer

    val rFrac = if (routinesTotal > 0) routinesDone.toFloat() / routinesTotal.toFloat() else 0f
    val tFrac = if (tasksTotal > 0) tasksDone.toFloat() / tasksTotal.toFloat() else 0f
    val hFrac = if (habitsTotal > 0) habitsDone.toFloat() / habitsTotal.toFloat() else 0f

    val animR by animateFloatAsState(targetValue = rFrac, animationSpec = tween(700), label = "animR")
    val animT by animateFloatAsState(targetValue = tFrac, animationSpec = tween(700), label = "animT")
    val animH by animateFloatAsState(targetValue = hFrac, animationSpec = tween(700), label = "animH")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ReflexTokens.ShapeCard)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
            .padding(22.dp, 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Concentric Rings Box (136dp)
            Box(
                modifier = Modifier.size(136.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 7.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val rR = 59.dp.toPx()
                    val rT = 49.dp.toPx()
                    val rH = 39.dp.toPx()

                    // Tracks
                    drawCircle(color = trackColor, radius = rR, center = center, style = Stroke(strokeW))
                    drawCircle(color = trackColor, radius = rT, center = center, style = Stroke(strokeW))
                    drawCircle(color = trackColor, radius = rH, center = center, style = Stroke(strokeW))

                    // Progress Arcs
                    if (animR > 0f) {
                        drawArc(
                            color = routineGreen,
                            startAngle = -90f,
                            sweepAngle = animR * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - rR, center.y - rR),
                            size = Size(2 * rR, 2 * rR),
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                    if (animT > 0f) {
                        drawArc(
                            color = taskCopper,
                            startAngle = -90f,
                            sweepAngle = animT * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - rT, center.y - rT),
                            size = Size(2 * rT, 2 * rT),
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                    if (animH > 0f) {
                        drawArc(
                            color = habitGold,
                            startAngle = -90f,
                            sweepAngle = animH * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - rH, center.y - rH),
                            size = Size(2 * rH, 2 * rH),
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                }

                // Centered percentage readout with comfortable interior clearance
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Text(
                        text = "$combinedPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = Lora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "of today",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Legend Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OverviewLegendRow(color = routineGreen, countText = "$routinesDone of $routinesTotal", label = "Routines")
                OverviewLegendRow(color = taskCopper, countText = "$tasksDone of $tasksTotal", label = "Tasks")
                OverviewLegendRow(color = habitGold, countText = "$habitsDone of $habitsTotal", label = "Habits")
            }
        }
    }
}

@Composable
private fun OverviewLegendRow(color: Color, countText: String, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                text = countText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Task Row matching reflex-home.html .row
 */
@Composable
private fun HomeTaskRow(
    task: Task,
    isLast: Boolean,
    onToggle: () -> Unit
) {
    val borderColor = when (task.priority) {
        Priority.HIGH -> MaterialTheme.colorScheme.error
        Priority.MEDIUM -> CopperPrimary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Round Checkbox (28dp)
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (task.isCompleted) {
                        Modifier.background(CopperPrimary)
                    } else {
                        Modifier.border(2.dp, borderColor, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (task.isCompleted) {
                SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.onCopper, size = 16.dp)
            }
        }

        // Title & Meta
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                color = if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val meta = when {
                task.dueTime != null -> {
                    val dt = java.time.Instant.ofEpochMilli(task.dueTime).atZone(ZoneId.systemDefault()).toLocalTime()
                    val ampm = if (dt.hour < 12) "am" else "pm"
                    val hr12 = if (dt.hour % 12 == 0) 12 else dt.hour % 12
                    "$hr12:${String.format("%02d", dt.minute)} $ampm"
                }
                task.dueDate != null -> "Due today"
                else -> "No time"
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = if (task.isCompleted) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.onSurfaceVariant,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
            )
        }

        // Priority Badge (hidden if completed)
        if (!task.isCompleted && task.priority != Priority.NONE) {
            val (chipBg, chipFg, chipLabel) = if (task.priority == Priority.HIGH) {
                Triple(MaterialTheme.colorScheme.error.copy(alpha = 0.16f), MaterialTheme.colorScheme.error, "High")
            } else {
                Triple(CopperContainer, CopperPrimary, "Medium")
            }
            Box(
                modifier = Modifier
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = chipLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = chipFg
                )
            }
        }
    }
}

/**
 * Habit Card matching reflex-home.html .ht
 */
@Composable
private fun HomeHabitCard(
    habit: Habit,
    value: Double,
    isDone: Boolean,
    fraction: Float,
    onClick: () -> Unit
) {
    val gold = MaterialTheme.colorScheme.goldColor
    val track = MaterialTheme.colorScheme.secondaryContainer
    val cardBorder = if (isDone) gold.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline

    Box(
        modifier = Modifier
            .width(138.dp)
            .clip(ReflexTokens.ShapeInnerTile)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(ReflexTokens.BorderThin, cardBorder), ReflexTokens.ShapeInnerTile)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // Circular Meter (46dp)
            Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeW = 5.dp.toPx()
                    val r = 18.dp.toPx()
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(color = track, radius = r, center = center, style = Stroke(strokeW))
                    if (fraction > 0f) {
                        drawArc(
                            color = gold,
                            startAngle = -90f,
                            sweepAngle = fraction * 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = Size(2 * r, 2 * r),
                            style = Stroke(strokeW, cap = StrokeCap.Round)
                        )
                    }
                }
                if (isDone) {
                    SettingsIcon(name = "check", tint = gold, size = 18.dp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = habit.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val sub = when (habit.habitKind) {
                HabitKind.MEASURABLE -> "${value.toInt()} / ${habit.target.toInt()} ${habit.unit.ifEmpty { "units" }}"
                HabitKind.LIMIT -> if (value > 0) "${value}h · max ${habit.target.toInt()}" else "Not logged"
                HabitKind.CHECK_OFF -> if (isDone) "Done" else "Check off"
            }
            Text(
                text = sub,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 6am to 12am Routine Timeline Band (.band)
 */
@Composable
private fun HomeTimelineBandCard(
    routines: List<RoutineWithSteps>,
    completedRoutineIds: Set<Long>,
    nextRoutineId: Long?,
    nowTime: LocalTime
) {
    val a = 6f
    val b = 24f
    val currentH = nowTime.hour + nowTime.minute / 60f
    val nowFraction = ((currentH - a) / (b - a)).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ReflexTokens.ShapeCard)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 10.dp)
    ) {
        Column {
            // Timeline Band with blocks and now pin
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
            ) {
                val fullWidth = maxWidth

                // Track capsule
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .align(Alignment.CenterStart)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                )

                // Blocks for routines
                routines.forEachIndexed { idx, rws ->
                    val isDone = rws.routine.id in completedRoutineIds
                    val isNext = rws.routine.id == nextRoutineId

                    val startH = rws.routine.reminderTime?.let { timeStr ->
                        val parts = timeStr.split(":")
                        val h = parts.getOrNull(0)?.toFloatOrNull() ?: 12f
                        val m = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                        h + m / 60f
                    } ?: (6.5f + idx * 4.2f)

                    val estMins = rws.steps.sumOf { st ->
                        when (st.stepType) {
                            StepType.TIMED -> st.durationSeconds ?: 60
                            StepType.REPEAT_COUNT -> (st.targetCount ?: 3) * 12 + (st.restDurationSeconds ?: 15) * 2
                            StepType.CHECK_OFF -> 15
                        }
                    } / 60f
                    val durH = (estMins / 60f).coerceAtLeast(0.4f)

                    val leftFrac = ((startH - a) / (b - a)).coerceIn(0f, 0.9f)
                    val widthFrac = (durH / (b - a)).coerceIn(0.08f, 0.35f)

                    val (bg, fg) = when {
                        isDone -> Pair(CopperPrimary.copy(alpha = 0.55f), MaterialTheme.colorScheme.onCopper)
                        isNext -> Pair(CopperPrimary, MaterialTheme.colorScheme.onCopper)
                        else -> Pair(CopperContainer, CopperPrimary)
                    }

                    Box(
                        modifier = Modifier
                            .offset(x = fullWidth * leftFrac)
                            .width(fullWidth * widthFrac)
                            .height(34.dp)
                            .align(Alignment.CenterStart)
                            .clip(CircleShape)
                            .background(bg)
                            .border(BorderStroke(1.5.dp, CopperPrimary), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            SettingsIcon(name = "check", tint = fg, size = 16.dp)
                        } else if (isNext) {
                            SettingsIcon(name = "play", tint = fg, size = 16.dp)
                        }
                    }
                }

                // NOW indicator line & badge
                Box(
                    modifier = Modifier
                        .offset(x = fullWidth * nowFraction - 1.dp)
                        .width(2.dp)
                        .height(50.dp)
                        .align(Alignment.BottomStart)
                        .background(MaterialTheme.colorScheme.onSurface)
                )

                // Current time pill label
                val nowFormatted = remember(nowTime) {
                    val h = nowTime.hour
                    val m = nowTime.minute
                    val ampm = if (h < 12) "am" else "pm"
                    val hr12 = if (h % 12 == 0) 12 else h % 12
                    "$hr12:${String.format("%02d", m)} $ampm"
                }

                Box(
                    modifier = Modifier
                        .offset(x = (fullWidth * nowFraction - 28.dp).coerceIn(0.dp, fullWidth - 60.dp))
                        .align(Alignment.TopStart)
                        .height(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = nowFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
            }

            // Time Ticks (6 am, 12 pm, 6 pm, 12 am)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("6 am", "12 pm", "6 pm", "12 am").forEach { tick ->
                    Text(
                        text = tick,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

/**
 * Routine Row in .rl list
 */
@Composable
private fun HomeRoutineRow(
    routineWithSteps: RoutineWithSteps,
    isDone: Boolean,
    isNext: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onStart: () -> Unit
) {
    val r = routineWithSteps.routine
    val iconName = remember(r.name, r.icon) {
        val lower = r.name.lowercase()
        when {
            lower.contains("sun") || lower.contains("morning") || lower.contains("reset") -> "sun"
            lower.contains("book") || lower.contains("study") || lower.contains("read") -> "book"
            lower.contains("gym") || lower.contains("workout") || lower.contains("fit") -> "gym"
            lower.contains("moon") || lower.contains("wind") || lower.contains("bed") || lower.contains("night") -> "moon"
            else -> "routines"
        }
    }

    val estMins = remember(routineWithSteps) {
        val totalSecs = routineWithSteps.steps.sumOf { st ->
            when (st.stepType) {
                StepType.TIMED -> st.durationSeconds ?: 60
                StepType.REPEAT_COUNT -> (st.targetCount ?: 3) * 12 + (st.restDurationSeconds ?: 15) * 2
                StepType.CHECK_OFF -> 15
            }
        }
        (totalSecs / 60).coerceAtLeast(1)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Tile (46dp, 18dp rounded)
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(ReflexTokens.ShapeIconTile)
                .background(CopperContainer),
            contentAlignment = Alignment.Center
        ) {
            SettingsIcon(name = iconName, tint = CopperPrimary, size = 22.dp)
        }

        // Title and Time info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = r.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            val time = r.reminderTime ?: "Anytime"
            Text(
                text = "$time · $estMins min",
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status Badge / Start Button
        if (isDone) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.routineGreen.copy(alpha = 0.18f))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.routineGreen, size = 16.dp)
                    Text(
                        text = "Done",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.routineGreen
                    )
                }
            }
        } else if (isNext) {
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(CircleShape)
                    .background(CopperContainer)
                    .clickable(onClick = onStart)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Next",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = CopperPrimary
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                SettingsIcon(name = "play", tint = MaterialTheme.colorScheme.onSurface, size = 16.dp)
            }
        }
    }
}

/**
 * History Bottom Sheet Content
 */
@Composable
private fun HomeHistorySheetContent(
    logs: List<CompletionLog>,
    routines: List<RoutineWithSteps>,
    onDismiss: () -> Unit
) {
    val routineMap = remember(routines) { routines.associate { it.routine.id to it.routine.name } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .padding(bottom = 32.dp)
    ) {
        // Grab Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${logs.size} sessions",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                SettingsIcon(name = "x", tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No routine sessions logged yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                logs.forEach { log ->
                    val rName = routineMap[log.routineId] ?: "Routine"
                    val mm = log.totalTimeTakenSeconds / 60
                    val ss = log.totalTimeTakenSeconds % 60
                    val timeFormatted = "${mm}:${String.format("%02d", ss)}"
                    val dateFormatted = remember(log.dateCompleted) {
                        val d = java.time.Instant.ofEpochMilli(log.dateCompleted).atZone(ZoneId.systemDefault()).toLocalDate()
                        val t = java.time.Instant.ofEpochMilli(log.dateCompleted).atZone(ZoneId.systemDefault()).toLocalTime()
                        val ampm = if (t.hour < 12) "am" else "pm"
                        val hr12 = if (t.hour % 12 == 0) 12 else t.hour % 12
                        val timeStr = "$hr12:${String.format("%02d", t.minute)} $ampm"
                        when {
                            d == LocalDate.now() -> "Today · $timeStr"
                            d == LocalDate.now().minusDays(1) -> "Yesterday · $timeStr"
                            else -> "${d.month.name.take(3)} ${d.dayOfMonth} · $timeStr"
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(ReflexTokens.ShapeIconTile)
                                .background(CopperContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            SettingsIcon(name = "gym", tint = CopperPrimary, size = 20.dp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = rName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val stepsMeta = if (!log.isCompleted && log.totalStepsCount > 0) {
                                " · ${log.stepsCompletedCount}/${log.totalStepsCount} steps"
                            } else ""
                            Text(
                                text = "$dateFormatted · $timeFormatted$stepsMeta",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Completed / Ended early chip
                        if (log.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.routineGreen.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Completed",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.routineGreen
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.goldColor.copy(alpha = 0.18f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Ended early",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.goldColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
