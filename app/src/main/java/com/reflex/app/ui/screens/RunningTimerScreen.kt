package com.reflex.app.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.reflex.app.ReflexApplication
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.Step
import com.reflex.app.data.StepType
import com.reflex.app.ui.components.LiquidTimer
import com.reflex.app.ui.components.SettingsIcon
import com.reflex.app.ui.theme.CopperContainer
import com.reflex.app.ui.theme.CopperPrimary
import com.reflex.app.ui.theme.DarkDestructiveRed
import com.reflex.app.ui.theme.Lora
import com.reflex.app.ui.theme.ReflexTokens
import com.reflex.app.ui.theme.SetStatusBarAppearance
import com.reflex.app.ui.theme.onCopper
import com.reflex.app.ui.theme.reflexStatusBarPadding
import com.reflex.app.ui.theme.routineGreen
import com.reflex.app.util.StreakCalculator
import com.reflex.app.viewmodel.RunningTimerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

enum class RunnerStepStatus {
    TODO,
    CURRENT,
    DONE,
    SKIP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunningTimerScreen(
    viewModel: RunningTimerViewModel,
    routineId: Long,
    onComplete: (Long) -> Unit,
    onCancel: () -> Unit
) {
    SetStatusBarAppearance()

    val context = LocalContext.current
    val view = LocalView.current
    val app = context.applicationContext as ReflexApplication
    val scope = rememberCoroutineScope()

    // Keep screen awake
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view.keepScreenOn = true
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            view.keepScreenOn = false
        }
    }

    // Load routine data
    var routineName by remember { mutableStateOf("Routine") }
    var steps by remember { mutableStateOf<List<Step>>(emptyList()) }
    var dataLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(routineId) {
        val rws = app.repository.getRoutineWithSteps(routineId).firstOrNull()
        if (rws != null) {
            routineName = rws.routine.name
            steps = rws.steps
        }
        dataLoaded = true
    }

    // Session runner state matching reflex-home.html
    var stepIndex by remember { mutableIntStateOf(0) }
    var currentSetIndex by remember { mutableIntStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var wasPausedBeforeDialog by remember { mutableStateOf(false) }

    var timeRemaining by remember { mutableIntStateOf(0) }
    var timeTotalForStep by remember { mutableIntStateOf(0) }
    var stepElapsedSeconds by remember { mutableIntStateOf(0) }
    var totalElapsedSeconds by remember { mutableIntStateOf(0) }

    val stepStatuses = remember { mutableStateMapOf<Int, RunnerStepStatus>() }
    val stepProgressLogs = remember { mutableStateMapOf<Int, String>() }

    var isSessionSummaryOpen by remember { mutableStateOf(false) }
    var isEndedEarly by remember { mutableStateOf(false) }
    var savedLogId by remember { mutableStateOf(0L) }
    var showEndDialog by remember { mutableStateOf(false) }
    var showStepsSheet by remember { mutableStateOf(false) }

    val allLogs by app.repository.getAllLogs().collectAsState(initial = emptyList())
    val streakStats = remember(allLogs) { StreakCalculator.calculateStreaks(allLogs) }

    fun formatMinutesSeconds(sec: Int): String {
        val c = sec.coerceAtLeast(0)
        return "${c / 60}:${String.format("%02d", c % 60)}"
    }

    fun stepDescription(st: Step): String {
        return when (st.stepType) {
            StepType.TIMED -> "Timed · ${formatMinutesSeconds(st.durationSeconds ?: 60)}"
            StepType.REPEAT_COUNT -> "${st.targetCount ?: 3} sets · ${st.restDurationSeconds ?: 15}s rest"
            StepType.CHECK_OFF -> "Check off"
        }
    }

    // Start a step at index i
    fun beginStep(i: Int) {
        if (steps.isEmpty()) return
        val clampedI = i.coerceIn(0, steps.size - 1)
        stepIndex = clampedI
        currentSetIndex = 0
        isResting = false
        stepElapsedSeconds = 0

        val current = steps.getOrNull(clampedI)
        if (current?.stepType == StepType.TIMED) {
            val d = current.durationSeconds ?: 60
            timeRemaining = d
            timeTotalForStep = d
        } else {
            timeRemaining = 0
            timeTotalForStep = 0
        }
    }

    // Finish entire session (normal completion or ended early)
    fun finishSession(ended: Boolean) {
        isSessionSummaryOpen = true
        isEndedEarly = ended

        val doneCount = stepStatuses.values.count { it == RunnerStepStatus.DONE }
        scope.launch {
            // Build step logs JSON
            val jsonArray = JSONArray()
            steps.forEachIndexed { idx, st ->
                val logItem = JSONObject().apply {
                    put("name", st.name)
                    put("status", (stepStatuses[idx] ?: RunnerStepStatus.TODO).name)
                    put("log", stepProgressLogs[idx] ?: stepDescription(st))
                }
                jsonArray.put(logItem)
            }

            val log = CompletionLog(
                routineId = routineId,
                dateCompleted = System.currentTimeMillis(),
                totalTimeTakenSeconds = totalElapsedSeconds,
                stepsCompletedCount = doneCount,
                isCompleted = !ended,
                totalStepsCount = steps.size,
                stepLogsJson = jsonArray.toString()
            )
            savedLogId = app.repository.insertLog(log)
        }
    }

    // Mark step as done or skipped, then advance
    fun completeOrSkipStep(index: Int, status: RunnerStepStatus) {
        stepStatuses[index] = status
        val current = steps.getOrNull(index)
        if (current != null) {
            when (current.stepType) {
                StepType.REPEAT_COUNT -> {
                    val target = current.targetCount ?: 3
                    stepProgressLogs[index] = "$currentSetIndex of $target sets logged"
                }
                StepType.TIMED -> {
                    val d = current.durationSeconds ?: 60
                    val loggedSecs = stepElapsedSeconds.coerceAtMost(d)
                    stepProgressLogs[index] = "${formatMinutesSeconds(loggedSecs)} of ${formatMinutesSeconds(d)} logged"
                }
                StepType.CHECK_OFF -> {
                    stepProgressLogs[index] = "Check off"
                }
            }
        }

        if (index >= steps.size - 1) {
            finishSession(ended = false)
        } else {
            beginStep(index + 1)
        }
    }

    // Reset and restart routine from step 1
    fun restartRoutine() {
        stepStatuses.clear()
        stepProgressLogs.clear()
        steps.indices.forEach { stepStatuses[it] = RunnerStepStatus.TODO }
        totalElapsedSeconds = 0
        isPaused = false
        isSessionSummaryOpen = false
        isEndedEarly = false
        beginStep(0)
    }

    // Initialize once data is ready
    LaunchedEffect(dataLoaded, steps) {
        if (dataLoaded && steps.isNotEmpty()) {
            restartRoutine()
        }
    }

    // Second-by-second ticker loop matching reflex-home.html
    LaunchedEffect(isSessionSummaryOpen, isPaused) {
        while (!isSessionSummaryOpen) {
            delay(1000L)
            if (isPaused) continue

            totalElapsedSeconds++
            if (!isResting) {
                stepElapsedSeconds++
            }

            val current = steps.getOrNull(stepIndex)
            val isTimed = current?.stepType == StepType.TIMED || isResting

            if (isTimed) {
                if (timeRemaining > 0) {
                    timeRemaining--
                    if (timeRemaining <= 0) {
                        if (isResting) {
                            // Exit rest, back to current sets step
                            isResting = false
                            timeRemaining = 0
                            timeTotalForStep = 0
                        } else {
                            val autoAdvance = com.reflex.app.util.SettingsRepository.getBoolean("r_auto", true)
                            if (autoAdvance) {
                                // Timed step finished naturally
                                completeOrSkipStep(stepIndex, RunnerStepStatus.DONE)
                            } else {
                                timeRemaining = 0
                            }
                        }
                    }
                } else if (timeRemaining <= 0) {
                    timeRemaining = 0
                }
            }
        }
    }

    // Back button behavior: prompt dialog instead of abrupt exit
    fun askEnd() {
        wasPausedBeforeDialog = isPaused
        isPaused = true
        showEndDialog = true
    }

    BackHandler(enabled = !isSessionSummaryOpen) {
        askEnd()
    }

    if (isSessionSummaryOpen) {
        // --- SUMMARY SCREEN (#sm in reflex-home.html) ---
        SessionSummaryContent(
            routineTitle = routineName,
            isEndedEarly = isEndedEarly,
            totalElapsedSeconds = totalElapsedSeconds,
            steps = steps,
            stepStatuses = stepStatuses,
            stepProgressLogs = stepProgressLogs,
            streakDays = streakStats.currentStreak,
            onDone = {
                onComplete(savedLogId)
                onCancel()
            },
            onRunAgain = {
                restartRoutine()
            }
        )
    } else {
        // --- ROUTINE RUNNER SCREEN (#run in reflex-home.html) ---
        val currentStep = steps.getOrNull(stepIndex)
        val nextStep = steps.getOrNull(stepIndex + 1)
        val isTimed = currentStep?.stepType == StepType.TIMED || isResting

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Ambient Radial Glow (.glow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(CopperContainer.copy(alpha = 0.5f), Color.Transparent),
                            center = Offset(Float.POSITIVE_INFINITY, 0f),
                            radius = 600f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .reflexStatusBarPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Top Bar (.rtop) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Back / Close button (44dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { askEnd() },
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsIcon(name = "back", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
                    }

                    // Routine Name + "Step X of Y ⌄" (Clickable to open steps sheet)
                    Column(
                        modifier = Modifier
                            .clip(ReflexTokens.ShapeChip)
                            .clickable { showStepsSheet = true }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = routineName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Step ${stepIndex + 1} of ${steps.size.coerceAtLeast(1)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsIcon(name = "chev-down", tint = MaterialTheme.colorScheme.onSurfaceVariant, size = 12.dp)
                        }
                    }

                    // Spacer to balance the top bar layout
                    Spacer(modifier = Modifier.size(44.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                // --- Weighted Segmented Progress Strip (.strip) ---
                if (steps.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        steps.forEachIndexed { idx, st ->
                            val weight = when (st.stepType) {
                                StepType.TIMED -> (st.durationSeconds ?: 60).toFloat()
                                StepType.REPEAT_COUNT -> ((st.targetCount ?: 3) * 12 + (st.restDurationSeconds ?: 15) * 2).toFloat()
                                StepType.CHECK_OFF -> 15f
                            }.coerceAtLeast(10f)

                            val status = stepStatuses[idx] ?: RunnerStepStatus.TODO
                            val segColor = when {
                                status == RunnerStepStatus.DONE -> CopperPrimary
                                status == RunnerStepStatus.SKIP -> MaterialTheme.colorScheme.outlineVariant
                                idx == stepIndex -> CopperPrimary.copy(alpha = 0.45f)
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }

                            Box(
                                modifier = Modifier
                                    .weight(weight)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(segColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // --- STEP VISUAL HERO ---
                if (isTimed) {
                    // TIMED STEP OR REST: Liquid Glass Timer (user explicit instruction)
                    val liquidRatio = {
                        if (timeTotalForStep > 0) (timeRemaining.toFloat() / timeTotalForStep.toFloat()).coerceIn(0f, 1f)
                        else 0f
                    }
                    val timerLabel = if (isResting) "Rest" else if (currentStep?.stepType == StepType.TIMED) "Timed step" else "Your pace"
                    val timerStatus = if (isPaused) "Paused" else if (isResting) "Resting" else "Running"

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidTimer(
                            ratio = liquidRatio,
                            timeText = formatMinutesSeconds(timeRemaining),
                            label = timerLabel,
                            status = timerStatus,
                            isBreak = isResting, // isBreak renders sage/green fluid waves
                            isActive = !isPaused
                        )
                    }
                } else if (currentStep?.stepType == StepType.REPEAT_COUNT) {
                    // SETS STEP (when not resting): 120dp Dumbbell Circle
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsIcon(name = "dumbbell", tint = CopperPrimary, size = 48.dp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sets Checkbox List (.reps)
                    val targetSets = currentStep.targetCount ?: 3
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (q in 0 until targetSets) {
                            val isCompletedSet = q < currentSetIndex
                            val isCurrentSet = q == currentSetIndex

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isCurrentSet) CopperPrimary else MaterialTheme.colorScheme.outline
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable(enabled = isCurrentSet && !isResting) {
                                        val newSet = currentSetIndex + 1
                                        currentSetIndex = newSet
                                        if (newSet >= targetSets) {
                                            // Step completed
                                            completeOrSkipStep(stepIndex, RunnerStepStatus.DONE)
                                        } else {
                                            // Transition to rest period between sets
                                            val restSec = currentStep.restDurationSeconds ?: 15
                                            isResting = true
                                            timeRemaining = restSec
                                            timeTotalForStep = restSec
                                        }
                                    }
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 22dp Checkbox square
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(7.dp))
                                        .then(
                                            if (isCompletedSet) Modifier.background(CopperPrimary)
                                            else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isCompletedSet) {
                                        SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.onCopper, size = 14.dp)
                                    }
                                }

                                Text(
                                    text = "Set ${q + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = if (isCompletedSet || isCurrentSet) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                } else {
                    // CHECK-OFF STEP: 120dp Checkmark squircle
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CopperPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.onCopper, size = 32.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // --- STEP TITLE & SUBTITLE (.h2 & .sub) ---
                Text(
                    text = if (isResting) "Rest" else currentStep?.name ?: "Step",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                val subDescription = if (isResting) {
                    val targetSets = currentStep?.targetCount ?: 3
                    "Next: set ${currentSetIndex + 1} of $targetSets"
                } else {
                    currentStep?.let { stepDescription(it) } ?: ""
                } + if (isPaused) " · Paused" else ""

                Text(
                    text = subDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                val upNextText = if (nextStep != null) "Up next: ${nextStep.name} · ${stepDescription(nextStep)}" else "Last step"
                Text(
                    text = upNextText,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // --- CONTROLS ROW (.ctl) ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = stepIndex > 0) {
                                if (stepIndex > 0) {
                                    stepStatuses[stepIndex] = RunnerStepStatus.TODO
                                    stepStatuses[stepIndex - 1] = RunnerStepStatus.TODO
                                    beginStep(stepIndex - 1)
                                } else {
                                    beginStep(0)
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            SettingsIcon(
                                name = "prev",
                                tint = if (stepIndex > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                size = 20.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Previous",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // -10 s
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isTimed) {
                                if (isTimed) {
                                    timeRemaining = (timeRemaining - 10).coerceAtLeast(0)
                                    if (timeRemaining <= 0) {
                                        if (isResting) {
                                            isResting = false
                                            timeRemaining = 0
                                        } else {
                                            completeOrSkipStep(stepIndex, RunnerStepStatus.DONE)
                                        }
                                    }
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−10",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "−10 s",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Pause / Resume (Big 72dp Copper button)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable { isPaused = !isPaused }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(CopperPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            SettingsIcon(
                                name = if (isPaused) "play" else "pause",
                                tint = MaterialTheme.colorScheme.onCopper,
                                size = 28.dp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isPaused) "Resume" else "Pause",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // +10 s
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(enabled = isTimed) {
                                if (isTimed) {
                                    timeRemaining += 10
                                    timeTotalForStep = maxOf(timeTotalForStep, timeRemaining)
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+10",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isTimed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "+10 s",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Skip
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { completeOrSkipStep(stepIndex, RunnerStepStatus.SKIP) }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            SettingsIcon(name = "next", tint = MaterialTheme.colorScheme.onSurface, size = 20.dp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- COMPLETE BUTTON (.cmpb) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), CircleShape)
                        .clickable {
                            if (isResting) {
                                isResting = false
                                timeRemaining = 0
                                timeTotalForStep = 0
                            } else {
                                completeOrSkipStep(stepIndex, RunnerStepStatus.DONE)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingsIcon(name = "check", tint = Color(0xFF0A0908), size = 20.dp)
                        Text(
                            text = "Complete",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp,
                            color = Color(0xFF0A0908)
                        )
                    }
                }
            }
        }
    }

    // --- END SESSION CONFIRMATION DIALOG (#dlg) ---
    if (showEndDialog) {
        Dialog(onDismissRequest = {
            isPaused = wasPausedBeforeDialog
            showEndDialog = false
        }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeDialog)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeDialog)
                    .padding(26.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "End this session?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Workout is still in progress. If you end now, it is saved to history as ended early.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Keep going button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable {
                                isPaused = wasPausedBeforeDialog
                                showEndDialog = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Keep going",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // End session (danger button)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(CircleShape)
                            .background(DarkDestructiveRed)
                            .clickable {
                                showEndDialog = false
                                finishSession(ended = true)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "End session",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // --- STEPS BOTTOM SHEET (#sheet) ---
    if (showStepsSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showStepsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            scrimColor = MaterialTheme.colorScheme.scrim,
            shape = ReflexTokens.ShapeSheet,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .padding(bottom = 32.dp)
            ) {
                // Grab Handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Steps",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val leftToGo = steps.size - stepStatuses.values.count { it != RunnerStepStatus.TODO }
                        Text(
                            text = "${steps.size} steps · $leftToGo to go · tap one to jump",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .clickable { showStepsSheet = false },
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsIcon(name = "x", tint = MaterialTheme.colorScheme.onSurface, size = 18.dp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(ReflexTokens.ShapeCard)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    steps.forEachIndexed { idx, st ->
                        val isCur = idx == stepIndex
                        val stStatus = stepStatuses[idx] ?: RunnerStepStatus.TODO

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Jump to step idx
                                    steps.indices.forEach { m ->
                                        if (m >= idx) stepStatuses[m] = RunnerStepStatus.TODO
                                    }
                                    showStepsSheet = false
                                    beginStep(idx)
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Marker (28dp)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .then(
                                        when {
                                            stStatus == RunnerStepStatus.DONE -> Modifier.background(CopperPrimary)
                                            isCur -> Modifier.border(2.dp, CopperPrimary, CircleShape)
                                            else -> Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stStatus == RunnerStepStatus.DONE) {
                                    SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.onCopper, size = 16.dp)
                                } else if (stStatus == RunnerStepStatus.SKIP) {
                                    Text("–", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outlineVariant)
                                } else {
                                    Text(
                                        text = "${idx + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isCur) CopperPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Title & Description
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCur) CopperPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stepDescription(st),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Status Tag
                            val statusTag = when {
                                isCur -> if (isPaused) "Paused" else "In progress"
                                stStatus == RunnerStepStatus.DONE -> "Done"
                                stStatus == RunnerStepStatus.SKIP -> "Skipped"
                                else -> "Up next"
                            }
                            val tagColor = when {
                                isCur -> CopperPrimary
                                stStatus == RunnerStepStatus.DONE -> MaterialTheme.colorScheme.routineGreen
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }

                            Text(
                                text = statusTag,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = tagColor
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Summary Screen Content reproducing reflex-home.html #sm
 */
@Composable
private fun SessionSummaryContent(
    routineTitle: String,
    isEndedEarly: Boolean,
    totalElapsedSeconds: Int,
    steps: List<Step>,
    stepStatuses: Map<Int, RunnerStepStatus>,
    stepProgressLogs: Map<Int, String>,
    streakDays: Int,
    onDone: () -> Unit,
    onRunAgain: () -> Unit
) {
    val totalSteps = steps.size
    val doneCount = stepStatuses.values.count { it == RunnerStepStatus.DONE }
    val skipCount = stepStatuses.values.count { it == RunnerStepStatus.SKIP }
    val notStartedCount = totalSteps - doneCount - skipCount

    val mm = totalElapsedSeconds / 60
    val ss = totalElapsedSeconds % 60
    val timeFormatted = "${mm}:${String.format("%02d", ss)}"

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .reflexStatusBarPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Hero Circle Mark (120dp)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (isEndedEarly) MaterialTheme.colorScheme.secondaryContainer else CopperPrimary),
                contentAlignment = Alignment.Center
            ) {
                SettingsIcon(
                    name = if (isEndedEarly) "stop" else "check",
                    tint = if (isEndedEarly) CopperPrimary else MaterialTheme.colorScheme.onCopper,
                    size = 56.dp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = if (isEndedEarly) "Session ended" else "$routineTitle complete",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isEndedEarly) "Saved to history as ended early." else "Logged to history. Nice and steady.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3 Stat Tiles (.sg)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Total Time
                SummaryStatTile(
                    modifier = Modifier.weight(1f),
                    value = timeFormatted,
                    label = "Total time"
                )

                // 2. Steps Done
                SummaryStatTile(
                    modifier = Modifier.weight(1f),
                    value = "$doneCount/$totalSteps",
                    label = if (skipCount > 0) "$skipCount skipped" else "Steps done"
                )

                // 3. Streak or Not started
                SummaryStatTile(
                    modifier = Modifier.weight(1f),
                    value = if (isEndedEarly) "$notStartedCount" else "$streakDays days",
                    label = if (isEndedEarly) "Not started" else "Routine streak"
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Step Progress Logs Card (.tl)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ReflexTokens.ShapeCard)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeCard)
                .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Column {
                    steps.forEachIndexed { idx, st ->
                        val stStatus = stepStatuses[idx] ?: RunnerStepStatus.TODO
                        val logText = stepProgressLogs[idx] ?: when (st.stepType) {
                            StepType.REPEAT_COUNT -> "${st.targetCount ?: 3} sets · ${st.restDurationSeconds ?: 15}s rest"
                            StepType.TIMED -> "Timed · ${st.durationSeconds ?: 60}s"
                            StepType.CHECK_OFF -> "Check off"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (stStatus == RunnerStepStatus.DONE) Modifier.background(CopperPrimary)
                                        else Modifier.border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (stStatus == RunnerStepStatus.DONE) {
                                    SettingsIcon(name = "check", tint = MaterialTheme.colorScheme.onCopper, size = 16.dp)
                                } else {
                                    Text("–", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = st.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = logText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val tagText = when (stStatus) {
                                RunnerStepStatus.DONE -> "Done"
                                RunnerStepStatus.SKIP -> "Skipped"
                                else -> "Not started"
                            }
                            val tagColor = when (stStatus) {
                                RunnerStepStatus.DONE -> MaterialTheme.colorScheme.routineGreen
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            Text(
                                text = tagText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = tagColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action: Done (56dp stadium pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface)
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Done",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.surface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action: Run again (56dp input pill)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable { onRunAgain() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Run again",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Completion Confetti Overlay (Clean finish only)
        if (!isEndedEarly) {
            com.reflex.app.ui.components.ConfettiOverlay()
        }
    }
}

@Composable
private fun SummaryStatTile(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Box(
        modifier = modifier
            .clip(ReflexTokens.ShapeInnerTile)
            .background(MaterialTheme.colorScheme.surface)
            .border(BorderStroke(ReflexTokens.BorderThin, MaterialTheme.colorScheme.outline), ReflexTokens.ShapeInnerTile)
            .padding(vertical = 16.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

