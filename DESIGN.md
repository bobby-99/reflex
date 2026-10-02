# Reflex Design System Specification (v1.0)

This document is the absolute single source of truth for the **Reflex Design System** and the complete UI/UX architecture of Reflex (Kotlin, Jetpack Compose). It reflects the exact current working implementation across all five core subsystems: **Routines**, **Calendar**, **Tasks**, **Habits**, and **Focus**.

---

## Executive Design Philosophy

Reflex is an offline, private, calm productivity ecosystem built around intentionality, tactile elegance, and serenity. Its visual language eliminates sensory overload, noisy gamification, and harsh contrasts in favor of warmth, clarity, and precision.

### Core Aesthetic Pillars:
1. **Dark-First Obsidian & Clean Warm Light**:
   - **Dark Mode (Flagship Canvas)**: Deep obsidian canvas (`#0A0908` background, `#141211` level 1 surface, `#1C1A17` level 2 elevated, `#24211E` level 3 input) for battery preservation, true OLED blacks, and deep calm focus.
   - **Light Mode (First-Class Warm Paper)**: Warm paper-like off-white canvas (`#F7F3EE`), pure white surface cards (`#FFFFFF`) with crisp 13% black hairline borders (`#211A1614`), deep warm ink typography (`#1A1614`), and deepened secondary text (`#5D5750`) ensuring high WCAG AA contrast (6.4:1).
   - **System Mode (Default)**: Automatically tracks Android system night/day configuration.
2. **Signature Warm Copper Accents**:
   - Rich metallic copper (`#D9A184` dark / `#8F4C2B` light) provides a tactile, focused visual anchor across active tabs, progress rings, heatmap cells, steppers, and hero elements.
3. **Floating Frosted Glass Tab Bar**:
   - 72dp tall stadium pill (36dp radius) floating **24dp** above the system gesture bar (`navigationBarsPadding()`) with 16dp horizontal margins.
   - Built with API 31+ hardware backdrop blur via the **Haze** library (`dev.chrisbanes.haze`), sampling content scrolling beneath the bar with 24dp blur radius, 72% tint, and zero noise. Graceful 94% near-opaque tint fallback for pre-API 31 devices.
4. **Universal Sentence Case**:
   - Strictly zero all-caps strings, zero `.uppercase()` calls, and zero allCaps flags across all screens, buttons, chips, dialogs, bottom sheets, and system notifications. Only true acronyms (AM, PM, NLP) retain uppercase characters.
5. **Unified Lora Typography**:
   - Handcrafted serif typography using locally bundled **Lora** (Regular, Medium, SemiBold, Bold) across all headers, body, numbers, and readouts.
   - Minimum font size floor of **13sp** across the entire application (no unreadable sub-12sp captions).
   - OpenType tabular figures (`fontFeatureSettings = "tnum"`) on all numbers, timers, and timestamps to eliminate countdown jitter.
   - Primary action buttons styled at 16–17sp Medium.
6. **Softer Curvature (>= 16dp Everywhere)**:
   - Absolute minimum radius of 16dp across the entire UI. Cards 32dp, inner tiles 26dp, input fields 24dp, thumbnails 22dp, icon tiles 18dp, bottom sheets and dialogs 36dp, buttons and chips full stadium pills.
7. **Offline, Zero-Network Privacy**:
   - 100% local operation. All fonts, icons, data storage (Room v12), and NLP parsing reside strictly on the user's device with zero external telemetry, zero Google Fonts network requests, and zero remote dependencies.

---

## 1. Color System & Semantic Tokens

All UI components consume `MaterialTheme.colorScheme` tokens defined in `com.reflex.app.ui.theme`. Direct hex literals inside screen files are strictly prohibited.

### 1.1 Palette Definitions (`Color.kt`)

```kotlin
// Dark Palette (Obsidian Copper)
val DarkBackground         = Color(0xFF0A0908) // Base canvas
val DarkSurface            = Color(0xFF141211) // Level 1 cards & list rows
val DarkSurfaceElevated    = Color(0xFF1C1A17) // Level 2 bottom sheets & floating cards
val DarkSurfaceInput       = Color(0xFF24211E) // Level 3 input boxes, steppers, chips
val DarkSurfaceVariant     = Color(0xFF1E1C19) // Secondary containers & stat tiles
val CopperPrimary          = Color(0xFFD9A184) // Warm metallic copper accent
val CopperDark             = Color(0xFFB57E63) // Pressed states & dark contrast
val CopperContainer        = Color(0xFF2A1C16) // Subtle copper wash container
val CopperOnContainer      = Color(0xFFF7DDD0) // High-contrast soft peach on container
val CopperSubtle           = Color(0x26D9A184) // 15% copper wash
val DarkInkText            = Color(0xFFF5F2EF) // Primary text: crisp warm off-white
val DarkSecondaryText      = Color(0xFFA39E98) // Secondary text: muted warm neutral
val DarkTertiaryText       = Color(0xFF6E6963) // Tertiary text: captions, inactive indicators
val DarkBorder             = Color(0xFF2E2A27) // Standard hairline border (1dp)
val DarkBorderSubtle       = Color(0xFF1F1D1A) // Muted hairline divider (0.75dp)
val ActionPillWhite        = Color(0xFFFFFFFF) // High-contrast primary pill action
val ActionPillOnWhite      = Color(0xFF0A0908) // Contrast text on white pill
val DarkDestructiveRed     = Color(0xFFE05D5D) // Warm crimson for overdue & delete
val DestructiveContainer   = Color(0xFF2C1414) // Subtle red container background
val DarkSuccessGreen       = Color(0xFF4ADE80) // Emerald green for checkmarks & streaks
val DarkEventBlue          = Color(0xFF6F9BFF) // Calendar device event indicator
val DarkRoutineGreen       = Color(0xFF86C9A4) // Calendar routine indicator

// Light Palette (Warm Paper & Rich Copper)
val LightBackground        = Color(0xFFF7F3EE) // Warm off-white canvas (#F7F3EE)
val LightSurface           = Color(0xFFFFFFFF) // Crisp pure white cards (#FFFFFF)
val LightSurfaceElevated   = Color(0xFFFFFFFF) // Floating sheets & modals
val LightSurfaceInput      = Color(0xFFEFEAE4) // Pill chips & input fields (#EFEAE4)
val LightSurfaceVariant    = Color(0xFFF3EEE8) // Secondary tiles
val LightInkText           = Color(0xFF1A1614) // Deep warm ink primary text (#1A1614)
val LightSecondaryText     = Color(0xFF5D5750) // High-contrast muted neutral (6.4:1 contrast)
val LightTertiaryText      = Color(0xFF948B83) // Captions & inactive days
val LightBorder            = Color(0x211A1614) // 13% black hairline border
val LightBorderSubtle      = Color(0x1A1A1614) // 10% black divider hairline
val LightCopperPrimary     = Color(0xFF8F4C2B) // WCAG AA rich copper (7.3:1 on white)
val LightCopperContainer   = Color(0x1EB5714F) // 12% tinted copper chip
val LightDestructiveRed    = Color(0xFFC0504D) // Warm crimson red
val LightEventBlue         = Color(0xFF3565D6) // Accessible calendar blue
val LightRoutineGreen      = Color(0xFF327A54) // Accessible calendar green
```

### 1.2 Semantic Color Mapping (`Theme.kt`)

