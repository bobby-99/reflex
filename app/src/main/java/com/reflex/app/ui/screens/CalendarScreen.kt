package com.reflex.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.PaddingValues
import com.reflex.app.data.Priority
import com.reflex.app.data.RoutineIcon
import com.reflex.app.data.Task
import com.reflex.app.ui.components.AddEventSheet
import com.reflex.app.ui.components.DeleteConfirmationDialog
import com.reflex.app.ui.components.DeviceEventDetailSheet
import com.reflex.app.ui.components.QuickAddCard
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.TaskEditSheet
import com.reflex.app.ui.components.rememberQuickAddState
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperOnContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.eventBlue
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.routineGreen
import com.reflex.app.util.AgendaDaySection
import com.reflex.app.util.AgendaItem
import com.reflex.app.util.CalendarChipFilter
import com.reflex.app.util.CalendarItemType
import com.reflex.app.util.DeviceCalendarEvent
import com.reflex.app.util.GridDay
import com.reflex.app.viewmodel.CalendarViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

private enum class DragDirection {
    HORIZONTAL,
    VERTICAL
}

@OptIn(FlowPreview::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToRoutine: ((Long) -> Unit)? = null
) {
    SetStatusBarAppearance()

    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val today = remember { LocalDate.now() }

    // Sheets & Dialog state
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskPendingDelete by remember { mutableStateOf<Task?>(null) }
    var selectedDeviceEvent by remember { mutableStateOf<DeviceCalendarEvent?>(null) }
    var isQuickAddOpen by remember { mutableStateOf(false) }
    var quickAddInitialDate by remember { mutableStateOf<LocalDate?>(null) }
    var isAddEventOpen by remember { mutableStateOf(false) }
    var addEventInitialDate by remember { mutableStateOf<LocalDate>(LocalDate.now()) }
    val quickAddState = rememberQuickAddState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Banner visibility
    var showPermissionBanner by remember { mutableStateOf(!state.hasCalendarPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.checkAndLoadCalendarPermission(context)
        if (isGranted) {
            showPermissionBanner = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.checkAndLoadCalendarPermission(context)
    }

    // Scroll state & Programmatic feedback-loop guard
    val listState = rememberLazyListState()
    var isProgrammaticScroll by remember { mutableStateOf(false) }
    var pendingScrollDate by remember { mutableStateOf<LocalDate?>(null) }

    // Synchronize agenda scrolling when selectedDate changes programmatically
    fun scrollToDate(date: LocalDate) {
        pendingScrollDate = date
    }

    LaunchedEffect(pendingScrollDate, state.agendaSections) {
        val target = pendingScrollDate ?: return@LaunchedEffect
        val targetIndex = state.agendaSections.indexOfFirst { it.date == target }
        if (targetIndex >= 0) {
            isProgrammaticScroll = true
            listState.animateScrollToItem(index = targetIndex, scrollOffset = 0)
            pendingScrollDate = null
            delay(500)
            isProgrammaticScroll = false
        }
    }

    // Sync topmost visible day in agenda to calendar selection (debounced ~150ms)
    // ONLY when user is actively scrolling the agenda list with touch gestures!
    val topmostVisibleDate by remember {
        derivedStateOf {
            val visibleIndex = listState.firstVisibleItemIndex
            state.agendaSections.getOrNull(visibleIndex)?.date
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            if (listState.isScrollInProgress && !isProgrammaticScroll && pendingScrollDate == null) {
                topmostVisibleDate
            } else {
                null
            }
        }
        .filterNotNull()
        .distinctUntilChanged()
        .debounce(150)
        .collectLatest { date ->
            if (date != state.selectedDate && listState.isScrollInProgress && !isProgrammaticScroll) {
                viewModel.setSelectedDate(date, context)
            }
        }
    }

    // Root is Box(fillMaxSize()).
    // Z-order from back to front: screen content, scrim, QuickAddCard, ReflexBottomNavBar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── TOP BAR (PINNED) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .reflexStatusBarPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Calendar",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // "+ Event" pill button (44dp tall)
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(CopperContainer)
                            .clickable {
                                addEventInitialDate = state.selectedDate
                                isAddEventOpen = true
                            }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = CopperPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Event",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = CopperPrimary
                            )
                        }
                    }

                    // "Today" pill button (44dp tall, 20dp horizontal padding)
                    val isTodaySelected = state.selectedDate == today
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(if (isTodaySelected) CopperPrimary else CopperContainer)
                            .clickable {
                                viewModel.selectToday()
                                scrollToDate(today)
                            }
                            .padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isTodaySelected) OnCopper else CopperPrimary
                        )
                    }

                    // Settings button
                    com.reflex.app.ui.components.SettingsTopBarButton(
                        onClick = onNavigateToSettings,
                        contentDescriptionText = "Calendar settings"
                    )
                }
            }

            // ── STICKY CALENDAR CARD (Month/Week morphing grid) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                CalendarCard(
                    state = state,
                    onToggleExpand = { viewModel.toggleMonthGridExpanded() },
                    onPrev = {
                        val targetDate = if (state.isMonthGridExpanded) {
                            viewModel.previousMonth(context)
                        } else {
                            viewModel.previousWeek()
                        }
                        scrollToDate(targetDate)
                    },
                    onNext = {
                        val targetDate = if (state.isMonthGridExpanded) {
                            viewModel.nextMonth(context)
                        } else {
                            viewModel.nextWeek()
                        }
                        scrollToDate(targetDate)
                    },
                    onDateSelected = { date ->
                        viewModel.setSelectedDate(date, context)
                        scrollToDate(date)
                    },
                    onExpandStateChange = { expanded ->
                        viewModel.setMonthGridExpanded(expanded)
                    }
                )
            }

            // ── FILTER CHIPS ROW (All, Events, Tasks, Routines) ──
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(CalendarChipFilter.entries) { filter ->
                    val isSelected = state.chipFilter == filter
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(if (isSelected) CopperContainer else MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                BorderStroke(
                                    ReflexTokens.BorderHairline,
                                    if (isSelected) CopperPrimary else outlineColor
                                ),
                                ReflexTokens.ShapeChip
                            )
                            .clickable { viewModel.setChipFilter(filter) }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── DISCREET PERMISSION BANNER ──
            if (!state.hasCalendarPermission && showPermissionBanner) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = CopperPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Connect device calendar",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = CopperPrimary,
                                modifier = Modifier.clickable {
                                    permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                                }
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { showPermissionBanner = false }
                        )
                    }
                }
            }

            // ── AGENDA TIMELINE LIST (LazyColumn scrolling under calendar card) ──
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (state.agendaSections.isEmpty()) {
                    // Empty state card if entire agenda has no items
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ReflexCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            borderColor = outlineColor,
                            contentPadding = PaddingValues(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "No upcoming items",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Your calendar is clear. Add a task or connect your device calendars.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .clip(ReflexTokens.ShapeChip)
                                            .background(CopperPrimary)
                                            .clickable {
                                                quickAddInitialDate = state.selectedDate
                                                isQuickAddOpen = true
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+ Add task",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = OnCopper
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .clip(ReflexTokens.ShapeChip)
                                            .background(CopperContainer)
                                            .clickable {
                                                addEventInitialDate = state.selectedDate
                                                isAddEventOpen = true
                                            }
                                            .padding(horizontal = 14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "+ Add event",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = CopperPrimary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(40.dp)
                                            .clip(ReflexTokens.ShapeChip)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), ReflexTokens.ShapeChip)
                                            .clickable(onClick = onNavigateToSettings)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Settings",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        items(state.agendaSections, key = { it.date.toString() }) { section ->
                            AgendaSectionBlock(
                                section = section,
                                isToday = section.date == today,
                                isSelected = section.date == state.selectedDate,
                                nowMinutes = state.nowMinutes,
                                is24Hour = state.is24Hour,
                                isLastSection = section == state.agendaSections.lastOrNull(),
                                onAddTask = {
                                    quickAddInitialDate = section.date
                                    isQuickAddOpen = true
                                },
                                onAddEvent = {
                                    addEventInitialDate = section.date
                                    isAddEventOpen = true
                                },
                                onToggleTask = { task ->
                                    val wasCompleted = task.isCompleted
                                    viewModel.toggleTaskComplete(task, context)
                                    if (!wasCompleted && state.preferences.excludeCompletedTasks) {
                                        coroutineScope.launch {
                                            val res = snackbarHostState.showSnackbar(
                                                message = "Task completed",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            if (res == SnackbarResult.ActionPerformed) {
                                                viewModel.toggleTaskComplete(task, context)
                                            }
                                        }
                                    }
                                },
                                onOpenTask = { task -> editingTask = task },
                                onOpenRoutine = { routine -> onNavigateToRoutine?.invoke(routine.id) },
                                onOpenEvent = { event -> selectedDeviceEvent = event }
                            )
                        }

                        // 112dp bottom clearance + navigationBarsPadding so last row clears floating tab bar
                        item {
                            Spacer(
                                modifier = Modifier
                                    .navigationBarsPadding()
                                    .height(112.dp)
                            )
                        }
                    }
                }
            }
        }

        // Floating Snackbar host for 5s task completion undo
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 104.dp)
        )

        // QuickAddCard overlay (floats above content, under nav bar in Z-order)
        QuickAddCard(
            isOpen = isQuickAddOpen,
            onDismiss = { isQuickAddOpen = false },
            initialDate = quickAddInitialDate ?: state.selectedDate,
            onAddTask = { text ->
                viewModel.addTaskFromNaturalLanguage(text, context, quickAddInitialDate ?: state.selectedDate)
                isQuickAddOpen = false
            },
            onMicClick = {},
            state = quickAddState
        )
    }

    // Modal Edit Sheet
    editingTask?.let { task ->
        TaskEditSheet(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { updated ->
                viewModel.updateTask(updated, context)
                editingTask = null
            },
            onDelete = { deleted ->
                taskPendingDelete = deleted
                editingTask = null
            }
        )
    }

    // Delete Confirmation Dialog
    taskPendingDelete?.let { task ->
        DeleteConfirmationDialog(
            taskTitle = task.title,
            onConfirm = {
                viewModel.deleteTask(task, context)
                taskPendingDelete = null
            },
            onDismiss = { taskPendingDelete = null }
        )
    }

    // Device Calendar Event Detail Modal Sheet
    selectedDeviceEvent?.let { event ->
        DeviceEventDetailSheet(
            event = event,
            onDismiss = { selectedDeviceEvent = null }
        )
    }

    // Google Calendar Type Event Creation Sheet
    if (isAddEventOpen) {
        AddEventSheet(
            initialDate = addEventInitialDate,
            onDismiss = { isAddEventOpen = false },
            onEventCreated = {
                viewModel.loadDeviceCalendarEvents(context, force = true)
                viewModel.checkAndLoadCalendarPermission(context)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CALENDAR CARD (Month/Week morphing single grid)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun CalendarCard(
    state: com.reflex.app.viewmodel.CalendarState,
    onToggleExpand: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onExpandStateChange: (Boolean) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val chevronRotation by animateFloatAsState(
        targetValue = if (state.isMonthGridExpanded) 180f else 0f,
        animationSpec = tween(300),
        label = "ChevronRotation"
    )

    val weekdayHeaders = remember(state.preferences.firstDayOfWeek) {
        val days = DayOfWeek.entries
        val startIndex = days.indexOf(state.preferences.firstDayOfWeek)
        (0..6).map { days[(startIndex + it) % 7].getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(2) }
    }

    // Single Morphing Grid animation parameters:
    // Transition animates height and vertical offset together over 320ms with CubicBezierEasing(0.2, 0.8, 0.2, 1)
    val numRows = (state.gridDays.size / 7).coerceAtLeast(1)
    val targetHeight = if (state.isMonthGridExpanded) (numRows * 56).dp else 56.dp
    val targetOffsetY = if (state.isMonthGridExpanded) 0.dp else (-state.selectedRowIndex * 56).dp

    val morphAnimationSpec = tween<androidx.compose.ui.unit.Dp>(
        durationMillis = 320,
        easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = morphAnimationSpec,
        label = "CalendarHeightMorph"
    )
    val animatedOffsetY by animateDpAsState(
        targetValue = targetOffsetY,
        animationSpec = morphAnimationSpec,
        label = "CalendarOffsetMorph"
    )

    // Direction-locked gesture state for the card
    var dragDirectionLocked by remember { mutableStateOf<DragDirection?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, outlineColor), RoundedCornerShape(32.dp))
            .pointerInput(state.isMonthGridExpanded) {
                detectDragGestures(
                    onDragStart = {
                        dragDirectionLocked = null
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        if (dragDirectionLocked == DragDirection.VERTICAL) {
                            if (totalDragY > 40f && !state.isMonthGridExpanded) {
                                onExpandStateChange(true)
                            } else if (totalDragY < -40f && state.isMonthGridExpanded) {
                                onExpandStateChange(false)
                            }
                        } else if (dragDirectionLocked == DragDirection.HORIZONTAL) {
                            if (totalDragX > 50f) {
                                onPrev()
                            } else if (totalDragX < -50f) {
                                onNext()
                            }
                        }
                        dragDirectionLocked = null
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        dragDirectionLocked = null
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        if (dragDirectionLocked == null) {
                            if (abs(totalDragX) > 16f || abs(totalDragY) > 16f) {
                                dragDirectionLocked = if (abs(totalDragX) > abs(totalDragY)) {
                                    DragDirection.HORIZONTAL
                                } else {
                                    DragDirection.VERTICAL
                                }
                            }
                        }
                    }
                )
            }
            .padding(top = 14.dp, start = 12.dp, end = 12.dp, bottom = 6.dp)
    ) {
        // Header Row: Prev / Next buttons and Center Title with 18dp rotating chevron
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prev button: 40dp circle, 48dp minimum touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onPrev),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Center: "{Month} {year}" in titleMedium (16sp SemiBold) + rotating copper chevron
            Row(
                modifier = Modifier
                    .clip(ReflexTokens.ShapeChip)
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = state.currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (state.isMonthGridExpanded) "Collapse month" else "Expand month",
                    tint = CopperPrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(chevronRotation)
                )
            }

            // Next button: 40dp circle, 48dp minimum touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onNext),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Weekday Headers: 7 columns, 13sp SemiBold, secondary text
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            weekdayHeaders.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Morphing Grid: single composable that morphs between modes.
        // Week mode clips the grid to one row (56dp) and translates to the selected date's row.
        // Month mode expands to all rows (numRows * 56dp).
        // Height and vertical offset animate together over 320ms with CubicBezierEasing(0.2, 0.8, 0.2, 1).
        Layout(
            content = {
                val chunkedRows = state.gridDays.chunked(7)
                chunkedRows.forEach { rowDays ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        rowDays.forEach { day ->
                            DayGridCell(
                                day = day,
                                isMonthMode = state.isMonthGridExpanded,
                                onClick = { onDateSelected(day.date) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(animatedHeight)
                .clipToBounds()
        ) { measurables, constraints ->
            val rowPlaceables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
            val rowHeightPx = 56.dp.roundToPx()
            val offsetYPx = animatedOffsetY.roundToPx()

            layout(constraints.maxWidth, animatedHeight.roundToPx()) {
                rowPlaceables.forEachIndexed { index, placeable ->
                    val y = index * rowHeightPx + offsetYPx
                    placeable.placeRelative(0, y)
                }
            }
        }

        // Grab handle pill at bottom of card: 40x5dp, 48dp touch target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clickable(onClick = onToggleExpand),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(outlineColor)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DAY GRID CELL (56dp tall, 40dp circle, up to 3 dots)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DayGridCell(
    day: GridDay,
    isMonthMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val isDimmed = isMonthMode && !day.isCurrentMonth

    Column(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = day.talkBackDescription },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // 40dp circle for day number
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (day.isSelected) CopperPrimary else Color.Transparent
                )
                .then(
                    if (!day.isSelected && day.isToday) {
                        Modifier.border(BorderStroke(1.5.dp, CopperPrimary), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isSelected || day.isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    day.isSelected -> OnCopper
                    day.isToday -> CopperPrimary
                    isDimmed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        Spacer(Modifier.height(3.dp))

        // Up to 3 dots: Event (EventBlue), Task (copper), Routine (RoutineGreen)
        Row(
            modifier = Modifier.height(5.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            day.dots.forEach { dotType ->
                val dotColor = when (dotType) {
                    CalendarItemType.EVENT -> MaterialTheme.colorScheme.eventBlue
                    CalendarItemType.TASK -> CopperPrimary
                    CalendarItemType.ROUTINE -> MaterialTheme.colorScheme.routineGreen
                }
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isDimmed) dotColor.copy(alpha = 0.35f) else dotColor)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AGENDA SECTION BLOCK (Timeline spine, day header, items, now divider)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgendaSectionBlock(
    section: AgendaDaySection,
    isToday: Boolean,
    isSelected: Boolean,
    nowMinutes: Int,
    is24Hour: Boolean,
    isLastSection: Boolean = false,
    onAddTask: () -> Unit,
    onAddEvent: () -> Unit,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenRoutine: (com.reflex.app.data.Routine) -> Unit,
    onOpenEvent: (DeviceCalendarEvent) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val date = section.date

    // Format section header date
    val formattedTitle = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault()))
    val countText = "${section.items.size} ${if (section.items.size == 1) "item" else "items"}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val strokeWidthPx = 1.5.dp.toPx()
                val x = 20.dp.toPx() // center of 40dp date circle
                val startY = 44.dp.toPx() // starts 4dp below 40dp date circle
                val endY = if (isLastSection) size.height else size.height + 18.dp.toPx()
                if (endY > startY) {
                    drawLine(
                        color = outlineVariant,
                        start = Offset(x, startY),
                        end = Offset(x, endY),
                        strokeWidth = strokeWidthPx
                    )
                }
            }
            .padding(top = 18.dp)
    ) {
        // Day Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left 40dp date circle node (timeline spine passes under its center at x=20dp)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSelected -> CopperPrimary
                            isToday -> Color.Transparent
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            when {
                                isSelected -> CopperPrimary
                                isToday -> CopperPrimary
                                else -> outlineColor
                            }
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isSelected -> OnCopper
                        isToday -> CopperPrimary
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }

            Spacer(Modifier.width(12.dp))

            // Middle: Title + Today badge + Items count
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (isToday) {
                        Box(
                            modifier = Modifier
                                .clip(ReflexTokens.ShapeChip)
                                .background(CopperContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = CopperOnContainer
                            )
                        }
                    }
                }

                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Right: "+ Event" and "+ Task" pill buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(ReflexTokens.ShapeChip)
                        .background(CopperContainer)
                        .clickable(onClick = onAddEvent)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = CopperPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Event",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CopperPrimary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .height(30.dp)
                        .clip(ReflexTokens.ShapeChip)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), ReflexTokens.ShapeChip)
                        .clickable(onClick = onAddTask)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Task",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Items list indented by 52dp so it clears timeline
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (section.items.isEmpty()) {
                // Empty selected day dashed card or empty indicator
                DashedEmptyCard(
                    isToday = isToday,
                    nowMinutes = nowMinutes,
                    is24Hour = is24Hour,
                    onAddTask = onAddTask,
                    onAddEvent = onAddEvent
                )
            } else {
                section.items.forEachIndexed { index, item ->
                    // "Now" indicator inserted chronologically before item if index == nowDividerIndex
                    if (isToday && section.nowDividerIndex == index) {
                        NowDivider(nowMinutes = nowMinutes, is24Hour = is24Hour)
                    }

                    AgendaItemCard(
                        item = item,
                        onToggleTask = onToggleTask,
                        onOpenTask = onOpenTask,
                        onOpenRoutine = onOpenRoutine,
                        onOpenEvent = onOpenEvent
                    )
                }

                // If "Now" indicator is at the very end of today's items
                if (isToday && section.nowDividerIndex == section.items.size) {
                    NowDivider(nowMinutes = nowMinutes, is24Hour = is24Hour)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NOW DIVIDER (9dp dot on timeline, "Now {time}" bold copper label)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun NowDivider(nowMinutes: Int, is24Hour: Boolean) {
    val timeStr = com.reflex.app.util.CalendarCalculations.formatMinutes(nowMinutes, is24Hour)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 9dp dot centered on the 20dp timeline line.
        // Parent column has padding(start = 52.dp), so timeline center is at 20 - 52 = -32.dp.
        // With dot size 9dp, offset = -32dp - 4.5dp = -36.5dp.
        Box(
            modifier = Modifier
                .offset(x = (-36.5).dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(CopperPrimary)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Now · $timeStr",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CopperPrimary
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(CopperPrimary.copy(alpha = 0.5f))
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHED EMPTY DAY CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DashedEmptyCard(
    isToday: Boolean,
    nowMinutes: Int,
    is24Hour: Boolean,
    onAddTask: () -> Unit,
    onAddEvent: () -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (isToday) {
            NowDivider(nowMinutes = nowMinutes, is24Hour = is24Hour)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    BorderStroke(1.5.dp, outlineColor),
                    RoundedCornerShape(26.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "No items scheduled for this day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(CopperContainer)
                            .clickable(onClick = onAddEvent)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Event",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CopperPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(30.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(CopperPrimary)
                            .clickable(onClick = onAddTask)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+ Task",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OnCopper
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AGENDA ITEM CARD (26dp radius, 44dp icon tile, 12x14 padding, checkbox for tasks)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun AgendaItemCard(
    item: AgendaItem,
    onToggleTask: (Task) -> Unit,
    onOpenTask: (Task) -> Unit,
    onOpenRoutine: (com.reflex.app.data.Routine) -> Unit,
    onOpenEvent: (DeviceCalendarEvent) -> Unit
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val typeColor = when (item.type) {
        CalendarItemType.EVENT -> MaterialTheme.colorScheme.eventBlue
        CalendarItemType.TASK -> CopperPrimary
        CalendarItemType.ROUTINE -> MaterialTheme.colorScheme.routineGreen
    }

    val tileBg = when (item.type) {
        CalendarItemType.EVENT -> MaterialTheme.colorScheme.eventBlue.copy(alpha = 0.18f)
        CalendarItemType.TASK -> CopperContainer
        CalendarItemType.ROUTINE -> MaterialTheme.colorScheme.routineGreen.copy(alpha = 0.18f)
    }

    val isDone = item.isCompleted

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(1.dp, outlineColor), RoundedCornerShape(26.dp))
            .clickable {
                when (item.type) {
                    CalendarItemType.TASK -> item.taskRef?.let { onOpenTask(it) }
                    CalendarItemType.ROUTINE -> item.routineRef?.let { onOpenRoutine(it) }
                    CalendarItemType.EVENT -> item.eventRef?.let { onOpenEvent(it) }
                }
            }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 44dp rounded-square icon tile (16dp radius), tinted at 18% opacity of type color
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(tileBg),
            contentAlignment = Alignment.Center
        ) {
            when (item.type) {
                CalendarItemType.EVENT -> {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                CalendarItemType.TASK -> {
                    Icon(
                        imageVector = Icons.Default.Checklist,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                CalendarItemType.ROUTINE -> {
                    val vectorIcon = item.routineRef?.let { RoutineIcon.fromKey(it.icon).icon } ?: Icons.Default.PlayArrow
                    Icon(
                        imageVector = vectorIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Middle Content (Time, Title, Subtitle)
        Column(
            modifier = Modifier
                .weight(1f)
                .then(if (isDone) Modifier.padding(end = 4.dp) else Modifier)
        ) {
            // Time line in type color (13sp SemiBold)
            Text(
                text = item.timeLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = typeColor
            )

            // Title (16sp SemiBold, onSurface)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Subtitle (13sp, secondary text)
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Circular Checkbox for Tasks (26dp diameter, 2dp border, 48dp touch target)
        if (item.type == CalendarItemType.TASK && item.taskRef != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onToggleTask(item.taskRef) },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDone) CopperPrimary else Color.Transparent)
                        .border(
                            BorderStroke(2.dp, if (isDone) CopperPrimary else outlineColor),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = OnCopper,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
