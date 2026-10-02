package com.reflex.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.ReflexRepository
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.StatTile
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.success
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CompletionSummaryScreen(
    routineId: Long,
    logId: Long,
    onDone: () -> Unit
) {
    SetStatusBarAppearance()
    val context = LocalContext.current

    val repository = remember(context) {
        val db = ReflexDatabase.getDatabase(context.applicationContext)
        ReflexRepository(db.routineDao(), db.stepDao(), db.completionLogDao(), db.taskDao())
    }

    val routineWithSteps by repository.getRoutineWithSteps(routineId).collectAsState(initial = null)
    val logs by repository.getLogsForRoutine(routineId).collectAsState(initial = emptyList())
    val currentLog = remember(logs, logId) {
        logs.find { it.id == logId } ?: logs.firstOrNull()
    }

    val routineName = routineWithSteps?.routine?.name ?: "Routine"
    val stepsDone = currentLog?.stepsCompletedCount ?: routineWithSteps?.steps?.size ?: 0
    val totalSteps = routineWithSteps?.steps?.size ?: stepsDone
    val timeSeconds = currentLog?.totalTimeTakenSeconds ?: 0

    val mins = timeSeconds / 60
    val secs = timeSeconds % 60
    val formattedTime = String.format("%02d:%02d", mins, secs)

    val dateFormatted = currentLog?.dateCompleted?.let { dateMillis ->
        Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"))
    } ?: "Today"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = "Session summary",
            onBackClick = onDone
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = ReflexTokens.SpaceLg)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // Celebration Hero Card
            ReflexCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.success), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.success,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                    Text(
                        text = "Routine completed",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = routineName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CopperPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

            // Stat Tiles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ReflexTokens.SpaceSm)
            ) {
                StatTile(
                    label = "Duration",
                    value = formattedTime,
                    icon = Icons.Default.HourglassTop,
                    modifier = Modifier.weight(1f)
                )

                StatTile(
                    label = "Steps done",
                    value = "$stepsDone / $totalSteps",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            // Primary Return Action (GymMane White Pill)
            ReflexButton(
                text = "Back to routines",
                onClick = onDone,
                variant = ReflexButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
