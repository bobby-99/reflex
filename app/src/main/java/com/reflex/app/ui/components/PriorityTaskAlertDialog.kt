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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Task
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.DestructiveContainer
import com.reflex.app.ui.theme.ReflexTokens
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class SnoozeOption(val label: String, val minutes: Long) {
    FIVE_MIN("5 min", 5),
    TEN_MIN("10 min", 10),
    FIFTEEN_MIN("15 min", 15),
    THIRTY_MIN("30 min", 30),
    ONE_HOUR("1 hour", 60),
    TOMORROW("Tomorrow", -1) // Special flag for tomorrow 9am
}

@Composable
fun PriorityTaskAlertDialog(
    task: Task,
    onComplete: (Task) -> Unit,
    onSnooze: (Task, Long) -> Unit,
    onOpenTask: (Task) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            PriorityTaskAlertCard(
                task = task,
                onComplete = onComplete,
                onSnooze = onSnooze,
                onOpenTask = onOpenTask,
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriorityTaskAlertCard(
    task: Task,
    onComplete: (Task) -> Unit,
    onSnooze: (Task, Long) -> Unit,
    onOpenTask: (Task) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isHigh = task.priority == Priority.HIGH
    val accentColor = if (isHigh) MaterialTheme.colorScheme.error else CopperPrimary
    val badgeBg = if (isHigh) DestructiveContainer else CopperSubtle
    val badgeText = if (isHigh) "High priority alert" else "Medium priority alert"

    var selectedSnooze by remember { mutableStateOf(SnoozeOption.TEN_MIN) }

    val dueStr = remember(task) {
        val timeMs = task.dueTime ?: task.reminderTime ?: task.dueDate
        if (timeMs != null) {
            val dateTime = Instant.ofEpochMilli(timeMs).atZone(ZoneId.systemDefault())
            dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d • h:mm a"))
        } else "Due now"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = ReflexTokens.ShapeDialog,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
        shadowElevation = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ReflexTokens.SpaceXl)
                .verticalScroll(rememberScrollState())
        ) {
            // ── TOP HEADER: PRIORITY BADGE + DISMISS BUTTON ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(ReflexTokens.ShapeButton)
                        .background(badgeBg)
                        .border(BorderStroke(0.75.dp, accentColor.copy(alpha = 0.5f)), ReflexTokens.ShapeButton)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = 13.sp,
                            letterSpacing = 0.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // ── TASK TITLE ──
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 28.sp
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            // ── DUE TIME & REPEAT BADGES ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                Box(
                    modifier = Modifier
                        .clip(ReflexTokens.ShapeButton)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant), ReflexTokens.ShapeButton)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = CopperPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = dueStr,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = CopperPrimary,
                            fontSize = 13.sp
                        )
                    }
                }

                if (task.recurrenceFrequency != RecurrenceFrequency.NONE) {
                    Box(
                        modifier = Modifier
                            .clip(ReflexTokens.ShapeButton)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant), ReflexTokens.ShapeButton)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = "Recurring",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.recurrenceFrequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── NOTES (IF ANY) ──
            if (!task.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ReflexTokens.ShapeChip)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant), ReflexTokens.ShapeChip)
                        .padding(ReflexTokens.SpaceMd)
                ) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

            // ── PRIMARY ACTIONS: MARK DONE & OPEN TASK ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
            ) {
                ReflexButton(
                    text = "Mark done",
                    onClick = { onComplete(task) },
                    modifier = Modifier.weight(1.3f),
                    variant = ReflexButtonVariant.PRIMARY
                )

                ReflexButton(
                    text = "Open",
                    onClick = { onOpenTask(task) },
                    modifier = Modifier.weight(0.9f),
                    variant = ReflexButtonVariant.SECONDARY
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

            // ── SNOOZE SECTION ──
            Text(
                text = "Snooze reminder",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            // Snooze Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SnoozeOption.entries.forEach { option ->
                    val isSel = selectedSnooze == option
                    Box(
                        modifier = Modifier
                            .clip(ReflexTokens.ShapeChip)
                            .background(if (isSel) CopperPrimary else MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                BorderStroke(
                                    ReflexTokens.BorderHairline,
                                    if (isSel) CopperPrimary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                ReflexTokens.ShapeChip
                            )
                            .clickable { selectedSnooze = option }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) ActionPillOnWhite else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

            // Snooze Confirm Button
            ReflexButton(
                text = "Snooze for ${selectedSnooze.label}",
                onClick = {
                    val targetMillis = if (selectedSnooze == SnoozeOption.TOMORROW) {
                        val tomorrow = LocalDate.now().plusDays(1)
                        LocalDateTime.of(tomorrow, LocalTime.of(9, 0))
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    } else {
                        System.currentTimeMillis() + (selectedSnooze.minutes * 60 * 1000L)
                    }
                    onSnooze(task, targetMillis)
                },
                modifier = Modifier.fillMaxWidth(),
                variant = ReflexButtonVariant.SECONDARY
            )
        }
    }
}
