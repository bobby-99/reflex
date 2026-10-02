package com.reflex.app.ui.theme

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Central design tokens for the Reflex Design System.
 * Clean, minimal, frosted glass aesthetic with rounder corners (>=16dp everywhere).
 */
object ReflexTokens {

    // --- 1. Spacing Scale ---
    val SpaceXxs = 2.dp
    val SpaceXs = 4.dp
    val SpaceSm = 8.dp
    val SpaceMd = 12.dp
    val SpaceLg = 16.dp
    val SpaceXl = 20.dp
    val SpaceXxl = 24.dp
    val SpaceXxxl = 32.dp

    // Extra spacing below the status bar (12dp)
    val StatusBarExtraGap = 12.dp

    // --- 2. Corner Radius Scale (Min 16dp anywhere) ---
    val RadiusCard = 32.dp          // Cards: 32dp
    val RadiusInnerTile = 26.dp     // Inner tiles: 26dp
    val RadiusInput = 24.dp         // Inputs and quick-add fields: 24dp
    val RadiusThumbnail = 22.dp     // Thumbnails: 22dp
    val RadiusIconTile = 18.dp      // Icon tiles: 18dp
    val RadiusMin = 16.dp           // Catch-all minimum radius: 16dp
    val RadiusDialog = 36.dp        // Dialogs: 36dp
    val RadiusSheet = 36.dp         // Bottom sheets: 36dp
    val RadiusNav = 36.dp           // Stadium nav bar (height 72dp / 2 = 36dp)
    val RadiusPill = 999.dp         // Full pill for buttons, chips, indicators

    // Legacy radius aliases mapped to >=16dp
    val RadiusXs = RadiusMin
    val RadiusSm = RadiusMin
    val RadiusMd = RadiusIconTile
    val RadiusLg = RadiusCard
    val RadiusXl = RadiusDialog
    val RadiusModal = RadiusSheet

    // Reusable Shapes
    val ShapeCard = RoundedCornerShape(RadiusCard)
    val ShapeInnerTile = RoundedCornerShape(RadiusInnerTile)
    val ShapeInput = RoundedCornerShape(RadiusInput)
    val ShapeIconTile = RoundedCornerShape(RadiusIconTile)
    val ShapeThumbnail = RoundedCornerShape(RadiusThumbnail)
    val ShapeDialog = RoundedCornerShape(RadiusDialog)
    val ShapeSheet = RoundedCornerShape(topStart = RadiusSheet, topEnd = RadiusSheet)
    val ShapeModal = ShapeSheet
    val ShapeFloatingNav = RoundedCornerShape(RadiusNav)
    val ShapeButton = RoundedCornerShape(RadiusPill)
    val ShapeChip = RoundedCornerShape(RadiusPill)
    val ShapeCircle = CircleShape

    // Backward compatibility aliases
    val CornerNone = ShapeCard
    val CornerMinimal = RoundedCornerShape(RadiusMin)

    // --- 3. Border & Hairline Rules ---
    val BorderHairline = 0.75.dp
    val BorderThin = 1.dp
    val BorderMedium = 1.dp
    val BorderThick = 1.5.dp
    val BorderHeavy = 1.5.dp
    val DividerWeight = 0.75.dp

    // --- 4. Elevation Rules ---
    val ElevationNone = 0.dp
    val ElevationLow = 2.dp
    val ElevationModal = 8.dp

    // --- 5. Component Sizes ---
    val ButtonHeight = 48.dp
    val ButtonHeightSm = 36.dp
    val ButtonMinWidth = 100.dp
    val TopBarHeight = 56.dp
    val CardMinHeight = 64.dp
    val ChipHeight = 32.dp
    val TextFieldHeight = 52.dp

