# Project Overview: Reflex — Routine, Habit & Task Tracker

## 1. Concept

Reflex is a **local-first, timer-driven routine, habit, task tracker, and focus management system** for Android. It is built from the ground up with:

- **Reflex Design System v1.0**: Warm organic obsidian dark mode (`#0A0908`), warm paper light mode (`#F7F3EE`), signature metallic copper accents (`#D9A184` dark / `#8F4C2B` light), handcrafted Lora serif typography with tabular numerals (`tnum`), softer curvature (radii $\ge 16\text{dp}$ everywhere), and universal sentence-case copy.
- **Floating Frosted Glass Tab Bar**: 72dp stadium pill floating 24dp above navigation bar insets, powered by the Haze library (`dev.chrisbanes.haze`) with API 31+ hardware backdrop blur (24dp blur radius, 72% tint) and an accessible 94% tint fallback.
- **Full Data Privacy & Offline-First Operation**: 100% on-device storage (Room SQLite Database v14 + DataStore / SharedPreferences). Zero cloud sync, zero telemetry/analytics, zero third-party tracking, and zero network dependencies. All fonts, icons, and NLP engines are locally bundled.
- **5 Top-Level Core Navigation Destinations**:
  1. **Routines**: Sequential timer-driven step sequences (Timed, Check-Off, Reps, Sets-only) with configurable default rest periods (`r_rest`), auto-advance step control (`r_auto`), completion cues (`r_snd`, `r_vib`), completion confetti, schedule-aware streaks, and pre-built templates.
  2. **Calendar**: Agenda timeline merging Reflex tasks, scheduled routine reminders, and native device calendar events with smart empty-day filtering, tri-state calendar permissions, and custom preferences.
  3. **Tasks**: Natural language task capture with live syntax highlighting, progress card, sticky filter chips, swipe gestures, priority tags, and a fixed round `+` quick-add button.
  4. **Habits**: Daily habit tracker supporting custom schedules (Daily, Weekdays, Weekends, Custom Days), OpenMoji visual icons, interactive 32dp completion rings, and streak metrics.
  5. **Focus**: Classic Pomodoro & Zen Flow timers (Timed & Open Flow) with a 3D hero sphere, pinned Start button, low-latency synthesized ambient tick (`f_tick`), flexible 0m break removal, non-invasive app-blocking overlay engine, and a 52-week activity heatmap.
- **System-Wide Priority Overlays & Exact Alarms**: Full-screen floating overlay dialog (`TYPE_APPLICATION_OVERLAY` + `showWhenLocked`) that alerts users to medium and high priority tasks over any active app with 6-option snooze chips, with automatic alarm cancellation upon task deletion.
- **Comprehensive Native Settings & User Profile**: Native Compose implementation featuring a personalized User Profile header (custom photo / Thunder vector fallback, 48dp camera touch target, Name & Bio), live diagnostic permissions hub (Overlay, Tri-state Calendar, Notification Channels, Exact Alarms, Battery Optimization), transactional JSON export/import with rollback, WorkManager automated weekly backups (`d_auto`), non-destructive onboarding tour replay, and an animated 3-way sliding pill theme switcher.

---

## 2. Core Subsystems & Architecture

### A. Routines Engine

1. **Sequential Execution Engine (`RoutineTimerService.kt`)**: Walks users through ordered steps with a large countdown/rep display powered by `LiquidTimer`, stay-awake screen lock (`FLAG_KEEP_SCREEN_ON`), audio/vibration completion cues, and background foreground service durability.
2. **Step Types**:
   - **Timed**: Countdown timer with customizable minutes and seconds (e.g., "Morning Stretch — 2:00").
   - **Simple Check-off**: Manual "done" tap for physical or untimed actions (e.g., "Make Bed").
   - **Repeat-Count**: Numerical target counter (e.g., "20 Pushups") with incremental rep controls.
   - **Sets-Only**: Set progression tracking for strength and flexibility workouts without time constraints.
3. **Runtime Controls & Settings**:
   - **Audio & Haptic Feedback (`r_snd`, `r_vib`)**: Dynamically gated in `RoutineTimerService.playCompletionCue()` respecting user preferences.
   - **Default Rest Duration (`r_rest`)**: Configurable global rest interval (Off, 10s, 15s, 20s, 30s; default 15s) automatically applied to new routines.
   - **Auto-Advance Control (`r_auto`)**: When enabled, timed steps advance automatically; when disabled, holds at 0 until the user taps Next/Done. Check-off steps never auto-complete.
   - **Rest Periods & Skip Semantics**: Rest interval automatically entered between steps (except after the final step) with countdown readout and a direct **"Skip rest"** button.
   - **Completion Confetti & History**: Multi-burst particle confetti celebration upon finishing and structured progress logging to Room database.
