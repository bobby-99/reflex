package com.reflex.app.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// REFLEX PREMIUM DARK-FIRST DESIGN PALETTE
// =========================================================================

// --- Obsidian Baseline & Elevated Surfaces ---
val DarkBackground = Color(0xFF0A0908)       // True dark-first AMOLED baseline (~#0A0908)
val DarkSurface = Color(0xFF141211)          // Level 1: Cards & primary containers
val DarkSurfaceElevated = Color(0xFF1C1A17)  // Level 2: Modals, bottom sheets, floating nav
val DarkSurfaceInput = Color(0xFF24211E)     // Level 3: Input boxes, steppers, chips
val DarkSurfaceVariant = Color(0xFF1E1C19)   // Level 2.5: Secondary containers & stat tiles

// --- Warm Copper / Peach Accent System ---
val CopperPrimary = Color(0xFFD9A184)        // Signature warm copper/peach accent (~#D9A184)
val CopperDark = Color(0xFFB57E63)           // Deeper copper for dark contrast & pressed states
val CopperContainer = Color(0xFF2A1C16)      // Subtle tinted copper container background
val CopperOnContainer = Color(0xFFF7DDD0)    // High-contrast soft peach on container
val CopperSubtle = Color(0x26D9A184)         // 15% copper wash for inactive/selected backgrounds
val DarkOnCopper = Color(0xFF0A0908)         // Contrast text on dark copper
val LightOnCopper = Color(0xFFFFFFFF)        // Contrast text on light copper
val OnCopper = DarkOnCopper

// --- High-Contrast Typography & Content ---
val DarkInkText = Color(0xFFF5F2EF)          // Text Primary: Crisp warm off-white
val DarkSecondaryText = Color(0xFFA39E98)    // Text Secondary: Muted warm neutral
val DarkTertiaryText = Color(0xFF6E6963)     // Text Tertiary: Subtle caption & guide lines

// --- Hairline Borders & Dividers ---
val DarkBorder = Color(0xFF2E2A27)           // Standard hairline border (1.dp)
val DarkBorderSubtle = Color(0xFF1F1D1A)     // Muted hairline divider (0.75.dp)
val DarkBorderFocused = Color(0xFFD9A184)    // Active/focused hairline border (1.dp)

// --- Pill Actions ---
val ActionPillWhite = Color(0xFFFFFFFF)      // GymMane-style white primary pill action
val ActionPillOnWhite = Color(0xFF0A0908)    // Dark text on white primary pill

// --- Functional Semantics ---
val DarkDestructiveRed = Color(0xFFE05D5D)   // Refined warm destructive red
val DestructiveContainer = Color(0xFF2C1414) // Subtle red container background
val DarkSuccessGreen = Color(0xFF4ADE80)     // Completed task / session checkmark
val DarkWarningAmber = Color(0xFFFBBF24)     // Warning / alert tint

// --- 5-Tier Copper Activity Heatmap Ramp ---
val CopperHeatmap0 = Color(0xFF181614)       // Tier 0: Inactive / rest
val CopperHeatmap1 = Color(0x40D9A184)       // Tier 1: Light activity (25% copper)
val CopperHeatmap2 = Color(0x75D9A184)       // Tier 2: Moderate activity (46% copper)
val CopperHeatmap3 = Color(0xB5D9A184)       // Tier 3: High activity (71% copper)
val CopperHeatmap4 = Color(0xFFD9A184)       // Tier 4: Peak activity (100% solid copper)

// --- Light Theme Tokens (First-Class Clean Warm Light) ---
val LightBackground = Color(0xFFF7F3EE)      // Canvas background: #F7F3EE
val LightSurface = Color(0xFFFFFFFF)         // Surface cards: #FFFFFF
val LightBorder = Color(0x211A1614)          // 13% black hairline border (raised from 8%)
val LightBorderSubtle = Color(0x1A1A1614)    // 10% black divider hairline (raised from 5%)
val LightBorderOutlined = Color(0x471A1614)  // 28% black border for outlined buttons
val LightBorderFocused = Color(0xFF8F4C2B)   // Active/focused hairline border
val LightCardShadowColor = Color(0x0F000000) // 6% black soft card shadow (1dp offset, 3dp blur)
val LightInkText = Color(0xFF1A1614)         // Primary text: #1A1614
val LightSecondaryText = Color(0xFF5D5750)   // Deepened to #5D5750 (WCAG AA 6.4:1 contrast on #F7F3EE canvas)
val LightTertiaryText = Color(0xFF9E958E)    // Subtle captions in light mode
val LightCopperPrimary = Color(0xFF8F4C2B)   // Darkened to #8F4C2B (WCAG AA 7.3:1 contrast on #FFFFFF surface)
val LightCopperContainer = Color(0xFFF7EBE3) // Subtle copper tint container
val LightCopperOnContainer = Color(0xFF4A2514)
val LightSurfaceInput = Color(0xFFEFEBE4)    // Soft input container
val LightSurfaceVariant = Color(0xFFF2EEE7)  // Card variant container

