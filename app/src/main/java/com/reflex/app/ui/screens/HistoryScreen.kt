package com.reflex.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.ReflexRepository
import com.reflex.app.ui.components.EmptyState
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.SectionHeader
import com.reflex.app.ui.components.StatTile
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperDark
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.CopperSubtle
import com.reflex.app.ui.theme.DestructiveContainer
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.util.StreakCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    routineId: Long = 0L,
    onNavigateBack: () -> Unit
) {
    SetStatusBarAppearance()
    val context = LocalContext.current

    val repository = remember(context) {
        val db = ReflexDatabase.getDatabase(context.applicationContext)
        ReflexRepository(db.routineDao(), db.stepDao(), db.completionLogDao(), db.taskDao())
    }

    val routineWithSteps by repository.getRoutineWithSteps(routineId).collectAsState(initial = null)
    val logsFlow = remember(routineId) {
        if (routineId != 0L) repository.getLogsForRoutine(routineId) else repository.getAllLogs()
    }
    val logs by logsFlow.collectAsState(initial = emptyList())

    val routine = routineWithSteps?.routine
    val streakResult = remember(logs, routine) { StreakCalculator.calculateStreaks(logs, routine) }
    val titleText = routine?.name ?: "Routine history"

    val currentMonth = remember { YearMonth.now() }
    val daysInMonth = remember(currentMonth) {
        (1..currentMonth.lengthOfMonth()).map { day ->
            currentMonth.atDay(day)
        }
    }

    val logsByDate = remember(logs) {
        logs.groupBy { log ->
            Instant.ofEpochMilli(log.dateCompleted)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    }

    val today = remember { LocalDate.now() }
    val scheduledDays = remember(routine) { routine?.scheduledDaysSet ?: emptySet() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = titleText,
            onBackClick = onNavigateBack
        )

        if (logs.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Check,
                message = "No completion history recorded yet",
                actionLabel = "Go back",
                onAction = onNavigateBack
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = ReflexTokens.SpaceLg),
                verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceMd)
            ) {
                item {
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                    // ── STATS ROW (3 STAT TILES) ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                    ) {
                        StatTile(
                            label = "Current",
                            value = "${streakResult.currentStreak}",
                            subValue = "days",
                            icon = Icons.Default.LocalFireDepartment,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Best",
                            value = "${streakResult.bestStreak}",
                            subValue = "days",
                            icon = Icons.Default.EmojiEvents,
                            modifier = Modifier.weight(1f)
                        )
                        StatTile(
                            label = "Total",
                            value = "${logs.count { it.isCompleted }}",
                            subValue = "sessions",
                            icon = Icons.Default.FitnessCenter,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── MONTH CONSISTENCY HEATMAP GRID ──
                item {
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                    SectionHeader(
                        title = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        subtitle = "Execution consistency and completion state"
                    )
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                    ReflexCard(
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        borderColor = MaterialTheme.colorScheme.outline
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Days of week header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                    Text(
                                        text = day,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                            val firstDayOfWeek = currentMonth.atDay(1).dayOfWeek.value % 7
                            val totalSlots = firstDayOfWeek + daysInMonth.size
                            val rows = (totalSlots + 6) / 7

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (r in 0 until rows) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        for (c in 0 until 7) {
                                            val dayIndex = r * 7 + c - firstDayOfWeek
                                            if (dayIndex in daysInMonth.indices) {
                                                val date = daysInMonth[dayIndex]
                                                val dayLogs = logsByDate[date] ?: emptyList()
                                                val hasCompletedOnTime = dayLogs.any { it.isCompleted }
                                                val hasSkipped = dayLogs.any { it.isSkipped }
                                                val nextDayLogs = logsByDate[date.plusDays(1)] ?: emptyList()
                                                val hasCompletedLate = !hasCompletedOnTime && !hasSkipped && nextDayLogs.any { it.isCompleted }
                                                val isScheduled = scheduledDays.isEmpty() || scheduledDays.contains(date.dayOfWeek)
                                                val isPastDate = date.isBefore(today)

                                                // 4 Distinct Visual States
                                                val (bgColor, borderColor, textColor) = when {
                                                    hasCompletedOnTime -> Triple(CopperPrimary, CopperPrimary, ActionPillOnWhite)
                                                    hasCompletedLate -> Triple(CopperSubtle, CopperPrimary, CopperPrimary)
                                                    hasSkipped -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurfaceVariant)
                                                    isScheduled && isPastDate && dayLogs.isEmpty() -> Triple(DestructiveContainer, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), MaterialTheme.colorScheme.error)
                                                    else -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .clip(ReflexTokens.ShapeChip)
                                                        .background(bgColor)
                                                        .border(BorderStroke(ReflexTokens.BorderHairline, borderColor), ReflexTokens.ShapeChip),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "${date.dayOfMonth}",
                                                        fontSize = 13.sp,
                                                        fontWeight = if (hasCompletedOnTime) FontWeight.Bold else FontWeight.Normal,
                                                        color = textColor
                                                    )
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                            // Legend Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendItem(color = CopperPrimary, label = "On-time")
                                LegendItem(color = CopperPrimary, isOutline = true, label = "Late")
                                LegendItem(color = MaterialTheme.colorScheme.onSurfaceVariant, label = "Skipped")
                                LegendItem(color = MaterialTheme.colorScheme.error, label = "Missed")
                            }
                        }
                    }
                }

                // ── COMPLETION LOGS SECTION ──
                item {
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                    SectionHeader(
                        title = "Completion logs",
                        subtitle = "${logs.size} sessions recorded"
                    )
                }

                items(logs, key = { it.id }) { log ->
                    CompletionLogRow(log = log)
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isOutline: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isOutline) Color.Transparent else color)
                .then(
                    if (isOutline) Modifier.border(BorderStroke(1.dp, color), CircleShape)
                    else Modifier
                )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CompletionLogRow(log: CompletionLog) {
    val dateStr = Instant.ofEpochMilli(log.dateCompleted)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy • h:mm a"))

    val mins = log.totalTimeTakenSeconds / 60
    val secs = log.totalTimeTakenSeconds % 60
    val durationStr = String.format("%02d:%02d", mins, secs)

    val (badgeText, badgeBg, badgeTextColor) = when {
        log.isCompleted -> Triple("Completed", CopperSubtle, CopperPrimary)
        log.isSkipped -> Triple("Skipped", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSurfaceVariant)
        else -> Triple("Exited early", DestructiveContainer, MaterialTheme.colorScheme.error)
    }

    ReflexCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(ReflexTokens.ShapeButton)
                        .background(badgeBg)
                        .border(BorderStroke(0.75.dp, badgeTextColor.copy(alpha = 0.5f)), ReflexTokens.ShapeButton)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        fontSize = 13.sp,
                        letterSpacing = 0.sp
                    )
                }

                Spacer(modifier = Modifier.height(ReflexTokens.SpaceXs))

                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${log.stepsCompletedCount} steps",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            }
        }
    }
}