4. **Schedule-Aware Streak Engine (`StreakCalculator.kt`)**:
   - Evaluates scheduled days (e.g., Mon, Wed, Fri) vs unscheduled routines.
   - **1-Day Grace Period**: Completing a missed day on the following day maintains the streak ("completed late").
   - **"Skip today" Action**: Allows logging an intentional skip (`isSkipped = true`) on `RoutineDetailScreen` without breaking streaks.
   - **4-State Visual Heatmap**: Completed on time, Completed late (grace), Explicitly skipped, and Missed.
5. **Pre-Built Routine Templates (`RoutineTemplates.kt`, `TemplatePickerModal.kt`)**: Morning Routine, Night Wind-Down, Focus Pomodoro, 7-Minute Workout, and Daily Reset.
6. **Drag-and-Drop Step Reordering (`RoutineEditorScreen.kt`)**: Interactive vertical drag handles with live list reflow and atomic Room DB ordering persistence.

---

### B. Habits Tracker

1. **Habit Data Model (`Habit.kt`, `HabitDao.kt`)**:
   - Core Properties: `id`, `name`, `frequency` (EVERY_DAY, WEEKDAYS, WEEKENDS, SPECIFIC_DAYS), `selectedDays` (comma-separated days of week), `openMojiHex` (OpenMoji avatar code), `reminderTime`, `isArchived`, `createdAt`.
   - Advanced Fields: `frequencyType` (DAILY, SPECIFIC_DAYS, TIMES_PER_WEEK), `targetCount`, `targetUnit`, `startDateEpochDay`, `endDateEpochDay`, `colorInt`, `iconName`, `notes`.
2. **Interactive Daily Check-ins (`HabitsScreen.kt`)**:
   - Interactive 32dp circular completion buttons with fluid checkmark animation.
   - Weekly horizontal completion dots showing Monday–Sunday status for the current week.
   - Current Streak and Best Streak computation based on scheduled frequency rules.
3. **Custom Tactile Reflex Pickers (`ReflexDatePickerModal.kt`, `ReflexTimePickerModal.kt`)**:
   - Custom in-app date and time dialogs designed according to Design System v3.0, replacing default system pickers with 36dp rounded corners, copper active highlights, and Lora tabular figures.
4. **Alarm Clock Reminders (`AlarmScheduler.kt`)**:
   - Scheduled daily or weekly habit nudge alarms running through `AlarmManager.setAlarmClock()`, surviving Doze mode and phone reboots.
5. **OpenMoji Visual Avatars (`OpenMojiImage.kt`, `OpenMojiMapper.kt`)**:
   - Offline-bundled OpenMoji PNG library mapped to categories (Wellness, Exercise, Mind, Nutrition, Productivity).
   - Custom emoji picker sheet allowing users to customize habit icons.

---

### C. Tasks Feature & Natural Recurrence Engine

1. **Task Data Model (`Task.kt`)**:
   - Properties: `id`, `title`, `notes`, `dueDate`, `dueTime`, `priority` (NONE, LOW, MEDIUM, HIGH), `isCompleted`, `completedAt`, `createdAt`, `reminderTime`.
   - Recurrence Fields: `recurrenceFrequency` (NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM), `recurrenceInterval`, `recurrenceUnit` (DAY, WEEK, MONTH, YEAR), `recurrenceDaysOfWeek` ("MON,WED,FRI"), `recurrenceMonthlyMode` (SAME_DATE, SAME_WEEKDAY_POS), `recurrenceEndType` (NEVER, ON_DATE, AFTER_OCCURRENCES), `recurrenceEndDate`, `recurrenceEndOccurrences`, `recurrenceBasis` (FROM_DUE_DATE, FROM_COMPLETION_DATE), `recurrenceOccurrenceCount`.
