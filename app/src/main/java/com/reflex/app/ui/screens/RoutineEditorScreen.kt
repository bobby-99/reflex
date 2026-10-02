package com.reflex.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reflex.app.ReflexApplication
import com.reflex.app.data.RoutineIcon
import com.reflex.app.data.Step
import com.reflex.app.data.StepType
import com.reflex.app.ui.components.DeleteConfirmationDialog
import com.reflex.app.ui.components.IconPickerModal
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTextField
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.SectionHeader
import com.reflex.app.ui.components.StepEditSheet
import com.reflex.app.ui.components.StepTypeChip
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.DestructiveContainer
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.viewmodel.RoutineEditorViewModel
import kotlinx.coroutines.launch
import java.time.DayOfWeek

@Composable
fun RoutineEditorScreen(
    routineId: Long,
    onNavigateBack: () -> Unit,
    viewModel: RoutineEditorViewModel = viewModel(
        factory = RoutineEditorViewModel.Factory(
            (LocalContext.current.applicationContext as ReflexApplication).repository,
            routineId
        )
    )
) {
    SetStatusBarAppearance()
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showIconPicker by remember { mutableStateOf(false) }
    var editingStep by remember { mutableStateOf<Step?>(null) }
    var showAddStepSheet by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTimePickerModal by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    fun handleVerticalDrag(dragAmount: Float) {
        val currentIdx = draggingIndex ?: return
        dragOffsetY += dragAmount

        val layoutInfo = lazyListState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        if (visibleItems.isEmpty()) return

        val currentKey = state.steps.getOrNull(currentIdx)?.let { s -> if (s.id != 0L) s.id else "temp_${s.orderIndex}" } ?: return
        val currentItemInfo = visibleItems.firstOrNull { it.key == currentKey } ?: return

        val currentCenterY = currentItemInfo.offset + (currentItemInfo.size / 2f) + dragOffsetY

        var targetIdxToSwap: Int? = null
        if (dragOffsetY > 0) {
            for (targetIdx in (currentIdx + 1)..state.steps.lastIndex) {
                val targetKey = state.steps.getOrNull(targetIdx)?.let { s -> if (s.id != 0L) s.id else "temp_${s.orderIndex}" }
                val targetItemInfo = visibleItems.firstOrNull { it.key == targetKey } ?: continue
                val targetCenterY = targetItemInfo.offset + (targetItemInfo.size / 2f)
                if (currentCenterY > targetCenterY) {
                    targetIdxToSwap = targetIdx
                }
            }
        } else if (dragOffsetY < 0) {
            for (targetIdx in (currentIdx - 1) downTo 0) {
                val targetKey = state.steps.getOrNull(targetIdx)?.let { s -> if (s.id != 0L) s.id else "temp_${s.orderIndex}" }
                val targetItemInfo = visibleItems.firstOrNull { it.key == targetKey } ?: continue
                val targetCenterY = targetItemInfo.offset + (targetItemInfo.size / 2f)
                if (currentCenterY < targetCenterY) {
                    targetIdxToSwap = targetIdx
                }
            }
        }

        if (targetIdxToSwap != null) {
            val targetKey = state.steps.getOrNull(targetIdxToSwap)?.let { s -> if (s.id != 0L) s.id else "temp_${s.orderIndex}" }
            val targetItemInfo = visibleItems.firstOrNull { it.key == targetKey }
            if (targetItemInfo != null) {
                val offsetDiff = (targetItemInfo.offset - currentItemInfo.offset).toFloat()
                viewModel.reorderSteps(currentIdx, targetIdxToSwap)
                draggingIndex = targetIdxToSwap
                dragOffsetY -= offsetDiff
            }
        }

        val viewportHeight = layoutInfo.viewportSize.height
        val dragTopOnScreen = currentItemInfo.offset + dragOffsetY
        if (dragTopOnScreen < 100f) {
            coroutineScope.launch { lazyListState.scrollBy(-25f) }
        } else if (dragTopOnScreen > viewportHeight - 150f) {
            coroutineScope.launch { lazyListState.scrollBy(25f) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = if (routineId == 0L) "New routine" else "Edit routine",
            onBackClick = onNavigateBack,
            actions = {
                if (routineId != 0L) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline), CircleShape)
                            .clickable { showDeleteConfirmDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Routine",
                            modifier = Modifier.size(ReflexTokens.IconSm),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ReflexTokens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceLg)
            ) {
                item {
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))

                    // Inline Error Banner
                    state.validationError?.let { errorMsg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(ReflexTokens.ShapeCard)
                                .background(DestructiveContainer)
                                .border(
                                    BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                    ReflexTokens.ShapeCard
                                )
                                .padding(ReflexTokens.SpaceMd)
                        ) {
                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                    }
                }

                // --- 1. BASIC DETAILS CARD ---
                item {
                    ReflexCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                            Text(
                                text = "Routine details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Routine Name Input
                            ReflexTextField(
                                value = state.name,
                                onValueChange = { viewModel.updateName(it) },
                                label = "Routine Name",
                                placeholder = "e.g. Morning Mobility, Nightly Routine",
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Icon Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(ReflexTokens.ShapeIconTile)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(
                                                BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
                                                ReflexTokens.ShapeIconTile
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = RoutineIcon.fromKey(state.iconKey).icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                            tint = CopperPrimary
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))

                                    Column {
                                        Text(
                                            text = "Routine icon",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = state.iconKey,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                ReflexButton(
                                    text = "Change",
                                    onClick = { showIconPicker = true },
                                    variant = ReflexButtonVariant.SECONDARY
                                )
                            }
                        }
                    }
                }

                // --- 2. REST BREAK CARD ---
                item {
                    ReflexCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Rest between steps",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Pauses timer between sequence steps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Switch(
                                    checked = state.restBetweenStepsEnabled,
                                    onCheckedChange = { viewModel.updateRestEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = ActionPillOnWhite,
                                        checkedTrackColor = CopperPrimary,
                                        uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            }

                            if (state.restBetweenStepsEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Rest duration",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    // GymMane-style numeric stepper capsule
                                    Row(
                                        modifier = Modifier
                                            .clip(ReflexTokens.ShapeButton)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .border(
                                                BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
                                                ReflexTokens.ShapeButton
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable { viewModel.updateRestDuration((state.restDurationSeconds - 5).coerceAtLeast(0)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .width(56.dp)
                                                .height(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            BasicTextField(
                                                value = "${state.restDurationSeconds}s",
                                                onValueChange = { input ->
                                                    val digits = input.filter { it.isDigit() }
                                                    val num = digits.toIntOrNull() ?: 0
                                                    viewModel.updateRestDuration(num.coerceIn(0, 120))
                                                },
                                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = CopperPrimary,
                                                    textAlign = TextAlign.Center
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .clickable { viewModel.updateRestDuration((state.restDurationSeconds + 5).coerceAtMost(120)) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // --- 3. SCHEDULE & REMINDERS CARD ---
                item {
                    ReflexCard(modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                            Column {
                                Text(
                                    text = "Schedule & reminders",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.scheduledDays.isEmpty()) "Unscheduled (Manual-start only)" else "${state.scheduledDays.size} days active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // 7 Circular Day Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val daysOfWeek = listOf(
                                    DayOfWeek.MONDAY to "M",
                                    DayOfWeek.TUESDAY to "T",
                                    DayOfWeek.WEDNESDAY to "W",
                                    DayOfWeek.THURSDAY to "T",
                                    DayOfWeek.FRIDAY to "F",
                                    DayOfWeek.SATURDAY to "S",
                                    DayOfWeek.SUNDAY to "S"
                                )

                                daysOfWeek.forEach { (day, label) ->
                                    val isSelected = state.scheduledDays.contains(day)
                                    Box(
                                        modifier = Modifier
                                            .size(ReflexTokens.DayCircleSize)
                                            .clip(CircleShape)
                                            .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                            .border(
                                                BorderStroke(
                                                    ReflexTokens.BorderHairline,
                                                    if (isSelected) CopperPrimary else MaterialTheme.colorScheme.outline
                                                ),
                                                CircleShape
                                            )
                                            .clickable { viewModel.toggleScheduledDay(day) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Reminder Alarm Row (if scheduled)
                            if (state.scheduledDays.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Reminder notification",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (state.reminderTime != null) "Alarm set for ${state.reminderTime}" else "Set daily alarm time",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Switch(
                                        checked = state.reminderEnabled,
                                        onCheckedChange = { viewModel.updateReminderEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ActionPillOnWhite,
                                            checkedTrackColor = CopperPrimary,
                                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    )
                                }

                                if (state.reminderEnabled) {
                                    ReflexButton(
                                        text = if (state.reminderTime != null) "Time: ${state.reminderTime}" else "Set time",
                                        onClick = { showTimePickerModal = true },
                                        variant = ReflexButtonVariant.SECONDARY,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 4. STEPS HEADER ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = ReflexTokens.SpaceXs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Routine steps",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${state.steps.size} steps sequence",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        ReflexButton(
                            text = "+ Add step",
                            onClick = { showAddStepSheet = true },
                            variant = ReflexButtonVariant.PRIMARY
                        )
                    }
                }

                // --- 5. REORDERABLE STEP ROWS ---
                itemsIndexed(state.steps, key = { _, step -> if (step.id != 0L) step.id else "temp_${step.orderIndex}" }) { index, step ->
                    val isDragging = draggingIndex == index
                    EditorStepRowItem(
                        index = index,
                        step = step,
                        totalSteps = state.steps.size,
                        isDragging = isDragging,
                        dragOffsetY = if (isDragging) dragOffsetY else 0f,
                        onDragStart = { idx ->
                            draggingIndex = idx
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            draggingIndex = null
                            dragOffsetY = 0f
                        },
                        onDragDelta = { delta ->
                            handleVerticalDrag(delta)
                        },
                        onEdit = { editingStep = step },
                        onDelete = { viewModel.removeStep(step) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }

            // --- 6. SAVE / CANCEL FLOATING ACTION BAR ---
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        horizontal = ReflexTokens.BottomNavFloatingMarginH,
                        vertical = ReflexTokens.BottomNavFloatingMarginV
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ReflexTokens.ShapeFloatingNav)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
                            ReflexTokens.ShapeFloatingNav
                        )
                        .padding(horizontal = ReflexTokens.SpaceLg, vertical = ReflexTokens.SpaceMd)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ReflexButton(
                            text = "Cancel",
                            onClick = onNavigateBack,
                            variant = ReflexButtonVariant.SECONDARY
                        )

                        ReflexButton(
                            text = if (routineId == 0L) "Create routine" else "Save changes",
                            onClick = {
                                viewModel.saveRoutine(context, onSuccess = onNavigateBack)
                            },
                            variant = ReflexButtonVariant.PRIMARY
                        )
                    }
                }
            }
        }
    }

    // Modal Time Picker
    if (showTimePickerModal) {
        com.reflex.app.ui.components.ReflexTimePickerModal(
            initialTime = state.reminderTime,
            onDismiss = { showTimePickerModal = false },
            onTimeSelected = { formattedTime ->
                viewModel.updateReminderTime(formattedTime)
                showTimePickerModal = false
            }
        )
    }

    // Modal Icon Picker
    if (showIconPicker) {
        IconPickerModal(
            selectedIconKey = state.iconKey,
            onSelectIcon = { viewModel.updateIcon(it) },
            onDismiss = { showIconPicker = false }
        )
    }

    // Modal Add Step Sheet
    if (showAddStepSheet) {
        StepEditSheet(
            step = null,
            orderIndex = state.steps.size,
            routineId = routineId,
            onDismiss = { showAddStepSheet = false },
            onSave = { newStep ->
                viewModel.addOrUpdateStep(newStep)
                showAddStepSheet = false
            }
        )
    }

    // Modal Edit Step Sheet
    editingStep?.let { stepToEdit ->
        StepEditSheet(
            step = stepToEdit,
            orderIndex = stepToEdit.orderIndex,
            routineId = routineId,
            onDismiss = { editingStep = null },
            onSave = { updatedStep ->
                viewModel.addOrUpdateStep(updatedStep)
                editingStep = null
            }
        )
    }

    // Modal Delete Confirm Dialog
    if (showDeleteConfirmDialog) {
        DeleteConfirmationDialog(
            taskTitle = state.name.ifBlank { "Routine" },
            onConfirm = {
                viewModel.deleteRoutine(context, onSuccess = onNavigateBack)
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
}

@Composable
private fun EditorStepRowItem(
    index: Int,
    step: Step,
    totalSteps: Int,
    isDragging: Boolean = false,
    dragOffsetY: Float = 0f,
    onDragStart: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onDragDelta: (Float) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 1f)
            .graphicsLayer { translationY = dragOffsetY }
    ) {
        ReflexCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onEdit
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Drag Handle Icon
                Box(
                    modifier = Modifier
                        .padding(end = ReflexTokens.SpaceSm)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragStart = { onDragStart(index) },
                                onDragEnd = { onDragEnd() },
                                onDragCancel = { onDragEnd() },
                                onVerticalDrag = { change, dragAmount ->
                                    change.consume()
                                    onDragDelta(dragAmount)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder step",
                        modifier = Modifier.size(24.dp),
                        tint = if (isDragging) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Step Number Badge
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))

                // Step Name & Detail
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = step.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val details = when (step.stepType) {
                        StepType.TIMED -> {
                            val secs = step.durationSeconds ?: 0
                            val mins = secs / 60
                            val remSecs = secs % 60
                            if (mins > 0 && remSecs > 0) "${mins}m ${remSecs}s"
                            else if (mins > 0) "${mins}m"
                            else "${remSecs}s"
                        }
                        StepType.CHECK_OFF -> "Check-off"
                        StepType.REPEAT_COUNT -> {
                            val sets = step.targetCount ?: 0
                            val rest = step.restDurationSeconds ?: run {
                                val r = com.reflex.app.util.SettingsRepository.getString("r_rest", "15s")
                                if (r != "Off") r.filter { it.isDigit() }.toIntOrNull() else null
                            }
                            if (rest != null && rest > 0) "$sets sets · ${rest}s rest" else "$sets sets"
                        }
                    }

                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StepTypeChip(stepType = step.stepType)

                Spacer(modifier = Modifier.width(ReflexTokens.SpaceSm))

                // Actions: Edit & Delete
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Step",
                    modifier = Modifier
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 4.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Step",
                    modifier = Modifier
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 4.dp)
                        .size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
