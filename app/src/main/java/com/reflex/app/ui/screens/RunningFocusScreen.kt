package com.reflex.app.ui.screens

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.reflex.app.ReflexApplication
import com.reflex.app.data.FocusMode
import com.reflex.app.service.FocusPhase
import com.reflex.app.service.FocusTimerService
import com.reflex.app.ui.components.DeleteConfirmationDialog
import com.reflex.app.ui.components.FocusQuote
import com.reflex.app.ui.components.LiquidTimer
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.actionPill
import com.reflex.app.ui.theme.onActionPill
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.sage
import com.reflex.app.viewmodel.RunningFocusViewModel
import kotlin.math.ceil
import kotlin.math.max

/**
 * Session Screen for Focus tab, replicating reflex-focus.html and DESIGN.md v3.0 Section 2:
 * - Full-screen route covering bottom nav bar with keepScreenOn enabled.
 * - Top row: Phase pill (40dp, 16dp horizontal padding).
 * - Center column:
 *   - LiquidTimer (diameter min(80% width, 340dp, 42% height)) with monotonic sub-frame clock.
 *   - Linked task title (15sp secondary, max 320dp width) or break reminder.
 *   - FocusQuote (focus phases only, 45s shuffle bag cycle).
 *   - Pomodoro session dots (completed, current ring with 2dp offset, upcoming).
 * - Bottom controls (56dp full pill):
 *   - Pomodoro: 3-button grid (1fr : 1.5fr : 1fr) End | Pause/Resume/Start | Skip
 *   - Timed flow: 2-button grid (1fr : 1.5fr) End | Pause/Resume
 *   - Open flow: 2-button grid (1fr : 1.5fr) Finish | Pause/Resume
 *   - Session complete: single "Back to Focus" button
 */