| Semantic Role | Dark Mode Value | Light Mode Value | Functional Purpose |
| :--- | :--- | :--- | :--- |
| `colorScheme.background` | `DarkBackground` (`#0A0908`) | `LightBackground` (`#F7F3EE`) | Canvas screen background |
| `colorScheme.surface` | `DarkSurface` (`#141211`) | `LightSurface` (`#FFFFFF`) | Cards, task rows, habit tiles |
| `colorScheme.elevatedSurface` | `DarkSurfaceElevated` (`#1C1A17`) | `LightSurface` (`#FFFFFF`) | Bottom sheets, `QuickAddCard`, floating overlays |
| `colorScheme.secondaryContainer` | `DarkSurfaceInput` (`#24211E`) | `LightSurfaceInput` (`#EFEAE4`) | Unselected filter chips, steppers, icon button backgrounds |
| `colorScheme.primary` | `CopperPrimary` (`#D9A184`) | `LightCopperPrimary` (`#8F4C2B`) | Copper accent, active chips, rings, hero elements |
| `colorScheme.onPrimary` | `DarkOnCopper` (`#0A0908`) | `Color.White` (`#FFFFFF`) | Text on solid copper chips & buttons |
| `colorScheme.primaryContainer` | `CopperContainer` (`#2A1C16`) | `LightCopperContainer` | Active badges, medium priority pills |
| `colorScheme.onSurface` | `DarkInkText` (`#F5F2EF`) | `LightInkText` (`#1A1614`) | High-emphasis titles & body copy |
| `colorScheme.onSurfaceVariant` | `DarkSecondaryText` (`#A39E98`) | `LightSecondaryText` (`#5D5750`) | Subtitles, date lines, inactive tab icons |
| `colorScheme.tertiaryText` | `DarkTertiaryText` (`#6E6963`) | `LightTertiaryText` (`#948B83`) | Strikethrough task titles, captions, low priority borders |
| `colorScheme.outline` | `DarkBorder` (`#2E2A27`) | `LightBorder` (`13% black`) | 1dp hairline container borders |
| `colorScheme.outlineVariant` | `DarkBorderSubtle` (`#1F1D1A`) | `LightBorderSubtle` (`10% black`)| Internal dividers & subtle separators |
| `colorScheme.actionPill` | `ActionPillWhite` (`#FFFFFF`) | `LightInkText` (`#1A1614`) | Pinned start buttons & primary action buttons |
| `colorScheme.onActionPill` | `ActionPillOnWhite` (`#0A0908`) | `Color.White` (`#FFFFFF`) | Text and icons inside action pills |
| `colorScheme.error` | `DarkDestructiveRed` (`#E05D5D`) | `LightDestructiveRed` (`#C0504D`)| Overdue titles, high priority badges, delete actions |
| `colorScheme.scrim` | `Color.Black.copy(0.62f)` | `Color(0x6B1A1614)` (42% dark ink) | Modal backdrops & bottom sheet scrims |

---

## 2. Typography Specification (Lora System)

Typography is 100% locally bundled Lora (`app/src/main/res/font/lora_*.ttf`). Network font loaders are forbidden.

### 2.1 Tabular Figures (`tnum`)
All countdown clocks, stopwatch timers, session counters, percentages, and numeric stat values strictly specify:
```kotlin
fontFeatureSettings = "tnum"
```
This forces fixed-width digits so numbers increment and decrement without horizontal layout shuddering.

### 2.2 Scale & Roles

| Typographic Role | Font Size | Font Weight | Line Height | Features | Application |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `displayLarge` | 79sp | Bold | 83sp | `tnum` | Full-screen focus session timers |
| `displayMedium` | 53sp | Bold | 57sp | `tnum` | Running routine step timers |
| `displaySmall` | 39sp | Bold | 43sp | `tnum` | LiquidTimer hero time readout |
| `headlineLarge` | 28sp | Bold | 32sp | Normal | Screen hero headers ("Tasks", "Routines") |
| `headlineMedium` | 22sp | Bold | 26sp | Normal | Pinned top bar titles ("Focus", "Calendar") |
| `headlineSmall` | 18sp | SemiBold | 22sp | Normal | Section headers, sheet titles |
| `titleLarge` | 17sp | SemiBold | 21sp | Normal | QuickAddCard header, dialog headers |
| `titleMedium` | 16sp | SemiBold | 21sp | Normal | Task row titles, habit names, routine titles |
| `titleSmall` | 15sp | Medium | 20sp | Normal | Stepper labels, progress card text |
| `bodyLarge` | 15sp | Normal | 21sp | Normal | Description text, natural language inputs |
| `bodyMedium` | 14sp | Medium | 18sp | Normal | Filter chips, segmented toggles |
| `bodySmall` | 13sp | Normal | 17sp | `tnum` (opt) | Meta text, dates, times, subtitles (Floor: 13sp) |
| `labelLarge` | 17sp | Medium | 21sp | Normal | Action pills ("Start pomodoro session") |
| `labelMedium` | 14sp | SemiBold | 18sp | Normal | Tune buttons, secondary action chips |
| `labelSmall` | 13sp | Medium | 17sp | Normal | Bottom nav labels, priority badges |

---

## 3. Shapes, Radii & Layout Tokens (`Tokens.kt`)

No radius in Reflex is less than 16dp.

```kotlin
object ReflexTokens {
    // --- Corner Radii (>= 16dp everywhere) ---
    val RadiusCard                 = 32.dp   // Outer cards, QuickAddCard, sheets
    val RadiusInnerTile            = 26.dp   // Task rows, progress cards, habit rows
    val RadiusInput                = 24.dp   // Text inputs, NLP bar
    val RadiusThumbnail            = 22.dp   // Media thumbnails
    val RadiusIconTile             = 18.dp   // Icon backdrops, avatar tiles
    val RadiusMin                  = 16.dp   // Absolute floor radius
    val RadiusDialog               = 36.dp   // Alert & confirmation dialogs
    val RadiusSheet                = 36.dp   // Bottom sheets (top-start / top-end)
    val RadiusNav                  = 36.dp   // Stadium floating tab bar (72dp / 2)
    val RadiusPill                 = 999.dp  // Buttons, chips, indicators, FABs

    // --- Borders & Hairlines ---
    val BorderThin                 = 1.dp    // Standard card & row border
    val BorderThick                = 1.5.dp  // Active inputs & selected items
    val DividerWeight              = 0.75.dp // Hairline card dividers

    // --- Floating Navigation Bar Tokens ---
    val BottomNavHeight            = 72.dp   // Stadium pill height
    val BottomNavFloatingMarginH   = 16.dp   // Horizontal margin to screen edges
    val BottomNavFloatingMarginV   = 24.dp   // Raised vertical margin above gesture bar
    val FabCenterSize              = 52.dp   // Circular copper action button
    val NavBlurRadius              = 24.dp   // Backdrop blur radius

    // --- Focus Screen Tokens ---
    val FocusHeroMarginTop         = 14.dp
    val FocusHeroMarginBottom      = 6.dp
    val FocusCycleStripHeight      = 12.dp
    val FocusCycleStripGap         = 4.dp
    val FocusConfigRowMinHeight    = 60.dp   // Stepper rows height with breathing room
    val FocusStepperButtonSize     = 40.dp
    val FocusStepperValueWidth     = 66.dp
    val FocusStartButtonHeight     = 56.dp

    // --- Tasks Screen Tokens ---
    val TasksProgressHeight        = 8.dp    // Full-pill progress bar height
    val TasksFilterChipHeight      = 40.dp   // Sticky filter chip height
    val TasksFabSize               = 60.dp   // Circular "+" action button
    val TasksFabHaloSize           = 74.dp   // 7dp subtle copper halo ring
    val TasksFabBottomPadding      = 140.dp  // Floats 44dp above raised nav bar
    val TasksFabEndPadding         = 28.dp   // Distance from right edge
    val TasksFadeHeight            = 182.dp  // Non-interactive bottom gradient fade
    val TasksBottomContentPadding  = 232.dp  // Bottom clearance for last task item
    val TasksCheckboxSize          = 28.dp   // Circular priority checkbox
    val TasksUndoBottomPadding     = 212.dp  // Undo pill bottom clearance
    val TasksUndoButtonHeight      = 34.dp

    // --- Calendar Tokens ---
    val CalendarRowHeight          = 56.dp
    val CalendarDateCircleSize     = 40.dp
    val CalendarDotSize            = 5.dp
    val CalendarDotGap             = 3.dp
    val CalendarTimelineIndent     = 52.dp
    val CalendarTimelineLineWidth  = 1.5.dp
}
```

