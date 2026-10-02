package com.reflex.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceBasis
import com.reflex.app.data.RecurrenceEndType
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.RecurrenceUnit
import com.reflex.app.data.Task
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperOnContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.util.CalendarProviderHelper
import com.reflex.app.util.TaskHighlightVisualTransformation
import com.reflex.app.util.TimeDefaults
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditSheet(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (Task) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(task.title) }
    var notes by remember { mutableStateOf(task.notes ?: "") }
    var priority by remember { mutableStateOf(task.priority) }
    var addToCalendar by remember { mutableStateOf(false) }

    var selectedDate by remember {
        mutableStateOf(
            task.dueDate?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        )
    }

    var selectedTime by remember {
        mutableStateOf(
            task.dueTime?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
        )
    }

    var showCustomTimePicker by remember { mutableStateOf(false) }
    val initialHour12 = selectedTime?.let { if (it.hour % 12 == 0) 12 else it.hour % 12 } ?: 9
    val initialMinute = selectedTime?.minute ?: 0
    val initialIsPm = (selectedTime?.hour ?: 9) >= 12

    var hourInput by remember { mutableStateOf(initialHour12.toString()) }
    var minInput by remember { mutableStateOf(String.format("%02d", initialMinute)) }
    var isPm by remember { mutableStateOf(initialIsPm) }

    // Recurrence State
    var frequency by remember { mutableStateOf(task.recurrenceFrequency) }
    var interval by remember { mutableIntStateOf(task.recurrenceInterval) }
    var unit by remember { mutableStateOf(task.recurrenceUnit) }
    var selectedDaysOfWeek by remember {
        mutableStateOf(
            task.recurrenceDaysOfWeek?.split(",")?.toSet() ?: emptySet()
        )
    }
    var endType by remember { mutableStateOf(task.recurrenceEndType) }
    var endOccurrences by remember { mutableIntStateOf(task.recurrenceEndOccurrences.coerceAtLeast(10)) }
    var recurrenceBasis by remember { mutableStateOf(task.recurrenceBasis) }

    val outlineColor = MaterialTheme.colorScheme.outline

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = ReflexTokens.ShapeModal,
        tonalElevation = 0.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = outlineColor)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ReflexTokens.SpaceLg)
                .padding(bottom = ReflexTokens.SpaceXxl)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Edit task",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // Title
            ReflexTextField(
                value = title,
                onValueChange = { title = it },
                label = "Task Title",
                placeholder = "Enter task title...",
                visualTransformation = TaskHighlightVisualTransformation(textColor = MaterialTheme.colorScheme.onSurface, highlightColor = CopperPrimary)
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            // Notes
            ReflexTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Notes",
                placeholder = "Add details or subtasks...",
                singleLine = false
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // Priority Selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                val secondaryContainerColor = MaterialTheme.colorScheme.secondaryContainer
                val errorColor = MaterialTheme.colorScheme.error

                Priority.entries.forEach { p ->
                    val isSelected = p == priority
                    val (bgColor, txtColor, borderColor) = when {
                        isSelected && p == Priority.HIGH -> Triple(errorColor.copy(alpha = 0.2f), errorColor, errorColor)
                        isSelected && p == Priority.MEDIUM -> Triple(CopperContainer, CopperOnContainer, CopperPrimary)
                        isSelected && p == Priority.LOW -> Triple(secondaryContainerColor, onSurfaceColor, CopperPrimary)
                        isSelected && p == Priority.NONE -> Triple(secondaryContainerColor, onSurfaceColor, outlineColor)
                        else -> Triple(Color.Transparent, onSurfaceVariantColor, outlineColor)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(bgColor)
                            .border(BorderStroke(ReflexTokens.BorderHairline, borderColor), ReflexTokens.ShapeChip)
                            .clickable { priority = p },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = txtColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            var showDatePicker by remember { mutableStateOf(false) }

            // Date Selector Quick Options + Custom Date Picker Modal Trigger
            Text(
                text = "Due date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                val today = LocalDate.now()
                val options = listOf(
                    "No date" to null,
                    "Today" to today,
                    "Tomorrow" to today.plusDays(1),
                    "Next week" to today.plusWeeks(1)
                )

                options.forEach { (label, dateVal) ->
                    val isSelected = selectedDate == dateVal && !showDatePicker
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                border = BorderStroke(
                                    ReflexTokens.BorderHairline,
                                    if (isSelected) CopperPrimary else outlineColor
                                ),
                                shape = ReflexTokens.ShapeChip
                            )
                            .clickable { selectedDate = dateVal },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = ReflexTokens.SpaceMd)
                        )
                    }
                }

                // Custom Date Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(ReflexTokens.ShapeChip)
                        .background(if (showDatePicker) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            border = BorderStroke(
                                ReflexTokens.BorderHairline,
                                if (showDatePicker) CopperPrimary else outlineColor
                            ),
                            shape = ReflexTokens.ShapeChip
                        )
                        .clickable { showDatePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom date",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (showDatePicker) FontWeight.Bold else FontWeight.Medium,
                        color = if (showDatePicker) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = ReflexTokens.SpaceMd)
                    )
                }
            }

            if (selectedDate != null) {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                Text(
                    text = "Selected Date: ${selectedDate?.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))} (Tap to change)",
                    style = MaterialTheme.typography.bodySmall,
                    color = CopperPrimary,
                    modifier = Modifier.clickable { showDatePicker = true }
                )
            }

            if (showDatePicker) {
                ReflexDatePickerModal(
                    initialDate = selectedDate ?: LocalDate.now(),
                    onDismiss = { showDatePicker = false },
                    onDateSelected = { date ->
                        selectedDate = date
                        showDatePicker = false
                    }
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // Time Selector
            Text(
                text = "Due time",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                val timeOptions = listOf(
                    "None" to null,
                    "8:00 AM" to TimeDefaults.MORNING,
                    "12:30 PM" to TimeDefaults.NOON,
                    "4:00 PM" to TimeDefaults.EVENING,
                    "8:00 PM" to TimeDefaults.NIGHT
                )

                timeOptions.forEach { (label, timeVal) ->
                    val isSelected = selectedTime == timeVal && !showCustomTimePicker
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                border = BorderStroke(
                                    ReflexTokens.BorderHairline,
                                    if (isSelected) CopperPrimary else outlineColor
                                ),
                                shape = ReflexTokens.ShapeChip
                            )
                            .clickable {
                                selectedTime = timeVal
                                showCustomTimePicker = false
                            }
                            .padding(horizontal = ReflexTokens.SpaceMd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Custom Time Button
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(ReflexTokens.ShapeChip)
                        .background(if (showCustomTimePicker) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            border = BorderStroke(
                                ReflexTokens.BorderHairline,
                                if (showCustomTimePicker) CopperPrimary else outlineColor
                            ),
                            shape = ReflexTokens.ShapeChip
                        )
                        .clickable { showCustomTimePicker = !showCustomTimePicker }
                        .padding(horizontal = ReflexTokens.SpaceMd),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom time",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (showCustomTimePicker) FontWeight.Bold else FontWeight.Medium,
                        color = if (showCustomTimePicker) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Custom Time Picker Builder
            if (showCustomTimePicker) {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                ReflexCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Set exact due time",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hour Field
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Hour (1-12)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
                                ReflexTextField(
                                    value = hourInput,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 1..12)) {
                                            hourInput = input
                                        }
                                    },
                                    modifier = Modifier.width(72.dp)
                                )
                            }

                            Text(":", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                            // Minute Field
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Min (0-59)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
                                ReflexTextField(
                                    value = minInput,
                                    onValueChange = { input ->
                                        if (input.isEmpty() || (input.toIntOrNull() != null && input.toInt() in 0..59)) {
                                            minInput = input
                                        }
                                    },
                                    modifier = Modifier.width(72.dp)
                                )
                            }

                            // AM/PM Toggle Button
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("AM / PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))
                                Row(
                                    modifier = Modifier
                                        .clip(ReflexTokens.ShapeChip)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), ReflexTokens.ShapeChip)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(topStart = ReflexTokens.RadiusPill, bottomStart = ReflexTokens.RadiusPill))
                                            .background(if (!isPm) CopperPrimary else Color.Transparent)
                                            .clickable { isPm = false }
                                            .padding(horizontal = ReflexTokens.SpaceMd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "AM",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isPm) ActionPillOnWhite else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(topEnd = ReflexTokens.RadiusPill, bottomEnd = ReflexTokens.RadiusPill))
                                            .background(if (isPm) CopperPrimary else Color.Transparent)
                                            .clickable { isPm = true }
                                            .padding(horizontal = ReflexTokens.SpaceMd),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "PM",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isPm) ActionPillOnWhite else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                        ReflexButton(
                            text = "Apply time",
                            onClick = {
                                val h12 = hourInput.toIntOrNull() ?: 9
                                val m = minInput.toIntOrNull() ?: 0
                                var h24 = if (h12 == 12) 0 else h12
                                if (isPm) h24 += 12
                                selectedTime = LocalTime.of(h24, m)
                                showCustomTimePicker = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            variant = ReflexButtonVariant.PRIMARY
                        )
                    }
                }
            }

            if (selectedTime != null) {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                Text(
                    text = "Selected Time: ${selectedTime?.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CopperPrimary
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // RECURRENCE PICKER & BUILDER
            Text(
                text = "Repeat / Recurrence",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                val presets = listOf(
                    "Does not repeat" to RecurrenceFrequency.NONE,
                    "Daily" to RecurrenceFrequency.DAILY,
                    "Weekly" to RecurrenceFrequency.WEEKLY,
                    "Monthly" to RecurrenceFrequency.MONTHLY,
                    "Yearly" to RecurrenceFrequency.YEARLY,
                    "Custom" to RecurrenceFrequency.CUSTOM
                )

                presets.forEach { (lbl, freqVal) ->
                    val isSelected = frequency == freqVal
                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .clip(ReflexTokens.ShapeChip)
                            .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                border = BorderStroke(
                                    ReflexTokens.BorderHairline,
                                    if (isSelected) CopperPrimary else outlineColor
                                ),
                                shape = ReflexTokens.ShapeChip
                            )
                            .clickable {
                                frequency = freqVal
                                if (freqVal == RecurrenceFrequency.DAILY) {
                                    interval = 1
                                    unit = RecurrenceUnit.DAY
                                } else if (freqVal == RecurrenceFrequency.WEEKLY) {
                                    interval = 1
                                    unit = RecurrenceUnit.WEEK
                                } else if (freqVal == RecurrenceFrequency.MONTHLY) {
                                    interval = 1
                                    unit = RecurrenceUnit.MONTH
                                } else if (freqVal == RecurrenceFrequency.YEARLY) {
                                    interval = 1
                                    unit = RecurrenceUnit.YEAR
                                }
                            }
                            .padding(horizontal = ReflexTokens.SpaceMd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lbl,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Custom Recurrence Builder UI
            if (frequency == RecurrenceFrequency.CUSTOM || frequency == RecurrenceFrequency.WEEKLY) {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                ReflexCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text(
                            text = "Recurrence builder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                        // Interval Stepper + Unit Selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Repeat every", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = "Dec Interval",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = "$interval",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = ReflexTokens.SpaceXs)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Inc Interval",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        if (frequency == RecurrenceFrequency.CUSTOM) {
                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                            ) {
                                RecurrenceUnit.entries.forEach { u ->
                                    val isSel = unit == u
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(ReflexTokens.ShapeChip)
                                            .background(if (isSel) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                            .border(
                                                BorderStroke(ReflexTokens.BorderHairline, if (isSel) CopperPrimary else outlineColor),
                                                ReflexTokens.ShapeChip
                                            )
                                            .clickable { unit = u },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            u.name.lowercase().replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Days of week multi-select (for WEEK unit or WEEKLY)
                        if (unit == RecurrenceUnit.WEEK || frequency == RecurrenceFrequency.WEEKLY) {
                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                            Text("Days of week", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceXs)
                            ) {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { d ->
                                    val isSel = selectedDaysOfWeek.contains(d.uppercase())
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .clip(ReflexTokens.ShapeChip)
                                            .background(if (isSel) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                            .border(
                                                BorderStroke(ReflexTokens.BorderHairline, if (isSel) CopperPrimary else outlineColor),
                                                ReflexTokens.ShapeChip
                                            )
                                            .clickable {
                                                selectedDaysOfWeek = if (isSel) {
                                                    selectedDaysOfWeek - d.uppercase()
                                                } else {
                                                    selectedDaysOfWeek + d.uppercase()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            d,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                        // Repeat Basis Toggle
                        Text("Repeat basis", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)) {
                            val isDueBasis = recurrenceBasis == RecurrenceBasis.FROM_DUE_DATE
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(if (isDueBasis) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                    .border(
                                        BorderStroke(ReflexTokens.BorderHairline, if (isDueBasis) CopperPrimary else outlineColor),
                                        ReflexTokens.ShapeChip
                                    )
                                    .clickable { recurrenceBasis = RecurrenceBasis.FROM_DUE_DATE },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "From due date",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDueBasis) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(if (!isDueBasis) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                    .border(
                                        BorderStroke(ReflexTokens.BorderHairline, if (!isDueBasis) CopperPrimary else outlineColor),
                                        ReflexTokens.ShapeChip
                                    )
                                    .clickable { recurrenceBasis = RecurrenceBasis.FROM_COMPLETION_DATE },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "From completion",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (!isDueBasis) ActionPillOnWhite else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Write-back to device calendar toggle
            if (selectedDate != null) {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ReflexTokens.ShapeCard)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), ReflexTokens.ShapeCard)
                        .clickable { addToCalendar = !addToCalendar }
                        .padding(ReflexTokens.SpaceMd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (addToCalendar) CopperPrimary else Color.Transparent)
                            .border(BorderStroke(ReflexTokens.BorderHairline, if (addToCalendar) CopperPrimary else outlineColor), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (addToCalendar) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ActionPillOnWhite
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(ReflexTokens.SpaceSm))
                    Text(
                        text = "Sync to device calendar",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
            ) {
                ReflexButton(
                    text = "Delete",
                    onClick = {
                        onDelete(task)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    variant = ReflexButtonVariant.DESTRUCTIVE
                )

                ReflexButton(
                    text = "Save task",
                    onClick = {
                        val dateMillis = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                        val timeMillis = if (selectedTime != null) {
                            val dateToUse = selectedDate ?: LocalDate.now()
                            dateToUse.atTime(selectedTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                        } else null

                        val reminderMillis = timeMillis ?: (if (dateMillis != null) dateMillis + (9 * 3600000L) else null)

                        val updatedTask = task.copy(
                            title = title.ifBlank { "Untitled Task" },
                            notes = notes.ifBlank { null },
                            priority = priority,
                            dueDate = dateMillis,
                            dueTime = timeMillis,
                            reminderTime = reminderMillis,
                            recurrenceFrequency = frequency,
                            recurrenceInterval = interval,
                            recurrenceUnit = unit,
                            recurrenceDaysOfWeek = if (selectedDaysOfWeek.isNotEmpty()) selectedDaysOfWeek.joinToString(",") else null,
                            recurrenceEndType = endType,
                            recurrenceEndOccurrences = endOccurrences,
                            recurrenceBasis = recurrenceBasis
                        )

                        if (addToCalendar && dateMillis != null) {
                            val startMs = timeMillis ?: dateMillis
                            val endMs = startMs + 3600000L
                            val eventId = CalendarProviderHelper.addEventToDeviceCalendar(
                                context = context,
                                title = updatedTask.title,
                                description = updatedTask.notes,
                                startMillis = startMs,
                                endMillis = endMs
                            )
                            if (eventId == null) {
                                CalendarProviderHelper.launchAddEventIntent(
                                    context = context,
                                    title = updatedTask.title,
                                    description = updatedTask.notes,
                                    startMillis = startMs,
                                    endMillis = endMs
                                )
                            }
                        }

                        onSave(updatedTask)
                        onDismiss()
                    },
                    modifier = Modifier.weight(2f),
                    variant = ReflexButtonVariant.PRIMARY
                )
            }
        }
    }
}
