package com.reflex.app.data

/**
 * Data structures defining the schema SC from reflex-settings.html.
 */
enum class SettingItemType {
    TG, // Switch / Toggle
    SG, // Segmented Pills
    ST, // Stepper (- / +)
    LN, // Link / Action row with chevron
    IN, // Info row (label + value)
    PM, // Permission status pill
    DB  // Destructive Danger button
}

data class SettingItem(
    val type: SettingItemType,
    val key: String? = null,
    val label: String,
    val description: String = "",
    val options: List<String> = emptyList(),
    val defaultBool: Boolean = false,
    val defaultString: String = "",
    val defaultInt: Int = 0,
    val min: Int = 0,
    val max: Int = 100,
    val step: Int = 1,
    val unit: String = "",
    val fmt: String = "", // e.g. "hr"
    val value: String = "",
    val toast: String = "",
    val targetScreenId: String? = null
)

data class SettingGroup(
    val heading: String,
    val caption: String? = null,
    val items: List<SettingItem>
)

data class SettingScreenDef(
    val id: String,
    val title: String,
    val iconName: String,
    val groups: List<SettingGroup>
)

object SettingsSchema {

    fun formatHour(h: Int): String {
        val hour12 = if (h % 12 == 0) 12 else h % 12
        val ampm = if (h < 12) "am" else "pm"
        return "$hour12 $ampm"
    }