2. **Tasks UI (`TasksScreen.kt`)**:
   - **Progress Card**: 26dp rounded surface displaying `{done} of {total} done today`, percentage readout, and animated copper progress bar.
   - **Sticky Filter Chips**: Full-bleed horizontally scrollable chips (**All**, **Today**, **Upcoming**, **No date**, **Completed**) with live task counts.
   - **Structured Grouping**: Sections for **Overdue** (crimson header), **Today**, **Upcoming**, **No date**, and a collapsible **Completed** row.
   - **Dedicated Priority Indicators**: Right-aligned priority tags (High in Destructive Red, Medium in Copper Primary). Low and normal priority tasks hide labels to reduce visual noise.
   - **Swipe Actions**:
     - Swipe right (offset $\ge 72\text{dp}$): Instant completion toggle with emerald checkmark reveal.
     - Swipe left (offset $\le -72\text{dp}$): Deletion action with crimson trash reveal and 4-second undo snackbar.
   - **Floating Quick-Add Button**: 56dp circular copper button with `+` icon positioned above the bottom bar opening `QuickAddCard`.
3. **Natural Language Recurrence Parsing (`TaskParser.kt`)**:
   - Triggered by natural keywords (`repeat`, `every`, `today`, `tomorrow`, `tonight`, `in X mins`, etc.).
   - Strips recurrence trigger words from final title, computes initial due date/time, and configures recurrence rules in one step.
4. **Live Highlight Transformation (`TaskHighlightVisualTransformation.kt`)**:
   - Real-time copper keyword and phrase highlighting for dates, relative times, durations, recurrence triggers, and priorities as the user types without displacing cursor positions.
5. **Reliable Next-Occurrence Generation (`ReflexRepository.kt`, `RecurrenceCalculator.kt`)**:
   - Completing a repeating task marks the current instance finished in history, generates the next occurrence in Room DB, and schedules its exact system alarm clock automatically.

---

### D. TaskParser Natural Language Vocabulary Table

| Category | Input Phrase Examples | Parsed Outcome / Defaults |
|---|---|---|
| **Recurrence Phrases** | `repeat every tuesday`, `sunday repeat` | Weekly recurrence on target day(s) |
| | `every day repeat`, `repeat daily` | Daily recurrence |
| | `every month 10th day repeat` | Monthly recurrence on the 10th |
| | `repeat every 2 weeks`, `every 3 days` | Custom interval recurrence |
| | `repeat every weekday`, `weekdays repeat` | Mon–Fri weekday recurrence |
| **Relative Days** | `today`, `2day` | Today's Date |
| | `tomorrow`, `tmrw`, `tmr` | Tomorrow's Date |
| | `yesterday` | Yesterday's Date (`isPastDate = true`) |
| | `tonight` | Today's Date + 8:00 PM |
| | `this friday`, `this mon` | Current week's occurrence |
| | `next week`, `next monday` | Upcoming week's occurrence |
| | `in 5 days`, `in 2 weeks` | Relative calendar offsets |
| **Relative Durations** | `in 5min`, `in 10 min`, `in 45 mins` | Current Time + N Minutes |
| | `in 1hr4min`, `in 2 hours` | Current Time + H Hours M Mins |
| **Spaced & 12h/24h Times** | `10:46am`, `8pm`, `3:15 pm` | Exact Clock Time |
| | `10 30 pm`, `8 45am` | Space-separated time parsing |
| | `14:30`, `09:00` | 24-hour time format |
| **Time-of-Day Keywords** | `morning`, `mrng`, `mrn` | 8:00 AM |
| | `noon`, `midday` | 12:30 PM |
| | `afternoon` | 3:00 PM |
| | `evening`, `evng`, `eve` | 4:00 PM |
| | `eod`, `end of day` | 6:00 PM |
| | `night`, `nite` | 8:00 PM |
| | `midnight` | 12:00 AM |
| **Priority Tokens** | `!!!`, `high priority`, `p1`, `urgent`, `asap` | `Priority.HIGH` |
| | `!!`, `medium priority`, `p2`, `med priority` | `Priority.MEDIUM` |
| | `!`, `low priority`, `p3` | `Priority.LOW` |

---

### E. System-Wide Priority Task Overlays & Exact Alarms

1. **System Window Overlay (`PriorityTaskOverlayManager.kt`)**:
   - Uses `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` to display a centered 32dp rounded card over **any active application** (games, video players, browsers, social media) when medium or high priority task alarms fire.
2. **Lock Screen Companion Activity (`PriorityTaskAlertActivity.kt`)**:
   - Configured with `showWhenLocked="true"`, `turnScreenOn="true"`, and `FLAG_KEEP_SCREEN_ON` paired with full-screen intent in `NotificationHelper.kt`, ensuring immediate waking and presentation on locked devices.
