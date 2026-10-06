package com.reflex.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.reflex.app.MainActivity
import com.reflex.app.R
import com.reflex.app.data.Step
import com.reflex.app.data.StepType
import com.reflex.app.util.NotificationHelper
import com.reflex.app.util.SettingsRepository
import com.reflex.app.util.SoundCuePlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class RoutineTimerState(
    val routineId: Long,
    val routineTitle: String,
    val steps: List<Step>,
    val currentStepIndex: Int,
    val currentStepRemainingSeconds: Int,
    val currentStepRepCount: Int,
    val isPaused: Boolean,
    val totalElapsedSeconds: Int,
    val completedStepsCount: Int,
    val isResting: Boolean = false,
    val restRemainingSeconds: Int = 0,
    val restDurationSeconds: Int = 15,
    val restBetweenStepsEnabled: Boolean = false,
    val nextStepName: String? = null,
    val isFinished: Boolean = false,
    val isCancelled: Boolean = false
)

class RoutineTimerService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var tickerJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannels(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must call startForeground immediately to avoid Android 14+ ForegroundServiceDidNotStartInTimeException
        startForegroundNotification()

        val action = intent?.action

        when (action) {
            ACTION_START -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                val routineTitle = intent.getStringExtra(EXTRA_ROUTINE_TITLE) ?: "Routine"
                val restEnabled = intent.getBooleanExtra(EXTRA_REST_ENABLED, false)
                val restDuration = intent.getIntExtra(EXTRA_REST_DURATION, 15)

                @Suppress("DEPRECATION", "UNCHECKED_CAST")
                val stepsExtra = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getSerializableExtra(EXTRA_STEPS, ArrayList::class.java) as? ArrayList<Step>
                } else {
                    intent.getSerializableExtra(EXTRA_STEPS) as? ArrayList<Step>
                } ?: emptyList()

                if (routineId != -1L) {
                    if (stepsExtra.isNotEmpty()) {
                        startRoutineTimer(routineId, routineTitle, stepsExtra, restEnabled, restDuration)
                    } else {
                        // Load steps from DB as fallback
                        val app = applicationContext as com.reflex.app.ReflexApplication
                        serviceScope.launch {
                            val data = app.repository.getRoutineWithSteps(routineId).firstOrNull()
                            if (data != null && data.steps.isNotEmpty()) {
                                startRoutineTimer(
                                    routineId = routineId,
                                    routineTitle = data.routine.name,
                                    steps = data.steps,
                                    restBetweenStepsEnabled = data.routine.restBetweenStepsEnabled,
                                    restDurationSeconds = data.routine.restDurationSeconds
                                )
                            }
                        }
                    }
                }
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_TOGGLE_PAUSE -> togglePause()
            ACTION_SKIP -> skipCurrent()
            ACTION_SKIP_REST -> skipRest()
            ACTION_PREVIOUS -> previousStep()
            ACTION_ADD_TIME -> addTime(intent.getIntExtra(EXTRA_ADD_SECONDS, 10))
            ACTION_INCREMENT_REP -> incrementRep()
            ACTION_MARK_DONE -> markStepDone()
            ACTION_STOP -> stopRoutineTimer()
        }

        return START_NOT_STICKY
    }

    private fun startRoutineTimer(
        routineId: Long,
        routineTitle: String,
        steps: List<Step>,
        restBetweenStepsEnabled: Boolean,
        restDurationSeconds: Int
    ) {
        val firstStep = steps.firstOrNull()
        val initialRemaining = if (firstStep?.stepType == StepType.TIMED) (firstStep.durationSeconds ?: 0) else 0

        _timerState.value = RoutineTimerState(
            routineId = routineId,
            routineTitle = routineTitle,
            steps = steps,
            currentStepIndex = 0,
            currentStepRemainingSeconds = initialRemaining,
            currentStepRepCount = 0,
            isPaused = false,
            totalElapsedSeconds = 0,
            completedStepsCount = 0,
            isResting = false,
            restRemainingSeconds = 0,
            restDurationSeconds = restDurationSeconds,
            restBetweenStepsEnabled = restBetweenStepsEnabled
        )

        startForegroundNotification()
        startTicker()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (true) {
                delay(1000L)
                val currentState = _timerState.value ?: break

                if (currentState.isFinished || currentState.isCancelled) break
                if (currentState.isPaused) continue

                val newTotalElapsed = currentState.totalElapsedSeconds + 1

                if (currentState.isResting) {
                    val remainingRest = currentState.restRemainingSeconds - 1
                    if (remainingRest <= 0) {
                        playCompletionCue()
                        exitRestAndAdvance(currentState, newTotalElapsed)
                    } else {
                        _timerState.value = currentState.copy(
                            totalElapsedSeconds = newTotalElapsed,
                            restRemainingSeconds = remainingRest
                        )
                    }
                } else {
                    val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex)
                    if (currentStep == null) break

                    if (currentStep.stepType == StepType.TIMED) {
                        val autoAdvance = SettingsRepository.getBoolean("r_auto", true)
                        val remaining = currentState.currentStepRemainingSeconds - 1
                        if (remaining < 0) {
                            // Hold timer at 0 and wait for user to tap Next (ACTION_SKIP or ACTION_MARK_DONE)
                            _timerState.value = currentState.copy(
                                totalElapsedSeconds = newTotalElapsed,
                                currentStepRemainingSeconds = 0
                            )
                        } else if (remaining == 0) {
                            playCompletionCue()
                            if (autoAdvance) {
                                advanceToNextStep(currentState, newTotalElapsed, wasCompleted = true)
                            } else {
                                _timerState.value = currentState.copy(
                                    totalElapsedSeconds = newTotalElapsed,
                                    currentStepRemainingSeconds = 0
                                )
                            }
                        } else {
                            _timerState.value = currentState.copy(
                                totalElapsedSeconds = newTotalElapsed,
                                currentStepRemainingSeconds = remaining
                            )
                        }
                    } else {
                        _timerState.value = currentState.copy(
                            totalElapsedSeconds = newTotalElapsed
                        )
                    }
                }

                updateNotification()
            }
        }
    }

    private fun advanceToNextStep(state: RoutineTimerState, newElapsed: Int, wasCompleted: Boolean) {
        val nextIndex = state.currentStepIndex + 1
        val newCompletedCount = if (wasCompleted) state.completedStepsCount + 1 else state.completedStepsCount

        if (nextIndex >= state.steps.size) {
            // Routine Finished Completely — no rest after final step
            _timerState.value = state.copy(
                isResting = false,
                currentStepIndex = state.steps.size - 1,
                totalElapsedSeconds = newElapsed,
                completedStepsCount = newCompletedCount,
                isFinished = true
            )
            stopRoutineTimer(clearState = false)
        } else {
            if (state.restBetweenStepsEnabled) {
                val nextStep = state.steps[nextIndex]
                _timerState.value = state.copy(
                    isResting = true,
                    restRemainingSeconds = state.restDurationSeconds,
                    nextStepName = nextStep.name,
                    completedStepsCount = newCompletedCount,
                    totalElapsedSeconds = newElapsed
                )
            } else {
                val nextStep = state.steps[nextIndex]
                val nextRemaining = if (nextStep.stepType == StepType.TIMED) (nextStep.durationSeconds ?: 0) else 0
                _timerState.value = state.copy(
                    isResting = false,
                    currentStepIndex = nextIndex,
                    currentStepRemainingSeconds = nextRemaining,
                    currentStepRepCount = 0,
                    totalElapsedSeconds = newElapsed,
                    completedStepsCount = newCompletedCount
                )
            }
        }
    }

    private fun exitRestAndAdvance(state: RoutineTimerState, newElapsed: Int) {
        val nextIndex = state.currentStepIndex + 1
        if (nextIndex >= state.steps.size) {
            _timerState.value = state.copy(
                isResting = false,
                currentStepIndex = state.steps.size - 1,
                totalElapsedSeconds = newElapsed,
                isFinished = true
            )
            stopRoutineTimer(clearState = false)
        } else {
            val nextStep = state.steps[nextIndex]
            val nextRemaining = if (nextStep.stepType == StepType.TIMED) (nextStep.durationSeconds ?: 0) else 0
            _timerState.value = state.copy(
                isResting = false,
                restRemainingSeconds = 0,
                currentStepIndex = nextIndex,
                currentStepRemainingSeconds = nextRemaining,
                currentStepRepCount = 0,
                totalElapsedSeconds = newElapsed
            )
        }
    }

    private fun skipCurrent() {
        val currentState = _timerState.value ?: return
        if (currentState.isResting) {
            skipRest()
        } else {
            advanceToNextStep(currentState, currentState.totalElapsedSeconds, wasCompleted = false)
            updateNotification()
        }
    }

    private fun skipRest() {
        val currentState = _timerState.value ?: return
        if (!currentState.isResting) return
        exitRestAndAdvance(currentState, currentState.totalElapsedSeconds)
        updateNotification()
    }

    private fun togglePause() {
        val currentState = _timerState.value ?: return
        _timerState.value = currentState.copy(isPaused = !currentState.isPaused)
        updateNotification()
    }

    private fun pauseTimer() {
        val currentState = _timerState.value ?: return
        _timerState.value = currentState.copy(isPaused = true)
        updateNotification()
    }

    private fun resumeTimer() {
        val currentState = _timerState.value ?: return
        _timerState.value = currentState.copy(isPaused = false)
        updateNotification()
    }

    private fun addTime(seconds: Int) {
        val currentState = _timerState.value ?: return
        if (currentState.isResting) {
            val newRest = currentState.restRemainingSeconds + seconds
            _timerState.value = currentState.copy(restRemainingSeconds = newRest)
        } else {
            val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex)
            if (currentStep?.stepType == StepType.TIMED) {
                val newRemaining = currentState.currentStepRemainingSeconds + seconds
                _timerState.value = currentState.copy(currentStepRemainingSeconds = newRemaining)
            }
        }
        updateNotification()
    }

    private fun previousStep() {
        val currentState = _timerState.value ?: return
        if (currentState.isResting) {
            // Cancel rest and go back to current step
            val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex)
            val currentRemaining = if (currentStep?.stepType == StepType.TIMED) (currentStep.durationSeconds ?: 0) else 0
            _timerState.value = currentState.copy(
                isResting = false,
                restRemainingSeconds = 0,
                currentStepRemainingSeconds = currentRemaining
            )
            updateNotification()
        } else if (currentState.currentStepIndex > 0) {
            val prevIndex = currentState.currentStepIndex - 1
            val prevStep = currentState.steps[prevIndex]
            val prevRemaining = if (prevStep.stepType == StepType.TIMED) (prevStep.durationSeconds ?: 0) else 0
            _timerState.value = currentState.copy(
                isResting = false,
                currentStepIndex = prevIndex,
                currentStepRemainingSeconds = prevRemaining,
                currentStepRepCount = 0
            )
            updateNotification()
        }
    }

    private fun incrementRep() {
        val currentState = _timerState.value ?: return
        if (currentState.isResting) return
        val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex) ?: return
        if (currentStep.stepType == StepType.REPEAT_COUNT) {
            val newCount = currentState.currentStepRepCount + 1
            val target = currentStep.targetCount ?: 1
            if (newCount >= target) {
                playCompletionCue()
                advanceToNextStep(currentState, currentState.totalElapsedSeconds, wasCompleted = true)
            } else {
                _timerState.value = currentState.copy(currentStepRepCount = newCount)
            }
            updateNotification()
        }
    }

    private fun markStepDone() {
        val currentState = _timerState.value ?: return
        if (currentState.isResting) return
        val currentStep = currentState.steps.getOrNull(currentState.currentStepIndex)
        // If it's timed and already reached 0, the completion cue was already played upon expiry
        if (currentStep?.stepType != StepType.TIMED || currentState.currentStepRemainingSeconds > 0) {
            playCompletionCue()
        }
        advanceToNextStep(currentState, currentState.totalElapsedSeconds, wasCompleted = true)
        updateNotification()
    }

    private fun stopRoutineTimer(clearState: Boolean = true) {
        tickerJob?.cancel()
        if (clearState) {
            _timerState.value = null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(NOTIFICATION_ID)
        stopSelf()
    }

    private fun playCompletionCue() {
        try {
            val soundEnabled = SettingsRepository.getBoolean("r_snd", true)
            val vibEnabled = SettingsRepository.getBoolean("r_vib", true)

            if (soundEnabled) {
                SoundCuePlayer.playTingChime()
            }
            if (vibEnabled) {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = getSystemService(android.os.VibratorManager::class.java)
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(200L)
                }
            }
        } catch (e: Exception) {
            com.reflex.app.util.AppLog.w("RoutineTimerService", "Vibration feedback error", e)
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val state = _timerState.value
        val title = state?.routineTitle ?: "Routine Timer"

        val contentText = if (state?.isResting == true) {
            val sec = state.restRemainingSeconds
            val m = sec / 60
            val s = sec % 60
            val restTime = String.format("%02d:%02d", m, s)
            val nextName = state.nextStepName ?: "Next Step"
            "Rest — $restTime (Next: $nextName)"
        } else {
            val step = state?.steps?.getOrNull(state.currentStepIndex)
            val stepName = step?.name ?: "Step"
            val stepIndex = (state?.currentStepIndex ?: 0) + 1
            val totalSteps = state?.steps?.size ?: 1

            val progressText = when (step?.stepType) {
                StepType.TIMED -> {
                    val sec = state?.currentStepRemainingSeconds ?: 0
                    val m = sec / 60
                    val s = sec % 60
                    String.format("%02d:%02d", m, s)
                }
                StepType.REPEAT_COUNT -> "${state?.currentStepRepCount ?: 0}/${step.targetCount ?: 0} reps"
                else -> "Tap done when ready"
            }
            "Step $stepIndex/$totalSteps: $stepName — $progressText"
        }

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.reflex.app.util.NotificationHelper.EXTRA_NAVIGATE_TO, "routines")
            putExtra(com.reflex.app.util.NotificationHelper.EXTRA_START_ROUTINE_ID, state?.routineId ?: -1L)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (state?.isPaused == true) {
            val intent = Intent(this, RoutineTimerService::class.java).apply { action = ACTION_RESUME }
            val pi = PendingIntent.getService(this, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "RESUME", pi)
        } else {
            val intent = Intent(this, RoutineTimerService::class.java).apply { action = ACTION_PAUSE }
            val pi = PendingIntent.getService(this, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "PAUSE", pi)
        }

        val skipIntent = Intent(this, RoutineTimerService::class.java).apply {
            action = if (state?.isResting == true) ACTION_SKIP_REST else ACTION_SKIP
        }
        val skipPi = PendingIntent.getService(this, 3, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val skipLabel = if (state?.isResting == true) "SKIP REST" else "SKIP"
        val skipAction = NotificationCompat.Action(android.R.drawable.ic_media_next, skipLabel, skipPi)

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_TIMER_SERVICE)
            .setSmallIcon(R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle(title)
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(pauseResumeAction)
            .addAction(skipAction)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        _timerState.value = null
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        } catch (ignored: Exception) {}
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopRoutineTimer(clearState = true)
        super.onTaskRemoved(rootIntent)
    }

    companion object {
        const val NOTIFICATION_ID = 2001

        fun cancelNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        }

        const val ACTION_START = "com.reflex.productivity.service.ACTION_START"
        const val ACTION_PAUSE = "com.reflex.productivity.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.reflex.productivity.service.ACTION_RESUME"
        const val ACTION_TOGGLE_PAUSE = "com.reflex.productivity.service.ACTION_TOGGLE_PAUSE"
        const val ACTION_SKIP = "com.reflex.productivity.service.ACTION_SKIP"
        const val ACTION_SKIP_REST = "com.reflex.productivity.service.ACTION_SKIP_REST"
        const val ACTION_PREVIOUS = "com.reflex.productivity.service.ACTION_PREVIOUS"
        const val ACTION_ADD_TIME = "com.reflex.productivity.service.ACTION_ADD_TIME"
        const val ACTION_INCREMENT_REP = "com.reflex.productivity.service.ACTION_INCREMENT_REP"
        const val ACTION_MARK_DONE = "com.reflex.productivity.service.ACTION_MARK_DONE"
        const val ACTION_STOP = "com.reflex.productivity.service.ACTION_STOP"

        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_ROUTINE_TITLE = "extra_routine_title"
        const val EXTRA_STEPS = "extra_steps"
        const val EXTRA_REST_ENABLED = "extra_rest_enabled"
        const val EXTRA_REST_DURATION = "extra_rest_duration"
        const val EXTRA_ADD_SECONDS = "extra_add_seconds"

        private val _timerState = MutableStateFlow<RoutineTimerState?>(null)
        val timerState: StateFlow<RoutineTimerState?> = _timerState.asStateFlow()

        fun cancelTimerState() {
            val curr = _timerState.value
            if (curr != null) {
                _timerState.value = curr.copy(isCancelled = true)
            }
        }
    }
}