    val SC: Map<String, SettingScreenDef> = mapOf(
        "routines" to SettingScreenDef(
            id = "routines",
            title = "Routines",
            iconName = "routines",
            groups = listOf(
                SettingGroup(
                    heading = "Reminders",
                    caption = "How routine reminders reach you",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "r_lead",
                            label = "Reminder lead time",
                            description = "When to alert before a routine starts",
                            options = listOf("At time", "5 min", "15 min"),
                            defaultString = "At time"
                        ),
                        SettingItem(
                            type = SettingItemType.ST,
                            key = "r_snooze",
                            label = "Snooze length",
                            description = "Minutes before a snoozed reminder returns",
                            min = 5,
                            max = 30,
                            step = 5,
                            unit = "min",
                            defaultInt = 10
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "r_snd",
                            label = "Sound on reminders",
                            description = "Audio cue when step finishes",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "r_vib",
                            label = "Vibrate on reminders",
                            description = "Haptic feedback with the alert",
                            defaultBool = true
                        )
                    )
                ),
                SettingGroup(
                    heading = "Timer",
                    caption = "Step transitions and rest breaks",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "r_auto",
                            label = "Auto-advance steps",
                            description = "Advance to the next step when a timer ends",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "r_rest",
                            label = "Default rest",
                            description = "Rest break between steps",
                            options = listOf("Off", "10s", "15s", "20s", "30s"),
                            defaultString = "15s"
                        )
                    )
                ),
                SettingGroup(
                    heading = "Schedule",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "r_cal",
                            label = "Show routines in the calendar",
                            description = "Display routine schedules on their weekdays",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "r_hol",
                            label = "Pause routines on holidays",
                            description = "Uses your holiday calendar",
                            defaultBool = false
                        )
                    )
                )
            )
        ),
        "calendar" to SettingScreenDef(
            id = "calendar",
            title = "Calendar",
            iconName = "calendar",
            groups = listOf(
                SettingGroup(
                    heading = "Agenda filters",
                    caption = "Control what appears on the chronological timeline",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "c_rep",
                            label = "Exclude repeating tasks",
                            description = "Hide recurring task instances to keep the agenda uncluttered",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "c_done",
                            label = "Exclude completed tasks",
                            description = "Hide finished tasks from the calendar agenda",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "c_rout",
                            label = "Show routine reminders",
                            description = "Display routine schedules on their respective weekdays",
                            defaultBool = true
                        )
                    )
                ),
                SettingGroup(
                    heading = "View preferences",
                    caption = "Customize calendar range and layout behavior",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "c_range",
                            label = "Agenda range",
                            description = "How many days ahead the agenda displays",
                            options = listOf("7d", "14d", "30d", "60d"),
                            defaultString = "60d"
                        ),
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "c_first",
                            label = "First day of week",
                            description = "Starting day for week strips and monthly grids",
                            options = listOf("Monday", "Sunday"),
                            defaultString = "Monday"
                        ),
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "c_land",
                            label = "Default landing view",
                            description = "Initial view when opening the Calendar tab",
                            options = listOf("Week strip & agenda", "Expanded month"),
                            defaultString = "Week strip & agenda"
                        )
                    )
                ),
                SettingGroup(
                    heading = "Device calendars",
                    caption = "Include events from your calendar accounts",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "choose_calendars",
                            label = "Choose calendars",
                            description = "Grouped by account, with colors",
                            value = "3 of 9",
                            toast = "Opens the calendar picker"
                        )
                    )
                )
            )
        ),
        "tasks" to SettingScreenDef(
            id = "tasks",
            title = "Tasks",
            iconName = "tasks",
            groups = listOf(
                SettingGroup(
                    heading = "Quick add",
                    caption = "Capture tasks in a few taps",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "t_nlp",
                            label = "Natural language parsing",
                            description = "Pick up dates, times, priority and repeats as you type",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "t_keep",
                            label = "Keep quick add open",
                            description = "Stay on the card after adding, for rapid entry",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "t_pri",
                            label = "Default priority",
                            description = "Used when you do not set one",
                            options = listOf("None", "Low", "Medium", "High"),
                            defaultString = "None"
                        )
                    )
                ),
                SettingGroup(
                    heading = "Reminders",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "t_rem",
                            label = "Default reminder",
                            description = "For tasks with a due time",
                            options = listOf("None", "At due time", "10 min", "1 hour"),
                            defaultString = "At due time"
                        )
                    )
                ),
                SettingGroup(
                    heading = "List",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "t_prog",
                            label = "Show daily progress card",
                            description = "The \"done today\" summary above the list",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "t_hide",
                            label = "Auto-hide completed",
                            description = "Move finished tasks out of the main list after",
                            options = listOf("Instantly", "1 day", "7 days", "Never"),
                            defaultString = "Instantly"
                        )
                    )
                )
            )
        ),
        "habits" to SettingScreenDef(
            id = "habits",
            title = "Habits",
            iconName = "habits",
            groups = listOf(
                SettingGroup(
                    heading = "Tracking",
                    caption = "How habits are counted",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.SG,
                            key = "h_type",
                            label = "Default habit type",
                            description = "Pre-selected when you create a habit",
                            options = listOf("Check-off", "Measurable", "Limit"),
                            defaultString = "Check-off"
                        ),
                        SettingItem(
                            type = SettingItemType.ST,
                            key = "h_roll",
                            label = "Day ends at",
                            description = "Late-night check-ins count for the previous day until this time",
                            min = 0,
                            max = 6,
                            step = 1,
                            fmt = "hr",
                            defaultInt = 3
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "h_grace",
                            label = "Streak grace day",
                            description = "Allow one missed day per week without breaking a streak",
                            defaultBool = false
                        )
                    )
                ),
                SettingGroup(
                    heading = "Reminders",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "h_nudge",
                            label = "Evening nudge",
                            description = "A gentle reminder if habits are still open",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.ST,
                            key = "h_nt",
                            label = "Nudge time",
                            description = "",
                            min = 17,
                            max = 23,
                            step = 1,
                            fmt = "hr",
                            defaultInt = 20
                        )
                    )
                ),
                SettingGroup(
                    heading = "Insights",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "h_work",
                            label = "Show habits to work on",
                            description = "Highlight habits you often leave behind",
                            defaultBool = true
                        )
                    )
                )
            )
        ),
        "focus" to SettingScreenDef(
            id = "focus",
            title = "Focus",
            iconName = "focus",
            groups = listOf(
                SettingGroup(
                    heading = "Alerts",
                    caption = "What happens when a phase ends",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_snd",
                            label = "Sound alerts",
                            description = "Play notification tone on phase end",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_vib",
                            label = "Vibration alert",
                            description = "Haptic feedback on phase end",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_auto",
                            label = "Auto-start next phase",
                            description = "Automatically continue without tapping start",
                            defaultBool = false
                        )
                    )
                ),
                SettingGroup(
                    heading = "Session",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_keep",
                            label = "Keep screen on",
                            description = "Prevent the display from sleeping during a session",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_tick",
                            label = "Ambient tick sound",
                            description = "Subtle sound while the focus timer is running",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_q",
                            label = "Show session quotes",
                            description = "A quiet line of text during focus phases",
                            defaultBool = true
                        )
                    )
                ),
                SettingGroup(
                    heading = "App blocking",
                    caption = "Restrict distracting apps during focus phases",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "f_blk",
                            label = "Enable app blocking",
                            description = "Pauses automatically during breaks",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "configure_blocking",
                            label = "Configure app blocking",
                            description = "Choose which apps to restrict",
                            toast = "Opens app blocking setup"
                        )
                    )
                )
            )
        ),
        "notifications" to SettingScreenDef(
            id = "notifications",
            title = "Notifications",
            iconName = "bell",
            groups = listOf(
                SettingGroup(
                    heading = "Channels",
                    caption = "Choose what can notify you",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "n_task",
                            label = "Task reminders",
                            description = "Due dates and times",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "n_rout",
                            label = "Routine alerts",
                            description = "Scheduled routine reminders",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "n_hab",
                            label = "Habit nudges",
                            description = "Evening check-in reminder",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "n_foc",
                            label = "Focus phase alerts",
                            description = "Session and break endings",
                            defaultBool = true
                        )
                    )
                ),
                SettingGroup(
                    heading = "Quiet hours",
                    caption = "Silence non-urgent alerts",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "q_on",
                            label = "Enable quiet hours",
                            description = "Alarms you set yourself still ring",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.ST,
                            key = "q_from",
                            label = "Starts at",
                            description = "",
                            min = 18,
                            max = 23,
                            step = 1,
                            fmt = "hr",
                            defaultInt = 23
                        ),
                        SettingItem(
                            type = SettingItemType.ST,
                            key = "q_to",
                            label = "Ends at",
                            description = "",
                            min = 5,
                            max = 10,
                            step = 1,
                            fmt = "hr",
                            defaultInt = 7
                        )
                    )
                ),
                SettingGroup(
                    heading = "Daily summary",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "n_sum",
                            label = "Morning summary",
                            description = "Tasks, routines and habits for the day",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.ST,
                            key = "n_st",
                            label = "Send at",
                            description = "",
                            min = 5,
                            max = 11,
                            step = 1,
                            fmt = "hr",
                            defaultInt = 8
                        )
                    )
                )
            )
        ),
        "permissions" to SettingScreenDef(
            id = "permissions",
            title = "Permissions and access",
            iconName = "shield",
            groups = listOf(
                SettingGroup(
                    heading = "What Reflex can use",
                    caption = "Everything stays on this device",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_not",
                            label = "Notifications",
                            description = "Reminders, alarms and session alerts",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_alm",
                            label = "Exact alarms",
                            description = "Uses alarm clock scheduling, no permission needed",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_cal",
                            label = "Calendar",
                            description = "Show device calendar events in the agenda",
                            defaultBool = true
                        ),
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_mic",
                            label = "Microphone",
                            description = "Voice input in quick add",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_blk",
                            label = "App blocking access",
                            description = "Detect and restrict distracting apps",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_ovr",
                            label = "Display over other apps",
                            description = "Show priority task alerts and app blocker overlays",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.PM,
                            key = "p_bat",
                            label = "Battery unrestricted",
                            description = "Keeps timers and background services alive",
                            defaultBool = true
                        )
                    )
                )
            )
        ),
        "data" to SettingScreenDef(
            id = "data",
            title = "Backup and data",
            iconName = "db",
            groups = listOf(
                SettingGroup(
                    heading = "Backup",
                    caption = "Your data lives only on this device",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "export",
                            label = "Export backup",
                            description = "Save tasks, habits, routines and settings as JSON",
                            toast = "Would open the file picker to save a JSON export"
                        ),
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "import",
                            label = "Import backup",
                            description = "Restore from a Reflex JSON file",
                            toast = "Would open the file picker to choose a backup"
                        ),
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "last_backup",
                            label = "Last backup",
                            value = "Never"
                        ),
                        SettingItem(
                            type = SettingItemType.TG,
                            key = "d_auto",
                            label = "Weekly auto-backup",
                            description = "Save a copy to a folder you choose every Sunday",
                            defaultBool = false
                        ),
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "backup_now",
                            label = "Back up now",
                            description = "Immediately save a backup to your chosen folder"
                        )
                    )
                ),
                SettingGroup(
                    heading = "Storage",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "storage",
                            label = "Stored on this device",
                            value = "Calculating..."
                        ),
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "db_stats",
                            label = "Database records",
                            value = "Calculating..."
                        ),
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "network",
                            label = "Network use",
                            value = "None (zero permissions)"
                        )
                    )
                ),
                SettingGroup(
                    heading = "Danger zone",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.DB,
                            key = "reset_all",
                            label = "Reset all data",
                            description = "Deletes everything on this device. This cannot be undone.",
                            toast = "Would ask you to confirm first"
                        )
                    )
                )
            )
        ),
        "about" to SettingScreenDef(
            id = "about",
            title = "About Reflex",
            iconName = "info",
            groups = listOf(
                SettingGroup(
                    heading = "Feedback & Support",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "feedback",
                            label = "Send feedback / report bugs",
                            description = com.reflex.app.util.AppConstants.FEEDBACK_EMAIL,
                            value = ""
                        ),
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "github_issues",
                            label = "Report an issue on GitHub",
                            description = "Open issue tracker in browser"
                        )
                    )
                ),
                SettingGroup(
                    heading = "App",
                    items = listOf(
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "version",
                            label = "Version",
                            value = com.reflex.app.BuildConfig.VERSION_NAME
                        ),
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "license",
                            label = "License",
                            value = "GPL-3.0 (No warranty)"
                        ),
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "source_code",
                            label = "Source code",
                            description = "View repository on GitHub"
                        ),
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "typeface",
                            label = "Typeface",
                            value = "Lora"
                        ),
                        SettingItem(
                            type = SettingItemType.IN,
                            key = "privacy",
                            label = "Privacy",
                            value = "Fully offline (no network)"
                        ),
                        SettingItem(
                            type = SettingItemType.LN,
                            key = "licenses",
                            label = "Open-source licenses",
                            toast = "Opens the licenses list"
                        )
                    )
                )
            )
        )
    )
}
