package com.reflex.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import android.os.Build
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reflex.app.ReflexApplication
import com.reflex.app.data.Habit
import com.reflex.app.data.HabitKind
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.AppTheme
import com.reflex.app.ui.theme.ReflexTheme
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.destructiveRed
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.tertiaryText
import com.reflex.app.util.HabitDayProgress
import com.reflex.app.util.HabitToWorkOnItem
import com.reflex.app.util.StreakStats
import com.reflex.app.util.TopHabitItem
import com.reflex.app.viewmodel.HabitsUiState
import com.reflex.app.viewmodel.HabitsViewMode
import com.reflex.app.viewmodel.HabitsViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * Habits Screen (Reflex Design System v3.0).
 * Offline local habit tracking and consistency analytics.
 * Replicates reflex-habits.html with pure Room storage, Lora typography,
 * tabular figures (tnum), and dual-mode contrast compliance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    onSettingsClick: () -> Unit,
    viewModel: HabitsViewModel = viewModel(
        factory = HabitsViewModel.Factory(
            (LocalContext.current.applicationContext as ReflexApplication).repository
        )
    )
) {
    SetStatusBarAppearance()

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val density = LocalContext.current.resources.displayMetrics.density
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.checkDateRefresh()
    }

    var selectedDayForSheet by remember { mutableStateOf<LocalDate?>(null) }
    var showManageSheet by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<Habit?>(null) }
    var showAddHabitSheet by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<Habit?>(null) }

    val mainScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val analyticsScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val matrixScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val heatmapScrollState = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }

    fun scrollToTodayInMatrix() {
        scope.launch {
            val todayDay = uiState.today.dayOfMonth
            val targetPx = ((todayDay - 1) * 40 * density).toInt()
            matrixScrollState.animateScrollTo(maxOf(0, targetPx - 100))
        }
    }

    LaunchedEffect(uiState.today, uiState.selectedMonth) {
        if (uiState.selectedMonth == YearMonth.from(uiState.today)) {
            val todayDay = uiState.today.dayOfMonth
            val targetPx = ((todayDay - 1) * 40 * density).toInt()
            matrixScrollState.scrollTo(maxOf(0, targetPx - 100))
        }
    }

    LaunchedEffect(uiState.viewMode) {
        if (uiState.viewMode == HabitsViewMode.ANALYTICS) {
            analyticsScrollState.scrollTo(0)
            heatmapScrollState.scrollTo(heatmapScrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 1. TOP BAR ---
        HabitsTopBar(
            viewMode = uiState.viewMode,
            selectedMonth = uiState.selectedMonth,
            onBackClick = {
                viewModel.setViewMode(HabitsViewMode.MAIN)
                scrollToTodayInMatrix()
            },
            onAnalyticsClick = {
                viewModel.setViewMode(HabitsViewMode.ANALYTICS)
            },
            onSettingsClick = onSettingsClick
        )

        // --- 2. SCREEN CONTENT (MAIN or ANALYTICS) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.viewMode == HabitsViewMode.MAIN) {
                HabitsMainContent(
                    uiState = uiState,
                    scrollState = mainScrollState,
                    matrixScrollState = matrixScrollState,
                    onTodayChipClick = { scrollToTodayInMatrix() },
                    onManageClick = { showManageSheet = true },
                    onAddHabitClick = {
                        habitToEdit = null
                        showAddHabitSheet = true
                    },
                    onToggleCheckOff = { habit, epochDay ->
                        viewModel.toggleCheckOff(habit, epochDay)
                    },
                    onStepHabit = { habit, epochDay, dir ->
                        viewModel.stepHabit(habit, epochDay, dir)
                    },
                    onDayHeaderClick = { day ->
                        selectedDayForSheet = uiState.selectedMonth.atDay(day)
                    },
                    onCellClick = { habit, day ->
                        if (habit.habitKind == HabitKind.CHECK_OFF) {
                            val epochDay = uiState.selectedMonth.atDay(day).toEpochDay()
                            viewModel.toggleCheckOff(habit, epochDay)
                        } else {
                            selectedDayForSheet = uiState.selectedMonth.atDay(day)
                        }
                    },
                    onPrevMonth = { viewModel.prevMonth() },
                    onNextMonth = { viewModel.nextMonth() },
                    onTodayMonthClick = {
                        viewModel.resetToCurrentMonth()
                        scrollToTodayInMatrix()
                    }
                )
            } else {
                HabitsAnalyticsContent(
                    uiState = uiState,
                    scrollState = analyticsScrollState,
                    heatmapScrollState = heatmapScrollState,
                    onHeatmapCellClick = { date ->
                        selectedDayForSheet = date
                    }
                )
            }
        }
    }

    // --- 3. BOTTOM SHEETS & DIALOGS ---

    // A. Selected Day Bottom Sheet
    selectedDayForSheet?.let { date ->
        SelectedDaySheet(
            date = date,
            habits = uiState.habits,
            logs = uiState.logs,
            onDismiss = { selectedDayForSheet = null },
            onToggleCheckOff = { habit ->
                viewModel.toggleCheckOff(habit, date.toEpochDay())
            },
            onStepHabit = { habit, dir ->
                viewModel.stepHabit(habit, date.toEpochDay(), dir)
            }
        )
    }

    // B. Manage Habits Bottom Sheet
    if (showManageSheet) {
        ManageHabitsSheet(
            habits = uiState.habits,
            onDismiss = { showManageSheet = false },
            onEditHabit = { habit ->
                habitToEdit = habit
                showManageSheet = false
                showAddHabitSheet = true
            },
            onDeleteHabit = { habit ->
                habitToDelete = habit
            },
            onMoveHabit = { fromIndex, toIndex ->
                if (fromIndex in uiState.habits.indices && toIndex in uiState.habits.indices) {
                    val list = uiState.habits.toMutableList()
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    viewModel.reorderHabits(list)
                }
            }
        )
    }

    // C. Add / Edit Habit Bottom Sheet
    if (showAddHabitSheet) {
        AddEditHabitSheet(
            initialHabit = habitToEdit,
            onDismiss = {
                showAddHabitSheet = false
                habitToEdit = null
            },
            onSave = { name, kind, target, unit, step, freqType, freqDays, freqTargetPerWeek, reminderEnabled, reminderTimes, startDay, endDay, colorHex, iconKey, notes ->
                viewModel.saveHabit(
                    name = name,
                    kind = kind,
                    target = target,
                    unit = unit,
                    step = step,
                    frequencyType = freqType,
                    frequencyDays = freqDays,
                    frequencyTargetPerWeek = freqTargetPerWeek,
                    reminderEnabled = reminderEnabled,
                    reminderTimes = reminderTimes,
                    startEpochDay = startDay,
                    endEpochDay = endDay,
                    colorHex = colorHex,
                    iconKey = iconKey,
                    notes = notes,
                    existingId = habitToEdit?.id ?: 0L,
                    context = context
                )
                showAddHabitSheet = false
                habitToEdit = null
            }
        )
    }

    // D. Delete Confirmation Dialog
    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            shape = ReflexTokens.ShapeDialog,
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Delete habit?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "All history for this habit will be lost.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                ReflexButton(
                    text = "Delete",
                    onClick = {
                        viewModel.deleteHabit(habit, context)
                        habitToDelete = null
                    },
                    variant = ReflexButtonVariant.DESTRUCTIVE
                )
            },
            dismissButton = {
                ReflexButton(
                    text = "Cancel",
                    onClick = { habitToDelete = null },
                    variant = ReflexButtonVariant.SECONDARY
                )
            }
        )
    }
}