---

## 4. Floating Frosted Glass Tab Bar (`ReflexBottomNavBar`)

The signature navigation bar is a detached, floating frosted glass stadium pill anchored to the bottom of the viewport:

```
+-------------------------------------------------------------------+
|  [Routines]     [Calendar]      (( = ))      [Habits]     [Focus] |
+-------------------------------------------------------------------+
|<---------------------- max 398dp / screen - 32dp ---------------->|
```

### 4.1 Geometry & Placement
- **Dimensions**: Height `72.dp`, stadium radius `36.dp`, width `min(screenWidth - 32.dp, 398.dp)`.
- **Anchor**: Centered horizontally (`Alignment.BottomCenter`).
- **Vertical Float**: Raised **`24.dp`** above Android's navigation gesture bar (`.navigationBarsPadding().padding(bottom = 24.dp)`). This provides generous breathing room from gesture indicators and prevents cramped taps on edge-to-edge devices.
- **Z-Order**: Rendered inside `ReflexMainScreen` above `NavHost` content. Screen content scrolls freely underneath.

### 4.2 Hardware Backdrop Blur (Haze)
- **Engine**: Built using `dev.chrisbanes.haze` (version 1.5.3).
- **Source**: `Modifier.hazeSource(state = hazeState)` attached directly to `NavHost`.
- **Effect**: Attached to the tab bar container plate:
  ```kotlin
  Modifier.hazeEffect(
      state = hazeState,
      style = HazeStyle(
          backgroundColor = MaterialTheme.colorScheme.background,
          tint = HazeTint(glassTint),
          blurRadius = 24.dp,
          noiseFactor = 0f
      )
  )
  ```
- **Tint**:
  - Dark: `rgba(28,26,23, 0.72)` (`0xB81C1A17`)
  - Light: `rgba(255,255,255, 0.72)` (`0xB8FFFFFF`)
- **Pre-API 31 Fallback**: Solid 94% tint (`0xF01C1A17` dark / `0xF0FFFFFF` light) ensuring content never shows through sharply when hardware blur is unavailable.
- **Border Rim**: 1dp border (`Color.White.copy(alpha = 0.12f)` dark / `Color.Black.copy(alpha = 0.08f)` light).
- **Shadow**: 8dp blur, 2dp Y offset, 12% ambient and spot black shadow (`ambientColor = Color.Black.copy(0.12f)`).

### 4.3 Five Tab Slots
1. **Routines** (`Icons.Default.Schedule` - clock, 22dp): Primary home screen.
2. **Calendar** (`Icons.Default.CalendarMonth`, 22dp): Morphing agenda & timeline.
3. **Tasks Action Button**: 52dp circular copper action button.
   - Shows checklist icon (`Icons.Default.Checklist`).
   - Tapping from any other tab immediately switches to Tasks.
   - Tapping while on Tasks smoothly animates the task list to the very top.
4. **Habits** (`Icons.Default.LocalFireDepartment`, 22dp): Offline habit tracker.
5. **Focus** (`Icons.Default.Adjust`, 22dp): LiquidTimer pomodoro & flow engine.

Tab slots are 64dp wide with 22dp icon, 2dp gap, and 13sp Medium label. Inactive tabs use `onSurfaceVariant`; active tab uses `CopperPrimary` with `SemiBold` weight.

---

## 5. Tasks System (v3.0 / `reflex-tasks.html`)

Rebuilt completely in Jetpack Compose to replicate `reflex-tasks.html` with pixel fidelity:

```
+-------------------------------------------------------------+
| Tasks                                                  [ * ]|
| Wednesday, September 30                                     |
| +---------------------------------------------------------+ |
| | 3 of 5 done today                                   60% | |
| | [====================----------]                        | |
| +---------------------------------------------------------+ |
| [ All 2 ]  [ Today 1 ]  [ Upcoming 1 ]  [ No date ]  [ Done ] |
|                                                             |
| Upcoming                                        1 scheduled |
| +---------------------------------------------------------+ |
| | ( ) Wish Susu Birthday                           [High] | |
| |     Mon, Oct 5 · 11:30 pm                               | |
| +---------------------------------------------------------+ |
|                                                       ( + ) |
|                                                             |
| [=================== Frosted Tab Bar =====================] |
+-------------------------------------------------------------+
```

### 5.1 Architecture & Removals
- **Removed**: The 4 legacy stat boxes (Pending, Today, Upcoming, Done) at the top are permanently gone.
- **Removed**: Automatic quick-add card popup from the tab bar is removed. The center button does not morph; it always shows the checklist icon.
- **Fixed Round "+" Button**: Added a dedicated 60dp floating action button (Section 5.6).

### 5.2 Top Bar & Progress Card
- **Header**: "Tasks" in 28sp Bold (`headlineLarge`) paired with a 44dp circular settings gear button (`secondaryContainer` fill, 22dp icon).
- **Date Line**: Formatted localized date (e.g., "Wednesday, September 30") in 13sp `onSurfaceVariant`, 2dp below title, 14dp above card.
- **Progress Card**: Surface container with 26dp corner radius, 1dp border, 14x16dp padding:
  - Header: "{done} of {total} done today" in 15sp SemiBold, right-aligned "{pct}%" in 13sp secondary.
  - Track: 8dp full-pill height, `secondaryContainer` track, `CopperPrimary` fill animates with a 400ms decelerate curve (`CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)`).
  - Empty State: When no tasks are scheduled for today, displays "Nothing due today" on left and "Enjoy the space" on right with an empty track.

### 5.3 Sticky Filter Chips Row
- **Placement**: Anchored using Compose `stickyHeader` with screen background fill, 12dp top and 10dp bottom padding, horizontally scrollable with 8dp gaps.
- **Chips**: `All`, `Today`, `Upcoming`, `No date`, `Completed`.
- **Dimensions**: 40dp height, full stadium pill.
- **State**: Inactive is `secondaryContainer` fill with 14sp Medium `onSurfaceVariant` text; active is `CopperPrimary` fill with `OnCopper` 14sp SemiBold text.
- **Count**: Dynamic count displayed at 13sp (80% opacity) next to chip label only when `count > 0`.

### 5.4 Categorized Sections & Ordering
Tasks are partitioned into sections with a 18dp top margin. Section header has title in 18sp SemiBold on left and subtitle in 13sp secondary on right (baseline aligned, 10dp above rows). Rows are spaced 8dp apart:
- **Overdue**: Title colored in `DarkDestructiveRed` (`LightDestructiveRed`), subtitle "needs attention".
- **Today**: Subtitle formatted as "Wed, Sep 30".
- **Upcoming**: Subtitle "{n} scheduled".
- **No date**: Subtitle shows count.
- **Completed**: In `All` filter, renders a 48dp toggle row with "Completed", count, and rotating chevron ($0^\circ \to 180^\circ$). When expanded, shows completed items.
- **Sorting**: Due date ascending (no date last), then time ascending (no time last).

### 5.5 Task Row & Swipe Gestures (`TaskRow`)
- **Card**: Surface background, 26dp radius, 1dp border, 16dp horizontal and 14dp vertical padding.
- **Checkbox**: 28dp circle on the left with 2dp border:
  - High priority: Red border (`DarkDestructiveRed`)
  - Medium priority: Copper border (`CopperPrimary`)
  - Normal/Low priority: Tertiary border (`tertiaryText`)
  - Completed state: Fills solid copper with a 15dp white checkmark drawn with a 3dp stroke.
  - Interactive delay: 380ms delay after toggle before row leaves section, allowing user to see check animation and strikethrough.
