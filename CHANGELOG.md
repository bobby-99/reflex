# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.3] - 2026-10-06

### Added
- **Expanded Routine Presets**: Added 5 new production presets (Deep Work Launch, Desk Break, Mobility Flow, Evening Shutdown, Weekly Review) alongside the original 5, featuring schedule-aware streaks (daily, weekdays, Sunday) and accurate dynamic duration calculations.
- **In-App Community Support**: Added warm, low-pressure "Star on GitHub" and "Support on Ko-fi" links under Settings → About, fully preserving the 100% offline guarantee without internet permissions.
- **Live Search Indexing**: Settings search now indexes "support", "donate", "tip", "star", "github", and "ko-fi".

### Changed
- **Website & Showcase**: High-res Play Store app icon adopted for favicon, metadata, and GitHub README; website phone preview navigation bar polished with real-time frosted glass backdrop blur matching the app.
- **CI / CD Pipeline**: Modernized GitHub Actions CI test matrix to run rock-solid automated instrumented tests without hypervisor emulation errors.

---

## [1.0.1] - 2026-10-05

### Added
- **Habit Customization & Custom Pickers**: Custom frequency (daily, specific weekdays, times per week), reminder schedules via AlarmManager, quantifiable targets, start/end dates, and custom tactile Reflex date & time picker dialogs.
- **Priority Task Alerts**: Full-screen floating overlay alert extended to both High and Medium priority tasks across home and third-party apps with 6 snooze chips.
- **Real-Time Calendar Sync**: Instant reflection for created/edited device calendar events via optimistic UI updates and live `ContentObserver` on `CalendarContract.Events.CONTENT_URI`.
- **Dual Calendar Permission Flow**: Combined `READ_CALENDAR` and `WRITE_CALENDAR` permission launcher with first-visit guidance toast and seamless event creation.
- **Deep Notification Navigation**: Tapping task, habit, routine, or focus notifications routes directly to that specific section or item.
- **NLP Input Highlighting**: Real-time soft-cornered copper badge highlighting for parsed NLP components (dates, times, priorities, recurrence) in Quick Add.
- **Instant Storage Reset**: Immediately reflects zero database records (`0 routines · 0 tasks · 0 habits · 0 focus`) and recalculated disk usage upon data wipe, with full SQLite WAL checkpointing.

### Changed
- **Focus Session End Flow**: Manual session end skips completion animation, preserves actual focused duration in history, analytics, and streaks, displays a dedicated "Session stopped early" summary screen, and adds an optional discard action.
- **Room Migration**: Single clean migration (v13 to v14) covering habit fields and session `end_reason` while preserving all existing user records.
- **Export / Import**: Bumped schema to v4 with backward compatibility for v1–v3 exports.
- **Documentation & Showcase**: Overhauled README with Orion Store aesthetic, horizontal scrollable screenshot shelf, and cleaned documentation hierarchy.

### Fixed
- **Onboarding Tour**: Dismiss virtual keyboard and clear focus automatically when advancing pages or submitting name input.
- **UI & Layout**: Responsive bottom navigation bar padding, settings layout adaptation for varying screen widths, and support screen text layout fix.
- **Calendar Lifecycle**: Automatically re-checks calendar permissions and reloads device events on `ON_RESUME`.

---

## [1.0.0] - 2026-10-02

### Added
- **Initial Open-Source Release** under GNU General Public License v3.0 (GPL-3.0-or-later).
- **Routines Engine**: Sequential step timer, audio cues, confetti effects, and pre-built templates.
- **Tasks & Priority Management**: Smart tasks with due dates, exact-time reminders, priority tags (P1-P4), and recurrence rules.
- **Habit Tracker**: Daily checkbox and numeric target tracking with annual activity heatmaps and consistency analytics.
- **Deep Focus Mode**: Pomodoro, interval, and open flow focus timer with ambient ticks, audio bells, and background service notification.
- **Distraction Shield (App Blocker)**: Optional distraction blocking with full-screen overlay windowing.
- **Local Calendar Integration**: Unified timeline linking routines, scheduled tasks, and local device calendar events via `CalendarContract`.
- **Data Sovereignty & Backups**: Local JSON export and import, plus weekly automated backups to a user-specified SAF directory.
- **Aesthetic Themes**: 6 customizable color themes (Obsidian, Warm Amber, Slate Dusk, Emerald, Rose Gold, Midnight) built on Design System v1.0.
- **Attribution & Licenses Screen**: In-app viewer and `THIRD_PARTY_LICENSES.md` detailing all open-source dependencies and assets.
