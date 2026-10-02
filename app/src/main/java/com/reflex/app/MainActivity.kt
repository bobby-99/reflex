package com.reflex.app

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.reflex.app.ui.screens.ReflexMainScreen
import com.reflex.app.ui.theme.ReflexTheme
import com.reflex.app.util.NotificationHelper

import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    private val _routineIdState = mutableStateOf(-1L)

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        handleIntent(intent)

        setContent {
            val activeTheme by com.reflex.app.util.ThemePreferenceRepository.currentTheme.collectAsState()
            ReflexTheme(appTheme = activeTheme) {
                ReflexMainScreen(
                    initialRoutineId = _routineIdState.value,
                    onInitialRoutineHandled = {
                        _routineIdState.value = -1L
                        intent?.removeExtra(NotificationHelper.EXTRA_START_ROUTINE_ID)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconcile and clear any orphaned timer notifications if services are inactive
        if (com.reflex.app.service.RoutineTimerService.timerState.value == null) {
            com.reflex.app.service.RoutineTimerService.cancelNotification(this)
        }
        if (com.reflex.app.service.FocusTimerService.timerState.value == null) {
            com.reflex.app.service.FocusTimerService.cancelNotification(this)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val routineId = intent?.getLongExtra(NotificationHelper.EXTRA_START_ROUTINE_ID, -1L) ?: -1L
        if (routineId != -1L) {
            _routineIdState.value = routineId
        }
    }
}
