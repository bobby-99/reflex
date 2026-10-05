package com.reflex.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.reflex.app.MainActivity
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSettings
import com.reflex.app.util.AppLog
import com.reflex.app.util.FocusTickPlayer
import com.reflex.app.util.NotificationHelper
import com.reflex.app.util.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FocusPhase {
    WORK,
    SHORT_BREAK,
    LONG_BREAK,
    TIMED_FLOW,
    OPEN_FLOW
}

data class FocusTimerState(
    val mode: FocusMode = FocusMode.CLASSIC_POMODORO,
    val phase: FocusPhase = FocusPhase.WORK,
    val remainingSeconds: Int = 0,
    val elapsedSeconds: Int = 0,
    val plannedDurationSeconds: Int? = null,
    val currentCycle: Int = 1,
    val totalCycles: Int = 4,
    val isPaused: Boolean = false,
    val isWaitingForNextPhase: Boolean = false,
    val isFinished: Boolean = false,
    val isCancelled: Boolean = false,
    val settings: FocusSettings = FocusSettings(),
    val blockedAttemptCount: Int = 0,
    val sessionTitle: String? = null,
    val checklist: List<com.reflex.app.data.FocusChecklistItem> = emptyList(),
    val tagId: Long? = null,
    val startTime: Long = System.currentTimeMillis()
)

