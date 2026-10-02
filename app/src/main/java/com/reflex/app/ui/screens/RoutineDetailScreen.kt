package com.reflex.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.RoutineIcon
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.SectionHeader
import com.reflex.app.ui.components.StatTile
import com.reflex.app.ui.components.StepTypeChip
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperOnContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.util.StreakCalculator
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RoutineDetailScreen(
    routineId: Long,
    onStartRoutine: () -> Unit,
    onHistoryClick: () -> Unit,
    onNavigateBack: () -> Unit
) {
    SetStatusBarAppearance()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val repository = remember(context) {
        val db = ReflexDatabase.getDatabase(context.applicationContext)
        ReflexRepository(db.routineDao(), db.stepDao(), db.completionLogDao(), db.taskDao())
    }

    val routineWithSteps by repository.getRoutineWithSteps(routineId).collectAsState(initial = null)
    val logs by repository.getLogsForRoutine(routineId).collectAsState(initial = emptyList())
    val routine = routineWithSteps?.routine
    val streakResult = remember(logs, routine) { StreakCalculator.calculateStreaks(logs, routine) }

    val steps = routineWithSteps?.steps ?: emptyList()
    val vectorIcon = RoutineIcon.fromKey(routine?.icon ?: "BOLT").icon

    val totalSeconds = steps.sumOf { step ->
        if (step.stepType == com.reflex.app.data.StepType.TIMED) (step.durationSeconds ?: 0) else 60
    }
    val estimatedMins = (totalSeconds / 60).coerceAtLeast(1)

    val today = remember { LocalDate.now() }
    val isScheduledToday = remember(routine, today) {
        routine?.scheduledDaysSet?.contains(today.dayOfWeek) == true
    }

    val todayLog = remember(logs, today) {
        logs.firstOrNull { log ->
            Instant.ofEpochMilli(log.dateCompleted)
                .atZone(ZoneId.systemDefault())
                .toLocalDate() == today
        }
    }
    val isSkippedToday = todayLog?.isSkipped == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = routine?.name ?: "Routine",
            onBackClick = onNavigateBack,
            actions = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline), CircleShape)
                        .clickable(onClick = onHistoryClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        modifier = Modifier.size(ReflexTokens.IconSm),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ReflexTokens.SpaceLg),
            verticalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceLg)
        ) {
            item { Spacer(Modifier.height(ReflexTokens.SpaceXs)) }

            // --- 1. HERO ROUTINE INFO CARD ---
            item {
                ReflexCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Soft Rounded Avatar Container (52dp, 18dp radius)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(ReflexTokens.ShapeIconTile)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(
                                    border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
                                    shape = ReflexTokens.ShapeIconTile
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = vectorIcon,
                                contentDescription = null,
                                modifier = Modifier.size(26.dp),
                                tint = CopperPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine?.name ?: "Routine",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "${steps.size} ${if (steps.size == 1) "step" else "steps"} • ~$estimatedMins mins",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Streak Badge
                        if (streakResult.currentStreak > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(ReflexTokens.ShapeChip)
                                    .background(CopperContainer)
                                    .border(
                                        border = BorderStroke(ReflexTokens.BorderHairline, CopperPrimary.copy(alpha = 0.4f)),
                                        shape = ReflexTokens.ShapeChip
                                    )
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${streakResult.currentStreak} streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CopperOnContainer
                                )
                            }
                        }
                    }
                }
            }

            // --- 2. STAT TILES ROW ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
                ) {
                    StatTile(
                        label = "Total steps",
                        value = "${steps.size}",
                        icon = Icons.Default.Check,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Est. duration",
                        value = "${estimatedMins}m",
                        icon = Icons.Default.HourglassTop,
                        modifier = Modifier.weight(1f)
                    )
                    StatTile(
                        label = "Rest break",
                        value = if (routine != null) {
                            if (routine.restBetweenStepsEnabled) "${routine.restDurationSeconds}s" else "Off"
                        } else {
                            com.reflex.app.util.SettingsRepository.getString("r_rest", "15s")
                        },
                        icon = Icons.Default.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- 3. SECTION HEADER ---
            item {
                SectionHeader(
                    title = "Routine steps",
                    subtitle = "${steps.size} steps in sequence"
                )
            }

            // --- 4. STEPS LIST ---
            itemsIndexed(steps) { idx, step ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ReflexTokens.ShapeCard)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(
                            border = BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outline),
                            shape = ReflexTokens.ShapeCard
                        )
                        .padding(ReflexTokens.SpaceLg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Step Number Pill
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .border(BorderStroke(ReflexTokens.BorderHairline, MaterialTheme.colorScheme.outlineVariant), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(ReflexTokens.SpaceMd))

                            Column {
                                Text(
                                    text = step.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val durationDesc = when (step.stepType) {
                                    com.reflex.app.data.StepType.TIMED -> "${(step.durationSeconds ?: 60) / 60}m ${(step.durationSeconds ?: 60) % 60}s"
                                    com.reflex.app.data.StepType.REPEAT_COUNT -> {
                                        val sets = step.targetCount ?: 3
                                        val rest = step.restDurationSeconds ?: run {
                                            val r = com.reflex.app.util.SettingsRepository.getString("r_rest", "15s")
                                            if (r != "Off") r.filter { it.isDigit() }.toIntOrNull() else null
                                        }
                                        if (rest != null && rest > 0) "$sets sets · ${rest}s rest" else "$sets sets"
                                    }
                                    com.reflex.app.data.StepType.CHECK_OFF -> "Check-off"
                                }
                                Text(
                                    text = durationDesc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        StepTypeChip(stepType = step.stepType)
                    }
                }
            }

            // --- 5. ACTION BUTTONS ---
            item {
                Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                // Skip Today action (if scheduled today)
                if (isScheduledToday) {
                    ReflexButton(
                        text = if (isSkippedToday) "Skipped today" else "Skip today",
                        onClick = {
                            if (!isSkippedToday && routine != null) {
                                scope.launch {
                                    repository.insertLog(
                                        CompletionLog(
                                            routineId = routine.id,
                                            dateCompleted = System.currentTimeMillis(),
                                            totalTimeTakenSeconds = 0,
                                            stepsCompletedCount = 0,
                                            isCompleted = false,
                                            isSkipped = true
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = ReflexButtonVariant.SECONDARY
                    )
                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                }

                // Primary Start Routine Action (White Pill)
                ReflexButton(
                    text = "Start routine",
                    onClick = onStartRoutine,
                    modifier = Modifier.fillMaxWidth(),
                    variant = ReflexButtonVariant.PRIMARY
                )

                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}