// =========================================================================
// TOP BAR
// =========================================================================

@Composable
private fun HabitsTopBar(
    viewMode: HabitsViewMode,
    selectedMonth: YearMonth,
    onBackClick: () -> Unit,
    onAnalyticsClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val monthName = selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .reflexStatusBarPadding()
            .padding(horizontal = ReflexTokens.SpaceLg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (viewMode == HabitsViewMode.MAIN) {
                Text(
                    text = "Habits",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    com.reflex.app.ui.components.TopBarIconButton(
                        iconName = "line-graph",
                        contentDescriptionText = "Habit analytics",
                        onClick = onAnalyticsClick
                    )

                    com.reflex.app.ui.components.SettingsTopBarButton(
                        onClick = onSettingsClick
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                ) {
                    TopBarCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBackClick
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.reflex.app.R.string.habits_analytics_label),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                com.reflex.app.ui.components.SettingsTopBarButton(
                    onClick = onSettingsClick
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (viewMode == HabitsViewMode.MAIN) "Local tracker · offline" else "$monthName · deep dive across all habits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiaryText,
            modifier = Modifier.padding(bottom = ReflexTokens.SpaceMd)
        )
    }
}

@Composable
private fun TopBarCircleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =========================================================================
// MAIN VIEW CONTENT
// =========================================================================

@Composable
private fun HabitsMainContent(
    uiState: HabitsUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    matrixScrollState: androidx.compose.foundation.ScrollState,
    onTodayChipClick: () -> Unit,
    onManageClick: () -> Unit,
    onAddHabitClick: () -> Unit,
    onToggleCheckOff: (Habit, Long) -> Unit,
    onStepHabit: (Habit, Long, Int) -> Unit,
    onDayHeaderClick: (Int) -> Unit,
    onCellClick: (Habit, Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayMonthClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = ReflexTokens.SpaceLg)
            .padding(bottom = 114.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        StreakCard(
            streakStats = uiState.streakStats,
            habits = uiState.habits,
            logs = uiState.logs,
            today = uiState.today,
            onTodayClick = onTodayChipClick
        )

        DailyProgressCard(
            habits = uiState.habits,
            logs = uiState.logs,
            today = uiState.today,
            progress = uiState.todayProgress,
            onManageClick = onManageClick,
            onAddHabitClick = onAddHabitClick,
            onToggleCheckOff = onToggleCheckOff,
            onStepHabit = onStepHabit
        )

        if (uiState.habits.isNotEmpty()) {
            MonthlyHabitMatrixCard(
                habits = uiState.habits,
                logs = uiState.logs,
                today = uiState.today,
                selectedMonth = uiState.selectedMonth,
                matrixScrollState = matrixScrollState,
                onDayHeaderClick = onDayHeaderClick,
                onCellClick = onCellClick,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth,
                onTodayClick = onTodayMonthClick
            )
        }

        ConsistencyCard(
            activeDays = uiState.consistencyStats.first,
            ratePercentage = uiState.consistencyStats.second,
            bestDayName = uiState.consistencyStats.third
        )

        if (uiState.topHabits.isNotEmpty()) {
            TopHabitsCard(topHabits = uiState.topHabits)
        }

        if (uiState.habitsToWorkOn.isNotEmpty()) {
            HabitsToWorkOnCard(habitsToWorkOn = uiState.habitsToWorkOn)
        }
    }
}

// =========================================================================
// 1. STREAK CARD
// =========================================================================

@Composable
private fun StreakCard(
    streakStats: StreakStats,
    habits: List<Habit>,
    logs: Map<Pair<Long, Long>, Double>,
    today: LocalDate,
    onTodayClick: () -> Unit
) {
    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeCard,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Text(
                text = "Streak",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "consecutive active days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "${streakStats.currentStreak}",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 64.sp,
                            lineHeight = 64.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "day streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = ReflexTokens.ShapeInnerTile,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.width(130.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "${streakStats.bestStreak}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 28.sp,
                                lineHeight = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Best",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "longest run",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val todayEpoch = today.toEpochDay()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (offset in 6 downTo 0) {
                    val dayEpoch = todayEpoch - offset
                    val dayDate = LocalDate.ofEpochDay(dayEpoch)
                    val isToday = offset == 0
                    val isActive = habits.any { habit ->
                        habit.startEpochDay <= dayEpoch && habit.isCompleted(logs[Pair(habit.id, dayEpoch)])
                    }
                    val letter = dayDate.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

                    Surface(
                        modifier = Modifier
                            .size(34.dp)
                            .then(
                                if (isToday) {
                                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                } else {
                                    Modifier
                                }
                            ),
                        shape = CircleShape,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = letter,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isActive) ActionPillOnWhite else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTodayClick),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// =========================================================================
// 2. DAILY PROGRESS CARD
// =========================================================================

@Composable
private fun DailyProgressCard(
    habits: List<Habit>,
    logs: Map<Pair<Long, Long>, Double>,
    today: LocalDate,
    progress: HabitDayProgress,
    onManageClick: () -> Unit,
    onAddHabitClick: () -> Unit,
    onToggleCheckOff: (Habit, Long) -> Unit,
    onStepHabit: (Habit, Long, Int) -> Unit
) {
    val todayEpoch = today.toEpochDay()

    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeCard,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Daily progress",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "completed · today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (habits.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .height(34.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onManageClick),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Manage",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (habits.isEmpty()) {
                ZeroHabitsEmptyCard(onAddHabitClick = onAddHabitClick)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Box(
                        modifier = Modifier.size(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val strokeWidth = 12.dp
                        val primaryColor = MaterialTheme.colorScheme.primary
                        val trackColor = MaterialTheme.colorScheme.secondaryContainer
                        val sweepAnim by animateFloatAsState(
                            targetValue = progress.fraction * 360f,
                            animationSpec = tween(400),
                            label = "ring_progress_anim"
                        )

                        Canvas(modifier = Modifier.size(132.dp)) {
                            val strokePx = strokeWidth.toPx()
                            val radius = (size.minDimension - strokePx) / 2f
                            drawCircle(
                                color = trackColor,
                                radius = radius,
                                style = Stroke(width = strokePx)
                            )
                            drawArc(
                                color = primaryColor,
                                startAngle = -90f,
                                sweepAngle = sweepAnim,
                                useCenter = false,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }

                        Text(
                            text = "${progress.completedCount}/${progress.totalCount}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = "${progress.percentage}%",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 40.sp,
                            lineHeight = 44.sp,
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    habits.forEach { habit ->
                        val value = logs[Pair(habit.id, todayEpoch)]
                        HabitInteractiveRow(
                            habit = habit,
                            value = value,
                            onToggle = { onToggleCheckOff(habit, todayEpoch) },
                            onStep = { dir -> onStepHabit(habit, todayEpoch, dir) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onAddHabitClick),
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(ReflexTokens.SpaceSm))
                        Text(
                            text = "Add habit",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// HABIT ROW COMPONENT (CHECK-OFF, MEASURABLE, LIMIT)
// =========================================================================

@Composable
private fun HabitInteractiveRow(
    habit: Habit,
    value: Double?,
    onToggle: () -> Unit,
    onStep: (Int) -> Unit
) {
    val isDone = habit.isCompleted(value)
    val isOverLimit = habit.habitKind == HabitKind.LIMIT && value != null && !isDone
    val fraction = habit.progressFraction(value)
    val destructiveColor = MaterialTheme.colorScheme.destructiveRed

    when (habit.habitKind) {
        HabitKind.CHECK_OFF -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(ReflexTokens.ShapeInnerTile)
                    .clickable(onClick = onToggle),
                shape = ReflexTokens.ShapeInnerTile,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isDone) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .border(
                                width = 2.dp,
                                color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryText,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ActionPillOnWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HabitKind.MEASURABLE, HabitKind.LIMIT -> {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(ReflexTokens.ShapeInnerTile)
                    .then(
                        if (isOverLimit) {
                            Modifier.border(1.dp, destructiveColor, ReflexTokens.ShapeInnerTile)
                        } else {
                            Modifier
                        }
                    ),
                shape = ReflexTokens.ShapeInnerTile,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (habit.habitKind == HabitKind.MEASURABLE && fraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isOverLimit -> destructiveColor
                                        isDone -> MaterialTheme.colorScheme.primary
                                        else -> Color.Transparent
                                    }
                                )
                                .border(
                                    width = 2.dp,
                                    color = when {
                                        isOverLimit -> destructiveColor
                                        isDone -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.tertiaryText
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ActionPillOnWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else if (isOverLimit) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = ActionPillOnWhite,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val subtitleText = when (habit.habitKind) {
                                HabitKind.MEASURABLE -> {
                                    val vStr = if (value == null) "0" else formatValue(value)
                                    val tStr = formatValue(habit.target)
                                    "$vStr / $tStr ${habit.unit}"
                                }
                                HabitKind.LIMIT -> {
                                    val tStr = formatValue(habit.target)
                                    if (value == null) {
                                        "Not logged · max $tStr ${habit.unit}"
                                    } else {
                                        val vStr = formatValue(value)
                                        "$vStr ${habit.unit} · max $tStr"
                                    }
                                }
                                else -> ""
                            }

                            Text(
                                text = subtitleText,
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = if (isOverLimit) destructiveColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onStep(-1) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "−",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable { onStep(1) },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 3. MONTHLY HABIT MATRIX CARD
// =========================================================================

@Composable
private fun MonthlyHabitMatrixCard(
    habits: List<Habit>,
    logs: Map<Pair<Long, Long>, Double>,
    today: LocalDate,
    selectedMonth: YearMonth,
    matrixScrollState: androidx.compose.foundation.ScrollState,
    onDayHeaderClick: (Int) -> Unit,
    onCellClick: (Habit, Int) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    val daysInMonth = selectedMonth.lengthOfMonth()
    val isCurrentMonth = selectedMonth == YearMonth.from(today)
    val todayDay = if (isCurrentMonth) today.dayOfMonth else -1
    val monthTitle = "${selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${selectedMonth.year}"

    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeCard,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Text(
                text = "Monthly habit matrix",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "tap a day to log · today follows automatically",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onPrevMonth, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onTodayClick),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.width(110.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.height(30.dp))

                    habits.forEach { habit ->
                        Box(
                            modifier = Modifier
                                .height(30.dp)
                                .fillMaxWidth()
                                .padding(end = 6.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = habit.name,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(matrixScrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (day in 1..daysInMonth) {
                        val isDayToday = day == todayDay
                        val dayDate = selectedMonth.atDay(day)
                        val dayEpoch = dayDate.toEpochDay()
                        val isFuture = dayEpoch > today.toEpochDay()

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable { onDayHeaderClick(day) },
                                shape = CircleShape,
                                color = if (isDayToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                border = if (isDayToday) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = if (isDayToday) FontWeight.Bold else FontWeight.Normal,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (isDayToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiaryText
                                    )
                                }
                            }

                            habits.forEach { habit ->
                                val value = logs[Pair(habit.id, dayEpoch)]
                                val isBeforeStart = dayEpoch < habit.startEpochDay
                                val isDone = !isBeforeStart && habit.isCompleted(value)
                                val isOver = habit.habitKind == HabitKind.LIMIT && value != null && !isDone
                                val progressFrac = habit.progressFraction(value)
                                val destructiveColor = MaterialTheme.colorScheme.destructiveRed

                                Surface(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .clickable(enabled = !isFuture && !isBeforeStart) {
                                            onCellClick(habit, day)
                                        },
                                    shape = CircleShape,
                                    color = when {
                                        isFuture || isBeforeStart -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                                        isOver -> destructiveColor
                                        isDone -> MaterialTheme.colorScheme.primary
                                        habit.habitKind == HabitKind.MEASURABLE && progressFrac > 0f ->
                                            MaterialTheme.colorScheme.primary.copy(alpha = progressFrac * 0.7f)
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (isDone) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ActionPillOnWhite,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        } else if (isOver) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = null,
                                                tint = ActionPillOnWhite,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. CONSISTENCY CARD
// =========================================================================

@Composable
private fun ConsistencyCard(
    activeDays: Int,
    ratePercentage: Int,
    bestDayName: String
) {
    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeCard,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Text(
                text = "Consistency",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "30-day rate",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$ratePercentage%",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontFeatureSettings = "tnum"
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(maxOf(0f, minOf(1f, ratePercentage / 100f)))
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConsistencyTile(
                    value = "$activeDays",
                    label = "Active",
                    caption = "days active",
                    modifier = Modifier.weight(1f)
                )
                ConsistencyTile(
                    value = "$ratePercentage%",
                    label = "Rate",
                    caption = "checks done",
                    modifier = Modifier.weight(1f)
                )
                ConsistencyTile(
                    value = bestDayName,
                    label = "Best",
                    caption = "in a day",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConsistencyTile(
    value: String,
    label: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = ReflexTokens.ShapeInnerTile,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum"
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = caption,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =========================================================================
// 5. TOP HABITS CARD
// =========================================================================

@Composable
private fun TopHabitsCard(topHabits: List<TopHabitItem>) {
    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeCard,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Text(
                text = "Top habits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                topHabits.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Text(
                            text = item.habit.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${item.completedDays} days · ${item.ratePercentage}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 6. HABITS TO WORK ON CARD
// =========================================================================

@Composable
private fun HabitsToWorkOnCard(habitsToWorkOn: List<HabitToWorkOnItem>) {
    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeCard,
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column {
            Text(
                text = "Habits to work on",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "often left behind",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column {
                habitsToWorkOn.forEachIndexed { index, item ->
                    if (index > 0) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.habit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatHabitTypeCaption(item.habit),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                    Text(
                                        text = "Missed ${item.missedCount} of last ${item.windowDays}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val daysSinceStr = when (item.daysSinceLastCompleted) {
                            null -> "Not completed yet this month"
                            0 -> "Done today"
                            1 -> "1 day since last completed"
                            else -> "${item.daysSinceLastCompleted} days since last completed"
                        }

                        Text(
                            text = "$daysSinceStr · ${item.monthlyRatePercentage}% this month",
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.last7DayStatus.forEach { isCompleted ->
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ANALYTICS VIEW CONTENT
// =========================================================================

@Composable
private fun HabitsAnalyticsContent(
    uiState: HabitsUiState,
    scrollState: androidx.compose.foundation.ScrollState,
    heatmapScrollState: androidx.compose.foundation.ScrollState,
    onHeatmapCellClick: (LocalDate) -> Unit
) {
    val monthName = uiState.selectedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = ReflexTokens.SpaceLg)
            .padding(bottom = 114.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ReflexCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ReflexTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column {
                Text(
                    text = "Month snapshot",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$monthName · deep dive across all habits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConsistencyTile(
                        value = "${uiState.monthSnapshot.totalCheckIns}",
                        label = "Total check-ins",
                        caption = "completed",
                        modifier = Modifier.weight(1f)
                    )
                    ConsistencyTile(
                        value = "${uiState.monthSnapshot.completionRatePercentage}%",
                        label = "Rate",
                        caption = "completed",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ConsistencyTile(
                        value = "${uiState.monthSnapshot.bestStreakThisMonth}",
                        label = "Best streak",
                        caption = "this month",
                        modifier = Modifier.weight(1f)
                    )
                    ConsistencyTile(
                        value = "${uiState.monthSnapshot.perfectDays}",
                        label = "Perfect days",
                        caption = "100% done",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ReflexCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ReflexTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column {
                Text(
                    text = "Consistency heatmap",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "all habits combined · last 365 days",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(heatmapScrollState),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        uiState.heatmapData.weeks.forEach { week ->
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                week.days.forEach { day ->
                                    if (day != null) {
                                        val levelColor = when (day.level) {
                                            1 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)
                                            2 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            3 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)
                                            4 -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(levelColor)
                                                .clickable { onHeatmapCellClick(day.localDate) }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .background(Color.Transparent)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Less",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.secondaryContainer))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.80f)))
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.primary))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "More",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        ReflexCard(
            modifier = Modifier.fillMaxWidth(),
            shape = ReflexTokens.ShapeCard,
            containerColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.outline,
            contentPadding = PaddingValues(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text(
                        text = "Analysis",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "insights from your tracking history",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                uiState.analysisItems.forEach { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ReflexTokens.ShapeInnerTile,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)) {
                                        Text(
                                            text = item.highlight,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// ZERO-HABITS EMPTY STATE
// =========================================================================

@Composable
private fun ZeroHabitsEmptyCard(onAddHabitClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeInnerTile,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Build daily momentum",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Track habits, measure progress, and build streaks that last.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            ReflexButton(
                text = "Add your first habit",
                onClick = onAddHabitClick,
                variant = ReflexButtonVariant.PRIMARY
            )
        }
    }
}

// =========================================================================
// BOTTOM SHEETS
// =========================================================================

// A. Selected Day Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectedDaySheet(
    date: LocalDate,
    habits: List<Habit>,
    logs: Map<Pair<Long, Long>, Double>,
    onDismiss: () -> Unit,
    onToggleCheckOff: (Habit) -> Unit,
    onStepHabit: (Habit, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dayEpoch = date.toEpochDay()
    val formattedDate = date.format(
        java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")
    )

    val activeHabits = habits.filter { it.startEpochDay <= dayEpoch }
    val completedCount = activeHabits.count { it.isCompleted(logs[Pair(it.id, dayEpoch)]) }
    val pct = if (activeHabits.isNotEmpty()) (completedCount * 100 / activeHabits.size) else 0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ReflexTokens.ShapeSheet,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Selected day",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = ReflexTokens.ShapeInnerTile,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Day progress",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$completedCount/${activeHabits.size} done ($pct%)",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontFeatureSettings = "tnum"
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeHabits.isEmpty()) {
                    Text(
                        text = "No habits were active on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    activeHabits.forEach { habit ->
                        val value = logs[Pair(habit.id, dayEpoch)]
                        HabitInteractiveRow(
                            habit = habit,
                            value = value,
                            onToggle = { onToggleCheckOff(habit) },
                            onStep = { dir -> onStepHabit(habit, dir) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ReflexButton(
                text = "Done",
                onClick = onDismiss,
                variant = ReflexButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// B. Manage Habits Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageHabitsSheet(
    habits: List<Habit>,
    onDismiss: () -> Unit,
    onEditHabit: (Habit) -> Unit,
    onDeleteHabit: (Habit) -> Unit,
    onMoveHabit: (Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ReflexTokens.ShapeSheet,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Manage habits",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "reorder, edit, or remove",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                habits.forEachIndexed { index, habit ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = ReflexTokens.ShapeInnerTile,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column {
                                IconButton(
                                    onClick = { onMoveHabit(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Move up",
                                        tint = if (index > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.tertiaryText.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveHabit(index, index + 1) },
                                    enabled = index < habits.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Move down",
                                        tint = if (index < habits.size - 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.tertiaryText.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = habit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = formatHabitTypeCaption(habit),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { onEditHabit(habit) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(onClick = { onDeleteHabit(habit) }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.destructiveRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            ReflexButton(
                text = "Close",
                onClick = onDismiss,
                variant = ReflexButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// C. Add / Edit Habit Sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditHabitSheet(
    initialHabit: Habit?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        kind: HabitKind,
        target: Double,
        unit: String,
        step: Double,
        frequencyType: String,
        frequencyDays: String,
        frequencyTargetPerWeek: Int,
        reminderEnabled: Boolean,
        reminderTimes: String,
        startEpochDay: Long,
        endEpochDay: Long?,
        colorHex: String?,
        iconKey: String?,
        notes: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialHabit?.name ?: "") }
    var kind by remember { mutableStateOf(initialHabit?.habitKind ?: HabitKind.CHECK_OFF) }
    var targetText by remember { mutableStateOf(if (initialHabit != null && initialHabit.habitKind != HabitKind.CHECK_OFF) formatValue(initialHabit.target) else "1") }
    var unit by remember { mutableStateOf(initialHabit?.unit ?: "") }
    var stepText by remember { mutableStateOf(if (initialHabit != null && initialHabit.habitKind != HabitKind.CHECK_OFF) formatValue(initialHabit.step) else "1") }

    // Advanced section state
    var isAdvancedExpanded by remember { mutableStateOf(false) }

    // Frequency state
    var frequencyType by remember { mutableStateOf(initialHabit?.frequencyType ?: "DAILY") }
    val allWeekdays = listOf("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY")
    val weekdayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
    var selectedDays by remember {
        mutableStateOf(
            if (!initialHabit?.frequencyDays.isNullOrBlank()) {
                initialHabit!!.frequencyDays.split(",").map { it.trim().uppercase() }.toSet()
            } else {
                allWeekdays.toSet()
            }
        )
    }
    var targetPerWeek by remember {
        mutableIntStateOf(if (initialHabit != null && initialHabit.frequencyTargetPerWeek > 0) initialHabit.frequencyTargetPerWeek else 3)
    }

    // Reminders state
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { _ -> }
    )
    var reminderEnabled by remember { mutableStateOf(initialHabit?.reminderEnabled ?: false) }
    var reminderTimeList by remember {
        mutableStateOf(
            if (!initialHabit?.reminderTimes.isNullOrBlank()) {
                initialHabit!!.reminderTimes.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                listOf("09:00")
            }
        )
    }

    // Dates state
    var startEpochDay by remember { mutableLongStateOf(initialHabit?.startEpochDay ?: java.time.LocalDate.now().toEpochDay()) }
    var endEpochDay by remember { mutableStateOf(initialHabit?.endEpochDay) }

    // Custom Reflex picker modal states
    var showReflexDatePickerForStart by remember { mutableStateOf(false) }
    var showReflexDatePickerForEnd by remember { mutableStateOf(false) }
    var editingReminderIndex by remember { mutableStateOf<Int?>(null) }
    var showReflexTimePickerForAdd by remember { mutableStateOf(false) }

    // Style state
    val colorPalette = listOf("#D9A184", "#8EAF9D", "#D97757", "#64748B", "#EAB308", "#A855F7")
    var selectedColorHex by remember { mutableStateOf(initialHabit?.colorHex ?: "#D9A184") }

    val iconOptions = listOf(
        "check" to Icons.Default.Check,
        "book" to Icons.Default.MenuBook,
        "fitness" to Icons.Default.FitnessCenter,
        "water" to Icons.Default.WaterDrop,
        "timer" to Icons.Default.Timer,
        "bed" to Icons.Default.Bedtime,
        "self_care" to Icons.Default.SelfImprovement,
        "code" to Icons.Default.Code
    )
    var selectedIconKey by remember { mutableStateOf(initialHabit?.iconKey ?: "check") }

    // Notes state
    var notes by remember { mutableStateOf(initialHabit?.notes ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = ReflexTokens.ShapeSheet,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (initialHabit == null) "New habit" else "Edit habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Habit name") },
                placeholder = { Text("e.g. Read books, Drink water, Workout") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = ReflexTokens.ShapeInput,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Column {
                Text(
                    text = "Habit type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HabitTypePill(
                        label = "Check-off",
                        isSelected = kind == HabitKind.CHECK_OFF,
                        onClick = { kind = HabitKind.CHECK_OFF },
                        modifier = Modifier.weight(1f)
                    )
                    HabitTypePill(
                        label = "Measurable",
                        isSelected = kind == HabitKind.MEASURABLE,
                        onClick = { kind = HabitKind.MEASURABLE },
                        modifier = Modifier.weight(1f)
                    )
                    HabitTypePill(
                        label = "Limit",
                        isSelected = kind == HabitKind.LIMIT,
                        onClick = { kind = HabitKind.LIMIT },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (kind != HabitKind.CHECK_OFF) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetText,
                        onValueChange = { targetText = it },
                        label = { Text(if (kind == HabitKind.MEASURABLE) "Target" else "Max limit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = ReflexTokens.ShapeInput,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit") },
                        placeholder = { Text("pages, min, L") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = ReflexTokens.ShapeInput,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                OutlinedTextField(
                    value = stepText,
                    onValueChange = { stepText = it },
                    label = { Text("Step increment") },
                    placeholder = { Text("1, 5, 0.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = ReflexTokens.ShapeInput,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            // ── ADVANCED / EXPANDABLE SECTION ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeCard)
                    .clickable { isAdvancedExpanded = !isAdvancedExpanded },
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                shape = ReflexTokens.ShapeCard
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Advanced options",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isAdvancedExpanded) "Tap to collapse" else "Frequency, reminders, schedule, style & notes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (isAdvancedExpanded) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isAdvancedExpanded) {
                // 1. Frequency
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitTypePill(
                            label = "Daily",
                            isSelected = frequencyType == "DAILY",
                            onClick = { frequencyType = "DAILY" },
                            modifier = Modifier.weight(1f)
                        )
                        HabitTypePill(
                            label = "Specific days",
                            isSelected = frequencyType == "SPECIFIC_DAYS",
                            onClick = { frequencyType = "SPECIFIC_DAYS" },
                            modifier = Modifier.weight(1.2f)
                        )
                        HabitTypePill(
                            label = "X / week",
                            isSelected = frequencyType == "TIMES_PER_WEEK",
                            onClick = { frequencyType = "TIMES_PER_WEEK" },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (frequencyType == "SPECIFIC_DAYS") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            allWeekdays.forEachIndexed { idx, dayKey ->
                                val isDaySelected = selectedDays.contains(dayKey)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (isDaySelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            BorderStroke(
                                                1.5.dp,
                                                if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            CircleShape
                                        )
                                        .clickable {
                                            selectedDays = if (isDaySelected) {
                                                if (selectedDays.size > 1) selectedDays - dayKey else selectedDays
                                            } else {
                                                selectedDays + dayKey
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = weekdayLabels[idx],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDaySelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else if (frequencyType == "TIMES_PER_WEEK") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..7).forEach { times ->
                                val isSel = targetPerWeek == times
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                            ),
                                            CircleShape
                                        )
                                        .clickable { targetPerWeek = times },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${times}x",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Reminders
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reminders",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = reminderEnabled,
                            onCheckedChange = { isChecked ->
                                reminderEnabled = isChecked
                                if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.POST_NOTIFICATIONS
                                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                                    ) {
                                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    if (reminderEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            reminderTimeList.forEachIndexed { idx, timeStr ->
                                val parts = timeStr.split(":")
                                val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.clickable {
                                        editingReminderIndex = idx
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = timeStr,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (reminderTimeList.size > 1) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove time",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clickable {
                                                        reminderTimeList = reminderTimeList.filterIndexed { i, _ -> i != idx }
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            if (reminderTimeList.size < 4) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable {
                                        showReflexTimePickerForAdd = true
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Add time",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Start & End Dates
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Schedule",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Start Date Tile
                        val startLocal = java.time.LocalDate.ofEpochDay(startEpochDay)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(ReflexTokens.ShapeCard)
                                .clickable {
                                    showReflexDatePickerForStart = true
                                },
                            shape = ReflexTokens.ShapeCard,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Start date",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = startLocal.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // End Date Tile
                        val endLocal = endEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(ReflexTokens.ShapeCard)
                                .clickable {
                                    showReflexDatePickerForEnd = true
                                },
                            shape = ReflexTokens.ShapeCard,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "End date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (endEpochDay != null) {
                                        Text(
                                            text = "Clear",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.clickable { endEpochDay = null }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = endLocal?.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")) ?: "No end date",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (endEpochDay != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Color & Icon
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Color & Icon",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Color row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorPalette.forEach { hex ->
                            val color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
                            val isSel = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColorHex = hex }
                                    .then(
                                        if (isSel) Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (hex == "#D9A184" || hex == "#EAB308") androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Icon row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        iconOptions.forEach { (key, vector) ->
                            val isSel = selectedIconKey == key
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                        ),
                                        CircleShape
                                    )
                                    .clickable { selectedIconKey = key },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = vector,
                                    contentDescription = null,
                                    tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Notes / Description
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Description") },
                    placeholder = { Text("Add tips, motivation, or rules for this habit...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = ReflexTokens.ShapeInput,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ReflexButton(
                text = "Save habit",
                onClick = {
                    if (name.isNotBlank()) {
                        val targetVal = targetText.toDoubleOrNull() ?: 1.0
                        val stepVal = stepText.toDoubleOrNull() ?: 1.0
                        val freqDaysStr = if (frequencyType == "SPECIFIC_DAYS") selectedDays.joinToString(",") else ""
                        val reminderTimesStr = if (reminderEnabled) reminderTimeList.joinToString(",") else ""
                        onSave(
                            name,
                            kind,
                            targetVal,
                            unit,
                            stepVal,
                            frequencyType,
                            freqDaysStr,
                            targetPerWeek,
                            reminderEnabled,
                            reminderTimesStr,
                            startEpochDay,
                            endEpochDay,
                            selectedColorHex,
                            selectedIconKey,
                            notes
                        )
                    }
                },
                variant = ReflexButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showReflexDatePickerForStart) {
        com.reflex.app.ui.components.ReflexDatePickerModal(
            initialDate = java.time.LocalDate.ofEpochDay(startEpochDay),
            title = "Select start date",
            onDismiss = { showReflexDatePickerForStart = false },
            onDateSelected = { selectedDate ->
                startEpochDay = selectedDate.toEpochDay()
                showReflexDatePickerForStart = false
            }
        )
    }

    if (showReflexDatePickerForEnd) {
        val initEnd = endEpochDay?.let { java.time.LocalDate.ofEpochDay(it) } ?: java.time.LocalDate.now().plusMonths(1)
        com.reflex.app.ui.components.ReflexDatePickerModal(
            initialDate = initEnd,
            title = "Select end date",
            onDismiss = { showReflexDatePickerForEnd = false },
            onDateSelected = { selectedDate ->
                endEpochDay = selectedDate.toEpochDay()
                showReflexDatePickerForEnd = false
            }
        )
    }

    if (editingReminderIndex != null) {
        val idx = editingReminderIndex!!
        val currentVal = reminderTimeList.getOrNull(idx) ?: "09:00"
        com.reflex.app.ui.components.ReflexTimePickerModal(
            initialTime = currentVal,
            title = "Edit reminder time",
            onDismiss = { editingReminderIndex = null },
            onTimeSelected = { newTime ->
                reminderTimeList = reminderTimeList.mapIndexed { i, t -> if (i == idx) newTime else t }
                editingReminderIndex = null
            }
        )
    }

    if (showReflexTimePickerForAdd) {
        com.reflex.app.ui.components.ReflexTimePickerModal(
            initialTime = "12:00",
            title = "Add reminder time",
            onDismiss = { showReflexTimePickerForAdd = false },
            onTimeSelected = { newTime ->
                if (!reminderTimeList.contains(newTime)) {
                    reminderTimeList = reminderTimeList + newTime
                }
                showReflexTimePickerForAdd = false
            }
        )
    }
}

@Composable
private fun HabitTypePill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// =========================================================================
// HELPERS
// =========================================================================

private fun formatValue(v: Double): String {
    return if (v % 1.0 == 0.0) {
        v.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", v)
    }
}

private fun formatHabitTypeCaption(habit: Habit): String {
    return when (habit.habitKind) {
        HabitKind.CHECK_OFF -> "Check-off"
        HabitKind.MEASURABLE -> "Measurable · ${formatValue(habit.target)} ${habit.unit}"
        HabitKind.LIMIT -> "Limit · max ${formatValue(habit.target)} ${habit.unit}"
    }
}

// =========================================================================
// PREVIEWS
// =========================================================================

@Preview(name = "Habits Screen - Main Dark", showBackground = true, backgroundColor = 0xFF0A0908)
@Composable
private fun HabitsMainDarkPreview() {
    ReflexTheme(appTheme = AppTheme.DARK) {
        HabitsScreen(onSettingsClick = {})
    }
}

@Preview(name = "Habits Screen - Main Light", showBackground = true, backgroundColor = 0xFFF7F3EE)
@Composable
private fun HabitsMainLightPreview() {
    ReflexTheme(appTheme = AppTheme.LIGHT) {
        HabitsScreen(onSettingsClick = {})
    }
}

@Preview(name = "Habits Empty State - Dark", showBackground = true, backgroundColor = 0xFF0A0908)
@Composable
private fun HabitsEmptyDarkPreview() {
    ReflexTheme(appTheme = AppTheme.DARK) {
        Box(modifier = Modifier.padding(16.dp)) {
            ZeroHabitsEmptyCard(onAddHabitClick = {})
        }
    }
}

@Preview(name = "Habits Empty State - Light", showBackground = true, backgroundColor = 0xFFF7F3EE)
@Composable
private fun HabitsEmptyLightPreview() {
    ReflexTheme(appTheme = AppTheme.LIGHT) {
        Box(modifier = Modifier.padding(16.dp)) {
            ZeroHabitsEmptyCard(onAddHabitClick = {})
        }
    }
}

@Preview(name = "Selected Day Sheet - Dark", showBackground = true, backgroundColor = 0xFF141211)
@Composable
private fun SelectedDaySheetDarkPreview() {
    ReflexTheme(appTheme = AppTheme.DARK) {
        SelectedDaySheet(
            date = LocalDate.now(),
            habits = listOf(
                Habit(id = 1L, name = "Workout", type = HabitKind.CHECK_OFF.name, target = 1.0, startEpochDay = 0L),
                Habit(id = 2L, name = "Reading", type = HabitKind.MEASURABLE.name, target = 20.0, unit = "pages", startEpochDay = 0L)
            ),
            logs = mapOf(Pair(1L, LocalDate.now().toEpochDay()) to 1.0),
            onDismiss = {},
            onToggleCheckOff = {},
            onStepHabit = { _, _ -> }
        )
    }
}

@Preview(name = "Manage Habits Sheet - Dark", showBackground = true, backgroundColor = 0xFF141211)
@Composable
private fun ManageHabitsSheetDarkPreview() {
    ReflexTheme(appTheme = AppTheme.DARK) {
        ManageHabitsSheet(
            habits = listOf(
                Habit(id = 1L, name = "Workout", type = HabitKind.CHECK_OFF.name, target = 1.0, startEpochDay = 0L),
                Habit(id = 2L, name = "Reading", type = HabitKind.MEASURABLE.name, target = 20.0, unit = "pages", startEpochDay = 0L)
            ),
            onDismiss = {},
            onEditHabit = {},
            onDeleteHabit = {},
            onMoveHabit = { _, _ -> }
        )
    }
}