class FocusTimerService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var tickerJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): FocusTimerService = this@FocusTimerService
    }

    companion object {
        const val NOTIFICATION_ID = 2001

        fun cancelNotification(context: Context) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        }

        const val ACTION_START_POMODORO = "com.reflex.productivity.focus.START_POMODORO"
        const val ACTION_START_TIMED_FLOW = "com.reflex.productivity.focus.START_TIMED_FLOW"
        const val ACTION_START_OPEN_FLOW = "com.reflex.productivity.focus.START_OPEN_FLOW"
        const val ACTION_PAUSE = "com.reflex.productivity.focus.PAUSE"
        const val ACTION_RESUME = "com.reflex.productivity.focus.RESUME"
        const val ACTION_SKIP_PHASE = "com.reflex.productivity.focus.SKIP_PHASE"
        const val ACTION_START_NEXT_PHASE = "com.reflex.productivity.focus.START_NEXT_PHASE"
        const val ACTION_TOGGLE_CHECKITEM = "com.reflex.productivity.focus.TOGGLE_CHECKITEM"
        const val ACTION_ADD_CHECKITEM = "com.reflex.productivity.focus.ADD_CHECKITEM"
        const val ACTION_STOP = "com.reflex.productivity.focus.STOP"

        const val EXTRA_WORK_MIN = "extra_work_min"
        const val EXTRA_SHORT_BREAK_MIN = "extra_short_break_min"
        const val EXTRA_LONG_BREAK_MIN = "extra_long_break_min"
        const val EXTRA_CYCLES = "extra_cycles"
        const val EXTRA_AUTO_START = "extra_auto_start"
        const val EXTRA_SOUND = "extra_sound"
        const val EXTRA_VIBRATION = "extra_vibration"
        const val EXTRA_TARGET_MIN = "extra_target_min"
        const val EXTRA_SESSION_TITLE = "extra_session_title"
        const val EXTRA_CHECKLIST_JSON = "extra_checklist_json"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TITLE = "extra_item_title"
        const val EXTRA_TAG_ID = "extra_tag_id"

        private val _timerState = MutableStateFlow<FocusTimerState?>(null)
        val timerState: StateFlow<FocusTimerState?> = _timerState.asStateFlow()

        fun incrementBlockedAttempt() {
            val state = _timerState.value ?: return
            _timerState.value = state.copy(blockedAttemptCount = state.blockedAttemptCount + 1)
        }

        fun clearActiveState(context: Context? = null) {
            _timerState.value = null
            if (context != null) {
                clearSnapshot(context)
            }
        }

        fun persistActiveSnapshot(context: Context, state: FocusTimerState) {
            try {
                val prefs = context.getSharedPreferences("reflex_focus_session_state", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("mode", state.mode.name)
                    .putLong("startTime", state.startTime)
                    .putLong("lastTick", System.currentTimeMillis())
                    .putInt("elapsedSeconds", state.elapsedSeconds)
                    .putInt("plannedDurationSeconds", state.plannedDurationSeconds ?: 0)
                    .putInt("currentCycle", state.currentCycle)
                    .putInt("blockedAttemptCount", state.blockedAttemptCount)
                    .putString("sessionTitle", state.sessionTitle)
                    .putString("checklistJson", com.reflex.app.data.FocusChecklistItem.toJsonArrayString(state.checklist))
                    .putLong("tagId", state.tagId ?: -1L)
                    .apply()
            } catch (_: Exception) {}
        }

        fun clearSnapshot(context: Context) {
            try {
                val prefs = context.getSharedPreferences("reflex_focus_session_state", Context.MODE_PRIVATE)
                prefs.edit().clear().apply()
            } catch (_: Exception) {}
        }

        suspend fun recoverInterruptedSession(context: Context, repository: com.reflex.app.data.ReflexRepository) {
            try {
                val prefs = context.getSharedPreferences("reflex_focus_session_state", Context.MODE_PRIVATE)
                val modeStr = prefs.getString("mode", null) ?: return
                val elapsed = prefs.getInt("elapsedSeconds", 0)
                val startTime = prefs.getLong("startTime", 0L)
                val lastTick = prefs.getLong("lastTick", System.currentTimeMillis())
                val plannedDuration = prefs.getInt("plannedDurationSeconds", 0)
                val cycle = prefs.getInt("currentCycle", 0)
                val blockedAttempts = prefs.getInt("blockedAttemptCount", 0)
                val title = prefs.getString("sessionTitle", null)
                val checklistJson = prefs.getString("checklistJson", null)
                val rawTagId = prefs.getLong("tagId", -1L)
                val tagId = if (rawTagId > 0L) rawTagId else null

                // Clear snapshot
                prefs.edit().clear().apply()

                // Only save if elapsed time >= 60 seconds (under-1-minute rule)
                if (elapsed >= 60) {
                    val mode = try { FocusMode.valueOf(modeStr) } catch (_: Exception) { FocusMode.FLOW_OPEN }
                    val session = com.reflex.app.data.FocusSession(
                        mode = mode,
                        startTime = if (startTime > 0) startTime else (lastTick - elapsed * 1000L),
                        endTime = lastTick,
                        plannedDurationSeconds = if (plannedDuration > 0) plannedDuration else null,
                        actualDurationSeconds = elapsed,
                        completedCycles = cycle,
                        completed = false,
                        blockedAttemptCount = blockedAttempts,
                        sessionTitle = title,
                        checklistJson = if (checklistJson == "[]") null else checklistJson,
                        tagId = tagId,
                        endReason = "stopped_early"
                    )
                    repository.saveFocusSession(session)
                }
            } catch (e: Exception) {
                AppLog.w("FocusTimerService", "Failed to recover interrupted focus session", e)
            }
        }
    }

    private var tickPlayer: FocusTickPlayer? = null

    override fun onCreate() {
        super.onCreate()
        tickPlayer = FocusTickPlayer(this)
        observeTickSetting()
    }

    private fun observeTickSetting() {
        serviceScope.launch {
            SettingsRepository.values.collect { values ->
                val tickEnabled = values["f_tick"] as? Boolean ?: false
                if (!tickEnabled) {
                    tickPlayer?.stop()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (tickPlayer == null) {
            tickPlayer = FocusTickPlayer(this)
            observeTickSetting()
        }
        startForegroundNotification()
        val tagId = if (intent?.hasExtra(EXTRA_TAG_ID) == true) {
            val raw = intent.getLongExtra(EXTRA_TAG_ID, -1L)
            if (raw > 0L) raw else null
        } else null

        when (intent?.action) {
            ACTION_START_POMODORO -> {
                val workMin = intent.getIntExtra(EXTRA_WORK_MIN, 25)
                val shortMin = intent.getIntExtra(EXTRA_SHORT_BREAK_MIN, 5)
                val longMin = intent.getIntExtra(EXTRA_LONG_BREAK_MIN, 15)
                val cycles = intent.getIntExtra(EXTRA_CYCLES, 4)
                val autoStart = intent.getBooleanExtra(EXTRA_AUTO_START, false)
                val sound = intent.getBooleanExtra(EXTRA_SOUND, true)
                val vibration = intent.getBooleanExtra(EXTRA_VIBRATION, true)
                val title = intent.getStringExtra(EXTRA_SESSION_TITLE)
                val checklistJson = intent.getStringExtra(EXTRA_CHECKLIST_JSON)
                val checklist = com.reflex.app.data.FocusChecklistItem.listFromJsonArrayString(checklistJson)

                val settings = FocusSettings(
                    workDurationMin = workMin,
                    shortBreakMin = shortMin,
                    longBreakMin = longMin,
                    sessionsBeforeLongBreak = cycles,
                    autoStartNextPhase = autoStart,
                    soundEnabled = sound,
                    vibrationEnabled = vibration
                )
                startPomodoroSession(settings, title, checklist, tagId)
            }
            ACTION_START_TIMED_FLOW -> {
                val targetMin = intent.getIntExtra(EXTRA_TARGET_MIN, 30)
                val title = intent.getStringExtra(EXTRA_SESSION_TITLE)
                val checklistJson = intent.getStringExtra(EXTRA_CHECKLIST_JSON)
                val checklist = com.reflex.app.data.FocusChecklistItem.listFromJsonArrayString(checklistJson)
                startTimedFlowSession(targetMin, title, checklist, tagId)
            }
            ACTION_START_OPEN_FLOW -> {
                val title = intent.getStringExtra(EXTRA_SESSION_TITLE)
                val checklistJson = intent.getStringExtra(EXTRA_CHECKLIST_JSON)
                val checklist = com.reflex.app.data.FocusChecklistItem.listFromJsonArrayString(checklistJson)
                startOpenFlowSession(title, checklist, tagId)
            }
            ACTION_TOGGLE_CHECKITEM -> {
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
                if (!itemId.isNullOrEmpty()) {
                    toggleChecklistItem(itemId)
                }
            }
            ACTION_ADD_CHECKITEM -> {
                val itemTitle = intent.getStringExtra(EXTRA_ITEM_TITLE)
                if (!itemTitle.isNullOrBlank()) {
                    addChecklistItem(itemTitle)
                }
            }
            ACTION_PAUSE -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_SKIP_PHASE -> skipPhase()
            ACTION_START_NEXT_PHASE -> startNextPhase()
            ACTION_STOP -> stopSession(cancelled = true)
        }
        return START_NOT_STICKY
    }

    private fun toggleChecklistItem(itemId: String) {
        val state = _timerState.value ?: return
        val updatedList = state.checklist.map { item ->
            if (item.id == itemId) item.copy(isCompleted = !item.isCompleted) else item
        }
        _timerState.value = state.copy(checklist = updatedList)
    }

    private fun addChecklistItem(title: String) {
        val state = _timerState.value ?: return
        val newItem = com.reflex.app.data.FocusChecklistItem(
            id = System.currentTimeMillis().toString(),
            title = title,
            isCompleted = false
        )
        _timerState.value = state.copy(checklist = state.checklist + newItem)
    }

    private fun startPomodoroSession(
        settings: FocusSettings,
        sessionTitle: String?,
        checklist: List<com.reflex.app.data.FocusChecklistItem>,
        tagId: Long? = null
    ) {
        val workSec = settings.workDurationMin * 60
        _timerState.value = FocusTimerState(
            mode = FocusMode.CLASSIC_POMODORO,
            phase = FocusPhase.WORK,
            remainingSeconds = workSec,
            elapsedSeconds = 0,
            plannedDurationSeconds = workSec,
            currentCycle = 1,
            totalCycles = settings.sessionsBeforeLongBreak,
            settings = settings,
            sessionTitle = sessionTitle,
            checklist = checklist,
            tagId = tagId
        )

        startForegroundNotification()
        startTicker()
        checkAndStartAppBlockMonitor()
    }

    private fun startTimedFlowSession(
        targetMin: Int,
        sessionTitle: String?,
        checklist: List<com.reflex.app.data.FocusChecklistItem>,
        tagId: Long? = null
    ) {
        val targetSec = targetMin * 60
        _timerState.value = FocusTimerState(
            mode = FocusMode.FLOW_TIMED,
            phase = FocusPhase.TIMED_FLOW,
            remainingSeconds = targetSec,
            elapsedSeconds = 0,
            plannedDurationSeconds = targetSec,
            sessionTitle = sessionTitle,
            checklist = checklist,
            tagId = tagId
        )

        startForegroundNotification()
        startTicker()
        checkAndStartAppBlockMonitor()
    }

    private fun startOpenFlowSession(
        sessionTitle: String?,
        checklist: List<com.reflex.app.data.FocusChecklistItem>,
        tagId: Long? = null
    ) {
        _timerState.value = FocusTimerState(
            mode = FocusMode.FLOW_OPEN,
            phase = FocusPhase.OPEN_FLOW,
            remainingSeconds = 0,
            elapsedSeconds = 0,
            plannedDurationSeconds = null,
            sessionTitle = sessionTitle,
            checklist = checklist,
            tagId = tagId
        )

        startForegroundNotification()
        startTicker()
        checkAndStartAppBlockMonitor()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (isActive) {
                delay(1000L)
                val state = _timerState.value ?: break

                if (state.isFinished || state.isCancelled) {
                    tickPlayer?.stop()
                    break
                }
                if (state.isPaused || state.isWaitingForNextPhase) {
                    tickPlayer?.stop()
                    continue
                }

                val isWorkPhase = state.phase == FocusPhase.WORK ||
                    state.phase == FocusPhase.TIMED_FLOW ||
                    state.phase == FocusPhase.OPEN_FLOW

                if (!isWorkPhase) {
                    tickPlayer?.stop()
                }

                val newElapsed = state.elapsedSeconds + 1

                when (state.mode) {
                    FocusMode.CLASSIC_POMODORO -> {
                        val newRemaining = state.remainingSeconds - 1
                        if (newRemaining <= 0) {
                            tickPlayer?.stop()
                            playCue(state.settings)
                            handlePomodoroPhaseEnd(state, newElapsed)
                        } else {
                            if (isWorkPhase && SettingsRepository.getBoolean("f_tick", false)) {
                                tickPlayer?.playTick()
                            }
                            _timerState.value = state.copy(
                                remainingSeconds = newRemaining,
                                elapsedSeconds = newElapsed
                            )
                        }
                    }
                    FocusMode.FLOW_TIMED -> {
                        val newRemaining = state.remainingSeconds - 1
                        if (newRemaining <= 0) {
                            tickPlayer?.stop()
                            playCue(state.settings)
                            _timerState.value = state.copy(
                                remainingSeconds = 0,
                                elapsedSeconds = newElapsed,
                                isFinished = true
                            )
                        } else {
                            if (isWorkPhase && SettingsRepository.getBoolean("f_tick", false)) {
                                tickPlayer?.playTick()
                            }
                            _timerState.value = state.copy(
                                remainingSeconds = newRemaining,
                                elapsedSeconds = newElapsed
                            )
                        }
                    }
                    FocusMode.FLOW_OPEN -> {
                        if (isWorkPhase && SettingsRepository.getBoolean("f_tick", false)) {
                            tickPlayer?.playTick()
                        }
                        _timerState.value = state.copy(
                            elapsedSeconds = newElapsed
                        )
                    }
                }

                updateNotification()
                if (newElapsed % 3 == 0) {
                    _timerState.value?.let { persistActiveSnapshot(this@FocusTimerService, it) }
                }
            }
        }
    }

    private fun handlePomodoroPhaseEnd(state: FocusTimerState, newElapsed: Int) {
        tickPlayer?.stop()
        val settings = state.settings
        val shouldWait = !settings.autoStartNextPhase

        when (state.phase) {
            FocusPhase.WORK -> {
                val isFinalCycle = state.currentCycle >= state.totalCycles
                if (isFinalCycle) {
                    if (settings.longBreakMin <= 0) {
                        // 0 min long break -> session finishes immediately
                        _timerState.value = state.copy(
                            remainingSeconds = 0,
                            elapsedSeconds = newElapsed,
                            isFinished = true
                        )
                    } else {
                        val nextSec = settings.longBreakMin * 60
                        _timerState.value = state.copy(
                            phase = FocusPhase.LONG_BREAK,
                            remainingSeconds = nextSec,
                            elapsedSeconds = newElapsed,
                            isWaitingForNextPhase = shouldWait,
                            isPaused = false
                        )
                    }
                } else {
                    if (settings.shortBreakMin <= 0) {
                        // 0 min short break -> advance straight to next work session
                        val nextCycle = state.currentCycle + 1
                        val workSec = settings.workDurationMin * 60
                        _timerState.value = state.copy(
                            phase = FocusPhase.WORK,
                            remainingSeconds = workSec,
                            elapsedSeconds = newElapsed,
                            currentCycle = nextCycle,
                            isWaitingForNextPhase = shouldWait,
                            isPaused = false
                        )
                    } else {
                        val nextSec = settings.shortBreakMin * 60
                        _timerState.value = state.copy(
                            phase = FocusPhase.SHORT_BREAK,
                            remainingSeconds = nextSec,
                            elapsedSeconds = newElapsed,
                            isWaitingForNextPhase = shouldWait,
                            isPaused = false
                        )
                    }
                }
            }
            FocusPhase.SHORT_BREAK -> {
                val nextCycle = state.currentCycle + 1
                val workSec = settings.workDurationMin * 60

                _timerState.value = state.copy(
                    phase = FocusPhase.WORK,
                    remainingSeconds = workSec,
                    elapsedSeconds = newElapsed,
                    currentCycle = nextCycle,
                    isWaitingForNextPhase = shouldWait,
                    isPaused = false
                )
            }
            FocusPhase.LONG_BREAK -> {
                // Entire Classic Pomodoro Completed!
                _timerState.value = state.copy(
                    remainingSeconds = 0,
                    elapsedSeconds = newElapsed,
                    isFinished = true
                )
            }
            else -> {}
        }
    }

    fun startNextPhase() {
        val state = _timerState.value ?: return
        _timerState.value = state.copy(isWaitingForNextPhase = false)
        updateNotification()
    }

    fun skipPhase() {
        val state = _timerState.value ?: return
        if (state.isFinished || state.isCancelled) return
        tickPlayer?.stop()
        playCue(state.settings)
        handlePomodoroPhaseEnd(state, state.elapsedSeconds)
    }

    fun pauseSession() {
        tickPlayer?.stop()
        val state = _timerState.value ?: return
        _timerState.value = state.copy(isPaused = true)
        updateNotification()
    }

    fun resumeSession() {
        val state = _timerState.value ?: return
        _timerState.value = state.copy(isPaused = false, isWaitingForNextPhase = false)
        updateNotification()
    }

    fun stopSession(cancelled: Boolean = false) {
        tickPlayer?.stop()
        val state = _timerState.value
        if (state != null) {
            _timerState.value = state.copy(
                isCancelled = cancelled,
                isFinished = !cancelled
            )
        }
        stopForegroundService()
    }

    private fun stopForegroundService() {
        tickPlayer?.stop()
        tickerJob?.cancel()
        _timerState.value = null
        clearSnapshot(this@FocusTimerService)
        stopAppBlockMonitor()
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

    private fun checkAndStartAppBlockMonitor() {
        serviceScope.launch {
            val repository = (applicationContext as com.reflex.app.ReflexApplication).repository
            val settings = repository.getFocusSettingsSync()
            if (settings.blockingMode != com.reflex.app.data.BlockingMode.OFF &&
                com.reflex.app.util.AppBlockPermissionHelper.hasUsageAccessPermission(applicationContext) &&
                com.reflex.app.util.AppBlockPermissionHelper.hasOverlayPermission(applicationContext)
            ) {
                val intent = Intent(applicationContext, AppBlockMonitorService::class.java).apply {
                    action = AppBlockMonitorService.ACTION_START_MONITOR
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
            }
        }
    }

    private fun stopAppBlockMonitor() {
        val intent = Intent(applicationContext, AppBlockMonitorService::class.java).apply {
            action = AppBlockMonitorService.ACTION_STOP_MONITOR
        }
        startService(intent)
    }

    private fun playCue(settings: FocusSettings) {
        if (settings.vibrationEnabled) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(300L)
                }
            } catch (e: Exception) {
                com.reflex.app.util.AppLog.w("FocusTimerService", "Vibration alert error", e)
            }
        }

        if (settings.soundEnabled) {
            com.reflex.app.util.SoundCuePlayer.playTingChime()
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
        val title = when (state?.phase) {
            FocusPhase.WORK -> "Focus — Work Phase (${state.currentCycle}/${state.totalCycles})"
            FocusPhase.SHORT_BREAK -> "Focus — Short Break"
            FocusPhase.LONG_BREAK -> "Focus — Long Break"
            FocusPhase.TIMED_FLOW -> "Focus — Timed Flow"
            FocusPhase.OPEN_FLOW -> "Focus — Open Flow"
            null -> "Focus Session"
        }

        val contentText = if (state?.mode == FocusMode.FLOW_OPEN) {
            val m = state.elapsedSeconds / 60
            val s = state.elapsedSeconds % 60
            String.format("Elapsed: %02d:%02d", m, s)
        } else {
            val sec = state?.remainingSeconds ?: 0
            val m = sec / 60
            val s = sec % 60
            val formatted = String.format("%02d:%02d", m, s)
            if (state?.isWaitingForNextPhase == true) "Phase Complete — Tap to start next" else "Remaining: $formatted"
        }

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(com.reflex.app.util.NotificationHelper.EXTRA_NAVIGATE_TO, "focus")
            putExtra("extra_open_focus_running", true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeAction = if (state?.isPaused == true) {
            val intent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_RESUME }
            val pi = PendingIntent.getService(this, 10, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_play, "RESUME", pi)
        } else {
            val intent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_PAUSE }
            val pi = PendingIntent.getService(this, 11, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            NotificationCompat.Action(android.R.drawable.ic_media_pause, "PAUSE", pi)
        }

        val stopIntent = Intent(this, FocusTimerService::class.java).apply { action = ACTION_STOP }
        val stopPi = PendingIntent.getService(this, 12, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopAction = NotificationCompat.Action(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPi)

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOCUS_TIMER)
            .setSmallIcon(com.reflex.app.R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
            .build()
    }

    override fun onDestroy() {
        tickPlayer?.stop()
        tickPlayer?.release()
        tickPlayer = null
        tickerJob?.cancel()
        _timerState.value = null
        stopAppBlockMonitor()
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
        stopForegroundService()
        super.onTaskRemoved(rootIntent)
    }
}
