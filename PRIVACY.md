# Privacy Policy for Reflex

**Last Updated**: October 2, 2026

At Reflex, privacy is not a policy setting or an afterthought—it is the foundational architecture of the application.

---

## 1. Zero Network Access
Reflex does not request or contain the `android.permission.INTERNET` permission in its Android manifest. The application cannot connect to any server, transmit metrics, upload crashes, or communicate across any network interface.

## 2. No Analytics, Tracking, or Advertising
- There are **no tracking SDKs** (e.g. Google Analytics, Firebase, Mixpanel).
- There are **no crash reporting SDKs** (e.g. Crashlytics, Sentry).
- There are **no advertisements** or ad frameworks.
- There are **no user profiling** mechanisms.

## 3. Local Data Storage
All data you enter into Reflex—including routines, steps, tasks, habit logs, focus sessions, user name, bio, and profile pictures—is stored exclusively on your local device in a private SQLite database managed via Android Room and internal app storage.

## 4. Permissions & On-Device Processing
Reflex may request certain permissions to provide core device functionality:
- **Notifications & Alarms**: Used locally to trigger alerts when a timer completes or a task is due.
- **Usage Access & Overlay Window**: Used entirely on-device to detect foreground applications during focus sessions and render the distraction block overlay. No app usage history is recorded, stored, or transmitted.
- **Calendar Access**: When granted, reads and writes calendar events directly with Android's on-device `CalendarContract`. Event details never leave your device.

## 5. Backups and Data Exports
When you use the **Export Data** or **Auto-Backup** features in Settings:
- Reflex writes your data into an unencrypted, human-readable `.json` file at a local storage location you choose via Android's Storage Access Framework.
- You have complete control over this file. Reflex does not sync or upload this file anywhere.

## 6. Feedback & Support
When you tap **Send Feedback** or **Report an Issue** in Settings:
- The app generates a draft email using an `ACTION_SENDTO` intent that opens your installed email client.
- A pre-filled draft containing basic diagnostic information (such as app version `1.0.0` and Android OS version) is presented to you.
- You can inspect, modify, or delete any content before sending the email manually.
- Emails are delivered to `reflexhelpdesk.unworried192@simplelogin.com`.

## 7. Open Source Verification
Reflex is 100% free and open-source software under the GPL-3.0-or-later license. You can inspect the complete source code, audit dependencies, and build the exact binary yourself on [GitHub](https://github.com/bobby-99/reflex).

---

## Contact
If you have questions regarding this policy:
- **Email**: `reflexhelpdesk.unworried192@simplelogin.com`
- **GitHub**: [github.com/bobby-99/reflex](https://github.com/bobby-99/reflex)