3. **In-App & Overlay Modal Controls (`PriorityTaskAlertDialog.kt`)**:
   - **Priority Badges**: "⚡ High priority alert" (Destructive Red) vs "⚠️ Medium priority alert" (Copper Primary).
   - **Instant Actions**:
     - **Mark done**: Completes task, schedules next recurrence, cancels notification, and dismisses overlay.
     - **Open**: Navigates directly into Reflex to view or edit the task.
     - **6-Option Quick Snooze**: Selectable chips (`5 min`, `10 min`, `15 min`, `30 min`, `1 hour`, `Tomorrow`) with immediate exact alarm rescheduling.

---

### F. Focus & Pomodoro Architecture

1. **Hero Visual Sphere & Controls (`FocusHomeScreen.kt`)**:
   - 3D interactive hero sphere with copper radial glow, breathing animations, and cycle progression strip.
   - Breathing room around duration steppers (Focus length, Short break, Long break, Cycles).
   - Fully customizable break controls: allows setting short and long breaks down to 0 min (or removing breaks completely) across all presets.
   - Auto-start breaks toggle and task-linking row.
   - **Enlarged Task Link & Tag Pill Row**: 72dp tall row accommodating multi-line linked task titles and prominent 28dp tag pill badges (`CopperContainer` + `CopperPrimary`).
   - **Task & Tag Picker Sheet**: Tag CRUD (Create, Rename, Delete with session preservation) and automatic task-tag linking.
   - **Pinned Start Button**: 56dp stadium pill pinned above the bottom bar with a non-interactive gradient fade to ensure it is always visible on any screen size without scrolling.
2. **Timer Modes & Audio (`FocusTimerService.kt`, `FocusTickPlayer.kt`)**:
   - **Classic Pomodoro**: 25m Work / 5m Short Break / 15m Long Break across 4 customizable cycles.
   - **Timed Flow**: Preset durations (15m, 30m, 45m, 60m, 90m, 120m) with auto-finish.
   - **Open Flow / Zen Mode**: Unbounded count-up timer with subtle pulse cues.
   - **Ambient Synthesized Tick Sound (`f_tick`)**: Low-latency synthesized click sound generated locally (18ms dual-resonance PCM wave, 44.1kHz, 0.20f amplitude) playing once per second strictly during WORK phases without audio focus loss; halts immediately on pause, break, completion, or end early; responds reactively to mid-session toggles.
3. **Non-Invasive App Blocker (`AppBlockMonitorService.kt`, `AppBlockOverlayManager.kt`)**:
   - Uses `UsageStatsManager` to detect foreground apps during active Work phases.
   - Renders a floating blocking overlay when a restricted app is opened, offering "Back to focus" or "End session early".
   - Automatically records block attempts into `blocked_app_events` Room table.
   - Automatically pauses blocking during breaks, timer pause states, or session completion.
   - Always-exempt apps: Reflex, Phone dialer, SMS, Settings, and System UI launcher.
4. **Native Focus Analytics Screen (`FocusAnalyticsScreen.kt`, `FocusAnalyticsViewModel.kt`)**:
   - Pixel-accurate native build adhering strictly to Design System v1.0 tokens, Lora typography with tabular numbers (`tnum`), and zero hex literals.
   - **Entry & Navigation**: Accessible from the Focus top bar (order: App Blocking, Analytics, Settings); back button (44dp visual, 48dp target) returns to Focus home; bottom navigation bar remains visible (`showBottomBar` includes `FocusAnalytics`, mapped to `NavTab.FOCUS`); scroll state retained via `rememberSaveable`.
   - **Top-to-Bottom Components**:
     1. Range Selector: Week (7d) / Month (30d) / Year (12m) segmented pill.
     2. Tag Filter Chips: Horizontally scrollable chips ("All" + custom tags) filtering every stat, chart, top task, and heatmap. Hidden if no tags exist.
     3. Stat Tiles (Row 1): Focus time, Sessions, Average per session.
     4. Daily Goal Card: 112dp circular progress ring, % center, focused today text, goal stepper ($15\text{min}$ steps, range $30..480\text{min}$, default $120\text{min}$, persisted in `SettingsRepository` and backup export/import).
     5. Focus Time Bar Chart: Live selected bar readout and date label, drag-scrub and tap selection with haptic feedback tick (`TextHandleMove`), copper selected bar with outline, met goal tint, dashed goal line on Week/Month, bottom labels, and empty state note.
     6. Stat Tiles (Row 2): Streak ("days in a row"), Best day ("longest total"), Finished % ("not ended early").
     7. Consistency Heatmap: 365 days rolling, 7 rows $\times$ 53 cols, 12dp round cells, 3dp gaps, 5 intensity levels, horizontal scroll starting at newest week (right side), single Canvas drawing for instant rendering, tap readout, and legend. Week starts on configured first day of week.
     8. Time of Day Card: Morning (5–12), Afternoon (12–5), Evening (5–10), Night (10–5) progress bars. "Best window" tip line with intelligent tie-breaker shown only if $\ge 5$ counted sessions exist in range.
     9. Session Types Card: Stacked pill bar + legend rows for Pomodoro (copper), Timed flow (sage), Open flow (tertiary).
     10. Top Tasks Card: Top 3 linked tasks + "No linked task", with bars relative to largest.
     11. By Tag Card: Focus time per tag + "Untagged", hidden if no tags exist.
      12. Distractions Blocked Card: Attempts and Kept ($\text{attempts} \times 2\text{min}$ saved), most blocked app name line, and "Manage app blocking" button opening `AppPickerSheet`. Visible if blocking ever enabled or events exist.