- **Content Column**:
  - Title: 16sp SemiBold, 21sp line height, max 2 lines. When completed, struck through and colored in `tertiaryText`.
  - Meta Line: Formatted date (Overdue/Upcoming/Done), lowercase time (e.g., "11:30 pm"), repeat badge (`Icons.Default.Repeat` + "Every day").
- **Far-Right Priority Badge**:
  - Positioned at the far right of the card, completely separated from title and date/time.
  - **High**: Red pill badge (`destructiveColor.copy(alpha = 0.16f)` background, `destructiveColor` text "High").
  - **Medium**: Copper pill badge (`primaryContainer` background, `primary` text "Medium").
  - **Normal / Low**: **No text badge is shown** (keeping the row clean and decluttered).
- **Gestures (`SwipeToDismissBox`)**:
  - Drag Right (StartToEnd): Reveals copper plate with edit icon (`Icons.Default.Edit`). Opening opens `TaskEditSheet`.
  - Drag Left (EndToStart): Reveals crimson plate with delete icon (`Icons.Default.Delete`). Releasing triggers delete confirmation dialog.

### 5.6 Fixed Round "+" Button (`TasksFab`) & Clearance
- **Geometry**: 60dp circular pill with solid `CopperPrimary` fill and 28dp icon with 2.3dp stroke.
- **Position**: Floating at `end = 28.dp` and `bottom = 140.dp + navBarInset` (exactly 44dp clearance above the 24dp-raised tab bar).
- **Halo & Shadow**: 7dp copper halo ring (`CopperPrimary.copy(alpha = 0.14f)`, outer diameter 74dp) and soft 14dp Y-offset shadow (36% black, 32dp blur).
- **Press & Dismiss**: 0.92 press scale. Smoothly animates out (`scale(0.5f)`, rotate $90^\circ$, `alpha = 0`) when `QuickAddCard` is open.
- **Clearance & Fade**:
  - LazyColumn has `contentPadding = PaddingValues(bottom = 232.dp + navBarInset)` so the final task scrolls completely clear of both the FAB and tab bar.
  - Bottom gradient fade: 182dp + navBarInset non-interactive gradient (`Color.Transparent` to background reaching 100% at 55%).
- **Undo Toast**: Stadium pill floating at `bottom = 212.dp + navBarInset` with 3.5s auto-dismiss timer and interactive "Undo" button (34dp height).

### 5.7 Quick-Add Card & Natural Language Engine (`QuickAddCard`)
- 32dp elevated sheet floating above the tab bar.
- Text input supporting natural language parsing:
  - Dates: "today", "tomorrow", "tmrw", "next week", "day after tomorrow", day names.
  - Times: "5pm", "11:30 am", "at 6:00 pm".
  - Priorities: "!high", "!!!", "urgent", "!med", "!low".
  - Repeats: "every day", "daily", "every week", "every monday".
- Dynamic NLP token chips previewing parsed parameters with individual remove ("x") buttons.
- Voice microphone input button with pulsing listening ring.
- Sequential rapid entry: stays open on submission for entering batches of tasks.

---

## 6. Focus System (v3.0 / `reflex-focus.html` + `reflex-focus(1).html`)

Rebuilt to replicate the interactive physics and visual hierarchy of the Focus prototypes:

```
+-------------------------------------------------------------+
| Focus                                            [X] [H] [*]|
| +---------------------------------------------------------+ |
| |      (==) Pomodoro                   Flow               | |
| +---------------------------------------------------------+ |
|                                                             |
|                         .-'""'-.                            |
|                       .'  Focus '.                          |
|                      /   10:00    \                         |
|                     |     Ready    |                        |
|                      \   ~~~~~~~  /                         |
|                       '.       .'                           |
|                         '-...-'                             |
|                                                             |
| [======] [=] [======] [=] [======] [=] [======] [=========] |
|              4 * 25 min focus · 2h 10m per cycle            |
|                                                             |
| +---------------------------------------------------------+ |
| | Focus length                               [-] 25 min [+] |
| |---------------------------------------------------------| |
| | Short break                                [-]  5 min [+] |
| |---------------------------------------------------------| |
| | Long break                                 [-] 15 min [+] |
| |---------------------------------------------------------| |
| | Sessions per cycle                         [-]    4   [+] |
| |---------------------------------------------------------| |
| | Auto-start next phase                               ( o ) |
| +---------------------------------------------------------+ |
|                                                             |
| +---------------------------------------------------------+ |
| | [=] Link a task                                       > | |
| +---------------------------------------------------------+ |
|                                                             |
| +---------------------------------------------------------+ |
| |                 Start pomodoro session                  | |
| +---------------------------------------------------------+ |
|                                                             |
| [=================== Frosted Tab Bar =====================] |
+-------------------------------------------------------------+
```

### 6.1 Top Bar & Modes
- **Top Bar**: "Focus" in 22sp Bold (`headlineMedium`) + three 44dp circular icon buttons (6dp gap):
  - App Blocking: Circle-with-slash icon (`Icons.Default.Block`). Tinted copper with `CopperContainer` background when blocking is active; opens `AppPickerSheet`.
  - History: Clock-arrow icon (`Icons.Default.History`); opens `FocusAnalyticsScreen`.
  - Settings: Gear icon (`Icons.Default.Settings`); opens `FocusSettingsSheet`.
- **Mode Toggle**: 44dp segmented pill container (`secondaryContainer` background, 4dp padding) with two segments: "Pomodoro" (hourglass icon) and "Flow" (waves icon). Active segment has `CopperPrimary` fill and `OnCopper` SemiBold text.
- **Flow Sub-Toggle**: When Flow is selected, a 38dp segmented pill appears below for "Timed flow" / "Open flow".

### 6.2 LiquidTimer Hero Sphere
- **Dimensions**: Sphere diameter is dynamically clamped:
  `heroDiameter = (screenHeight * 0.24f).coerceIn(150.dp, 230.dp).coerceAtMost(screenWidth * 0.56f)`.
- **Wave Physics & Canvas Rendering**:
  - Sinusoidal wave formula: `y = fillY + amp * sin(freq * x + phase)`.
  - Idle state: Waves animate smoothly at $1.1\text{ rad/s}$ with 8dp amplitude.
  - Fill ratio: Pomodoro and Timed flow reflect remaining time fraction ($1.0 \to 0.0$); Open flow idles stably at 0.55 fill.
  - Colors: Blends copper accent with liquid depth gradients.
- **Readout**: Center text with Phase Label ("Focus" / "Short break" / "Timed flow" / "Open flow"), giant countdown time in Lora Bold (`tnum`), and status caption ("Ready" / "Running" / "Paused").

### 6.3 Pomodoro Cycle Strip
- **Height**: 12dp stadium pill segments with 4dp gaps, 10dp above and 6dp below.
- **Weights**: Focus segments weighted by `workDurationMin`; Short break weighted by `shortBreakMin`; final Long break weighted by `longBreakMin`.
- **Colors**: Focus segments in `CopperPrimary`; breaks in Sage green (`colorScheme.sage`, 60% opacity for short breaks, 100% for long break).
- **Caption**: Centered text 12dp below strip: `"{sessions} × {focusMin} min focus · {cycleTotal} per cycle"`.

### 6.4 Config Card with Balanced Breathing Room
- **Card**: Surface container with 32dp corner radius, 1dp border, and **18dp horizontal / 10dp vertical padding**.
- **Stepper Rows**: Row height is **60.dp** (providing 10dp top and bottom clearance for the 40dp circular stepper buttons):
  - Focus length: 5–120 min (step 5).
  - Short break: 5–30 min (step 5).
  - Long break: 10–60 min (step 5).
  - Sessions per cycle: 1–8 sessions (step 1).
