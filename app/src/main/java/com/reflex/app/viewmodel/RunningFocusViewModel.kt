package com.reflex.app.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.FocusChecklistItem
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.data.FocusSettings
import com.reflex.app.data.ReflexRepository
import com.reflex.app.service.FocusPhase
import com.reflex.app.service.FocusTimerService
import com.reflex.app.service.FocusTimerState
import com.reflex.app.util.AppLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RunningFocusViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    private val sessionCompletionHandled = java.util.concurrent.atomic.AtomicBoolean(false)

    val timerState: StateFlow<FocusTimerState?> = FocusTimerService.timerState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun startPomodoro(
        context: Context,
        sessionTitle: String? = null,
        checklistJson: String? = null,
        tagId: Long? = null
    ) {
        viewModelScope.launch {
            val settings = repository.getFocusSettingsSync()
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = FocusTimerService.ACTION_START_POMODORO
                putExtra(FocusTimerService.EXTRA_WORK_MIN, settings.workDurationMin)
                putExtra(FocusTimerService.EXTRA_SHORT_BREAK_MIN, settings.shortBreakMin)
                putExtra(FocusTimerService.EXTRA_LONG_BREAK_MIN, settings.longBreakMin)
                putExtra(FocusTimerService.EXTRA_CYCLES, settings.sessionsBeforeLongBreak)
                putExtra(FocusTimerService.EXTRA_AUTO_START, settings.autoStartNextPhase)
                putExtra(FocusTimerService.EXTRA_SOUND, settings.soundEnabled)
                putExtra(FocusTimerService.EXTRA_VIBRATION, settings.vibrationEnabled)
                if (!sessionTitle.isNullOrBlank()) putExtra(FocusTimerService.EXTRA_SESSION_TITLE, sessionTitle)
                if (!checklistJson.isNullOrBlank()) putExtra(FocusTimerService.EXTRA_CHECKLIST_JSON, checklistJson)
                if (tagId != null) putExtra(FocusTimerService.EXTRA_TAG_ID, tagId)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    fun startTimedFlow(
        context: Context,
        targetMin: Int,
        sessionTitle: String? = null,
        checklistJson: String? = null,
        tagId: Long? = null
    ) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START_TIMED_FLOW
            putExtra(FocusTimerService.EXTRA_TARGET_MIN, targetMin)
            if (!sessionTitle.isNullOrBlank()) putExtra(FocusTimerService.EXTRA_SESSION_TITLE, sessionTitle)
            if (!checklistJson.isNullOrBlank()) putExtra(FocusTimerService.EXTRA_CHECKLIST_JSON, checklistJson)
            if (tagId != null) putExtra(FocusTimerService.EXTRA_TAG_ID, tagId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun startOpenFlow(
        context: Context,
        sessionTitle: String? = null,
        checklistJson: String? = null,
        tagId: Long? = null
    ) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START_OPEN_FLOW
            if (!sessionTitle.isNullOrBlank()) putExtra(FocusTimerService.EXTRA_SESSION_TITLE, sessionTitle)
            if (!checklistJson.isNullOrBlank()) putExtra(FocusTimerService.EXTRA_CHECKLIST_JSON, checklistJson)
            if (tagId != null) putExtra(FocusTimerService.EXTRA_TAG_ID, tagId)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun toggleChecklistItem(context: Context, itemId: String) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_TOGGLE_CHECKITEM
            putExtra(FocusTimerService.EXTRA_ITEM_ID, itemId)
        }
        context.startService(intent)
    }

    fun addChecklistItem(context: Context, title: String) {
        val intent = Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_ADD_CHECKITEM
            putExtra(FocusTimerService.EXTRA_ITEM_TITLE, title)
        }
        context.startService(intent)
    }

    fun togglePauseResume(context: Context) {
        val state = timerState.value ?: return
        val action = if (state.isPaused) FocusTimerService.ACTION_RESUME else FocusTimerService.ACTION_PAUSE
        context.startService(Intent(context, FocusTimerService::class.java).apply { this.action = action })
    }

    fun skipPhase(context: Context) {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_SKIP_PHASE
        })
    }

    fun startNextPhase(context: Context) {
        context.startService(Intent(context, FocusTimerService::class.java).apply {
            action = FocusTimerService.ACTION_START_NEXT_PHASE
        })
    }

    fun stopSession(context: Context, onCompleteSession: (Long) -> Unit) {
        if (!sessionCompletionHandled.compareAndSet(false, true)) return
        val state = timerState.value
        val startTime = System.currentTimeMillis() - ((state?.elapsedSeconds ?: 0) * 1000L)
        val endTime = System.currentTimeMillis()
        val isFlowSession = state?.mode == FocusMode.FLOW_OPEN || state?.mode == FocusMode.FLOW_TIMED
        val completed = (state?.isFinished == true) || isFlowSession || ((state?.elapsedSeconds ?: 0) >= 10)

        try {
            context.stopService(Intent(context, FocusTimerService::class.java))
        } catch (e: Exception) {
            AppLog.w("RunningFocusViewModel", "Failed to stop FocusTimerService cleanly", e)
        }
        FocusTimerService.clearActiveState()

        if (state != null) {
            viewModelScope.launch {
                val checklistJsonStr = FocusChecklistItem.toJsonArrayString(state.checklist)
                val session = FocusSession(
                    mode = state.mode,
                    startTime = startTime,
                    endTime = endTime,
                    plannedDurationSeconds = state.plannedDurationSeconds,
                    actualDurationSeconds = state.elapsedSeconds,
                    completedCycles = if (state.mode == FocusMode.CLASSIC_POMODORO) state.currentCycle else 0,
                    completed = completed,
                    blockedAttemptCount = state.blockedAttemptCount,
                    sessionTitle = state.sessionTitle,
                    checklistJson = if (checklistJsonStr == "[]") null else checklistJsonStr,
                    tagId = state.tagId
                )
                val id = repository.saveFocusSession(session)
                onCompleteSession(id)
            }
        } else {
            onCompleteSession(0L)
        }
    }

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RunningFocusViewModel(repository) as T
        }
    }
}
