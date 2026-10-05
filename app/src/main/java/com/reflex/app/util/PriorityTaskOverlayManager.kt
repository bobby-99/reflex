package com.reflex.app.util

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.reflex.app.MainActivity
import com.reflex.app.ReflexApplication
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.Task
import com.reflex.app.ui.components.SnoozeOption
import com.reflex.app.ui.theme.DestructiveRed
import com.reflex.app.ui.theme.ReflexTheme
import com.reflex.app.ui.theme.ReflexTokens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Manages a system-level full-screen overlay for Medium and High priority tasks,
 * floating over games, media apps, and any other running applications on Android.
 */
object PriorityTaskOverlayManager {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var customLifecycleOwner: CustomLifecycleOwner? = null
    private var isShowing = false
    private var currentTaskId: Long? = null

    private var currentTaskState by mutableStateOf<Task?>(null)

    fun show(context: Context, task: Task) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showInternal(context, task)
        } else {
            Handler(Looper.getMainLooper()).post {
                showInternal(context, task)
            }
        }
    }

    fun hide() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            hideInternal()
        } else {
            Handler(Looper.getMainLooper()).post {
                hideInternal()
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showInternal(context: Context, task: Task) {
        val appContext = context.applicationContext
        if (!AppBlockPermissionHelper.hasOverlayPermission(appContext)) {
            try {
                val intent = Intent(appContext, com.reflex.app.ui.PriorityTaskAlertActivity::class.java).apply {
                    putExtra(com.reflex.app.ui.PriorityTaskAlertActivity.EXTRA_TASK_ID, task.id)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                appContext.startActivity(intent)
            } catch (e: Exception) {
                AppLog.e("PriorityTaskOverlayManager", "Failed to start PriorityTaskAlertActivity fallback", e)
            }
            return
        }

        currentTaskState = task
        currentTaskId = task.id

        if (isShowing && overlayView != null) {
            return
        }

        if (windowManager == null) {
            windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        if (overlayView == null) {
            val lifecycleOwner = CustomLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            customLifecycleOwner = lifecycleOwner

            val view = ComposeView(appContext).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                })
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    ReflexTheme {
                        val activeTask = currentTaskState
                        if (activeTask != null) {
                            PriorityOverlayContent(
                                context = appContext,
                                task = activeTask,
                                onDismiss = { hide() }
                            )
                        }
                    }
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager?.addView(view, params)
                overlayView = view
                isShowing = true
            } catch (e: Exception) {
                AppLog.e("PriorityTaskOverlayManager", "Failed to show priority task overlay, falling back to activity", e)
                try {
                    val intent = Intent(appContext, com.reflex.app.ui.PriorityTaskAlertActivity::class.java).apply {
                        putExtra(com.reflex.app.ui.PriorityTaskAlertActivity.EXTRA_TASK_ID, task.id)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    appContext.startActivity(intent)
                } catch (e2: Exception) {
                    AppLog.e("PriorityTaskOverlayManager", "Failed to start PriorityTaskAlertActivity fallback", e2)
                }
            }
        }
    }

    private fun hideInternal() {
        if (isShowing && overlayView != null) {
            try {
                customLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                customLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                customLifecycleOwner?.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                AppLog.w("PriorityTaskOverlayManager", "Failed to remove priority task overlay view cleanly", e)
            }
            overlayView = null
            windowManager = null
            customLifecycleOwner = null
            isShowing = false
            currentTaskId = null
            currentTaskState = null
        }
    }
}

@Composable
fun PriorityOverlayContent(
    context: Context,
    task: Task,
    onDismiss: () -> Unit
) {
    val app = context.applicationContext as ReflexApplication
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    // Outer full-screen dimming backdrop
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clickable(enabled = false) {}
        ) {
            com.reflex.app.ui.components.PriorityTaskAlertCard(
                task = task,
                onComplete = { completedTask ->
                    notificationManager.cancel(completedTask.id.toInt())
                    CoroutineScope(Dispatchers.IO).launch {
                        app.repository.toggleTaskCompleted(completedTask, context)
                    }
                    onDismiss()
                },
                onOpenTask = {
                    onDismiss()
                    val intent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(NotificationHelper.EXTRA_NAVIGATE_TO, "tasks")
                        putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
                    }
                    context.startActivity(intent)
                },
                onSnooze = { snoozedTask, targetMillis ->
                    notificationManager.cancel(snoozedTask.id.toInt())
                    val updatedTask = snoozedTask.copy(reminderTime = targetMillis)
                    CoroutineScope(Dispatchers.IO).launch {
                        app.repository.updateTask(updatedTask)
                        AlarmScheduler.scheduleTaskReminder(context, updatedTask)
                    }
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}

