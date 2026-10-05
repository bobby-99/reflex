# Reflex v1.0.1 Test Report & Quality Audit

This document details the test suite built for Reflex v1.0.1, real bugs discovered during testing, their root causes, and verification across Unit, Integration, Instrumented, and Release pipelines.

---

## 1. Test Suite Architecture

| Test Layer | Framework / Tools | Scope & Coverage |
| :--- | :--- | :--- |
| **Unit Tests** | JUnit 4, Kotlin Coroutines Test, Turbine, Injected `java.time.Clock` | `TaskParser`, `StreakCalculator`, `RecurrenceCalculator`, `FocusSessionLogic`, Database Migrations 4-14, Backup Resilience & Round-Trip |
| **Database Migrations** | Room Migration Test, SQLite Proxy | Schema migrations from v4 through v14, column alterations, foreign keys, table creations |
| **Backup Resilience** | JSON validation, schema degradation | Corrupted/truncated JSON, 5,000-task payloads, unknown forward-compatible fields, v1 legacy backups |
| **Connected UI / Instrumented** | AndroidJUnit4, ActivityScenario, Device Execution (`motorola edge 70 fusion`, Android 16 API 36) | Lifecycle recreation (rotation), Alarm scheduling & cancellation, BootReceiver, bulk data queries, concurrency stress |
| **Stress & Robustness** | ADB Monkey (2,000 touch/motion events), bulk Room insertions | Memory stability, cold start, zero ANRs, zero crashes |
| **Release Verification** | R8 Minification, Resource Shrinking, Lint Vital | `assembleRelease` with full optimization and dead-code stripping |
| **CI / CD Pipeline** | GitHub Actions (`.github/workflows/ci.yml`) | Automated unit tests and headless Android emulator instrumented test execution |

---

## 2. Real Bugs Found and Resolved

### Bug 1: `BootReceiver` NullPointerException on Non-Framework Broadcast Dispatch
- **Cause**: In `BootReceiver.kt`, `goAsync()` returns `null` when invoked outside of active system broadcast dispatch (e.g. during test runs or custom intents). Line 26 invoked `pendingResult.finish()` directly, crashing the host process with a fatal NPE.
- **Fix**: Replaced with null-safe invocation `pendingResult?.finish()`.

### Bug 2: Race Condition in Rapid/Concurrent Completion of Repeating Tasks
- **Cause**: When a repeating task was tapped rapidly in succession or completed concurrently across threads, SQLite reads both saw `isCompleted == false` before the first transaction finished. Both calls scheduled duplicate recurring tasks and duplicate alarms.
- **Fix**: Implemented atomic compare-and-swap query in `TaskDao` (`markCompletedAtomic`) returning the count of rows updated. Only the winning atomic execution creates the next recurrence and schedules its alarm.

### Bug 3: Active Focus Session Lost Upon Mid-Session Process Death
- **Cause**: `FocusTimerService` held active timer state purely in-memory. If Android killed the process during a session due to memory pressure or app termination, the user's focus minutes were lost permanently.
- **Fix**: Added active session snapshot persistence to local SharedPreferences updated during timer ticks. Upon launch in `ReflexApplication.onCreate`, `recoverInterruptedSession` automatically detects interrupted sessions ($\ge 60$s) and records them with `endReason = "stopped_early"` so focus history is preserved.

### Bug 4: `TaskParser` Exclamation Marks Inside Words Parsed as Priority
- **Cause**: Using `contains("!")` or `(?<!\w)!(?!\w)` caused words like `foo!!!bar` and `file!name.txt` to be stripped and classified as Priority markers because `!` itself is a non-word character (`\W`).
- **Fix**: Developed boundary regex `(?<=\s|^)!{1,3}(?=\s|$)|(?<=[a-zA-Z0-9])!{1,3}(?=\s|$)` that strictly requires whitespace or token-end boundaries, leaving embedded exclamations untouched.

### Bug 5: Multi-Priority Strings Downgrading Highest Priority
- **Cause**: If an input string contained both high and lower priority markers (e.g. `urgent submit low priority paperwork`), later sequential regex checks unconditionally overwrote `detectedPriority`.
- **Fix**: Implemented `maxOfPriority(a, b)` rank comparison (`HIGH > MEDIUM > LOW > NONE`), guaranteeing highest priority always takes precedence.

### Bug 6: Explicit Calendar Dates Overwritten by Generic Relative Terms
- **Cause**: When parsing phrases like `"Flight on 15 Aug booked today"`, explicit calendar date parser recognized `15 Aug`, but later generic token loops for `"today"` unconditionally overwrote `detectedDate`.
- **Fix**: Added null-checks (`if (detectedDate == null)`) before generic date tokens can claim the date.

### Bug 7: Recurrence Drift for Month-End Tasks (`Jan 31 + 1 Month`)
- **Cause**: A monthly task due Jan 31 rolled over to Feb 28. On the next cycle, `dueDate` (Feb 28) $+ 1$ month produced March 28 instead of returning to March 31.
- **Fix**: Preserved the original day-of-month (`createdAt.dayOfMonth`), clamped to `minOf(originalDayOfMonth, YearMonth.lengthOfMonth())`, so March returns to March 31 and leap day Feb 29 returns on leap years.

### Bug 8: Late Completion of Repeating Tasks Generating Overdue Occurrences
- **Cause**: When completing a daily task 4 days overdue under `FROM_DUE_DATE`, it produced `dueDate + 1 day`, which was still 3 days in the past.
- **Fix**: Implemented automatic cycle roll-forward so newly scheduled recurring tasks are always scheduled on or after the completion date.

### Bug 9: Schedule-Aware Streak Double-Counting Grace Days
- **Cause**: In `StreakCalculator`, a single completion on Tuesday counted as both on-time for Tuesday and as a late grace completion for missed Monday.
- **Fix**: Restricted grace day credit on scheduled days to only apply if the next day was an off-day or had more than one completed log.

### Bug 10: Uncanceled Alarms After Repository Data Wipe
- **Cause**: `ReflexRepository.wipeAllData()` cleared database tables but lacked the Context reference to cancel active alarms in `AlarmManager`.
- **Fix**: Extended `wipeAllData(context)` to invoke `AlarmScheduler.cancelAllAlarms(context)` before clearing Room tables.

---

## 3. Test Verification Results

### Unit Tests
```
Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 30s
95 tests executed, 0 failed, 0 skipped
```

### Connected Instrumented Tests (Physical Device: motorola edge 70 fusion, Android 16 API 36)
```
Task :app:connectedDebugAndroidTest
Starting 5 tests on motorola edge 70 fusion - 16
Finished 5 tests on motorola edge 70 fusion - 16
BUILD SUCCESSFUL in 1m 16s
5 tests executed, 0 failed, 0 skipped
```

### Stress & Monkey Testing
- **1,000-Task Bulk Room Insertion**: < 5,000ms budget ($\approx 1,200\text{ms}$ actual).
- **Incomplete Tasks Query**: < 500ms budget ($\approx 18\text{ms}$ actual).
- **Monkey Event Injection**: 2,000 rapid touch, motion, and navigation events injected via ADB with **0 crashes** and **0 ANRs**.

### Minified Release Build (R8)
```
Task :app:assembleRelease
> Task :app:minifyReleaseWithR8
> Task :app:optimizeReleaseResources
> Task :app:packageRelease
BUILD SUCCESSFUL in 6m 32s
```
- Full ProGuard / R8 code optimization verified.
- Unused code and dead resources stripped.
- Lint vital check passed with 0 fatal errors.