- **Dividers**: 1dp hairline dividers between each row.
- **Auto-Start Switch**: 60dp row with title "Auto-start next phase", subtitle "Continue without tapping start", and `ReflexSwitch`.
- **Timed Flow Config**: Flow duration stepper (5–180 min) + 5 quick chips (`15m`, `30m`, `45m`, `60m`, `90m`).
- **Open Flow Config**: Zen mode explanatory card ("Counts up, no target").
- **Flexible Break Controls**: Fully customizable work, short break (down to 0m / off), and long break durations across all presets.
- **Ambient Synthesized Tick Sound (`f_tick`)**:
  - Optional subtle mechanical/wood click synthesized locally via `FocusTickPlayer` (18ms dual-resonance PCM wave at 44.1kHz, low volume 0.20f amplitude).
  - Uses `USAGE_ASSISTANCE_SONIFICATION` without requesting audio focus (background audio / podcasts never duck or pause).
  - Ticks once per second strictly during WORK phases; stops instantly on pause, break, completion, early stop, and service destruction.
  - Reacts immediately to mid-session toggle changes.

### 6.5 Task Linking & Fixed Start Button
- **Task Linking**: 52dp row (32dp radius, surface, 1dp border, 14dp gap below config card) with 36dp circular copper icon tile. Tapping opens `TaskPickerSheet` to bind a pending task to the session.
- **Pinned Start Button**:
  - 56dp full stadium pill, `ActionPillWhite` background, 17sp Medium text.
  - Anchored at `Alignment.BottomCenter` with `widthIn(max = 398.dp)`.
  - Floating clearance: **`bottom = 108.dp + navBarInset`** (exactly 12dp gap above the 96dp top edge of the 24dp-raised tab bar).
  - Press scale: 0.98 animated scale.
  - Scroll clearance: Column content has `bottom = 184.dp + navBarInset` padding so all rows scroll fully above the Start button.
  - Gradient fade: 208dp + navBarInset vertical fade (`Color.Transparent` to background reaching 100% at 42%).

### 6.6 Running Focus Session Screen
- Full-screen ambient immersion covering the tab bar.
- Ambient radial glow pulsing with session breath.
- Giant countdown display (`displayLarge`, 79sp Bold, `tnum`).
- Phase badge pill: "Focus session {n} of {total}" / "Break".
- Linked task banner with interactive checklist items.
- Controls: Pause / Resume, Skip phase, and End session (with confirmation dialog).
- Foreground service: `FocusTimerService` with sticky persistent notification, wake locks, and sound/vibration cues on phase transitions.

---

## 7. Routines System

The Routines module is the primary launch screen (`Screen.RoutineList.route = "routine_list"`).

### 7.1 Home Screen Structure
- **Week Strip**: 7 circular day indicators (38dp) showing activity dots and copper highlights for active/today dates.
- **Stat Cards Grid**: 4 tactile stat tiles (2x2):
  1. Active routines count (`Icons.Default.FlashOn`)
  2. Pending tasks count (`Icons.Default.Check`)
  3. Focus time accumulated today (`Icons.Default.HourglassBottom`)
  4. Current streak count (`Icons.Default.Bolt`)
- **Active Routines List**: Routine cards with 32dp radius, icon tile, duration, step count, and quick "Start" action pill.
- **Swipe Actions**: Swipe to edit or archive routines.
- **Archived Routines**: Collapsible archive section.
- **Bottom Clearance**: 114dp spacer ensuring full clearance of the raised nav bar.

### 7.2 Routine Editor & Running Timer
- **Editor**: Name, category, color swatch, icon selector, and reorderable steps list with duration steppers and audio chimes.
- **Running Timer (`RunningTimerScreen`)**:
  - Full-screen circular step progress display (`displayMedium`, 53sp Bold) utilizing `LiquidTimer` with physical wave rendering.
  - Step progression: Timed, Check-off, and Reps/Sets steps.
  - Current step title, rep counters, and next step preview.
  - **Audio & Haptics Gating**: Respects `r_snd` and `r_vib` preferences at call time in `RoutineTimerService.playCompletionCue()`.
  - **Default Rest (`r_rest`)**: Configurable global rest duration (`Off`, `10s`, `15s`, `20s`, `30s`; default 15s) automatically applied to new routines.
  - **Step Auto-Advance (`r_auto`)**: When enabled, timed steps auto-advance upon countdown completion; when disabled, holds step at 0 waiting for user tap. Check-off steps never auto-complete.
  - **Completion Confetti & Summary**: Multi-burst particle confetti celebration upon routine completion and structured progress logging to Room database.

---

## 8. Calendar System (v3.0 / `reflex-calendar.html`)

A unified chronological timeline, single morphing grid calendar, and device calendar sync replicating `reflex-calendar.html`:

### 8.1 Pure Kotlin Calculations Engine (`CalendarCalculations`)
- Testable, zero Android/Compose dependencies.
- Handles all-day UTC date preservation, localized 12/24-hour time formatting, multi-day span labeling, deterministic tie-breaking (Events -> Tasks -> Routines -> title A-Z), dynamic range clipping `[today .. today + agendaRangeDays]`, mandatory injection of `selectedDate`, chronological "Now" divider insertion, and morphing grid row computation.

### 8.2 Sticky Morphing Calendar Card
- 32dp corner radius, Surface background, 1dp Border, 14dp top / 16dp side / 6dp bottom padding.
- Header row with 40dp circular prev/next buttons (48dp touch target) and "{Month} {year}" in `titleMedium` with 18dp copper chevron rotating $180^\circ$ when expanded.
- Localized 2-letter weekday headers respecting `firstDayOfWeek` preference (Monday or Sunday).
- **Single Morphing Grid**: One unified grid animating height and vertical offset simultaneously over 320ms using `CubicBezierEasing(0.2, 0.8, 0.2, 1)`. Week mode clips to 1 row (56dp) translated to the selected date's row; Month mode expands to 4–6 rows (rows * 56dp) at y=0dp. Outside-month days dimmed to 35% opacity.
- 40x5dp grab handle pill at bottom. Vertical drag follows finger and settles past 40% threshold.
- Direction-locked gestures: distinguishes horizontal swipe (moves week/month) from vertical drag once displacement exceeds 16px.

### 8.3 Day Cells & Dots Hierarchy
- 56dp tall cell with 40dp date circle: copper filled with on-copper text for selected day; 1.5dp copper outline with copper text for today (unselected); transparent for other days.
- Up to 3 indicator dots (5dp, 3dp gap) in fixed order: Event (`eventBlue`), Task (`CopperPrimary`), Routine (`routineGreen`). Filtered dynamically by active chip.
- Accessible TalkBack descriptions: `"{Weekday}, {Month} {d}, {n} items, selected"`.

### 8.4 Agenda Timeline List (`LazyColumn`)
- **Left Spine**: 1.5dp border-colored line at x=19dp directly under the date circle center.
- **Day Sections**: Day header with 40dp circle, date title, "Today" copper badge, item count, and "+ Task" pill button (32dp, `SurfaceInput`) prefilling `QuickAddCard(initialDate = date)`.
- **Chronological "Now" Divider**: Placed on today's section only between past and future items with 9dp copper dot on the timeline and "Now · {time}" text.
- **Item Cards**:
  - 26dp radius, Surface bg, 1dp border, 12x14dp padding, 8dp vertical gap.
  - Left 44dp icon tile (16dp radius) tinted at 18% opacity of type color with 20dp icon.
  - Content: Time label (13sp SemiBold in type color), Title (16sp SemiBold, up to 2 lines), Subtitle (13sp secondary).
  - Tasks include 26dp circular checkbox (48dp touch target). Completing strikes through and dims to 70%; with "Exclude completed tasks" enabled, animates out with a 5-second undo snackbar.
