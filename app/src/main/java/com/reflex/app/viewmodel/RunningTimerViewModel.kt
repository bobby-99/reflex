package com.reflex.app.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.RoutineWithSteps
import com.reflex.app.data.Step
import com.reflex.app.service.RoutineTimerService
import com.reflex.app.service.RoutineTimerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class RunningTimerViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    val timerState: StateFlow<RoutineTimerState?> = RoutineTimerService.timerState

    private val _routineWithSteps = MutableStateFlow<RoutineWithSteps?>(null)
    val routineWithSteps: StateFlow<RoutineWithSteps?> = _routineWithSteps.asStateFlow()

    fun loadAndStartRoutine(routineId: Long, context: Context) {
        viewModelScope.launch {
            val data = repository.getRoutineWithSteps(routineId).firstOrNull()
            _routineWithSteps.value = data

            val currentServiceState = RoutineTimerService.timerState.value
            if (data != null && (currentServiceState == null || currentServiceState.routineId != routineId || currentServiceState.isFinished || currentServiceState.isCancelled)) {
                startService(context, data)
            }
        }
    }

    private fun startService(context: Context, data: RoutineWithSteps) {
        val intent = Intent(context, RoutineTimerService::class.java).apply {
            action = RoutineTimerService.ACTION_START
            putExtra(RoutineTimerService.EXTRA_ROUTINE_ID, data.routine.id)
            putExtra(RoutineTimerService.EXTRA_ROUTINE_TITLE, data.routine.name)
            putExtra(RoutineTimerService.EXTRA_STEPS, ArrayList(data.steps))
            putExtra(RoutineTimerService.EXTRA_REST_ENABLED, data.routine.restBetweenStepsEnabled)
            putExtra(RoutineTimerService.EXTRA_REST_DURATION, data.routine.restDurationSeconds)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun togglePause(context: Context) {
        sendServiceAction(context, RoutineTimerService.ACTION_TOGGLE_PAUSE)
    }

    fun skipStep(context: Context) {
        sendServiceAction(context, RoutineTimerService.ACTION_SKIP)
    }

    fun skipRest(context: Context) {
        sendServiceAction(context, RoutineTimerService.ACTION_SKIP_REST)
    }

    fun previousStep(context: Context) {
        sendServiceAction(context, RoutineTimerService.ACTION_PREVIOUS)
    }

    fun addTime(context: Context, seconds: Int) {
        val intent = Intent(context, RoutineTimerService::class.java).apply {
            action = RoutineTimerService.ACTION_ADD_TIME
            putExtra(RoutineTimerService.EXTRA_ADD_SECONDS, seconds)
        }
        context.startService(intent)
    }

    fun incrementRep(context: Context) {
        sendServiceAction(context, RoutineTimerService.ACTION_INCREMENT_REP)
    }

    fun markStepDone(context: Context) {
        sendServiceAction(context, RoutineTimerService.ACTION_MARK_DONE)
    }

    fun exitRoutine(context: Context, onLogCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val currentState = RoutineTimerService.timerState.value
            val routineId = currentState?.routineId ?: _routineWithSteps.value?.routine?.id ?: 0L
            val totalTime = currentState?.totalElapsedSeconds ?: 0
            val stepsDone = currentState?.completedStepsCount ?: 0

            // Stop Service
            sendServiceAction(context, RoutineTimerService.ACTION_STOP)
            RoutineTimerService.cancelTimerState()

            // Save Partial Completion Log (isCompleted = false)
            if (routineId != 0L) {
                val log = CompletionLog(
                    routineId = routineId,
                    dateCompleted = System.currentTimeMillis(),
                    totalTimeTakenSeconds = totalTime,
                    stepsCompletedCount = stepsDone,
                    isCompleted = false
                )
                val logId = repository.insertLog(log)
                onLogCreated(logId)
            } else {
                onLogCreated(0L)
            }
        }
    }

    fun completeRoutine(context: Context, onLogCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val currentState = RoutineTimerService.timerState.value
            val routineId = currentState?.routineId ?: _routineWithSteps.value?.routine?.id ?: 0L
            val totalTime = currentState?.totalElapsedSeconds ?: 0
            val totalSteps = currentState?.steps?.size ?: _routineWithSteps.value?.steps?.size ?: 0

            sendServiceAction(context, RoutineTimerService.ACTION_STOP)

            if (routineId != 0L) {
                val log = CompletionLog(
                    routineId = routineId,
                    dateCompleted = System.currentTimeMillis(),
                    totalTimeTakenSeconds = totalTime,
                    stepsCompletedCount = totalSteps,
                    isCompleted = true
                )
                val logId = repository.insertLog(log)
                onLogCreated(logId)
            } else {
                onLogCreated(0L)
            }
        }
    }

    private fun sendServiceAction(context: Context, actionStr: String) {
        val intent = Intent(context, RoutineTimerService::class.java).apply {
            action = actionStr
        }
        context.startService(intent)
    }
}