@Composable
fun RunningFocusScreen(
    modeStr: String,
    targetMin: Int,
    onCompleteSession: (Long) -> Unit,
    onCancel: () -> Unit,
    viewModel: RunningFocusViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = RunningFocusViewModel.Factory(
            (LocalContext.current.applicationContext as ReflexApplication).repository
        )
    )
) {
    SetStatusBarAppearance()
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
        }
    }

    val state by viewModel.timerState.collectAsState()
    var completionHandled by remember { mutableStateOf(false) }
    var showUnderOneMinDialog by remember { mutableStateOf(false) }
    var sessionStarted by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    val isDone = state?.isFinished == true

    LaunchedEffect(Unit) {
        if (!sessionStarted) {
            sessionStarted = true
            val currentState = FocusTimerService.timerState.value
            if (currentState == null || currentState.isFinished || currentState.isCancelled || currentState.mode.name != modeStr) {
                when (modeStr) {
                    FocusMode.CLASSIC_POMODORO.name -> viewModel.startPomodoro(context)
                    FocusMode.FLOW_TIMED.name -> viewModel.startTimedFlow(context, targetMin)
                    FocusMode.FLOW_OPEN.name -> viewModel.startOpenFlow(context)
                }
            }
        }
    }

    LaunchedEffect(isDone) {
        if (isDone) {
            val soundEnabled = com.reflex.app.util.SettingsRepository.getBoolean("f_snd", true)
            val vibEnabled = com.reflex.app.util.SettingsRepository.getBoolean("f_vib", true)
            if (soundEnabled) {
                com.reflex.app.util.SoundCuePlayer.playTingChime()
            }
            if (vibEnabled) {
                val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(android.os.VibratorManager::class.java)
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(200L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200L)
                }
            }
        }
    }

    val isWaiting = state?.isWaitingForNextPhase == true
    val isPaused = state?.isPaused == true
    val mode = state?.mode ?: FocusMode.valueOf(modeStr)
    val phase = state?.phase ?: FocusPhase.WORK
    val isBreak = phase == FocusPhase.SHORT_BREAK || phase == FocusPhase.LONG_BREAK
    val isPomodoro = mode == FocusMode.CLASSIC_POMODORO
    val isTimedFlow = mode == FocusMode.FLOW_TIMED
    val isOpenFlow = mode == FocusMode.FLOW_OPEN

    val serviceElapsedSec = state?.elapsedSeconds ?: 0

    val handleManualEnd = {
        val elapsed = serviceElapsedSec
        if (elapsed < 60) {
            showUnderOneMinDialog = true
        } else {
            if (!completionHandled) {
                completionHandled = true
                viewModel.stopSession(context, endReason = "stopped_early") { sessionId ->
                    onCompleteSession(sessionId)
                }
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = !isDone) {
        handleManualEnd()
    }

    // Sub-frame monotonic clock tracking for ultra-smooth 60fps+ liquid draining
    val currentRemainingMs = remember { mutableLongStateOf(0L) }
    val currentElapsedMs = remember { mutableLongStateOf(0L) }

    val serviceRemainingSec = state?.remainingSeconds ?: (targetMin * 60)

    LaunchedEffect(serviceRemainingSec, isPaused, isWaiting, isDone) {
        if (isPaused || isWaiting || isDone) {
            currentRemainingMs.longValue = serviceRemainingSec * 1000L
        } else {
            val targetEndUptime = SystemClock.uptimeMillis() + serviceRemainingSec * 1000L
            while (!isPaused && !isWaiting && !isDone) {
                withFrameNanos { _ ->
                    val rem = targetEndUptime - SystemClock.uptimeMillis()
                    currentRemainingMs.longValue = rem.coerceAtLeast(0L)
                }
            }
        }
    }

    LaunchedEffect(serviceElapsedSec, isPaused, isWaiting, isDone) {
        if (isPaused || isWaiting || isDone) {
            currentElapsedMs.longValue = serviceElapsedSec * 1000L
        } else {
            val startUptime = SystemClock.uptimeMillis() - serviceElapsedSec * 1000L
            while (!isPaused && !isWaiting && !isDone) {
                withFrameNanos { _ ->
                    val el = SystemClock.uptimeMillis() - startUptime
                    currentElapsedMs.longValue = el.coerceAtLeast(0L)
                }
            }
        }
    }

    val plannedDurationSec = state?.plannedDurationSeconds ?: (targetMin * 60)
    val totalPhaseMs = remember(phase, plannedDurationSec) {
        max(1, plannedDurationSec) * 1000L
    }

    val timeText = remember(isDone, isOpenFlow, currentRemainingMs.longValue, currentElapsedMs.longValue) {
        if (isDone) {
            "00:00"
        } else if (isOpenFlow) {
            val totalSec = (currentElapsedMs.longValue / 1000L).toInt()
            formatClock(totalSec)
        } else {
            val remSec = max(0, ceil(currentRemainingMs.longValue / 1000.0).toInt())
            formatClock(remSec)
        }
    }

    val labelText = when {
        isDone -> "Session"
        isOpenFlow -> "Open flow"
        phase == FocusPhase.SHORT_BREAK -> "Short break"
        phase == FocusPhase.LONG_BREAK -> "Long break"
        else -> "Focus"
    }

    val statusText = when {
        isDone -> "Done"
        isWaiting -> "Ready"
        isPaused -> "Paused"
        else -> "Running"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .reflexStatusBarPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Row: Phase pill (40dp tall, 16dp horizontal padding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val phasePillText = when {
                isDone -> "Session complete"
                isOpenFlow -> "Open flow"
                isTimedFlow -> "Timed flow"
                phase == FocusPhase.SHORT_BREAK -> "Short break · ${state?.currentCycle ?: 1} of ${state?.totalCycles ?: 4}"
                phase == FocusPhase.LONG_BREAK -> "Long break · ${state?.currentCycle ?: 1} of ${state?.totalCycles ?: 4}"
                else -> "Focus · ${state?.currentCycle ?: 1} of ${state?.totalCycles ?: 4}"
            }

            val pillBg = if (isBreak) MaterialTheme.colorScheme.sage.copy(alpha = 0.16f) else CopperContainer
            val pillFg = if (isBreak) MaterialTheme.colorScheme.sage else CopperPrimary

            Box(
                modifier = Modifier
                    .height(ReflexTokens.FocusPhasePillHeight)
                    .clip(CircleShape)
                    .background(pillBg)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = phasePillText,
                    fontFamily = Lora,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = pillFg
                )
            }
        }

        // Center Column: LiquidTimer + Linked task / break reminder + FocusQuote + Dots
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val timerSize = minOf(maxWidth * 0.80f, 340.dp, maxHeight * 0.55f)

                LiquidTimer(
                    ratio = {
                        if (isDone) 0f
                        else if (isOpenFlow) 0.55f
                        else (currentRemainingMs.longValue.toFloat() / totalPhaseMs.toFloat()).coerceIn(0f, 1f)
                    },
                    timeText = timeText,
                    label = labelText,
                    status = statusText,
                    isBreak = isBreak,
                    isActive = !isPaused && !isWaiting && !isDone,
                    modifier = Modifier.size(timerSize)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Linked task title or "Step away, breathe, stretch"
            val promptText = if (isBreak) {
                "Step away, breathe, stretch"
            } else if (!state?.sessionTitle.isNullOrBlank()) {
                state?.sessionTitle ?: ""
            } else {
                ""
            }

            if (promptText.isNotEmpty()) {
                Text(
                    text = promptText,
                    fontFamily = Lora,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 320.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Focus Quote (focus phases only)
            if (!isBreak && !isDone) {
                FocusQuote(
                    active = !isPaused && !isWaiting,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Pomodoro session dots (N dots, 12dp diameter, 10dp gap)
            if (isPomodoro) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ReflexTokens.FocusSessionDotGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalDots = state?.totalCycles ?: 4
                    val currentDotIndex = (state?.currentCycle ?: 1) - 1

                    for (i in 0 until totalDots) {
                        val isCompletedDot = isDone || i < currentDotIndex
                        val isCurrentDot = !isDone && i == currentDotIndex

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .then(
                                    if (isCurrentDot) {
                                        Modifier.border(2.dp, CopperPrimary, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ReflexTokens.FocusSessionDotSize)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCompletedDot -> CopperPrimary
                                            isCurrentDot -> CopperPrimary.copy(alpha = 0.5f)
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        }
                                    )
                            )
                        }
                    }
                }
            }
        }

        // Bottom Controls (56dp height, full pill)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            if (isDone) {
                // Session complete: single full-width "Back to Focus" button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ReflexTokens.FocusControlPillHeight)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.actionPill)
                        .clickable {
                            if (!completionHandled) {
                                completionHandled = true
                                viewModel.stopSession(context, endReason = "completed") { sessionId ->
                                    onCompleteSession(sessionId)
                                }
                            } else {
                                onCompleteSession(0L)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Back to Focus",
                        fontFamily = Lora,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onActionPill
                    )
                }
            } else if (isPomodoro) {
                // Pomodoro: 3-button grid (1fr : 1.5fr : 1fr) End | Pause/Resume/Start | Skip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // End button (SurfaceInput, 16sp ink)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { handleManualEnd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "End",
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Main action button (actionPill)
                    val mainBtnText = when {
                        isWaiting -> {
                            val nextName = if (phase == FocusPhase.WORK) "break" else "focus"
                            "Start $nextName"
                        }
                        isPaused -> "Resume"
                        else -> "Pause"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.actionPill)
                            .clickable {
                                if (isWaiting) {
                                    viewModel.startNextPhase(context)
                                } else {
                                    viewModel.togglePauseResume(context)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mainBtnText,
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onActionPill
                        )
                    }

                    // Skip button (SurfaceInput, 16sp ink)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { viewModel.skipPhase(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Skip",
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            } else if (isTimedFlow) {
                // Timed flow: 2-button grid (1fr : 1.5fr) End | Pause/Resume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { handleManualEnd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "End",
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val mainBtnText = if (isPaused) "Resume" else "Pause"
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.actionPill)
                            .clickable { viewModel.togglePauseResume(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mainBtnText,
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onActionPill
                        )
                    }
                }
            } else {
                // Open flow: 2-button grid (1fr : 1.5fr) Finish | Pause/Resume
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { handleManualEnd() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Finish",
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val mainBtnText = if (isPaused) "Resume" else "Pause"
                    Box(
                        modifier = Modifier
                            .weight(1.5f)
                            .height(ReflexTokens.FocusControlPillHeight)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.actionPill)
                            .clickable { viewModel.togglePauseResume(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mainBtnText,
                            fontFamily = Lora,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onActionPill
                        )
                    }
                }
            }
        }
    }

    if (showUnderOneMinDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnderOneMinDialog = false },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Session under 1 minute",
                    fontFamily = Lora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "This session has run for less than a minute. Would you like to save it to your records or discard it?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                com.reflex.app.ui.components.ReflexButton(
                    text = "Save",
                    onClick = {
                        showUnderOneMinDialog = false
                        if (!completionHandled) {
                            completionHandled = true
                            viewModel.stopSession(context, endReason = "stopped_early") { sessionId ->
                                onCompleteSession(sessionId)
                            }
                        }
                    },
                    variant = com.reflex.app.ui.components.ReflexButtonVariant.PRIMARY
                )
            },
            dismissButton = {
                com.reflex.app.ui.components.ReflexButton(
                    text = "Discard",
                    onClick = {
                        showUnderOneMinDialog = false
                        viewModel.discardCurrentSession(context)
                        onCancel()
                    },
                    variant = com.reflex.app.ui.components.ReflexButtonVariant.DESTRUCTIVE
                )
            }
        )
    }

    if (isDone) {
        com.reflex.app.ui.components.ConfettiOverlay()
    }
}

private fun formatClock(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, sec)
    } else {
        String.format("%02d:%02d", m, sec)
    }
}