5. **Streamlined Early Stop Flow & Duration Preservation**:
   - Manually stopping a session (Pomodoro, Timed Flow, Open Flow) skips the celebration animation and routes immediately to a single summary screen titled **"Session stopped early"**.
   - Saves actual focused elapsed time (e.g. 15 of 30 min counts as 15 min) by default into Room DB, persisting `endReason = "stopped_early"` and updating streaks, analytics, and heatmaps.
   - Sessions stopped under 1 minute display a prompt to save or discard.
   - Pre-end delete confirmation cards are removed in favor of an explicit "Discard session" action on the summary screen.

---

### G. Agenda-First Calendar Architecture

1. **Agenda Timeline (`CalendarScreen.kt`)**:
   - **Smart Empty-Day Filtering**: Displays only dates that contain actual tasks, routine reminders, or device events, eliminating endless empty scrolling.
   - **On-Demand Date Selection**: Clicking any day in the month dropdown or week strip dynamically anchors that date in the agenda feed with an inline "+ Add task" action.
   - **Compact Week Strip**: 7-day row with date numbers, selection fills, and completion dot indicators.
   - **Expandable Month Grid**: Smooth month overview dropdown with lunar/calendar day indicators.
   - **Unified Color Coding**: Tasks (amber/copper), Routine schedules (emerald green), Device calendar events (blue `#3565D6` / `#6F9BFF`).
2. **Dedicated Calendar Settings (`CalendarSettingsScreen.kt`, `CalendarPreferenceRepository.kt`)**:
   - Configurable toggles for excluding completed tasks, excluding repeating tasks, showing routine reminders, selecting enabled device calendars, agenda preloading range (7, 14, 30, 60 days), and first day of the week (Monday vs Sunday).
3. **Dual Calendar Permissions & Direct Creation**:
   - Combined permission launcher requesting both `READ_CALENDAR` and `WRITE_CALENDAR` simultaneously with a guidance toast on first visit.
   - Full in-app event creation sheet (`AddEventSheet.kt`) inserting directly into the system Calendar Provider without requiring third-party app redirects.
4. **Real-Time Live Reflection & ContentObserver**:
   - Newly created events are immediately pushed into UI StateFlow for 0ms instantaneous display in the agenda and day dots.
   - A live `ContentObserver` on `CalendarContract.Events.CONTENT_URI` automatically picks up any additions or modifications across the entire device.
   - Lifecycle `ON_RESUME` ensures up-to-date events when returning from external settings or calendar apps.

---

### H. Universal Settings & Diagnostics Architecture (`reflex-settings.html`)

1. **User Profile Header**:
   - Prominent profile card at the top of Settings with 80dp circular avatar ring in metallic copper (`CopperPrimary`).
   - Custom photo selection via Photo Picker with EXIF rotation correction and local persistence (`context.filesDir/profile_avatar.jpg`).
   - Default avatar: signature Thunder vector icon (`SettingsIcons.Thunder`).
   - Camera badge overlay in 24dp visual circle placed half-in, half-out of the profile ring, wrapped in an accessible 48dp touch target (`Role.Button`, `contentDescription = "Change profile photo"`).
   - In-app Name & Bio editing dialog with immediate persistence in `UserProfileRepository` and SharedPreferences.
   - Base64 avatar and profile data round-trip reliably in JSON exports and imports.