- **Bottom Clearance**: 112dp bottom clearance + navigationBarsPadding so last item clears the raised floating tab bar.

---

## 9. Habits System (v3.0 / `reflex-habits.html`)

Offline habit tracking, streak momentum, and consistency analytics:

### 9.1 Habit Types (Sealed Class)
1. **CheckOff**: Binary completion. Done when `value >= 1.0`.
2. **Measurable(target, unit, step)**: Numeric progress. Done when `value >= target`. Row displays animated progress bar, "x / target unit", and -/+ circular steppers (38dp).
3. **Limit(max, unit, step)**: Cap tracker. Done when logged and `value <= max`. Over the limit displays warning in `colorScheme.destructiveRed`. Displays "Not logged · max N unit" when no log exists.
- **Progress Fraction**: Check-off is `0.0` or `1.0`; Measurable is `min(1.0, value / target)`; Limit is `1.0` if logged and `<= max`, else `0.0`.
- **Stepper Constraints**: Values never drop below `0.0`, rounded to 1 decimal place, capped at `3 * target` (Measurable) or `4 * max` (Limit).

### 9.2 Main View Layout
- **Streak Card**: Big 64sp Bold copper number, "day streak", "Best" record tile, 7-day indicator dots (34dp circles; filled copper if active, outline ring on today), and "Today" chip that smooth-scrolls matrix to current day. Active day requires $\ge 1$ habit completed; counts from yesterday if today has no completions yet.
- **Daily Progress**: 132dp circular progress ring (12dp stroke width, smooth animation), "done/total" centered with percentage readout, followed by today's habit rows with -/+ steppers, "Manage" chip, and "Add habit" action pill.
- **Monthly Habit Matrix**: Sticky habit names column, smooth horizontal scrolling day columns (1..lastDayOfMonth), day header pills (today highlighted), cell states (complete copper filled with check, partial outline ring, empty subtle dash, over-limit red alert, inactive dot for days prior to `startEpochDay`). Tapping any day opens the Selected Day Bottom Sheet.
- **Consistency**: 30-day completion rate progress bar, and 3 stat tiles (30-day rate %, active days count, completions count).
- **Bottom Clearance**: 114dp padding ensuring full clearance above the raised tab bar.

### 9.3 Analytics View
- **Month Snapshot**: 4 metric tiles in 2x2 grid (Total check-ins, Completion rate %, Best streak this month, Perfect days).
- **Consistency Heatmap**: 365 days grouped into Sunday-start week columns (7 rows: Sun-Sat), smooth horizontal scroll to rightmost edge, level color bins (0 = surface variant, 1-2 = 30% copper, 3-4 = 60% copper, 5+ = 100% copper), and legend.
- **Analysis**: 3 bullet summary cards dynamically derived from user history.

---

## 10. Settings, Backup & Diagnostics System

Universally accessible from the top-right gear icon (`SettingsTopBarButton`) across all 5 main screen top bars with deep-linking support (`settings?subScreen={id}`).

### 10.1 User Profile Header
Positioned at the top of the Settings screen (above Appearance), naturally aligned with the Reflex Design System:
- **Profile Avatar**: 80dp circular container with 2dp metallic copper border ring (`CopperPrimary`).
  - **Custom Photo**: Supported via Android Photo Picker (`ActivityResultContracts.PickVisualMedia`). Photo is downsampled, upright-oriented (EXIF corrected), and persisted locally as JPEG in `context.filesDir/profile_avatar.jpg`.
  - **Default Fallback**: Signature Thunder vector icon (`SettingsIcons.Thunder`) centered in copper tint on `secondaryContainer` fill.
  - **Camera Badge Overlay**: 24dp visual circle (surface background, 1.5dp border, 12dp camera icon) placed half-way in, half-way out of the 80dp ring at `Alignment.BottomEnd`. Wrapped in an accessible **48dp touch target** (`Role.Button`, `contentDescription = "Change profile photo"`).
- **Name & Bio Display**:
  - Name rendered in 20sp Bold Lora (`colors.ink`).
  - Bio rendered in 14sp secondary text ("Tap to set bio" when empty).
  - Tapping opens the Profile Edit Dialog with auto-focused text inputs, character limits, and persistent saving to `UserProfileRepository` and `SettingsRepository`.
- **Backup Portability**: Full name, bio, and Base64-encoded avatar round-trip cleanly in JSON backup exports/imports.

### 10.2 Appearance Preferences
- **3-Way Segmented Control**: 44dp segmented pill with smooth sliding copper pill indicator (240ms cubic-bezier):
  - **System (Default)**: Automatically tracks OS light/dark modes dynamically.
  - **Dark**: Deep obsidian palette (`#0A0908`).
  - **Light**: Crisp warm paper canvas (`#F7F3EE`).
- **Immediate Live Theming**: Applies instantly across all Compose hierarchies via `ThemePreferenceRepository` without activity recreation.
- **Contrast & Legibility**: Fully tuned for status bar icons, gesture navigation bars, and frosted glass tab bar readability in both light and dark modes.

### 10.3 Permissions & System Diagnostics Hub
Live diagnostic hub providing real-time visibility and deep linking for all device capabilities:
1. **Overlay Permission (`p_ovr`)**:
   - Status: Active when `Settings.canDrawOverlays(context)` is true.
   - Action: Tapping launches `ACTION_MANAGE_OVERLAY_PERMISSION` with `package:${context.packageName}`. Required for priority alert overlays and app blocking.
2. **Device Calendar Access (`p_cal`)**:
   - **Tri-State Architecture**:
     - `GRANTED`: Both `READ_CALENDAR` and `WRITE_CALENDAR` permissions granted. Displays "Allowed".
     - `PARTIAL`: One permission granted. Displays "Partial" pill; tapping requests missing permission.
     - `DENIED`: Neither granted. Displays "Allow".
   - **Permanent Denial Fallback**: If permissions are permanently denied (`!shouldShowRequestPermissionRationale`), routes directly to `ACTION_APPLICATION_DETAILS_SETTINGS` with user guidance toast.
3. **Notifications & Channel Audit (`p_not`)**:
   - Evaluates master notification permission (`areNotificationsEnabled()`) and inspects real reminder channels (`channel_task_reminders`, `channel_routine_reminders`, `channel_focus_alerts`, `channel_habit_nudges`) on API 26+.
   - Statuses: Granted (all unblocked), Partially blocked (app enabled but one or more reminder channels muted to `IMPORTANCE_NONE`), Denied.
   - Deep Linking: Tapping a blocked channel navigates directly to `ACTION_CHANNEL_NOTIFICATION_SETTINGS` with `EXTRA_CHANNEL_ID`.
4. **Exact Alarms (`p_alm`)**:
   - Verified that all scheduling uses `AlarmManager.setAlarmClock()`, exempt from `SCHEDULE_EXACT_ALARM` Doze restrictions on API 23+.
   - Displayed as permanently active informational item: "Uses alarm clock scheduling, no permission needed".
