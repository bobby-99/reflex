package com.reflex.app.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.util.CalendarPreferenceRepository
import com.reflex.app.util.CalendarProviderHelper
import com.reflex.app.util.DeviceCalendar
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Google Calendar style Event Creation Sheet for adding events directly to Android / Google Calendar.
 * Supports: Title, Date & Time pickers, All-day toggle, Calendar account selection, Location, and Description.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventSheet(
    initialDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onEventCreated: () -> Unit
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isAllDay by remember { mutableStateOf(false) }

    // Start & End date-times
    var startDate by remember { mutableStateOf(initialDate) }
    var startTime by remember {
        val now = LocalTime.now().plusHours(1).withMinute(0)
        mutableStateOf(now)
    }
    var endDate by remember { mutableStateOf(initialDate) }
    var endTime by remember {
        val later = LocalTime.now().plusHours(2).withMinute(0)
        mutableStateOf(later)
    }

    // Available calendars
    var availableCalendars by remember { mutableStateOf<List<DeviceCalendar>>(emptyList()) }
    var selectedCalendarId by remember { mutableStateOf<Long?>(null) }
    var showCalendarDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (CalendarProviderHelper.hasReadPermission(context)) {
            val cals = CalendarProviderHelper.getAvailableCalendars(context)
            availableCalendars = cals
            selectedCalendarId = cals.firstOrNull { it.isPrimary }?.id ?: cals.firstOrNull()?.id
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val cals = CalendarProviderHelper.getAvailableCalendars(context)
            availableCalendars = cals
            if (selectedCalendarId == null) {
                selectedCalendarId = cals.firstOrNull { it.isPrimary }?.id ?: cals.firstOrNull()?.id
            }
        }
    }

    fun saveEvent() {
        if (title.isBlank()) return

        val zone = ZoneId.systemDefault()
        val startMillis: Long
        val endMillis: Long

        if (isAllDay) {
            startMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
            endMillis = endDate.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        } else {
            val startDt = LocalDateTime.of(startDate, startTime)
            val endDt = LocalDateTime.of(endDate, endTime)
            startMillis = startDt.atZone(zone).toInstant().toEpochMilli()
            endMillis = if (endDt.isAfter(startDt)) endDt.atZone(zone).toInstant().toEpochMilli()
            else startDt.plusHours(1).atZone(zone).toInstant().toEpochMilli()
        }

        if (CalendarProviderHelper.hasWritePermission(context)) {
            val eventId = CalendarProviderHelper.addEventToDeviceCalendar(
                context = context,
                title = title.trim(),
                description = description.trim().ifEmpty { null },
                startMillis = startMillis,
                endMillis = endMillis,
                allDay = isAllDay,
                location = location.trim().ifEmpty { null },
                calendarId = selectedCalendarId
            )
            if (eventId != null) {
                onEventCreated()
                onDismiss()
            } else {
                // Fallback to intent if provider insertion fails
                CalendarProviderHelper.launchAddEventIntent(
                    context = context,
                    title = title.trim(),
                    description = description.trim().ifEmpty { null },
                    startMillis = startMillis,
                    endMillis = endMillis,
                    allDay = isAllDay,
                    location = location.trim().ifEmpty { null }
                )
                onDismiss()
            }
        } else {
            // Request permission or launch intent
            CalendarProviderHelper.launchAddEventIntent(
                context = context,
                title = title.trim(),
                description = description.trim().ifEmpty { null },
                startMillis = startMillis,
                endMillis = endMillis,
                allDay = isAllDay,
                location = location.trim().ifEmpty { null }
            )
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 14.dp)
                    .size(width = ReflexTokens.FocusSheetDragHandleWidth, height = ReflexTokens.FocusSheetDragHandleHeight)
                    .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row: "New event" and "Save" pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CopperContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = CopperPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "New event",
                        fontFamily = Lora,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Save action pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (title.isNotBlank()) CopperPrimary else MaterialTheme.colorScheme.outlineVariant)
                        .clickable(enabled = title.isNotBlank()) { saveEvent() }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        fontFamily = Lora,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (title.isNotBlank()) OnCopper else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Add title", fontFamily = Lora, fontSize = 18.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Lora,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopperPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = CopperPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Calendar selector (if multiple calendars available)
            if (availableCalendars.isNotEmpty()) {
                val selectedCal = availableCalendars.find { it.id == selectedCalendarId } ?: availableCalendars.first()
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { showCalendarDropdown = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selectedCal.color != 0) Color(selectedCal.color) else CopperPrimary)
                        )
                        Text(
                            text = selectedCal.name,
                            fontFamily = Lora,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showCalendarDropdown,
                        onDismissRequest = { showCalendarDropdown = false }
                    ) {
                        availableCalendars.forEach { cal ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (cal.color != 0) Color(cal.color) else CopperPrimary)
                                        )
                                        Text(cal.name, fontFamily = Lora, fontSize = 14.sp)
                                    }
                                },
                                onClick = {
                                    selectedCalendarId = cal.id
                                    showCalendarDropdown = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // All-day switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "All-day",
                        fontFamily = Lora,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                ReflexSwitch(
                    checked = isAllDay,
                    onCheckedChange = { isAllDay = it }
                )
            }

            // Start Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Starts",
                    fontFamily = Lora,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Date Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        startDate = LocalDate.of(y, m + 1, d)
                                        if (endDate.isBefore(startDate)) endDate = startDate
                                    },
                                    startDate.year,
                                    startDate.monthValue - 1,
                                    startDate.dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = startDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Time Chip (if not all day)
                    if (!isAllDay) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, min ->
                                            startTime = LocalTime.of(h, min)
                                            endTime = startTime.plusHours(1)
                                        },
                                        startTime.hour,
                                        startTime.minute,
                                        false
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = startTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // End Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Ends",
                    fontFamily = Lora,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Date Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        endDate = LocalDate.of(y, m + 1, d)
                                        if (endDate.isBefore(startDate)) startDate = endDate
                                    },
                                    endDate.year,
                                    endDate.monthValue - 1,
                                    endDate.dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = endDate.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())),
                            fontFamily = Lora,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Time Chip (if not all day)
                    if (!isAllDay) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, h, min -> endTime = LocalTime.of(h, min) },
                                        endTime.hour,
                                        endTime.minute,
                                        false
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = endTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())),
                                fontFamily = Lora,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

            // Location Input
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = { Text("Add location", fontFamily = Lora) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopperPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = CopperPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                placeholder = { Text("Add description or notes", fontFamily = Lora) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CopperPrimary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    cursorColor = CopperPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