2. **Data Model & Schema (`SettingsModel.kt`, `SettingsRepository.kt`)**:
   - Comprehensive configuration schema covering 9 distinct screens:
     - `routines`: default rest time (`r_rest`), step auto-advance (`r_auto`), completion sound (`r_snd`) and haptics (`r_vib`), keep screen on.
     - `calendar`: first day of week, agenda preload, sync device calendars, exclude completed/repeating, show routines.
     - `tasks`: default reminder time, smart NLP parser toggle, auto-snooze, overdue notifications, priority overlays.
     - `habits`: reset hour, streak grace period, celebrate completions, daily reminders.
     - `focus`: work/break durations, customizable breaks down to 0m, auto-start breaks, app blocking enforcement, ambient synthesized tick sound (`f_tick`).
     - `notifications`: master notification switch, reminder channels, quiet hours.
     - `permissions`: live diagnostic permission state for Overlay (`p_ovr`), Tri-State Calendar (`p_cal`), Notification channels (`p_not`), Exact Alarms (`p_alm`), Battery Optimization (`p_bat`), Microphone (`p_mic`), and App Blocking (`p_blk`).
      - `data`: Room DB statistics, SAF JSON export/import backup (Schema v4), WorkManager automated weekly backups (`d_auto`), immediate synchronous data reset with SQLite WAL checkpointing.
      - `about`: dynamic version display (`BuildConfig.VERSION_NAME` and `VERSION_CODE`), typeface, privacy, open-source licenses, non-destructive onboarding tour replay (`isReplay = true`).
3. **Live Search & Navigation**:
   - Instant search across all setting labels and descriptions with breadcrumb navigation.
   - 22dp horizontal slide + fade sub-screen transitions with native Android back handling.
4. **Animated 3-Way Appearance Control**:
   - Fluid sliding copper pill indicator (240ms cubic-bezier) toggling **System**, **Dark**, and **Light** modes.
   - Immediate app-wide theme synchronization via `ThemePreferenceRepository.kt`.
5. **Universal Access**:
   - Consistent 44dp circular `SettingsTopBarButton` integrated on all 5 main screen top bars with deep-linking support (`settings?subScreen={id}`).

---

### I. Reliability & System Diagnostics

1. **Exact Alarms (`AlarmScheduler.kt`)**: Uses `AlarmManager.setAlarmClock()` for Doze mode immunity on Android 16 (API 36). Exempt from `SCHEDULE_EXACT_ALARM` permissions.
2. **Reboot Resiliency (`BootReceiver.kt`)**: Re-registers all incomplete task alarms and active routine reminders on `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` via `AlarmScheduler.rescheduleAllAlarms(context)`.
3. **Transactional Database Operations (`DataExportImportManager.kt`)**:
   - Strict pre-import JSON structure validation before touching the database.
   - All import writes execute inside `db.withTransaction { ... }` with automatic rollback on any failure.
   - Default `REPLACE` import mode cancels existing alarms (`cancelAllAlarms()`) and clears prior data inside the transaction before importing.
   - Automatically triggers `AlarmScheduler.rescheduleAllAlarms(context)` immediately after the transaction commits.
4. **Cascade Alarm Cancellation (`ReflexRepository.kt`)**:
   - Deleting a task or routine automatically cancels its associated reminder alarm and dismisses any active priority overlay dialogs.
   - "Reset all data" danger-zone action cancels all system alarms, wipes tables inside a single Room transaction, executes WAL checkpointing, and immediately updates settings UI stats to 0.
5. **Automated Weekly Backups (`AutoBackupWorker.kt`)**:
   - Powered by WorkManager `PeriodicWorkRequest` (7 days) with persistable Storage Access Framework folder URI permissions (`takePersistableUriPermission`).
   - Automatically writes `reflex-backup-YYYY-MM-DD.json` via `DocumentFile` and prunes older backups to retain the 4 newest files.
   - Offers an instant "Back up now" trigger button in Settings.