5. **Battery Optimization (`p_bat`)**:
   - Checks `PowerManager.isIgnoringBatteryOptimizations()`.
   - Tapping directly requests `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with package URI; falls back to system battery settings on exception.
6. **Usage Access (`p_blk`)** & **Microphone (`p_mic`)**: Direct routing to usage stats settings and runtime audio permission launcher.

### 10.4 Data Management, Import/Export & Auto-Backup
1. **Full JSON Schema Pre-Validation (`DataExportImportManager`)**:
   - Strict pre-validation of imported JSON backup files prior to database writes. Rejects malformed structures with clear error messaging without modifying existing records.
2. **Transactional DB Writes**:
   - All import writes execute inside `db.withTransaction { ... }`, ensuring atomic all-or-nothing rollback on any database or parsing failure.
3. **REPLACE Import Mode**:
   - Existing active task and routine alarms are cleanly cancelled (`cancelAllAlarms()`).
   - Existing database tables are wiped cleanly inside the transaction before importing fresh records.
   - Full profile restoration: decodes avatar Base64 to `filesDir/profile_avatar.jpg`, restores preferences, and reinitializes `SettingsRepository` and `ThemePreferenceRepository`.
4. **Post-Import & Boot Alarm Rescheduling**:
   - `AlarmScheduler.rescheduleAllAlarms(context)` is invoked immediately following a successful import transaction and inside `BootReceiver` on system reboot.
5. **Deletion Cascade Cleanup**:
   - Deleting a task or routine via `ReflexRepository` automatically cancels associated reminder alarms, snooze alarms, and active priority alert dialogs.
   - "Reset all data" danger-zone action cancels all system alarms and wipes tables inside a single Room transaction.
6. **Weekly Automated Backups (`d_auto`)**:
   - Powered by Android WorkManager (`androidx.work:work-runtime-ktx`).
   - Toggling on opens Storage Access Framework `OpenDocumentTree` picker; acquires persistable URI permission (`takePersistableUriPermission`).
   - Enqueues a 7-day unique `PeriodicWorkRequest` (`AutoBackupWorker`) running export logic to `reflex-backup-YYYY-MM-DD.json` via `DocumentFile`.
   - Automatic rotation: retains the newest 4 backups in the chosen directory, pruning older files.
   - Includes dynamic "Last backup: <date>" status and an instant "Back up now" trigger button.

### 10.5 Onboarding Tour & Non-Destructive Replay
- Interactive 6-step welcome walkthrough (`TourScreen.kt`) showcasing Routines, Tasks, Habits, Focus, and Permissions.
- **Replay Tour (`isReplay = true`)**:
  - Replayable on-demand from Settings without calling `OnboardingManager.resetOnboarding()`.
  - Skips runtime permission request screens.
  - Leaves onboarding completion flags untouched.
  - Back, skip, and finish buttons return the user cleanly back to Settings.
- **Dynamic Versioning**: Displays live `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` (e.g. `2.0.0 (2)`).

---

## 11. Engineering & Architectural Constraints

1. **UI Layer & Navigation Purity**: Feature improvements consume existing ViewModels, Room DAOs, entities, and services. Zero regressions to database schema integrity or service contracts.
2. **Room Database (v13)**:
   - Bumped to version 13 with `MIGRATION_12_13`.
   - New table: `focus_tags` (`id` INTEGER PRIMARY KEY AUTOINCREMENT, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL).
   - Column additions: `steps.restDurationSeconds` (INTEGER), `completion_logs.totalStepsCount` (INTEGER NOT NULL DEFAULT 0), `completion_logs.stepLogsJson` (TEXT), and `focus_sessions.tagId` (INTEGER DEFAULT NULL).
3. **Zero Network Permissions**: The application does not declare `android.permission.INTERNET`. Zero external HTTP requests, zero web sockets, zero external CDN font links.
4. **Token Purity**: Every padding, color, typography size, and radius is bound to `Tokens.kt`, `Theme.kt`, and `Color.kt`.

---

## 12. Component & Navigation Patterns (v3.1)

### 12.1 Shared Top Bar Specifications (`ReflexTopBar.kt`)
- **Action Buttons**: Unified `TopBarIconButton` and `SettingsTopBarButton` composables used consistently across all top-level tabs and sub-screens.
- **Dimensions**:
  - Touch Target: $\ge 48\text{dp} \times 48\text{dp}$ (`minimumInteractiveComponentSize()`).
  - Visual Surface: $44\text{dp} \times 44\text{dp}$ stadium circle, centered.
  - Spacing & Alignment: $6\text{dp}$ horizontal gap between buttons; aligned to `Bottom` or `CenterVertically` with consistent `reflexStatusBarPadding()`.
  - Rightmost Position: Settings button is always the rightmost action.
- **Canonical Icon Standard**:
  | Action | Canonical Icon | Name / Visual | Visual State |
  | :--- | :--- | :--- | :--- |
  | **Settings** | Gear only | `"settings"` | Default surfaceContainer / outline |
  | **History** | Clock-arrow | `"hist"` | Default surfaceContainer / outline |
  | **App Blocking** | Circle-with-slash | `"block"` | Active: `CopperContainer` + `CopperPrimary` tint |
  | **Analytics** | Line graph | `"chart"` | Default surfaceContainer / outline |

### 12.2 Hierarchical Back Navigation & Exit Safety
- **Layer 1: Transient Overlays**: System back first closes any open `QuickAddCard`, bottom sheet (`ModalBottomSheet`), or alert dialog.
- **Layer 2: Sub-Screen Return**: From any sub-screen (Settings sub-pages, history, detail views), back navigates to its parent screen.
- **Layer 3: Top-Level Tab Return**: From any secondary tab (Calendar, Tasks, Habits, Focus), back automatically returns the user to the flagship **Routines (Home)** tab (`selectedTab = Tab.ROUTINES`).
- **Layer 4: Root Exit Confirmation**: Pressing back on the Routines/Home tab prompts an "Exit Reflex?" dialog with a 36dp corner radius (`ReflexTokens.ShapeSheet`), preventing accidental exits.
- **Running Timers**: Pressing back in `RunningTimerScreen` or `RunningFocusScreen` shows a confirmation dialog and leaves the background timer service running unimpeded.

### 12.3 Focus Tab Task Link & Tag Pill Badge
- **Enlarged Task Link Row**: Increased height from 52dp to **72dp**, providing ample room for multi-line linked task titles and prominent tag pill badges.
- **Tag Pill Badge**:
  - Rendered inline beside or below the task title as a $28\text{dp}$ stadium pill.
  - Background: `CopperContainer` with subtle copper border; Text: $13\text{sp}$ Medium `CopperPrimary`.
- **Task & Tag Picker Sheet**:
  - Combined bottom sheet with flow chips for tags and a `+ New tag` action.
  - Tag CRUD: Creation dialog (1–24 characters, case-insensitive uniqueness check), rename dialog, and delete dialog with clear disclaimer: *"Sessions keep their time but become untagged"*.
  - When a linked task already belongs to a tag or is chosen, the tag auto-selects.

### 12.4 Routines List Swipe Actions (`SwipeToDismissBox`)
- **Swipe Right (`StartToEnd`)**:
  - Action: Edit Routine (`onEditRoutine(id)`).
  - Physical Reaction: Springs back to rest position immediately after firing the action.
  - Background: `CopperContainer` background, displaying edit pencil icon in `CopperPrimary` aligned to start ($18\text{dp}$ horizontal padding).
  - Feedback: Haptic feedback triggered at $35\%$ swipe threshold.
- **Swipe Left (`EndToStart`)**:
  - Action: Delete Routine with Undo window.
  - Physical Reaction: Item is optimistically removed from the UI list while `SnackbarHost` displays `"Routine deleted"` with an `"Undo"` action.
  - Reversal: Tapping `"Undo"` restores the item without touching the database. If dismissed, `viewModel.deleteRoutine()` permanently removes it.
  - Background: `MaterialTheme.colorScheme.error` (`DarkDestructiveRed` / `LightDestructiveRed`), displaying trash can icon in white aligned to end ($18\text{dp}$ horizontal padding).
  - Feedback: Haptic feedback triggered at $35\%$ swipe threshold.

### 12.5 Persistent Scroll Retention Across All Screens
- Root scrolling columns and sub-screens maintain exact scroll positions across tab navigation and system recreation.
- Implemented via `rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }` across:
  - `RoutineListScreen.kt` (Home / Routines)
  - `FocusHomeScreen.kt` (Focus setup)
  - `FocusAnalyticsScreen.kt` (Focus analytics & history)
  - `HabitsScreen.kt` (`mainScrollState`, `analyticsScrollState`, `matrixScrollState`, `heatmapScrollState`)
  - `SettingsScreen.kt` (`homeScrollState` and `rememberSaveable(screenDef.id)` for sub-screens).

### 12.6 Focus Analytics Architecture & Native Mockup Fidelity (Chunk 2)
The Focus Analytics subsystem (`FocusAnalyticsScreen.kt`, `FocusAnalyticsViewModel.kt`) provides a native, pixel-accurate Jetpack Compose implementation of `focus-analytics.html` (`anHTML()`), strictly adhering to Design System v3.0 tokens, Lora typography with tabular figures (`tnum`), and zero-network local storage.

#### Visual Hierarchy & Screen Order:
1. **Top Bar**:
   - Touch Target: $\ge 48\text{dp}$, visual size $44\text{dp}$ circular back button with auto-mirrored arrow icon.
   - Title: `"Focus analytics"` ($22\text{sp}$ Bold Lora).
   - Navigation: Back pops to `FocusHome`. Bottom navigation bar remains visible (`showBottomBar` includes `Screen.FocusAnalytics.route`, mapped to `NavTab.FOCUS`).
   - Scroll Retention: Entire view wrapped in a column with `rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }`.
2. **Range Segmented Control**:
   - Full pill ($48\text{dp}$ height, $999\text{dp}$ radius) with background `secondaryContainer` (`#24211E` dark / `#EFEAE4` light).
   - Segment items: `"Week"` (7 days), `"Month"` (30 days), `"Year"` (12 calendar months).
   - Active pill: `CopperPrimary` with `onCopper` text ($15\text{sp}$ SemiBold Lora).
