package com.reflex.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun SetStatusBarAppearance(isLightBackground: Boolean? = null) {
    val view = LocalView.current
    val bgLuminance = MaterialTheme.colorScheme.background.luminance()
    val isLight = isLightBackground ?: (bgLuminance > 0.5f)
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = isLight
            }
        }
    }
}