    // Bottom Navigation & Center Action (72dp tall, 16dp sides, 24dp bottom)
    val BottomNavHeight = 72.dp
    val BottomNavFloatingMarginH = 16.dp
    val BottomNavFloatingMarginV = 24.dp
    val FabCenterSize = 52.dp
    val NavBlurRadius = 20.dp
    val navBlurRadius: androidx.compose.ui.unit.Dp
        @JvmName("getNavBlurRadiusDp") get() = NavBlurRadius

    // Week Strip & Day Circles
    val DayCircleSize = 38.dp

    // Calendar Tokens (Reflex Design System v1.0)
    val CalendarRowHeight = 56.dp
    val CalendarDateCircleSize = 40.dp
    val CalendarDotSize = 5.dp
    val CalendarDotGap = 3.dp
    val CalendarHandleWidth = 40.dp
    val CalendarHandleHeight = 5.dp
    val CalendarTimelineIndent = 52.dp
    val CalendarTimelineLineWidth = 1.5.dp
    val CalendarItemIconTileSize = 44.dp
    val CalendarCheckboxSize = 26.dp

    // Stat Tiles & Analytics
    val StatTileMinHeight = 76.dp
    val HeatmapTileSize = 14.dp
    val HeatmapTileSpacing = 3.dp
    val HeatmapTileRadius = 4.2.dp  // ~30% of 14dp cell size

    // --- 6. Icon Sizing Rules ---
    val IconXs = 16.dp
    val IconSm = 20.dp
    val IconMd = 24.dp
    val IconLg = 26.dp              // Tab bar icons: 26dp
    val IconXl = 32.dp

    val IconSizeSm = IconSm
    val IconSizeMd = IconMd
    val IconSizeLg = IconLg

    // --- 7. Motion & Animation Rules ---
    const val AnimDurationFast = 80
    const val AnimDurationNormal = 200 // Subtle 200ms transitions
    const val AnimDurationMedium = 240
    const val PressedScale = 0.97f

    // --- 8. Focus Tokens (Reflex Design System v1.0) ---
    val FocusHeroMarginTop = 14.dp
    val FocusHeroMarginBottom = 6.dp
    val FocusCycleStripHeight = 12.dp
    val FocusCycleStripGap = 4.dp
    val FocusCycleStripMarginTop = 14.dp
    val FocusCycleStripMarginBottom = 6.dp
    val FocusConfigRowMinHeight = 60.dp
    val FocusStepperButtonSize = 40.dp
    val FocusStepperValueWidth = 66.dp
    val FocusFlowChipHeight = 36.dp
    val FocusTaskRowHeight = 60.dp
    val FocusStartButtonHeight = 56.dp
    val FocusPhasePillHeight = 40.dp
    val FocusControlPillHeight = 56.dp
    val FocusSessionDotSize = 12.dp
    val FocusSessionDotGap = 10.dp
    val FocusSwitchWidth = 52.dp
    val FocusSwitchHeight = 30.dp
    val FocusSwitchThumbSize = 22.dp
    val FocusSheetDragHandleWidth = 40.dp
    val FocusSheetDragHandleHeight = 5.dp
    val FocusQuoteMinHeight = 36.dp
    val FocusQuoteMaxWidth = 300.dp

    // --- 9. Tasks Tokens (Reflex Design System v1.0 / reflex-tasks.html) ---
    val TasksProgressHeight = 8.dp
    val TasksFilterChipHeight = 40.dp
    val TasksFabSize = 60.dp
    val TasksFabHaloSize = 74.dp
    val TasksFabBottomPadding = 140.dp
    val TasksFabEndPadding = 28.dp
    val TasksFadeHeight = 182.dp
    val TasksBottomContentPadding = 232.dp
    val TasksCheckboxSize = 28.dp
    val TasksUndoBottomPadding = 212.dp
    val TasksUndoButtonHeight = 34.dp
}

/**
 * Universal status bar padding with the required 12dp extra clearance gap.
 */
fun Modifier.reflexStatusBarPadding(): Modifier = this
    .statusBarsPadding()
    .padding(top = ReflexTokens.StatusBarExtraGap)