3. **Tag Filter Chips Row**:
   - Horizontally scrollable row containing `"All"` and custom tags from the `focus_tags` table.
   - Height: $36\text{dp}$, full stadium pill. Active: `CopperContainer` + `CopperPrimary` text ($14\text{sp}$ Medium). Inactive: `secondaryContainer` + `onSurfaceVariant` text.
   - Filters every stat tile, chart, breakdown, top task, and the 365-day heatmap. Hidden automatically if no tags exist in the database.
4. **Primary Stat Tiles (Row 1)**:
   - 3-column grid of $26\text{dp}$ rounded tiles (`surfaceVariant`):
     - **Focus time**: Sum of `actualDurationSeconds` of counted sessions ($\ge 60\text{s}$) in range. Subtitle: `"7 days"` / `"30 days"` / `"12 months"`.
     - **Sessions**: Count of counted sessions. Subtitle: `"completed"`.
     - **Average**: Focus time divided by session count. Subtitle: `"per session"`.
5. **Daily Goal Card**:
   - $112\text{dp}$ circular progress ring drawn in `Canvas` (stroke width $11\text{dp}$, background track `secondaryContainer`, progress arc `CopperPrimary`, rounded caps).
   - Center text: Target achievement percentage ($24\text{sp}$ Bold Lora `tnum`).
   - Right layout: `"Focused today"` subtitle, `"$todayFocus of $goal"` ($20\text{sp}$ Bold Lora), and stepper controls ($36\text{dp}$ pill buttons with $15\text{min}$ steps, range $30..480\text{min}$, default $120\text{min}$).
   - Persistence: Saved directly to `SettingsRepository` (`KEY_FOCUS_DAILY_GOAL_MINS`) and included in backup export/import.
6. **Focus Time Bar Chart Card**:
   - Header readout: Live value ($20\text{sp}$ Bold Lora) + date label ($13\text{sp}$ Lora) for the currently selected bar.
   - Interactive scrubbing: Tap or drag horizontally across the $150\text{dp}$ chart area to scrub between bars. Haptic feedback tick (`TextHandleMove`) fires on each index change. Defaults to latest bar.
   - Bar styling:
     - Selected bar: `CopperPrimary` fill with a $2\text{dp}$ outline in copper 30% wash.
     - Met goal bar: Copper 72% tint (`cbar.ok`).
     - Standard bar: Copper 38% tint.
   - Dashed Goal Line: Horizontal dashed line (`PathEffect.dashPathEffect`) at the goal height on Week and Month views; omitted on Year view.
   - Bar Labels: Week (S M T W T F S), Month (day number on every 5th bar), Year (month initial J F M A M J J A S O N D).
   - Empty state note: Shows *"No focus time recorded for this period"* if all bars are 0.
7. **Secondary Stat Tiles (Row 2)**:
   - 3-column grid of $26\text{dp}$ rounded tiles:
     - **Streak**: Consecutive calendar days ending today or yesterday with $\ge 1$ counted session ($\ge 60\text{s}$). Subtitle: `"days in a row"`.
     - **Best day**: Maximum daily focus time in range (Year evaluates last 365 days). Subtitle: `"longest total"`.
     - **Finished %**: Completed sessions divided by total counted sessions ($"-"$ when 0 sessions). Subtitle: `"not ended early"`.
8. **Consistency Heatmap (365 Days Rolling)**:
   - Always displays the last 365 calendar days ending today, regardless of range selector; tag filter applies.
   - Geometry: 7 rows $\times$ 53 columns. Cell size $12\text{dp}$ circular pills with $3\text{dp}$ gaps. Total height $102\text{dp}$.
   - Week Alignment: Row 0 corresponds to the user's configured `firstDayOfWeek` (Monday or Sunday).
   - Single Canvas Architecture: Drawn in **one single `Canvas`** element rather than 365 individual Composables, achieving sub-millisecond draw time.
   - Intensity Scale: Level 0 (0m, `secondaryContainer`), Level 1 (1–29m, 30% copper), Level 2 (30–59m, 55% copper), Level 3 (60–119m, 80% copper), Level 4 ($\ge 120\text{m}$, 100% solid copper).
   - Horizontal Scroll: Initialized to the rightmost edge (newest week).
   - Tap Readout: Tap coordinate maps to date cell index, showing an outline ring and date + duration readout above the grid.
   - Legend: `"Less"` followed by 5 indicator dots and `"More"` aligned to bottom-end.
9. **Time of Day Card**:
   - 4 time buckets: Morning (05:00–11:59:59), Afternoon (12:00–16:59:59), Evening (17:00–21:59:59), Night (22:00–04:59:59).
   - Progress bars ($10\text{dp}$ height) with percentage breakdown.
   - Best Window Tip: Analyzes 24 hour-aligned 2-hour windows with an intelligent tie-breaker favoring windows starting with actual sessions. Formatted as `"Best window: 9 to 11 am"`. Rendered inside a `CopperContainer` pill with Bolt icon only when $\ge 5$ counted sessions exist in range; omitted otherwise.
10. **Session Types Card**:
    - Mode breakdown: Pomodoro (`CopperPrimary`), Timed flow (`sage`), Open flow (`onSurfaceVariant`).
    - Stacked Pill Bar: $14\text{dp}$ height full pill bar divided proportionally by duration.
    - Legend Rows: Color dot, mode name, formatted duration, and percentage.
11. **Top Tasks Card**:
    - Groups sessions in range by `sessionTitle` (or linked task title). Sessions without titles categorized under `"No linked task"`.
    - Displays top 3 tasks plus `"No linked task"` if present, with progress bars relative to the highest task's focus time.
12. **By Tag Card**:
    - Focus duration per custom tag + `"Untagged"`, with relative proportional progress bars. Hidden entirely if no tags exist in the database.
13. **Distractions Blocked Card**:
    - 2 stat tiles: **Attempts** (count in last 7 days) and **Kept** ($\text{attempts} \times 2\text{min}$ estimated saved).
    - Subtitle: `"Most blocked: [App name], [N] times"` resolved via `PackageManager`.
    - Manage Button: Full pill button opening `AppPickerSheet` to configure mode and packages.
    - Visibility: Displayed only if app blocking has ever been enabled or if events exist.