6. **Notification Channels (`NotificationHelper.kt`)**:
   - `channel_task_reminders`: High Importance (heads-up alerts + full-screen intent).
   - `channel_routine_reminders`: High Importance.
   - `channel_focus_alerts`: High Importance.
   - `channel_habit_nudges`: Default Importance.
   - `channel_timer_service`: Low Importance (ongoing routine timer).
   - `channel_focus_timer`: Low Importance (ongoing focus timer).

---

## 3. Tech Stack

- **Language & UI Toolkit**: Kotlin 2.0+ & Jetpack Compose (Material3 + Custom Reflex Design System v1.0 Tokens)
- **Design Tokens**: `ReflexTokens.kt`, `Color.kt`, `Type.kt`, `SettingsTheme.kt`
- **Typography**: Bundled Lora Serif (400 Regular, 500 Medium, 600 SemiBold, 700 Bold) with `fontFeatureSettings = "tnum"`
- **Backdrop Blur Engine**: Haze (`dev.chrisbanes.haze`)
- **Architecture**: MVVM with unidirectional data flow (`StateFlow` + Coroutines)
- **Local Persistence**: Room SQLite Database (Version 14) + DataStore / SharedPreferences
- **Background Automation**: AndroidX WorkManager (`androidx.work:work-runtime-ktx`) & Storage Access Framework (`androidx.documentfile`)
- **Audio & Haptics Engine**: Synthesized low-latency audio via `SoundPool` / `AudioTrack` (`FocusTickPlayer.kt`), `SoundCuePlayer.kt`, Android System Vibrator
- **Foreground Services**: `RoutineTimerService`, `FocusTimerService`, `AppBlockMonitorService`
- **System Integration**: `AlarmManager.setAlarmClock()`, `UsageStatsManager`, `WindowManager` (`TYPE_APPLICATION_OVERLAY`), `TileService` (`ReflexQuickAddTileService`), `CalendarContract` ContentResolver & ContentObserver
- **Target Platform**: Android 16 (API level 36), Min SDK 26
- **Build System**: Gradle (Kotlin DSL), R8 Full Minification + Resource Shrinking

---

## 4. Feature Status Matrix

| Module | Status | Highlights |
|---|---|---|
| **Routines** | **Complete** | Step sequences (Timed, Check-Off, Reps, Sets-only), Rest periods, Schedule-aware streaks, Grace days, Templates, Completion confetti, `r_snd`/`r_vib`/`r_rest`/`r_auto` runtime controls. |
| **Habits** | **Complete** | Frequency rules (Daily, Specific days, X per week), Scheduled reminders, Target goals, Start/end dates, Bespoke Reflex date/time pickers, OpenMoji avatars, Completion rings, Streaks. |
| **Tasks & NLP Engine** | **Complete** | Natural language recurrence (`repeat`/`every`), Spaced times, Live copper badge syntax highlighting, Progress card, Filter chips, Swipe gestures, Priority alerts for High & Medium. |
| **Priority Overlays** | **Complete** | System-wide floating alert (`TYPE_APPLICATION_OVERLAY`) for Medium and High tasks, Lock screen activity, 6-option quick snooze, Auto-rescheduling. |
| **Focus & Pomodoro** | **Complete** | 3D hero sphere, Pinned Start button, Classic Pomodoro, Timed Flow, Open Flow, Streamlined early stop flow with actual duration saved, App blocker overlay, 52-week activity heatmap, Synthesized tick (`f_tick`). |
| **Agenda Calendar** | **Complete** | Smart empty-day filtering, On-demand date selection, Device calendar merge, Dual permissions (read/write), Direct in-app event creation, Live `ContentObserver` instant sync. |
| **Frosted Glass Tab Bar** | **Complete** | 72dp stadium pill, Haze hardware backdrop blur (24dp radius), 24dp elevated float, 5 core tab destinations. |
| **Settings & Diagnostics** | **Complete** | User Profile header (Thunder fallback, 48dp camera touch target, Name/Bio), 9-screen native settings, Live permissions hub (Overlay, Tri-state Calendar, Channels, Alarms, Battery), Animated 3-way theme switcher, Non-destructive tour replay. |
| **Data & Backup** | **Complete** | Transactional Room import with validation & rollback (Schema v4), Instant storage stats wipe, Alarm rescheduling post-import/reboot, WorkManager 7-day auto-backup (`d_auto`) with 4-backup rotation, "Back up now" trigger. |
| **Design System v1.0** | **Complete** | Obsidian dark & warm paper light, Lora typography with tabular numbers, All radii $\ge 16\text{dp}$, Universal sentence case. |

