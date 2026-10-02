package com.reflex.app.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Task
import com.reflex.app.ui.components.DeleteConfirmationDialog
import com.reflex.app.ui.components.QuickAddCard
import com.reflex.app.ui.components.QuickAddState
import com.reflex.app.ui.components.TaskEditSheet
import com.reflex.app.ui.components.rememberQuickAddState
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.ActionPillWhite
import com.reflex.app.ui.theme.DarkDestructiveRed
import com.reflex.app.ui.theme.DarkTertiaryText
import com.reflex.app.ui.theme.LightDestructiveRed
import com.reflex.app.ui.theme.LightTertiaryText
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTheme
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.viewmodel.TasksViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class TaskFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    UPCOMING("Upcoming"),
    NO_DATE("No date"),
    COMPLETED("Completed")
}

/**
 * Tasks Screen — Reflex Design System v1.0 / reflex-tasks.html.
 *
 * Replicates the visual design, sizing, typography, tokens and interactive state machine:
 * - 1. Pinned status bar gap + Top row: "Tasks" 28sp Bold + 44dp circular Settings gear button.
 * - 2. Date line: today as "Wednesday, September 30" (13sp secondary).
 * - 3. Progress card: Surface, 1dp Border, 26dp radius, padding 14x16dp. "{done} of {total} done today" + "{pct}%",
 *      and animated 8dp full-pill progress bar. If none: "Nothing due today" + "Enjoy the space".
 * - 4. Filter chips row (stickyHeader): full bleed, 40dp tall chips (All, Today, Upcoming, No date, Completed) with counts.
 * - 5. Sections: Overdue (needs attention, red), Today, Upcoming, No date, and Completed toggle (All filter).
 *      Sorted by due date ascending (no date last), then time ascending (no time last).
 * - 6. Task row: Surface, 1dp Border, 26dp radius, 28dp checkbox (priority border color), title 16sp SemiBold,
 *      meta line (date label, time, priority tag, repeat icon), 0.55 alpha and strikethrough when completed.
 * - 7. Completion interaction: instantaneous check, strikethrough & dim, 380ms delay before leaving section via animateItem(),
 *      and animated Undo pill at bottom center auto-hiding after 3.5s.
 * - 8. Fixed round "+" button: 60dp circle, copper fill, OnCopper "+" icon, 7dp halo, 14dp Y offset shadow,
 *      floats at bottom-end (bottom = 128dp + navBarInset, end = 28dp), opens QuickAddCard.
 * - 9. Bottom fade: 170dp + navBarInset vertical gradient (transparent to background reaching 100% at 55%).
 * - 10. Empty state: 72dp circle with 32dp check icon, "All clear" / "Nothing completed yet".
 */
