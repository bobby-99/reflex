package com.reflex.app.util

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.reflex.app.MainActivity
import com.reflex.app.data.FocusMode
import com.reflex.app.service.FocusPhase
import com.reflex.app.service.FocusTimerService
import com.reflex.app.service.FocusTimerState
import com.reflex.app.ui.components.DeleteConfirmationDialog
import com.reflex.app.ui.components.ReflexButton
import com.reflex.app.ui.components.ReflexButtonVariant
import com.reflex.app.ui.theme.DestructiveRed
import com.reflex.app.ui.theme.ReflexTokens

class CustomLifecycleOwner : SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }

    fun performRestore(savedState: android.os.Bundle?) {
        savedStateRegistryController.performRestore(savedState)
    }
}

object AppBlockOverlayManager {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var customLifecycleOwner: CustomLifecycleOwner? = null
    private var isShowing = false
    private var currentPackageName: String? = null

    private var overlayState by mutableStateOf<OverlayUiData?>(null)

    data class OverlayUiData(
        val appName: String,
        val packageName: String,
        val phaseLabel: String,
        val timeDisplay: String,
        val cycleInfo: String?
    )

    fun show(context: Context, packageName: String, timerState: FocusTimerState) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showInternal(context, packageName, timerState)
        } else {
            Handler(Looper.getMainLooper()).post {
                showInternal(context, packageName, timerState)
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
    private fun showInternal(context: Context, packageName: String, timerState: FocusTimerState) {
        if (!AppBlockPermissionHelper.hasOverlayPermission(context)) return

        val appName = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        val phaseLabel = when (timerState.phase) {
            FocusPhase.WORK -> "FOCUS WORK"
            FocusPhase.TIMED_FLOW -> "TIMED FLOW"
            FocusPhase.OPEN_FLOW -> "OPEN FLOW"
            else -> "FOCUS"
        }

        val formattedTime = if (timerState.mode == FocusMode.FLOW_OPEN) {
            val sec = timerState.elapsedSeconds
            String.format("%02d:%02d", sec / 60, sec % 60)
        } else {
            val sec = timerState.remainingSeconds
            String.format("%02d:%02d", sec / 60, sec % 60)
        }

        val cycleInfo = if (timerState.mode == FocusMode.CLASSIC_POMODORO) {
            "CYCLE ${timerState.currentCycle} OF ${timerState.totalCycles}"
        } else null

        val data = OverlayUiData(
            appName = appName,
            packageName = packageName,
            phaseLabel = phaseLabel,
            timeDisplay = formattedTime,
            cycleInfo = cycleInfo
        )

        overlayState = data

        if (isShowing && currentPackageName == packageName) {
            return
        }

        if (windowManager == null) {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }

        if (overlayView == null) {
            val lifecycleOwner = CustomLifecycleOwner()
            lifecycleOwner.performRestore(null)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            customLifecycleOwner = lifecycleOwner

            val view = ComposeView(context).apply {
                setViewTreeLifecycleOwner(lifecycleOwner)
                setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                    override val viewModelStore = ViewModelStore()
                })
                setViewTreeSavedStateRegistryOwner(lifecycleOwner)

                setContent {
                    OverlayContent(
                        context = context,
                        onBackToFocus = {
                            hide()
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                                putExtra("extra_open_focus_running", true)
                            }
                            context.startActivity(intent)
                        },
                        onEndSessionEarly = {
                            hide()
                            val intent = Intent(context, FocusTimerService::class.java).apply {
                                action = FocusTimerService.ACTION_STOP
                            }
                            context.startService(intent)
                        }
                    )
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            try {
                windowManager?.addView(view, params)
                overlayView = view
                isShowing = true
                currentPackageName = packageName
            } catch (e: Exception) {
                AppLog.e("AppBlockOverlayManager", "Failed to add blocking overlay view", e)
            }
        } else {
            currentPackageName = packageName
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
                AppLog.w("AppBlockOverlayManager", "Failed to remove blocking overlay view cleanly", e)
            }
            overlayView = null
            windowManager = null
            customLifecycleOwner = null
            isShowing = false
            currentPackageName = null
            overlayState = null
        }
    }

    @Composable
    private fun OverlayContent(
        context: Context,
        onBackToFocus: () -> Unit,
        onEndSessionEarly: () -> Unit
    ) {
        var showExitConfirmDialog by remember { mutableStateOf(false) }
        val data = overlayState

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(vertical = 24.dp),
                shape = ReflexTokens.ShapeDialog,
                color = com.reflex.app.ui.theme.DarkSurfaceElevated,
                border = BorderStroke(ReflexTokens.BorderHairline, com.reflex.app.ui.theme.DarkBorder),
                shadowElevation = 16.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Reflex Brand Mark
                    com.reflex.app.ui.components.ReflexMark(
                        size = 48.dp,
                        tint = com.reflex.app.ui.theme.CopperPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Header Pill: Block Icon & Title
                    Box(
                        modifier = Modifier
                            .clip(ReflexTokens.ShapeButton)
                            .background(com.reflex.app.ui.theme.DestructiveContainer)
                            .border(
                                BorderStroke(0.75.dp, com.reflex.app.ui.theme.DarkDestructiveRed.copy(alpha = 0.5f)),
                                ReflexTokens.ShapeButton
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = com.reflex.app.ui.theme.DarkDestructiveRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "App restricted",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.reflex.app.ui.theme.DarkDestructiveRed,
                                fontSize = 13.sp,
                                letterSpacing = 0.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceLg))

                    // Blocked app name tag
                    Text(
                        text = (data?.appName ?: "This app"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = com.reflex.app.ui.theme.DarkInkText,
                        letterSpacing = 0.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))

                    Text(
                        text = "You are currently in an active focus session. Stay in the zone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.reflex.app.ui.theme.DarkSecondaryText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

                    // Active Session Time & Phase Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ReflexTokens.ShapeCard)
                            .background(com.reflex.app.ui.theme.DarkSurface)
                            .border(
                                BorderStroke(ReflexTokens.BorderHairline, com.reflex.app.ui.theme.DarkBorderSubtle),
                                ReflexTokens.ShapeCard
                            )
                            .padding(ReflexTokens.SpaceLg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = (data?.phaseLabel ?: "Focus work"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = com.reflex.app.ui.theme.CopperPrimary,
                                letterSpacing = 0.sp
                            )
                            if (!data?.cycleInfo.isNull_or_empty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = data?.cycleInfo ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = com.reflex.app.ui.theme.DarkSecondaryText,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(ReflexTokens.SpaceSm))
                            Text(
                                text = data?.timeDisplay ?: "00:00",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontFeatureSettings = "tnum"
                                ),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = com.reflex.app.ui.theme.DarkInkText,
                                letterSpacing = 0.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceXl))

                    // Action 1: Back to Focus (White Pill Primary)
                    ReflexButton(
                        text = "Return to focus",
                        onClick = onBackToFocus,
                        modifier = Modifier.fillMaxWidth(),
                        variant = ReflexButtonVariant.PRIMARY
                    )

                    Spacer(modifier = Modifier.height(ReflexTokens.SpaceMd))

                    // Action 2: End Session Early
                    Text(
                        text = "End session early",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = com.reflex.app.ui.theme.DarkSecondaryText,
                        letterSpacing = 0.sp,
                        modifier = Modifier
                            .clickable { showExitConfirmDialog = true }
                            .padding(8.dp)
                    )
                }
            }
        }

        if (showExitConfirmDialog) {
            DeleteConfirmationDialog(
                taskTitle = "End Focus Session Early?",
                onConfirm = {
                    showExitConfirmDialog = false
                    onEndSessionEarly()
                },
                onDismiss = { showExitConfirmDialog = false }
            )
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
}
