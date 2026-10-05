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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ReflexApplication
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.components.ReflexCard
import com.reflex.app.ui.components.ReflexTopBar
import com.reflex.app.ui.components.StatTile
import com.reflex.app.ui.theme.ActionPillOnWhite
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperOnContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.success

@Composable
fun FocusSummaryScreen(
    sessionId: Long,
    onDone: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onDone)
    SetStatusBarAppearance()
    val context = LocalContext.current

    val repository = remember(context) {
        val app = context.applicationContext as ReflexApplication
        app.repository
    }

    var session by remember { mutableStateOf<FocusSession?>(null) }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val outlineColor = MaterialTheme.colorScheme.outline

    LaunchedEffect(sessionId) {
        if (sessionId != 0L) {
            repository.getAllFocusSessions().collect { list ->
                val found = list.find { it.id == sessionId }
                if (found != null) session = found
            }
        }
    }

    val isStoppedEarly = session?.endReason == "stopped_early" || (session?.completed == false && session?.endReason != "completed")
    val isCompleted = !isStoppedEarly
    val modeText = when (session?.mode) {
        FocusMode.CLASSIC_POMODORO -> "Pomodoro"
        FocusMode.FLOW_TIMED -> "Timed flow"
        FocusMode.FLOW_OPEN -> "Open flow"
        null -> "Focus"
    }

    val actualSec = session?.actualDurationSeconds ?: 0
    val mins = actualSec / 60
    val secs = actualSec % 60
    val formattedDuration = String.format("%02d:%02d", mins, secs)

    val plannedSec = session?.plannedDurationSeconds
    val plannedMins = if (plannedSec != null) plannedSec / 60 else 0
    val plannedSecs = if (plannedSec != null) plannedSec % 60 else 0
    val formattedPlanned = if (plannedSec != null) String.format("%02d:%02d", plannedMins, plannedSecs) else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ReflexTopBar(
            title = "Summary",
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

            // Celebration / Completion Hero Card
            ReflexCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val stopColor = MaterialTheme.colorScheme.error
                    val successColor = MaterialTheme.colorScheme.success
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) successColor.copy(alpha = 0.15f) else stopColor.copy(alpha = 0.15f))
                            .border(
                                BorderStroke(
                                    ReflexTokens.BorderThin,
                                    if (isCompleted) successColor else stopColor
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = if (isCompleted) successColor else stopColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                    Text(
                        text = if (isCompleted) "Focus session complete" else "Session stopped early",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    val subtitleText = if (isCompleted) {
                        "Excellent work maintaining deliberate focus."
                    } else if (formattedPlanned != null && plannedSec != null && plannedSec > 0) {
                        "Focused $formattedDuration of planned $formattedPlanned. Every minute of focus counts."
                    } else {
                        "Every minute of focus counts toward momentum."
                    }

                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    value = formattedDuration,
                    label = if (formattedPlanned != null) "Actual ($formattedDuration)" else "Focused time",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f)
                )

                if (formattedPlanned != null) {
                    StatTile(
                        value = formattedPlanned,
                        label = "Planned time",
                        icon = Icons.Default.HourglassBottom,
                        modifier = Modifier.weight(1f)
                    )
                }

                StatTile(
                    value = modeText,
                    label = "Technique",
                    icon = Icons.Default.HourglassBottom,
                    modifier = Modifier.weight(1f)
                )

                if (session?.mode == FocusMode.CLASSIC_POMODORO) {
                    StatTile(
                        value = "${session?.completedCycles ?: 0}",
                        label = "Cycles",
                        icon = Icons.Default.Repeat,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceXxl))

            ReflexButton(
                text = "Back to focus",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
                variant = ReflexButtonVariant.PRIMARY
            )

            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

            ReflexButton(
                text = "Discard session",
                onClick = { showDiscardConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                variant = ReflexButtonVariant.SECONDARY
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDiscardConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Discard this session?",
                    fontFamily = com.reflex.app.ui.theme.Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This session will be removed and will not be counted in your focus stats.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                ReflexButton(
                    text = "Discard",
                    onClick = {
                        showDiscardConfirm = false
                        coroutineScope.launch {
                            if (sessionId != 0L) {
                                repository.deleteFocusSession(sessionId)
                            }
                            onDone()
                        }
                    },
                    variant = ReflexButtonVariant.DESTRUCTIVE
                )
            },
            dismissButton = {
                ReflexButton(
                    text = "Cancel",
                    onClick = { showDiscardConfirm = false },
                    variant = ReflexButtonVariant.SECONDARY
                )
            }
        )
    }
}