@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
    isQuickAddOpen: Boolean = false,
    onQuickAddOpenChange: (Boolean) -> Unit = {},
    quickAddState: QuickAddState = rememberQuickAddState(),
    quickAddText: String = "",
    onQuickAddTextChange: (String) -> Unit = {},
    onQuickAddSubmit: () -> Unit = {},
    onCalendarClick: (() -> Unit)? = null,
    onSettingsClick: () -> Unit,
    scrollToTopTrigger: Int = 0
) {
    SetStatusBarAppearance()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.tasksState.collectAsState()

    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }
    var completedExpanded by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskPendingDelete by remember { mutableStateOf<Task?>(null) }

    // List state with animated scroll-to-top trigger from bottom bar center button
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

    // Local transient completion state map: taskId -> isCompleted (supports immediate UI update + 380ms delay)
    val transientCompleted = remember { mutableStateMapOf<Long, Boolean>() }
    var undoMessage by remember { mutableStateOf("") }
    var undoAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var undoPillVisible by remember { mutableStateOf(false) }
    var undoDismissJob by remember { mutableStateOf<Job?>(null) }

    // Today's date calculations
    val today = remember { LocalDate.now() }
    val todayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    val todaySubtitleFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }

    // Combine all tasks (pending + completed)
    val allTasks = remember(state) {
        state.todayTasks + state.tomorrowTasks + state.upcomingTasks + state.noDateTasks + state.completedTasks
    }

    // Task date offset helper
    val getDaysOffset: (Task) -> Long? = remember(today) {
        { task ->
            task.dueDate?.let { epoch ->
                val date = Instant.ofEpochMilli(epoch).atZone(ZoneId.systemDefault()).toLocalDate()
                ChronoUnit.DAYS.between(today, date)
            }
        }
    }

    // Comparator: due date ascending (no date last), then time ascending (no time last)
    val taskComparator = remember {
        Comparator<Task> { a, b ->
            val dateA = a.dueDate ?: Long.MAX_VALUE
            val dateB = b.dueDate ?: Long.MAX_VALUE
            val dateCmp = dateA.compareTo(dateB)
            if (dateCmp != 0) return@Comparator dateCmp

            val timeA = a.dueTime ?: Long.MAX_VALUE
            val timeB = b.dueTime ?: Long.MAX_VALUE
            val timeCmp = timeA.compareTo(timeB)
            if (timeCmp != 0) return@Comparator timeCmp

            a.id.compareTo(b.id)
        }
    }

    // Partition tasks based on database state + transient states
    // A task is pending if (!task.isCompleted && transientCompleted[task.id] != true) OR (task.isCompleted && transientCompleted[task.id] == false)
    val isTaskEffectiveCompleted: (Task) -> Boolean = { task ->
        transientCompleted[task.id] ?: task.isCompleted
    }

    val pendingTasks = remember(allTasks, transientCompleted.toMap()) {
        allTasks.filter { !isTaskEffectiveCompleted(it) }
    }
    val doneTasks = remember(allTasks, transientCompleted.toMap()) {
        allTasks.filter { isTaskEffectiveCompleted(it) }.sortedWith(taskComparator)
    }

    val overdueTasks = remember(pendingTasks, today) {
        pendingTasks.filter {
            val offset = getDaysOffset(it)
            offset != null && offset < 0
        }.sortedWith(taskComparator)
    }

    val todayTasks = remember(pendingTasks, today) {
        pendingTasks.filter {
            val offset = getDaysOffset(it)
            offset != null && offset == 0L
        }.sortedWith(taskComparator)
    }

    val upcomingTasks = remember(pendingTasks, today) {
        pendingTasks.filter {
            val offset = getDaysOffset(it)
            offset != null && offset > 0L
        }.sortedWith(taskComparator)
    }

    val noDateTasks = remember(pendingTasks) {
        pendingTasks.filter { it.dueDate == null }.sortedWith(taskComparator)
    }

    // Progress card calculation: tasks whose due date is today (done or not)
    val todayAllTasks = remember(allTasks, today) {
        allTasks.filter {
            val offset = getDaysOffset(it)
            offset != null && offset == 0L
        }
    }
    val doneTodayCount = remember(todayAllTasks, transientCompleted.toMap()) {
        todayAllTasks.count { isTaskEffectiveCompleted(it) }
    }
    val totalTodayCount = todayAllTasks.size
    val todayProgressPct = if (totalTodayCount > 0) (doneTodayCount * 100 / totalTodayCount) else 0
    val todayProgressFraction = if (totalTodayCount > 0) (doneTodayCount.toFloat() / totalTodayCount.toFloat()) else 0f

    // Filter counts
    val allCount = pendingTasks.size
    val todayChipCount = overdueTasks.size + todayTasks.size
    val upcomingChipCount = upcomingTasks.size
    val noDateChipCount = noDateTasks.size
    val completedChipCount = doneTasks.size

    // Voice recognition launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        quickAddState.isListening = false
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                quickAddState.text = spokenText
                onQuickAddOpenChange(true)
            }
        }
    }

    val launchVoiceInput = {
        quickAddState.isListening = true
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your task...")
            }
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            quickAddState.isListening = false
            Toast.makeText(context, "Voice recognition not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    // Bottom insets calculation
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Task toggle handler: immediate visual toggle + 380ms delay before committing to DB
    val handleToggleComplete: (Task) -> Unit = { task ->
        val currentEffective = isTaskEffectiveCompleted(task)
        val targetCompleted = !currentEffective

        transientCompleted[task.id] = targetCompleted

        // Show undo pill
        undoDismissJob?.cancel()
        undoMessage = if (targetCompleted) "Task completed" else "Moved back to pending"
        undoAction = {
            // Revert
            transientCompleted[task.id] = currentEffective
            undoPillVisible = false
            coroutineScope.launch {
                viewModel.toggleTaskComplete(task, context)
            }
        }
        undoPillVisible = true
        undoDismissJob = coroutineScope.launch {
            delay(3500)
            undoPillVisible = false
        }

        // Commit change after 380ms delay so user sees check/strikethrough before row leaves section
        coroutineScope.launch {
            delay(380)
            if (transientCompleted[task.id] == targetCompleted) {
                viewModel.toggleTaskComplete(task, context)
                transientCompleted.remove(task.id)
            }
        }
    }

    // Root Box: Z-order back to front: list content -> bottom fade -> + button -> Undo pill -> QuickAddCard
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Main Scrollable List
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = ReflexTokens.TasksBottomContentPadding + navBarBottom
            )
        ) {
            // Top Bar & Date Header
            item(key = "tasks_top_bar") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .reflexStatusBarPadding()
                        .padding(horizontal = 16.dp)
                ) {
                    // Top Row: "Tasks" (28sp Bold) and 44dp circular Settings gear button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tasks",
                            fontFamily = Lora,
                            fontSize = 28.sp,
                            lineHeight = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        com.reflex.app.ui.components.SettingsTopBarButton(
                            onClick = onSettingsClick
                        )
                    }

                    // Date line: today as "Wednesday, September 30" (13sp secondary)
                    Text(
                        text = today.format(todayFormatter),
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )
                }
            }

            // Progress Card
            item(key = "tasks_progress_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    TasksProgressCard(
                        totalCount = totalTodayCount,
                        doneCount = doneTodayCount,
                        pct = todayProgressPct,
                        fraction = todayProgressFraction
                    )
                }
            }

            // Sticky Filter Chips Row
            stickyHeader(key = "tasks_sticky_chips") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(top = 12.dp, bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TasksFilterChip(
                            label = "All",
                            count = allCount,
                            isSelected = selectedFilter == TaskFilter.ALL,
                            onClick = { selectedFilter = TaskFilter.ALL }
                        )
                        TasksFilterChip(
                            label = "Today",
                            count = todayChipCount,
                            isSelected = selectedFilter == TaskFilter.TODAY,
                            onClick = { selectedFilter = TaskFilter.TODAY }
                        )
                        TasksFilterChip(
                            label = "Upcoming",
                            count = upcomingChipCount,
                            isSelected = selectedFilter == TaskFilter.UPCOMING,
                            onClick = { selectedFilter = TaskFilter.UPCOMING }
                        )
                        TasksFilterChip(
                            label = "No date",
                            count = noDateChipCount,
                            isSelected = selectedFilter == TaskFilter.NO_DATE,
                            onClick = { selectedFilter = TaskFilter.NO_DATE }
                        )
                        TasksFilterChip(
                            label = "Completed",
                            count = completedChipCount,
                            isSelected = selectedFilter == TaskFilter.COMPLETED,
                            onClick = { selectedFilter = TaskFilter.COMPLETED }
                        )
                    }
                }
            }

            // Filtered Sections
            // Section 1: Overdue (in ALL or TODAY filters)
            if ((selectedFilter == TaskFilter.ALL || selectedFilter == TaskFilter.TODAY) && overdueTasks.isNotEmpty()) {
                item(key = "header_overdue") {
                    TasksSectionHeader(
                        title = "Overdue",
                        subtitle = "needs attention",
                        isDestructive = true,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(overdueTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TaskRow(
                            task = task,
                            isDone = isTaskEffectiveCompleted(task),
                            showDate = true,
                            onToggleComplete = { handleToggleComplete(task) },
                            onClick = { editingTask = task },
                            onDelete = { taskPendingDelete = task },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Section 2: Today (in ALL or TODAY filters)
            if ((selectedFilter == TaskFilter.ALL || selectedFilter == TaskFilter.TODAY) && todayTasks.isNotEmpty()) {
                item(key = "header_today") {
                    TasksSectionHeader(
                        title = "Today",
                        subtitle = today.format(todaySubtitleFormatter),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(todayTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TaskRow(
                            task = task,
                            isDone = isTaskEffectiveCompleted(task),
                            showDate = false,
                            onToggleComplete = { handleToggleComplete(task) },
                            onClick = { editingTask = task },
                            onDelete = { taskPendingDelete = task },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Section 3: Upcoming (in ALL or UPCOMING filters)
            if ((selectedFilter == TaskFilter.ALL || selectedFilter == TaskFilter.UPCOMING) && upcomingTasks.isNotEmpty()) {
                item(key = "header_upcoming") {
                    TasksSectionHeader(
                        title = "Upcoming",
                        subtitle = "${upcomingTasks.size} scheduled",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(upcomingTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TaskRow(
                            task = task,
                            isDone = isTaskEffectiveCompleted(task),
                            showDate = true,
                            onToggleComplete = { handleToggleComplete(task) },
                            onClick = { editingTask = task },
                            onDelete = { taskPendingDelete = task },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Section 4: No date (in ALL or NO_DATE filters)
            if ((selectedFilter == TaskFilter.ALL || selectedFilter == TaskFilter.NO_DATE) && noDateTasks.isNotEmpty()) {
                item(key = "header_no_date") {
                    TasksSectionHeader(
                        title = "No date",
                        subtitle = if (noDateTasks.isNotEmpty()) "${noDateTasks.size}" else "",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(noDateTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TaskRow(
                            task = task,
                            isDone = isTaskEffectiveCompleted(task),
                            showDate = false,
                            onToggleComplete = { handleToggleComplete(task) },
                            onClick = { editingTask = task },
                            onDelete = { taskPendingDelete = task },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Section 5: Completed filter list
            if (selectedFilter == TaskFilter.COMPLETED && doneTasks.isNotEmpty()) {
                item(key = "header_completed_filter") {
                    TasksSectionHeader(
                        title = "Completed",
                        subtitle = "${doneTasks.size} finished",
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(doneTasks, key = { it.id }) { task ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        TaskRow(
                            task = task,
                            isDone = isTaskEffectiveCompleted(task),
                            showDate = true,
                            onToggleComplete = { handleToggleComplete(task) },
                            onClick = { editingTask = task },
                            onDelete = { taskPendingDelete = task },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }

            // Collapsible Completed toggle row (in ALL filter only)
            if (selectedFilter == TaskFilter.ALL && doneTasks.isNotEmpty()) {
                item(key = "toggle_completed_all") {
                    TasksCompletedToggleRow(
                        doneCount = doneTasks.size,
                        isExpanded = completedExpanded,
                        onToggle = { completedExpanded = !completedExpanded },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (completedExpanded) {
                    items(doneTasks, key = { "done_${it.id}" }) { task ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            TaskRow(
                                task = task,
                                isDone = isTaskEffectiveCompleted(task),
                                showDate = true,
                                onToggleComplete = { handleToggleComplete(task) },
                                onClick = { editingTask = task },
                                onDelete = { taskPendingDelete = task },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                }
            }

            // Empty State (when selected filter has no tasks)
            val isCurrentFilterEmpty = when (selectedFilter) {
                TaskFilter.ALL -> pendingTasks.isEmpty() && doneTasks.isEmpty()
                TaskFilter.TODAY -> overdueTasks.isEmpty() && todayTasks.isEmpty()
                TaskFilter.UPCOMING -> upcomingTasks.isEmpty()
                TaskFilter.NO_DATE -> noDateTasks.isEmpty()
                TaskFilter.COMPLETED -> doneTasks.isEmpty()
            }

            if (isCurrentFilterEmpty) {
                item(key = "tasks_empty_state") {
                    TasksEmptyState(
                        isCompletedFilter = selectedFilter == TaskFilter.COMPLETED,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // 2. Bottom Fade: full width, height 170dp + navigationBarsInset, non-interactive
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(ReflexTokens.TasksFadeHeight + navBarBottom)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.55f to MaterialTheme.colorScheme.background,
                        1f to MaterialTheme.colorScheme.background
                    )
                )
        )

        // 3. Fixed Round "+" Button: 60dp circle, copper fill, floats above tab bar
        TasksFab(
            isQuickAddOpen = isQuickAddOpen,
            onClick = { onQuickAddOpenChange(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = ReflexTokens.TasksFabEndPadding,
                    bottom = ReflexTokens.TasksFabBottomPadding + navBarBottom
                )
        )

        // 4. Undo Pill: full pill at bottom center, bottom offset 200dp + navBarBottom
        TaskUndoPill(
            message = undoMessage,
            onUndo = { undoAction?.invoke() },
            visible = undoPillVisible,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = ReflexTokens.TasksUndoBottomPadding + navBarBottom)
        )

        // 5. QuickAddCard and Scrim (floats above screen content, tab bar renders in ReflexMainScreen on top)
        QuickAddCard(
            isOpen = isQuickAddOpen,
            onDismiss = { onQuickAddOpenChange(false) },
            onAddTask = { text ->
                viewModel.addTaskFromNaturalLanguage(text, context)
            },
            onMicClick = { launchVoiceInput() },
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
            onDismiss = {
                taskPendingDelete = null
            }
        )
    }
}

/**
 * 3. Progress Card
 * Surface, 1dp Border, 26dp radius, padding 14x16dp.
 * "{done} of {total} done today" + "{pct}%", and animated 8dp full-pill progress bar.
 */
@Composable
private fun TasksProgressCard(
    totalCount: Int,
    doneCount: Int,
    pct: Int,
    fraction: Float,
    modifier: Modifier = Modifier
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "tasks_prog_fill"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeInnerTile, // 26dp radius
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics {
                    contentDescription = if (totalCount > 0) "$doneCount of $totalCount tasks done today" else "Nothing due today"
                }
        ) {
            // Row 1: space-between, baseline aligned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (totalCount > 0) {
                    Text(
                        text = "$doneCount of $totalCount done today",
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$pct%",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Nothing due today",
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Enjoy the space",
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 8dp full-pill progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ReflexTokens.TasksProgressHeight)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(ReflexTokens.TasksProgressHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/**
 * 4. Filter Chip
 * 40dp tall full pill, 14sp Medium, SurfaceInput bg with secondary text.
 * Selected = copper bg, OnCopper text, SemiBold. Count in 13sp at 80% alpha when > 0.
 */
@Composable
private fun TasksFilterChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .height(ReflexTokens.TasksFilterChipHeight)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontFamily = Lora,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg
        )

        if (count > 0) {
            Text(
                text = "$count",
                fontFamily = Lora,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = fg.copy(alpha = 0.80f)
            )
        }
    }
}

/**
 * 5. Section Header
 * Title (18sp SemiBold) on left, subtitle (13sp secondary) on right, baseline aligned, 10dp above rows.
 */
@Composable
private fun TasksSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val titleColor = if (isDestructive) {
        if (isDark) DarkDestructiveRed else LightDestructiveRed
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 10.dp, start = 4.dp, end = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            fontFamily = Lora,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor
        )

        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                fontFamily = Lora,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Collapsed Completed Toggle Row (All filter)
 * 48dp row, "Completed" 16sp SemiBold secondary on left, count (13sp) + 20dp chevron rotating 180° on right.
 */
@Composable
private fun TasksCompletedToggleRow(
    doneCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(250),
        label = "tasks_completed_chevron"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .height(48.dp)
            .clip(ReflexTokens.ShapeInnerTile)
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Completed",
            fontFamily = Lora,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$doneCount",
                fontFamily = Lora,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse completed" else "Expand completed",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation)
            )
        }
    }
}

/**
 * 6. Task Row
 * Surface, 1dp Border, 26dp radius, padding 14x16dp, 14dp gap between checkbox and text.
 * Checkbox: 28dp circle, 2dp border (priority color), 15dp check with 3dp stroke.
 * Title: 16sp SemiBold, line height 21sp.
 * Meta: 13sp secondary, "·" separator, priority tag, repeat icon.
 * Done state: row alpha 0.55, title struck through in tertiary text.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskRow(
    task: Task,
    isDone: Boolean,
    showDate: Boolean,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val tertiaryColor = if (isDark) DarkTertiaryText else LightTertiaryText
    val destructiveColor = if (isDark) DarkDestructiveRed else LightDestructiveRed
    val copperColor = MaterialTheme.colorScheme.primary
    val onCopperColor = MaterialTheme.colorScheme.onPrimary

    val today = remember { LocalDate.now() }
    val daysOffset = remember(task.dueDate, today) {
        task.dueDate?.let {
            val date = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            ChronoUnit.DAYS.between(today, date)
        }
    }
    val isOverdue = daysOffset != null && daysOffset < 0 && !isDone

    // Checkbox border color
    val checkboxBorder = when (task.priority) {
        Priority.HIGH -> destructiveColor
        Priority.MEDIUM -> copperColor
        else -> tertiaryColor
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isDone) 0.55f else 1.0f,
        animationSpec = tween(ReflexTokens.AnimDurationNormal),
        label = "task_done_alpha"
    )

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onClick()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
        positionalThreshold = { totalDistance -> totalDistance * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.clip(ReflexTokens.ShapeInnerTile),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val (bgColor, iconVal, alignment) = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Triple(
                    copperColor,
                    Icons.Default.Edit,
                    Alignment.CenterStart
                )
                SwipeToDismissBoxValue.EndToStart -> Triple(
                    destructiveColor,
                    Icons.Default.Delete,
                    Alignment.CenterEnd
                )
                else -> Triple(Color.Transparent, null, Alignment.Center)
            }

            if (iconVal != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = iconVal,
                        contentDescription = if (direction == SwipeToDismissBoxValue.StartToEnd) "Edit task" else "Delete task",
                        modifier = Modifier.size(22.dp),
                        tint = ActionPillOnWhite
                    )
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = animatedAlpha }
                .clickable(onClick = onClick),
            shape = ReflexTokens.ShapeInnerTile, // 26dp radius
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox: 28dp circle with 2dp border
            Box(
                modifier = Modifier
                    .size(ReflexTokens.TasksCheckboxSize)
                    .clip(CircleShape)
                    .background(if (isDone) copperColor else Color.Transparent)
                    .border(
                        width = 2.dp,
                        color = if (isDone) copperColor else checkboxBorder,
                        shape = CircleShape
                    )
                    .clickable(onClick = onToggleComplete)
                    .semantics {
                        contentDescription = if (isDone) "Mark as not done: ${task.title}" else "Complete: ${task.title}"
                    },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = isDone,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150))
                ) {
                    Canvas(modifier = Modifier.size(15.dp)) {
                        val strokePx = 3.dp.toPx()
                        val path = Path().apply {
                            moveTo(size.width * 0.20f, size.height * 0.50f)
                            lineTo(size.width * 0.42f, size.height * 0.76f)
                            lineTo(size.width * 0.85f, size.height * 0.26f)
                        }
                        drawPath(
                            path = path,
                            color = onCopperColor,
                            style = Stroke(
                                width = strokePx,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title & Meta Line
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp)
            ) {
                Text(
                    text = task.title,
                    fontFamily = Lora,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDone) tertiaryColor else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Meta Line: date label, time, repeat icon
                val hasMeta = (showDate && daysOffset != null) || task.dueTime != null || task.recurrenceFrequency != RecurrenceFrequency.NONE
                if (hasMeta) {
                    Spacer(modifier = Modifier.height(3.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Date label (only in Overdue, Upcoming, Completed sections)
                        if (showDate && daysOffset != null) {
                            val dateText = when (daysOffset) {
                                0L -> "Today"
                                1L -> "Tomorrow"
                                -1L -> "Yesterday"
                                in Long.MIN_VALUE..-2L -> "${-daysOffset} days ago"
                                else -> {
                                    val date = Instant.ofEpochMilli(task.dueDate!!).atZone(ZoneId.systemDefault()).toLocalDate()
                                    date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
                                }
                            }
                            Text(
                                text = dateText,
                                fontFamily = Lora,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = if (isOverdue) destructiveColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Time label
                        if (task.dueTime != null) {
                            val localTime = Instant.ofEpochMilli(task.dueTime).atZone(ZoneId.systemDefault()).toLocalTime()
                            val timeStr = localTime.format(DateTimeFormatter.ofPattern("h:mm a")).lowercase()
                            Text(
                                text = timeStr,
                                fontFamily = Lora,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Repeat
                        if (task.recurrenceFrequency != RecurrenceFrequency.NONE) {
                            val repeatLabel = when (task.recurrenceFrequency) {
                                RecurrenceFrequency.DAILY -> "Every day"
                                RecurrenceFrequency.WEEKLY -> {
                                    if (!task.recurrenceDaysOfWeek.isNullOrBlank()) "Every ${task.recurrenceDaysOfWeek}" else "Every week"
                                }
                                RecurrenceFrequency.MONTHLY -> "Every month"
                                else -> "Repeating"
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = repeatLabel,
                                    fontFamily = Lora,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Far-right Priority indicator: High (red badge) and Medium (copper badge). Normal/Low has no badge.
            if (task.priority == Priority.HIGH || task.priority == Priority.MEDIUM) {
                Spacer(modifier = Modifier.width(8.dp))
                val (tagBg, tagFg) = when (task.priority) {
                    Priority.HIGH -> Pair(destructiveColor.copy(alpha = 0.16f), destructiveColor)
                    Priority.MEDIUM -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    else -> Pair(Color.Transparent, Color.Transparent)
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tagBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = task.priority.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontFamily = Lora,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tagFg
                    )
                }
            }
        }
    }
}
}

/**
 * 7. Undo Pill
 * Full pill at bottom center, bottom offset 200dp + navBarInset, 14sp Medium, Undo button (34dp pill, 14sp SemiBold).
 * Auto-hides after 3.5s with a fade and 10dp slide.
 */
@Composable
fun TaskUndoPill(
    message: String,
    onUndo: () -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val pillBg = if (isDark) ActionPillWhite else Color(0xFF1A1614)
    val pillFg = if (isDark) ActionPillOnWhite else Color.White

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { it / 2 },
        exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { it / 2 },
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = pillBg,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, top = 6.dp, end = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = message,
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = pillFg
                )

                Box(
                    modifier = Modifier
                        .height(ReflexTokens.TasksUndoButtonHeight)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.25f))
                        .clickable(onClick = onUndo)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Undo",
                        fontFamily = Lora,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pillFg
                    )
                }
            }
        }
    }
}

/**
 * 8. Fixed Round "+" Button
 * 60dp circle, copper fill, OnCopper "+" icon (28dp, 2.3dp stroke).
 * Floats at bottom-end (bottom = 128dp + navBarInset, end = 28dp).
 * 7dp halo ring, 14dp Y offset shadow (36% black), press scale .92, entrance pop-in, animates out when QuickAdd is open.
 */
@Composable
fun TasksFab(
    isQuickAddOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.5f
    val copperColor = MaterialTheme.colorScheme.primary
    val onCopperColor = MaterialTheme.colorScheme.onPrimary

    // Screen entrance animation: scale from 0.4 to 1 and fade in over 450ms with 100ms delay
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        entered = true
    }
    val entranceScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.4f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "fab_entrance_scale"
    )
    val entranceAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "fab_entrance_alpha"
    )

    // Press scale animation
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = tween(120),
        label = "fab_press_scale"
    )

    // Card open/close animation: scale 0.5, rotate 90°, alpha 0 over 250ms
    val openScale by animateFloatAsState(
        targetValue = if (isQuickAddOpen) 0.5f else 1.0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "fab_open_scale"
    )
    val openRotation by animateFloatAsState(
        targetValue = if (isQuickAddOpen) 90f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "fab_open_rot"
    )
    val openAlpha by animateFloatAsState(
        targetValue = if (isQuickAddOpen) 0f else 1.0f,
        animationSpec = tween(200),
        label = "fab_open_alpha"
    )

    val currentAlpha = entranceAlpha * openAlpha
    val currentScale = entranceScale * pressScale * openScale

    if (currentAlpha > 0.01f) {
        Box(
            modifier = modifier
                .graphicsLayer {
                    scaleX = currentScale
                    scaleY = currentScale
                    rotationZ = openRotation
                    alpha = currentAlpha
                }
                .semantics { contentDescription = "Add task" },
            contentAlignment = Alignment.Center
        ) {
            // Halo ring: 7dp halo ring drawn around it in copper at 14% alpha (60 + 14 = 74dp)
            Box(
                modifier = Modifier
                    .size(ReflexTokens.TasksFabHaloSize)
                    .clip(CircleShape)
                    .background(copperColor.copy(alpha = 0.14f))
            )

            // Main 60dp button with 14dp Y offset shadow (36% black)
            Box(
                modifier = Modifier
                    .size(ReflexTokens.TasksFabSize)
                    .shadow(
                        elevation = 14.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.36f),
                        spotColor = Color.Black.copy(alpha = 0.36f)
                    )
                    .clip(CircleShape)
                    .background(copperColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = !isQuickAddOpen,
                        onClick = onClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                // OnCopper "+" icon (28dp, 2.3dp stroke)
                Canvas(modifier = Modifier.size(28.dp)) {
                    val strokePx = 2.3.dp.toPx()
                    val center = size.width / 2f
                    val padding = 4.dp.toPx()

                    // Horizontal stroke
                    drawLine(
                        color = onCopperColor,
                        start = androidx.compose.ui.geometry.Offset(padding, center),
                        end = androidx.compose.ui.geometry.Offset(size.width - padding, center),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                    // Vertical stroke
                    drawLine(
                        color = onCopperColor,
                        start = androidx.compose.ui.geometry.Offset(center, padding),
                        end = androidx.compose.ui.geometry.Offset(center, size.height - padding),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * 10. Empty State
 * 56dp top margin, centered. 72dp circle with 32dp copper check icon.
 * Title 18sp SemiBold ("All clear" / "Nothing completed yet"), caption 14sp secondary.
 */
@Composable
private fun TasksEmptyState(
    isCompletedFilter: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 72dp circle (CopperContainer) with 32dp copper check icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = if (isCompletedFilter) "Nothing completed yet" else "All clear",
            fontFamily = Lora,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = if (isCompletedFilter) "Finished tasks will collect here." else "Tap + to add a task.",
            fontFamily = Lora,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =========================================================================
// TASKS SCREEN PREVIEWS (Dark and Light)
// =========================================================================

@Preview(name = "Tasks Screen - All Filter (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tasks Screen - All Filter (Light)", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun TasksScreenAllPreview() {
    ReflexTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                TasksProgressCard(totalCount = 4, doneCount = 2, pct = 50, fraction = 0.5f)
                Spacer(modifier = Modifier.height(14.dp))
                TaskRow(
                    task = Task(id = 1, title = "Finish TCS Xplore module", priority = Priority.HIGH),
                    isDone = false,
                    showDate = false,
                    onToggleComplete = {},
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                TaskRow(
                    task = Task(id = 2, title = "Buy groceries", priority = Priority.NONE),
                    isDone = false,
                    showDate = false,
                    onToggleComplete = {},
                    onClick = {}
                )
                Spacer(modifier = Modifier.height(8.dp))
                TaskRow(
                    task = Task(id = 3, title = "Review DSA notes", priority = Priority.MEDIUM),
                    isDone = true,
                    showDate = true,
                    onToggleComplete = {},
                    onClick = {}
                )
            }

            TasksFab(
                isQuickAddOpen = false,
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 128.dp)
            )

            TaskUndoPill(
                message = "Task completed",
                onUndo = {},
                visible = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 200.dp)
            )
        }
    }
}

@Preview(name = "Tasks Screen - Empty State (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tasks Screen - Empty State (Light)", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun TasksScreenEmptyPreview() {
    ReflexTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            TasksEmptyState(isCompletedFilter = false)
        }
    }
}
