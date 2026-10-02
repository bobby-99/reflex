package com.reflex.app.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.reflex.app.ReflexApplication
import com.reflex.app.data.BlockingMode
import com.reflex.app.util.AppBlockOverlayManager
import com.reflex.app.util.AppBlockPermissionHelper
import com.reflex.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AppBlockMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private var monitorJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 2002
        const val ACTION_START_MONITOR = "com.reflex.productivity.focus.START_APP_MONITOR"
        const val ACTION_STOP_MONITOR = "com.reflex.productivity.focus.STOP_APP_MONITOR"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceNotification()
        when (intent?.action) {
            ACTION_START_MONITOR -> {
                startMonitoring()
            }
            ACTION_STOP_MONITOR -> {
                stopMonitoring()
            }
        }
        return START_NOT_STICKY
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            val repository = (applicationContext as ReflexApplication).repository
            var currentlyBlockedPackage: String? = null

            while (isActive) {
                val currentState = FocusTimerService.timerState.value
                val settings = repository.getFocusSettingsSync()

                if (currentState == null || currentState.isFinished || currentState.isCancelled || settings.blockingMode == BlockingMode.OFF) {
                    currentlyBlockedPackage = null
                    AppBlockOverlayManager.hide()
                    delay(1000L)
                    continue
                }

                // Blocking applies ONLY during active work/flow phases
                val isWorkPhase = when (currentState.phase) {
                    FocusPhase.WORK, FocusPhase.TIMED_FLOW, FocusPhase.OPEN_FLOW -> true
                    FocusPhase.SHORT_BREAK, FocusPhase.LONG_BREAK -> false
                }

                val isActiveAndUnpaused = isWorkPhase && !currentState.isPaused && !currentState.isWaitingForNextPhase

                if (!isActiveAndUnpaused) {
                    currentlyBlockedPackage = null
                    AppBlockOverlayManager.hide()
                } else {
                    val fgPackage = getForegroundPackage(applicationContext)
                    val hardcodedExempt = AppBlockPermissionHelper.getHardcodedExemptPackages(applicationContext)
                    val selfPackage = applicationContext.packageName
                    val isLauncher = isLauncherPackage(applicationContext, fgPackage)

                    if (fgPackage == selfPackage && ReflexApplication.isAppInForeground) {
                        // User actively opened Reflex main app UI (MainActivity)
                        currentlyBlockedPackage = null
                        AppBlockOverlayManager.hide()
                    } else if (isLauncher) {
                        // User returned to Home / Launcher screen
                        currentlyBlockedPackage = null
                        AppBlockOverlayManager.hide()
                    } else if (fgPackage != null && hardcodedExempt.contains(fgPackage) && fgPackage != selfPackage) {
                        // Phone dialer, SMS, or System Settings
                        currentlyBlockedPackage = null
                        AppBlockOverlayManager.hide()
                    } else {
                        val activePackage = fgPackage ?: currentlyBlockedPackage

                        if (activePackage != null && activePackage != selfPackage && !hardcodedExempt.contains(activePackage)) {
                            val selectedPkgs = settings.selectedPackagesSet
                            val isBlocked = when (settings.blockingMode) {
                                BlockingMode.OFF -> false
                                BlockingMode.BLOCK_LIST -> selectedPkgs.contains(activePackage)
                                BlockingMode.ALLOW_LIST -> !selectedPkgs.contains(activePackage)
                            }

                            if (isBlocked) {
                                if (currentlyBlockedPackage != activePackage) {
                                    FocusTimerService.incrementBlockedAttempt()
                                    repository.recordBlockedAppEvent(activePackage, System.currentTimeMillis())
                                }
                                currentlyBlockedPackage = activePackage
                                AppBlockOverlayManager.show(applicationContext, activePackage, currentState)
                            } else {
                                currentlyBlockedPackage = null
                                AppBlockOverlayManager.hide()
                            }
                        } else if (currentlyBlockedPackage != null && !ReflexApplication.isAppInForeground) {
                            AppBlockOverlayManager.show(applicationContext, currentlyBlockedPackage!!, currentState)
                        } else {
                            currentlyBlockedPackage = null
                            AppBlockOverlayManager.hide()
                        }
                    }
                }

                delay(500L)
            }
        }
    }

    private fun getForegroundPackage(context: Context): String? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 60000L
        val events = usm.queryEvents(startTime, endTime)
        var fgPackage: String? = null
        val event = UsageEvents.Event()

        val ignoredPackages = setOf(
            "com.android.systemui",
            "android",
            "com.android.keyguard"
        )

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                if (!ignoredPackages.contains(event.packageName)) {
                    fgPackage = event.packageName
                }
            }
        }

        if (fgPackage == null) {
            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            fgPackage = stats?.filter { !ignoredPackages.contains(it.packageName) }
                ?.maxByOrNull { it.lastTimeUsed }
                ?.packageName
        }

        return fgPackage
    }

    private fun isLauncherPackage(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrEmpty()) return false
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val pm = context.packageManager
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        return resolveInfos.any { it.activityInfo.packageName == packageName }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        AppBlockOverlayManager.hide()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_FOCUS_TIMER)
            .setSmallIcon(com.reflex.app.R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle("Focus App Blocker Active")
            .setContentText("Monitoring background applications during focus work")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        stopMonitoring()
        super.onDestroy()
    }
}