// Status Badge Tokens (WCAG AA >= 4.5:1 compliant on white surface cards)
val LightStatusBadgeText = Color(0xFF8F4C2B)        // #8F4C2B: 7.3:1 contrast against white
val LightStatusBadgeContainer = Color(0x1F8F4C2B)   // ~12% copper wash container
val LightStatusBadgeBorder = Color(0x4D8F4C2B)      // ~30% copper border

val LightDestructiveRed = Color(0xFFC0504D)
val LightDestructiveContainer = Color(0xFFFEE2E2)
val LightSuccessGreen = Color(0xFF16A34A)
val LightWarningAmber = Color(0xFFD97706)

// Light Heatmap Ramp
val LightCopperHeatmap0 = Color(0xFFEBE6DF)
val LightCopperHeatmap1 = Color(0x40B5714F)
val LightCopperHeatmap2 = Color(0x75B5714F)
val LightCopperHeatmap3 = Color(0xB5B5714F)
val LightCopperHeatmap4 = Color(0xFFB5714F)

// --- Floating Frosted Glass Tab Bar Tokens ---
// API 31+ Glass Tint (72% opacity: dark rgba(28, 26, 23, 0.72) = #B81C1A17, light rgba(255, 255, 255, 0.72) = #B8FFFFFF)
val DarkNavGlassTint = Color(0xB81C1A17)
val LightNavGlassTint = Color(0xB8FFFFFF)

// Fallback below API 31 / no blur (94% opacity: dark #F01C1A17, light #F0FFFFFF)
val DarkNavGlassFallback = Color(0xF01C1A17)
val LightNavGlassFallback = Color(0xF0FFFFFF)

// --- Event and Routine Semantic Colors (WCAG AA 4.5:1 compliant) ---
val DarkEventBlue = Color(0xFF6F9BFF)
val LightEventBlue = Color(0xFF3565D6) // 5.15:1 contrast on #FFFFFF, 4.82:1 on #F7F3EE (darkened from #3B6FE0)

val DarkRoutineGreen = Color(0xFF86C9A4)
val LightRoutineGreen = Color(0xFF327A54) // 5.18:1 contrast on #FFFFFF, 4.86:1 on #F7F3EE (darkened from #3F8A63 which fails at 4.13:1)

// --- LiquidTimer & Focus System Tokens (Dark / Light) ---
val LiquidFocusTopDark = Color(0xFFE1AD93)
val LiquidFocusBottomDark = Color(0xFFB57E63)
val LiquidFocusTopLight = Color(0xFFC98460)
val LiquidFocusBottomLight = Color(0xFFA5623F)

val LiquidBreakTopDark = Color(0xFFA6D8BF)
val LiquidBreakBottomDark = Color(0xFF5E9C7E)
val LiquidBreakTopLight = Color(0xFF7FB39A)
val LiquidBreakBottomLight = Color(0xFF4F8A6B)

val LiquidOnLiquidDark = Color(0xFF0A0908)
val LiquidOnLiquidLight = Color(0xFFFFFFFF)

val LiquidGlareDark = Color(0x38FFFFFF)  // 22% white
val LiquidGlareLight = Color(0x8CFFFFFF) // 55% white

val SageDark = Color(0xFF86C9A4)
val SageLight = Color(0xFF3F8A63)

// Aliases
val EventBlue = DarkEventBlue
val RoutineGreen = DarkRoutineGreen

// =========================================================================
// BACKWARD COMPATIBILITY ALIASES
// =========================================================================
val LemonYellow = CopperPrimary
val DarkLemonYellow = CopperPrimary
val PaperWhite = DarkSurface
val PaperLightSecondarySurface = DarkSurfaceElevated
val PaperLightSecondaryText = DarkSecondaryText
val DeepInk = DarkInkText
val Ink = DarkInkText
val PureWhite = ActionPillWhite
val DestructiveRed = DarkDestructiveRed
val PaperLightBg = LightBackground
val PaperLightSurface = LightSurface
val PaperLightPrimary = LightCopperPrimary
val PaperLightText = LightInkText
val PaperLightBorder = LightBorder
val LavenderBackground = DarkBackground
val LavenderSurface = DarkSurface
val LavenderPrimary = CopperPrimary
val LavenderInkText = DarkInkText
val LavenderBorder = DarkBorder
val LavenderSecondaryText = DarkSecondaryText
val OrangeBackground = DarkBackground
val OrangeSurface = DarkSurface
val OrangePrimary = CopperPrimary
val OrangeInkText = DarkInkText
val OrangeBorder = DarkBorder
val OrangeSecondaryText = DarkSecondaryText
