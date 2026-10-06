package com.reflex.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class AppTheme(val displayName: String) {
    SYSTEM("System"),
    DARK("Dark"),
    LIGHT("Light");

    companion object {
        fun fromString(name: String?): AppTheme {
            return when (name?.uppercase()) {
                "LIGHT", "PAPER_LIGHT" -> LIGHT
                "DARK", "OBSIDIAN_COPPER" -> DARK
                else -> SYSTEM
            }
        }
    }
}

// Backward compatibility alias for any legacy references
typealias ThemePalette = AppTheme

// Flagship Dark-First Obsidian Copper ColorScheme
private val DarkColorScheme = darkColorScheme(
    primary = CopperPrimary,
    onPrimary = ActionPillOnWhite,
    primaryContainer = CopperContainer,
    onPrimaryContainer = CopperOnContainer,
    secondary = ActionPillWhite,
    onSecondary = ActionPillOnWhite,
    secondaryContainer = DarkSurfaceInput,
    onSecondaryContainer = DarkInkText,
    background = DarkBackground,
    onBackground = DarkInkText,
    surface = DarkSurface,
    onSurface = DarkInkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkSecondaryText,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = DarkDestructiveRed,
    onError = DarkInkText,
    errorContainer = DestructiveContainer,
    onErrorContainer = DarkInkText,
    scrim = Color.Black.copy(alpha = 0.62f)
)

// First-Class Clean Warm Light ColorScheme
private val LightColorScheme = lightColorScheme(
    primary = LightCopperPrimary,
    onPrimary = Color.White,
    primaryContainer = LightCopperContainer,
    onPrimaryContainer = LightCopperOnContainer,
    secondary = LightInkText,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceInput,
    onSecondaryContainer = LightInkText,
    background = LightBackground,
    onBackground = LightInkText,
    surface = LightSurface,
    onSurface = LightInkText,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightSecondaryText,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle,
    error = LightDestructiveRed,
    onError = Color.White,
    errorContainer = LightDestructiveContainer,
    onErrorContainer = LightDestructiveRed,
    scrim = Color(0x6B1A1614)
)

@Composable
fun ReflexTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReflexTypography,
        content = content
    )
}

val androidx.compose.material3.ColorScheme.success: Color
    get() = if (background == DarkBackground) DarkSuccessGreen else LightSuccessGreen

val androidx.compose.material3.ColorScheme.elevatedSurface: Color
    get() = if (background == DarkBackground) DarkSurfaceElevated else LightSurface

val androidx.compose.material3.ColorScheme.tertiaryText: Color
    get() = if (background == DarkBackground) DarkTertiaryText else LightTertiaryText

val androidx.compose.material3.ColorScheme.actionPill: Color
    get() = if (background == DarkBackground) ActionPillWhite else LightInkText

val androidx.compose.material3.ColorScheme.onActionPill: Color
    get() = if (background == DarkBackground) ActionPillOnWhite else Color.White

val androidx.compose.material3.ColorScheme.outlineOutlined: Color
    get() = if (background == DarkBackground) DarkBorder else LightBorderOutlined

val androidx.compose.material3.ColorScheme.cardShadowColor: Color
    get() = if (background == DarkBackground) Color.Transparent else LightCardShadowColor

val androidx.compose.material3.ColorScheme.statusBadgeText: Color
    get() = if (background == DarkBackground) CopperPrimary else LightStatusBadgeText

val androidx.compose.material3.ColorScheme.statusBadgeContainer: Color
    get() = if (background == DarkBackground) CopperSubtle else LightStatusBadgeContainer

val androidx.compose.material3.ColorScheme.statusBadgeBorder: Color
    get() = if (background == DarkBackground) CopperPrimary.copy(alpha = 0.5f) else LightStatusBadgeBorder

val androidx.compose.material3.ColorScheme.destructiveRed: Color
    get() = if (background == DarkBackground) DarkDestructiveRed else LightDestructiveRed

val androidx.compose.material3.ColorScheme.navGlassTint: Color
    get() = if (background == DarkBackground) DarkNavGlassTint else LightNavGlassTint

val androidx.compose.material3.ColorScheme.navGlassFallback: Color
    get() = if (background == DarkBackground) DarkNavGlassFallback else LightNavGlassFallback

val androidx.compose.material3.ColorScheme.eventBlue: Color
    get() = if (background == DarkBackground) DarkEventBlue else LightEventBlue

val androidx.compose.material3.ColorScheme.routineGreen: Color
    get() = if (background == DarkBackground) DarkRoutineGreen else LightRoutineGreen

val androidx.compose.material3.ColorScheme.onCopper: Color
    get() = if (background == DarkBackground) DarkOnCopper else LightOnCopper

val androidx.compose.material3.ColorScheme.liquidTop: Color
    get() = if (background == DarkBackground) LiquidFocusTopDark else LiquidFocusTopLight

val androidx.compose.material3.ColorScheme.liquidBottom: Color
    get() = if (background == DarkBackground) LiquidFocusBottomDark else LiquidFocusBottomLight

val androidx.compose.material3.ColorScheme.breakLiquidTop: Color
    get() = if (background == DarkBackground) LiquidBreakTopDark else LiquidBreakTopLight

val androidx.compose.material3.ColorScheme.breakLiquidBottom: Color
    get() = if (background == DarkBackground) LiquidBreakBottomDark else LiquidBreakBottomLight

val androidx.compose.material3.ColorScheme.onLiquid: Color
    get() = if (background == DarkBackground) LiquidOnLiquidDark else LiquidOnLiquidLight

val androidx.compose.material3.ColorScheme.goldColor: Color
    get() = if (background == DarkBackground) Color(0xFFE3C27A) else Color(0xFFA9790F)

val androidx.compose.material3.ColorScheme.glare: Color
    get() = if (background == DarkBackground) LiquidGlareDark else LiquidGlareLight

val androidx.compose.material3.ColorScheme.sage: Color
    get() = if (background == DarkBackground) SageDark else SageLight

val androidx.compose.material3.ColorScheme.border: Color
    get() = outline

val androidx.compose.material3.ColorScheme.ink: Color
    get() = onSurface

object ReflexTheme {
    val colors: androidx.compose.material3.ColorScheme
        @Composable
        get() = MaterialTheme.colorScheme

    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography
}





