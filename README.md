# Reflex

<p align="center">
  <strong>Calm, local-first productivity for Android.</strong><br>
  Routines &bull; Tasks &bull; Habits &bull; Focus &bull; App Blocking &bull; Calendar
</p>

<p align="center">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="License: GPL-3.0-or-later"></a>
  <a href="https://github.com/bobby-99/reflex/releases"><img src="https://img.shields.io/badge/Release-1.0.0-emerald.svg" alt="Latest Release"></a>
  <img src="https://img.shields.io/badge/Offline-100%25%20Local-success.svg" alt="100% Local">
  <img src="https://img.shields.io/badge/Tracking-Zero%20Analytics-red.svg" alt="Zero Analytics">
</p>

---

## Overview

**Reflex** is an intentional, privacy-respecting productivity assistant built entirely in Kotlin and Jetpack Compose. Designed around tactile design, soft frosted glass aesthetics, and strict offline principles, Reflex combines routine execution, smart task organization, habit tracking, deep focus timers with optional distraction shielding, and calendar integration into one cohesive experience.

### Why Reflex?
- **100% Local-First**: No servers, no accounts, no cloud sync, no tracking. All your data lives solely in a local Room SQLite database on your device.
- **Zero Network Permission**: The app does not request or contain `android.permission.INTERNET`. It is physically incapable of transmitting your data over the internet.
- **Design System v1.0**: Built with bespoke typography (Lora), tactile micro-interactions, custom spring physics, and subtle frosted blur materials.
- **Full Data Sovereignty**: Export and import your entire workspace as unencrypted, human-readable JSON files whenever you wish.

---

## Features

### 🌅 Routines
- Step-by-step sequential routine runner with timer support.
- Custom recurrence rules (daily, weekly, specific days).
- Audio chimes, celebratory confetti, and completion summaries.
- Pre-built morning, evening, and work-shutdown starter routines.

### 📅 Calendar & Timeline
- Unified daily schedule integrating routines, timed tasks, and device calendars.
- Bi-directional local sync with Android's native Calendar Provider (`READ_CALENDAR` / `WRITE_CALENDAR`).
- Full offline privacy: device calendar events stay on your phone.

### ✅ Smart Tasks
- Priority scoring (P1 urgent to P4 casual).
- Natural recurrence support (daily, weekly, weekdays, custom intervals).
- Exact-time reminder notifications with Doze mode immunity.
- Full-screen priority alerts for critical commitments.

### 📈 Habits & Heatmaps
- Daily checkboxes and numeric target counters (e.g. glasses of water, pages read).
- Yearly contribution heatmaps, streak counts, and consistency analysis.
- Monthly snapshot performance reports.

### ⏳ Focus Timer & Distraction Shielding
- Pomodoro, short/long intervals, and open flow sessions.
- Ambient ticking cues and audio completions.
- **App Blocking**: Restrict distracting apps during active focus sessions using Android Usage Stats and Overlay windowing.

### ⚙️ Settings, Themes & Backups
- 6 curated aesthetic themes: Obsidian, Warm Amber, Slate Dusk, Emerald, Rose Gold, Midnight.
- Live local storage statistics breakdown (database files, item counts, cache).
- Automated weekly backups to a user-selected local directory with retention pruning.
- Full backup export/import via Android Storage Access Framework (SAF).

---

## Permissions Breakdown

Reflex requests only the permissions necessary to provide local device automation. It contains **no network permissions**.

| Permission | Purpose |
| :--- | :--- |
| `RECEIVE_BOOT_COMPLETED` | Reschedules scheduled task and routine alarms after device reboot. |
| `POST_NOTIFICATIONS` | Delivers local task reminders, routine alerts, and active timer notifications (Android 13+). |
| `SCHEDULE_EXACT_ALARM` | Schedules exact-time reminders so notifications arrive precisely on time. |
| `USE_EXACT_ALARM` | Ensures alarms fire reliably even during battery saver / Doze modes. |
| `FOREGROUND_SERVICE` | Keeps the countdown timer active and visible while running focus or routine sessions. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Complies with Android 14 foreground service type requirements for countdown timers. |
| `VIBRATE` | Provides haptic feedback for timers, completions, and alerts. |
| `PACKAGE_USAGE_STATS` *(Optional)* | Detects foreground package changes to trigger the distraction shield during focus sessions. |
| `SYSTEM_ALERT_WINDOW` *(Optional)* | Displays the blocking screen when opening a blocked app during focus mode. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` *(Optional)* | Prevents aggressive OEM background cleaners from killing active timers. |
| `READ_CALENDAR` / `WRITE_CALENDAR` *(Optional)* | Reads and writes events to your local device calendar. |

---

## Installation

### GitHub Releases
Download the latest APK from the [Releases](https://github.com/bobby-99/reflex/releases) page.

### F-Droid
*F-Droid package submission in progress.*

> [!IMPORTANT]
> **Switching Installation Sources**: APKs built and signed by GitHub Releases and F-Droid use different signing keys. If you switch between GitHub and F-Droid builds, Android requires you to uninstall the old version first. **Always perform a backup in Settings > Export Data before switching sources.**

---

## Building from Source

### Prerequisites
- **JDK**: Java 17 or higher
- **Android SDK**: API 35 (Android 15) build tools
- **Android Studio**: Ladybug / Meerkat or newer (optional, for IDE development)

### Build Commands

Clone the repository:
```bash
git clone https://github.com/bobby-99/reflex.git
cd reflex
```

Build debug APK:
```bash
./gradlew assembleDebug
```

Build unsigned release APK:
```bash
./gradlew assembleRelease
```

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

---

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) (100%)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) & Material 3
- **Architecture**: MVVM with Unidirectional Data Flow (StateFlow / SharedFlow)
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) & Jetpack DataStore Preferences
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) & Exact `AlarmManager`
- **Effects & UI**: [Haze](https://github.com/chrisbanes/haze) (frosted glass blurs), [OpenMoji](https://openmoji.org/) (visual icons)
- **Typography**: [Lora](https://fonts.google.com/specimen/Lora) (SIL OFL 1.1)

---

## Contributing

Contributions, bug reports, and feature requests are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) and our [Code of Conduct](CODE_OF_CONDUCT.md) before submitting pull requests.

### Core Philosophy for Contributions
1. **Zero Network / No Telemetry**: PRs introducing network dependencies, cloud trackers, or proprietary analytics will be rejected.
2. **Design Integrity**: All UI elements must adhere to the Design System v1.0 specifications described in [DESIGN.md](DESIGN.md).

---

## Security & Privacy

Read our plain-language [Privacy Policy](PRIVACY.md) and [Security Policy](SECURITY.md).
To report a security vulnerability or bug privately:
- Contact: `reflexhelpdesk.unworried192@simplelogin.com`
- Or open an issue on [GitHub Issues](https://github.com/bobby-99/reflex/issues/new/choose).

*Reflex is an independent, solo-maintained project. Inquiries and contributions will be reviewed on a best-effort basis.*

---

## License

Reflex is free software released under the terms of the **GNU General Public License v3.0 or later (GPL-3.0-or-later)**. See [LICENSE](LICENSE) for details.

Third-party attributions and licenses are cataloged in [THIRD_PARTY_LICENSES.md](THIRD_PARTY_LICENSES.md).
