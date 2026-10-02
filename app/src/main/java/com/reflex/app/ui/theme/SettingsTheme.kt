package com.reflex.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Dedicated color tokens for Reflex Settings (pixel-faithful to reflex-settings.html).
 */
data class SettingsColorTokens(
    val bg: Color,
    val surface: Color,
    val input: Color,
    val variant: Color,
    val ink: Color,
    val secondary: Color,
    val tertiary: Color,
    val border: Color,
    val copper: Color,
    val onCopper: Color,
    val chip: Color,
    val pill: Color,
    val pillFg: Color,
    val red: Color,
    val redBg: Color,
    val ok: Color,
    val okBg: Color
)

val DarkSettingsColors = SettingsColorTokens(
    bg = Color(0xFF0A0908),
    surface = Color(0xFF141211),
    input = Color(0xFF24211E),
    variant = Color(0xFF1E1C19),
    ink = Color(0xFFF5F2EF),
    secondary = Color(0xFFA39E98),
    tertiary = Color(0xFF6E6963),
    border = Color(0xFF2E2A27),
    copper = Color(0xFFD9A184),
    onCopper = Color(0xFF0A0908),
    chip = Color(0xFF2A1C16),
    pill = Color(0xFFFFFFFF),
    pillFg = Color(0xFF0A0908),
    red = Color(0xFFE05D5D),
    redBg = Color(0xFFE05D5D).copy(alpha = 0.16f),
    ok = Color(0xFF86C9A4),
    okBg = Color(0xFF86C9A4).copy(alpha = 0.16f)
)

val LightSettingsColors = SettingsColorTokens(
    bg = Color(0xFFF7F4F0),
    surface = Color(0xFFFFFFFF),
    input = Color(0xFFEFEAE4),
    variant = Color(0xFFF3EEE8),
    ink = Color(0xFF1A1614),
    secondary = Color(0xFF6B625B),
    tertiary = Color(0xFF948B83),
    border = Color(0x141A1614), // rgba(26,22,20,.08)
    copper = Color(0xFFB5714F),
    onCopper = Color(0xFFFFFFFF),
    chip = Color(0x1FB5714F),  // rgba(181,113,79,.12)
    pill = Color(0xFF1A1614),
    pillFg = Color(0xFFFFFFFF),
    red = Color(0xFFC0504D),
    redBg = Color(0xFFC0504D).copy(alpha = 0.16f),
    ok = Color(0xFF3F8A63),
    okBg = Color(0xFF3F8A63).copy(alpha = 0.16f)
)

val LocalSettingsColors = staticCompositionLocalOf { DarkSettingsColors }

object SettingsTheme {
    val colors: SettingsColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalSettingsColors.current
}
