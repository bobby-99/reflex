package com.reflex.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.SectionHeader
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.OnCopper
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.eventBlue
import com.reflex.app.util.CalendarPreferenceRepository
import com.reflex.app.util.CalendarProviderHelper
import com.reflex.app.util.DeviceCalendar
import java.time.DayOfWeek

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarSettingsScreen(
    onNavigateBack: () -> Unit
) {
    SetStatusBarAppearance()

    val context = LocalContext.current
    val prefs by CalendarPreferenceRepository.preferences.collectAsState()
    val outlineColor = MaterialTheme.colorScheme.outline
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCalendarPermission by remember {
        mutableStateOf(CalendarProviderHelper.hasReadPermission(context) && CalendarProviderHelper.hasWritePermission(context))
    }

    var availableCalendars by remember {
        mutableStateOf<List<DeviceCalendar>>(emptyList())
    }

    var calendarToRename by remember { mutableStateOf<DeviceCalendar?>(null) }
    var renameDialogText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val readGranted = permissions[Manifest.permission.READ_CALENDAR] == true
        val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] == true
        val allGranted = readGranted && writeGranted
        hasCalendarPermission = allGranted
        if (allGranted) {
            availableCalendars = CalendarProviderHelper.getAvailableCalendars(context)
        } else {
            val activity = context.findActivity()
            val permanentlyDenied = activity != null && permissions.any { (perm, granted) ->
                !granted && !ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
            }
            if (permanentlyDenied) {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Enable Calendar in app settings", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {}
            } else {
                Toast.makeText(context, "Calendar permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = CalendarProviderHelper.hasReadPermission(context) &&
                              CalendarProviderHelper.hasWritePermission(context)
                hasCalendarPermission = granted
                if (granted) {
                    availableCalendars = CalendarProviderHelper.getAvailableCalendars(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(hasCalendarPermission) {
        if (hasCalendarPermission) {
            availableCalendars = CalendarProviderHelper.getAvailableCalendars(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = "Calendar settings",
            onBackClick = onNavigateBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ReflexTokens.SpaceLg, vertical = ReflexTokens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceLg)
        ) {
            // ── SECTION 1: AGENDA FILTERS ──
            SectionHeader(
                title = "Agenda filters",
                subtitle = "Control what appears on the chronological calendar timeline"
            )

            ReflexCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                borderColor = outlineColor,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                    // Exclude Repeating Tasks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exclude repeating tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hide recurring task instances to keep agenda uncluttered",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.excludeRepeatingTasks,
                            onCheckedChange = { CalendarPreferenceRepository.setExcludeRepeatingTasks(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnCopper,
                                checkedTrackColor = CopperPrimary,
                                uncheckedThumbColor = outlineColor,
                                uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(outlineVariant))

                    // Exclude Completed Tasks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exclude completed tasks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Hide finished tasks from appearing in the calendar agenda",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.excludeCompletedTasks,
                            onCheckedChange = { CalendarPreferenceRepository.setExcludeCompletedTasks(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnCopper,
                                checkedTrackColor = CopperPrimary,
                                uncheckedThumbColor = outlineColor,
                                uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(outlineVariant))

                    // Show Routine Reminders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Show routine reminders",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display routine schedules on their respective weekdays",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = prefs.showRoutineReminders,
                            onCheckedChange = { CalendarPreferenceRepository.setShowRoutineReminders(context, it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnCopper,
                                checkedTrackColor = CopperPrimary,
                                uncheckedThumbColor = outlineColor,
                                uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }

            // ── SECTION 2: VIEW PREFERENCES ──
            SectionHeader(
                title = "View preferences",
                subtitle = "Customize calendar range and layout behavior"
            )

            ReflexCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                borderColor = outlineColor,
                contentPadding = PaddingValues(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                    // Agenda range: Segmented pill selector (44dp tall)
                    Column {
                        Text(
                            text = "Agenda range",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "How many days ahead the agenda timeline displays",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(ReflexTokens.SpaceSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                        ) {
                            listOf(7, 14, 30, 60).forEach { days ->
                                val isSelected = prefs.agendaRangeDays == days
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(ReflexTokens.ShapeChip)
                                        .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            BorderStroke(ReflexTokens.BorderHairline, if (isSelected) CopperPrimary else outlineColor),
                                            ReflexTokens.ShapeChip
                                        )
                                        .clickable { CalendarPreferenceRepository.setAgendaRangeDays(context, days) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$days days",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) OnCopper else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(outlineVariant))

                    // First day of week: Segmented pill selector (44dp tall)
                    Column {
                        Text(
                            text = "First day of week",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Starting day for week strips and monthly grids",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(ReflexTokens.SpaceSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                        ) {
                            listOf(DayOfWeek.MONDAY, DayOfWeek.SUNDAY).forEach { day ->
                                val isSelected = prefs.firstDayOfWeek == day
                                val label = if (day == DayOfWeek.MONDAY) "Monday" else "Sunday"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(ReflexTokens.ShapeChip)
                                        .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            BorderStroke(ReflexTokens.BorderHairline, if (isSelected) CopperPrimary else outlineColor),
                                            ReflexTokens.ShapeChip
                                        )
                                        .clickable { CalendarPreferenceRepository.setFirstDayOfWeek(context, day) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) OnCopper else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(outlineVariant))

                    // Default landing view: Segmented pill selector (44dp tall)
                    Column {
                        Text(
                            text = "Default landing view",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Initial view mode when opening the Calendar tab on cold start",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(ReflexTokens.SpaceSm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                        ) {
                            val viewOptions = listOf(
                                "WEEK_STRIP" to "Week strip",
                                "MONTH_GRID" to "Month grid"
                            )
                            viewOptions.forEach { (key, label) ->
                                val isSelected = prefs.defaultLandingView == key
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(ReflexTokens.ShapeChip)
                                        .background(if (isSelected) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                                        .border(
                                            BorderStroke(ReflexTokens.BorderHairline, if (isSelected) CopperPrimary else outlineColor),
                                            ReflexTokens.ShapeChip
                                        )
                                        .clickable { CalendarPreferenceRepository.setDefaultLandingView(context, key) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) OnCopper else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── SECTION 3: DEVICE CALENDARS ──
            SectionHeader(
                title = "Device calendars",
                subtitle = "Include events from your device's calendar accounts"
            )

            ReflexCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                borderColor = outlineColor,
                contentPadding = PaddingValues(16.dp)
            ) {
                val enabledCalIds = prefs.enabledCalendarIds
                var toggleState by remember(hasCalendarPermission, enabledCalIds) {
                    mutableStateOf(hasCalendarPermission && (enabledCalIds == null || enabledCalIds.isNotEmpty()))
                }

                Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                    // Sync Device Calendars Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sync device calendars",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Display events from device calendars on your timeline",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = toggleState,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!hasCalendarPermission) {
                                        // Request permission, leave toggle off when permission is missing
                                        toggleState = false
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.READ_CALENDAR,
                                                Manifest.permission.WRITE_CALENDAR
                                            )
                                        )
                                    } else {
                                        toggleState = true
                                        if (prefs.enabledCalendarIds?.isEmpty() == true) {
                                            val allIds = availableCalendars.map { it.id }.toSet()
                                            CalendarPreferenceRepository.setEnabledCalendarIds(context, if (allIds.isNotEmpty()) allIds else null)
                                        }
                                    }
                                } else {
                                    toggleState = false
                                    CalendarPreferenceRepository.setEnabledCalendarIds(context, emptySet())
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = OnCopper,
                                checkedTrackColor = CopperPrimary,
                                uncheckedThumbColor = outlineColor,
                                uncheckedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(outlineVariant))

                    if (!hasCalendarPermission) {
                        Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = CopperPrimary
                                )
                                Spacer(Modifier.width(ReflexTokens.SpaceSm))
                                Text(
                                    text = "Calendar permission required",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Grant permission to read events from Google Calendar, Outlook, and local accounts directly on-device without cloud sync.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(ReflexTokens.SpaceSm))
                            ReflexButton(
                                text = "Grant calendar access",
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.READ_CALENDAR,
                                            Manifest.permission.WRITE_CALENDAR
                                        )
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                variant = ReflexButtonVariant.PRIMARY
                            )
                        }
                    } else if (availableCalendars.isEmpty()) {
                        Text(
                            text = "No device calendar accounts found on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val currentEnabled = prefs.enabledCalendarIds ?: availableCalendars.map { it.id }.toSet()

                    Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                        // Subtitle
                        Text(
                            text = "${currentEnabled.size} of ${availableCalendars.size} calendars visible",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = CopperPrimary
                        )

                        // Group calendars by account
                        val groupedByAccount = remember(availableCalendars) {
                            availableCalendars.groupBy { it.accountName ?: "Local account" }
                        }

                        groupedByAccount.forEach { (accountName, calendarsInAccount) ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                            ) {
                                // Account header with Select all / Clear all
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = accountName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)) {
                                        Text(
                                            text = "Select all",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = CopperPrimary,
                                            modifier = Modifier.clickable {
                                                val accountIds = calendarsInAccount.map { it.id }.toSet()
                                                val newSet = currentEnabled + accountIds
                                                CalendarPreferenceRepository.setEnabledCalendarIds(context, newSet)
                                            }
                                        )
                                        Text(
                                            text = "Clear all",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.clickable {
                                                val accountIds = calendarsInAccount.map { it.id }.toSet()
                                                val newSet = currentEnabled - accountIds
                                                CalendarPreferenceRepository.setEnabledCalendarIds(context, newSet)
                                            }
                                        )
                                    }
                                }

                                // Calendars in account
                                calendarsInAccount.forEach { cal ->
                                    val isChecked = currentEnabled.contains(cal.id)
                                    val stableKey = CalendarPreferenceRepository.getCalendarStableKey(
                                        cal.accountName, cal.syncId, cal.name
                                    )
                                    val resolvedName = CalendarProviderHelper.resolveDisplayName(
                                        cal, prefs.calendarDisplayNameOverrides
                                    )
                                    val nameCollision = availableCalendars.count { it.name == cal.name } > 1
                                    val swatchColor = if (cal.color != 0) Color(cal.color) else MaterialTheme.colorScheme.eventBlue

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .border(BorderStroke(ReflexTokens.BorderHairline, outlineColor), RoundedCornerShape(16.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    val newSet = if (isChecked) {
                                                        currentEnabled - cal.id
                                                    } else {
                                                        currentEnabled + cal.id
                                                    }
                                                    CalendarPreferenceRepository.setEnabledCalendarIds(context, newSet)
                                                },
                                                onLongClick = {
                                                    calendarToRename = cal
                                                    renameDialogText = resolvedName
                                                }
                                            )
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 12dp colored circle swatch
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor)
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        // Name & subtitle
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resolvedName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (nameCollision && !cal.accountName.isNullOrBlank()) {
                                                Text(
                                                    text = cal.accountName,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(8.dp))

                                        // 48dp Checkbox touch target with 22dp circle
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clickable {
                                                    val newSet = if (isChecked) {
                                                        currentEnabled - cal.id
                                                    } else {
                                                        currentEnabled + cal.id
                                                    }
                                                    CalendarPreferenceRepository.setEnabledCalendarIds(context, newSet)
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isChecked) CopperPrimary else Color.Transparent)
                                                    .border(
                                                        BorderStroke(
                                                            1.5.dp,
                                                            if (isChecked) CopperPrimary else outlineColor
                                                        ),
                                                        CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isChecked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = OnCopper,
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

            Spacer(Modifier.height(ReflexTokens.SpaceXxl))
        }
    }

    // Rename Calendar Dialog (on long-press)
    calendarToRename?.let { cal ->
        val stableKey = CalendarPreferenceRepository.getCalendarStableKey(cal.accountName, cal.syncId, cal.name)
        val hasCustomName = prefs.calendarDisplayNameOverrides.containsKey(stableKey)

        AlertDialog(
            onDismissRequest = { calendarToRename = null },
            title = {
                Text(
                    text = "Rename calendar",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)) {
                    Text(
                        text = "Original: ${cal.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = renameDialogText,
                        onValueChange = { renameDialogText = it },
                        singleLine = true,
                        placeholder = { Text(cal.name) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CopperPrimary,
                            cursorColor = CopperPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = renameDialogText.trim()
                        if (trimmed.isNotBlank() && trimmed != cal.name) {
                            CalendarPreferenceRepository.setCalendarDisplayName(context, stableKey, trimmed)
                        } else {
                            CalendarPreferenceRepository.setCalendarDisplayName(context, stableKey, null)
                        }
                        calendarToRename = null
                    }
                ) {
                    Text(
                        text = "Save",
                        fontWeight = FontWeight.Bold,
                        color = CopperPrimary
                    )
                }
            },
            dismissButton = {
                Row {
                    if (hasCustomName) {
                        TextButton(
                            onClick = {
                                CalendarPreferenceRepository.setCalendarDisplayName(context, stableKey, null)
                                calendarToRename = null
                            }
                        ) {
                            Text(
                                text = "Reset to default",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    TextButton(onClick = { calendarToRename = null }) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = RoundedCornerShape(26.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
}

private fun Context.findActivity(): android.app.Activity? {
    var ctx = this
    while (ctx is android.content.ContextWrapper) {
        if (ctx is android.app.Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

