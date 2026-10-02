package com.reflex.app.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.reflex.app.data.BlockingMode
import com.reflex.app.data.SettingItemType
import com.reflex.app.data.SettingsSchema
import com.reflex.app.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek

enum class PermissionBadgeState {
    GRANTED,
    PARTIAL,
    DENIED
}

data class PermissionDetail(
    val state: PermissionBadgeState,
    val blockedChannelId: String? = null
)

data class AppStorageStats(
    val formattedStorageSize: String,
    val databaseSizeBytes: Long,
    val routineCount: Int,
    val taskCount: Int,
    val habitCount: Int,
    val focusSessionCount: Int
) {
    val formattedRecordSummary: String
        get() = "$taskCount tasks · $habitCount habits · $routineCount routines · $focusSessionCount focus"
}

object SettingsRepository {

    private const val TAG = "SettingsRepository"
    private const val PREFS_NAME = "reflex_app_settings"

    private val _values = MutableStateFlow<Map<String, Any>>(emptyMap())
    val values: StateFlow<Map<String, Any>> = _values.asStateFlow()

    private val _permissionStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val permissionStates: StateFlow<Map<String, Boolean>> = _permissionStates.asStateFlow()

    private val _permissionDetails = MutableStateFlow<Map<String, PermissionDetail>>(emptyMap())
    val permissionDetails: StateFlow<Map<String, PermissionDetail>> = _permissionDetails.asStateFlow()

    private val _storageStats = MutableStateFlow<AppStorageStats?>(null)
    val storageStats: StateFlow<AppStorageStats?> = _storageStats.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialMap = mutableMapOf<String, Any>()

        // Load default values from Schema
        SettingsSchema.SC.values.forEach { screenDef ->
            screenDef.groups.forEach { group ->
                group.items.forEach { item ->
                    val k = item.key ?: return@forEach
                    when (item.type) {
                        SettingItemType.TG -> {
                            initialMap[k] = prefs.getBoolean(k, item.defaultBool)
                        }
                        SettingItemType.SG -> {
                            initialMap[k] = prefs.getString(k, item.defaultString) ?: item.defaultString
                        }
                        SettingItemType.ST -> {
                            initialMap[k] = prefs.getInt(k, item.defaultInt)
                        }
                        SettingItemType.PM -> {
                            // Permission will be checked dynamically
                            initialMap[k] = item.defaultBool
                        }
                        else -> {}
                    }
                }
            }
        }

        // Bridge calendar preferences
        val calPrefs = CalendarPreferenceRepository.preferences.value
        initialMap["c_rep"] = calPrefs.excludeRepeatingTasks
        initialMap["c_done"] = calPrefs.excludeCompletedTasks
        initialMap["c_rout"] = calPrefs.showRoutineReminders
        initialMap["c_range"] = "${calPrefs.agendaRangeDays}d"
        initialMap["c_first"] = if (calPrefs.firstDayOfWeek == DayOfWeek.MONDAY) "Monday" else "Sunday"
        initialMap["c_land"] = if (calPrefs.defaultLandingView == "MONTH_GRID") "Expanded month" else "Week strip & agenda"

        // Bridge theme preference
        val themePref = ThemePreferenceRepository.currentTheme.value
        initialMap["theme"] = when (themePref) {
            AppTheme.SYSTEM -> "system"
            AppTheme.DARK -> "dark"
            AppTheme.LIGHT -> "light"
        }

        initialMap[KEY_FOCUS_DAILY_GOAL_MINS] = prefs.getInt(KEY_FOCUS_DAILY_GOAL_MINS, DEFAULT_FOCUS_DAILY_GOAL_MINS).coerceIn(MIN_FOCUS_DAILY_GOAL_MINS, MAX_FOCUS_DAILY_GOAL_MINS)

        _values.value = initialMap
        refreshPermissions(context)
    }

    fun refreshPermissions(context: Context) {
        // 1. Notifications: inspect overall permission and reminder channels on API 26+
        val notifManagerCompat = NotificationManagerCompat.from(context)
        val appNotifsEnabled = notifManagerCompat.areNotificationsEnabled()
        var blockedChannelId: String? = null
        val notifState: PermissionBadgeState = if (!appNotifsEnabled) {
            PermissionBadgeState.DENIED
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                val reminderChannels = listOf(
                    NotificationHelper.CHANNEL_TASK_REMINDERS,
                    NotificationHelper.CHANNEL_ROUTINE_REMINDERS,
                    NotificationHelper.CHANNEL_FOCUS_ALERTS,
                    NotificationHelper.CHANNEL_HABIT_NUDGES
                )
                var hasBlockedChannel = false
                if (nm != null) {
                    for (chId in reminderChannels) {
                        val ch = nm.getNotificationChannel(chId)
                        if (ch != null && ch.importance == NotificationManager.IMPORTANCE_NONE) {
                            hasBlockedChannel = true
                            blockedChannelId = chId
                            break
                        }
                    }
                }
                if (hasBlockedChannel) {
                    PermissionBadgeState.PARTIAL
                } else {
                    PermissionBadgeState.GRANTED
                }
            } else {
                PermissionBadgeState.GRANTED
            }
        }

        // 2. Exact alarms: AlarmScheduler uses setAlarmClock on API 23+, no permission needed
        val almState = PermissionBadgeState.GRANTED

        // 3. Calendar: tri-state checking READ_CALENDAR and WRITE_CALENDAR
        val hasReadCal = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val hasWriteCal = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
        val calState = when {
            hasReadCal && hasWriteCal -> PermissionBadgeState.GRANTED
            hasReadCal || hasWriteCal -> PermissionBadgeState.PARTIAL
            else -> PermissionBadgeState.DENIED
        }

        // 4. Microphone
        val micAllowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val micState = if (micAllowed) PermissionBadgeState.GRANTED else PermissionBadgeState.DENIED

        // 5. App blocking
        val blkAllowed = AppBlockPermissionHelper.hasUsageAccessPermission(context)
        val blkState = if (blkAllowed) PermissionBadgeState.GRANTED else PermissionBadgeState.DENIED

        // 6. Overlay (Display over other apps)
        val ovrAllowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        val ovrState = if (ovrAllowed) PermissionBadgeState.GRANTED else PermissionBadgeState.DENIED

        // 7. Battery unrestricted
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
        val batAllowed = powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
        val batState = if (batAllowed) PermissionBadgeState.GRANTED else PermissionBadgeState.DENIED

        val details = mapOf(
            "p_not" to PermissionDetail(notifState, blockedChannelId),
            "p_alm" to PermissionDetail(almState),
            "p_cal" to PermissionDetail(calState),
            "p_mic" to PermissionDetail(micState),
            "p_blk" to PermissionDetail(blkState),
            "p_ovr" to PermissionDetail(ovrState),
            "p_bat" to PermissionDetail(batState)
        )
        _permissionDetails.value = details

        val perms = mapOf(
            "p_not" to (notifState == PermissionBadgeState.GRANTED),
            "p_alm" to true,
            "p_cal" to (calState == PermissionBadgeState.GRANTED),
            "p_mic" to micAllowed,
            "p_blk" to blkAllowed,
            "p_ovr" to ovrAllowed,
            "p_bat" to batAllowed
        )
        _permissionStates.value = perms

        // Also merge into values
        val current = _values.value.toMutableMap()
        current.putAll(perms)
        _values.value = current
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString("user_name", null)
            ?: UserProfileRepository.profile.value.name.takeIf { it != "User" }
            ?: ""
    }

    fun setUserName(context: Context, name: String) {
        val trimmed = name.trim()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString("user_name", trimmed).apply()
        val current = _values.value.toMutableMap()
        current["user_name"] = trimmed
        _values.value = current

        // Also sync with UserProfileRepository
        val existingProfile = UserProfileRepository.profile.value
        val newProfile = existingProfile.copy(name = if (trimmed.isNotBlank()) trimmed else "User")
        UserProfileRepository.updateProfile(context, newProfile)
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return (_values.value[key] as? Boolean) ?: default
    }

    fun getString(key: String, default: String = ""): String {
        return (_values.value[key] as? String) ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return (_values.value[key] as? Int) ?: default
    }

    fun setBoolean(context: Context, key: String, value: Boolean) {
        val current = _values.value.toMutableMap()
        current[key] = value
        _values.value = current

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, value).apply()

        // Sync with calendar repository if applicable
        when (key) {
            "c_rep" -> CalendarPreferenceRepository.setExcludeRepeatingTasks(context, value)
            "c_done" -> CalendarPreferenceRepository.setExcludeCompletedTasks(context, value)
            "c_rout" -> CalendarPreferenceRepository.setShowRoutineReminders(context, value)
            "f_blk" -> {
                // Update focus settings blocking mode in database
                val app = context.applicationContext as? com.reflex.app.ReflexApplication
                if (app != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val settings = app.repository.getFocusSettingsSync()
                        val newMode = if (value) BlockingMode.BLOCK_LIST else BlockingMode.OFF
                        app.repository.saveFocusSettings(settings.copy(blockingMode = newMode))
                    }
                }
            }
            "f_snd", "f_vib", "f_auto" -> {
                val app = context.applicationContext as? com.reflex.app.ReflexApplication
                if (app != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val settings = app.repository.getFocusSettingsSync()
                        val updated = when (key) {
                            "f_snd" -> settings.copy(soundEnabled = value)
                            "f_vib" -> settings.copy(vibrationEnabled = value)
                            "f_auto" -> settings.copy(autoStartNextPhase = value)
                            else -> settings
                        }
                        app.repository.saveFocusSettings(updated)
                    }
                }
            }
        }
    }

    fun setString(context: Context, key: String, value: String) {
        val current = _values.value.toMutableMap()
        current[key] = value
        _values.value = current

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()

        when (key) {
            "c_range" -> {
                val days = value.replace("d", "").toIntOrNull() ?: 30
                CalendarPreferenceRepository.setAgendaRangeDays(context, days)
            }
            "c_first" -> {
                val day = if (value.equals("Sunday", ignoreCase = true)) DayOfWeek.SUNDAY else DayOfWeek.MONDAY
                CalendarPreferenceRepository.setFirstDayOfWeek(context, day)
            }
            "c_land" -> {
                val viewMode = if (value.contains("month", ignoreCase = true)) "MONTH_GRID" else "WEEK_STRIP"
                CalendarPreferenceRepository.setDefaultLandingView(context, viewMode)
            }
            "theme" -> {
                val theme = when (value) {
                    "dark" -> AppTheme.DARK
                    "light" -> AppTheme.LIGHT
                    else -> AppTheme.SYSTEM
                }
                ThemePreferenceRepository.setTheme(context, theme)
            }
        }
    }

    fun setInt(context: Context, key: String, value: Int) {
        val current = _values.value.toMutableMap()
        current[key] = value
        _values.value = current

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(key, value).apply()
    }

    fun stepInt(context: Context, key: String, delta: Int, min: Int, max: Int, step: Int) {
        val cur = getInt(key, min)
        val next = (cur + delta * step).coerceIn(min, max)
        setInt(context, key, next)
    }

    fun getSummary(screenId: String): String {
        return when (screenId) {
            "routines" -> {
                val lead = getString("r_lead", "At time").lowercase()
                val snooze = getInt("r_snooze", 10)
                "Lead time $lead · snooze $snooze min"
            }
            "calendar" -> {
                val land = getString("c_land", "Week strip & agenda")
                val range = getString("c_range", "60d")
                "$land · $range range"
            }
            "tasks" -> {
                val nlp = if (getBoolean("t_nlp", true)) "on" else "off"
                val pri = getString("t_pri", "None").lowercase()
                "Parsing $nlp · default priority $pri"
            }
            "habits" -> {
                val type = getString("h_type", "Check-off").lowercase()
                val roll = getInt("h_roll", 3)
                "New habits are $type · day ends ${SettingsSchema.formatHour(roll)}"
            }
            "focus" -> {
                val blk = if (getBoolean("f_blk", false)) "on" else "off"
                val auto = if (getBoolean("f_auto", false)) "on" else "off"
                "App blocking $blk · auto-start $auto"
            }
            "notifications" -> {
                val qOn = getBoolean("q_on", true)
                if (qOn) {
                    val from = getInt("q_from", 23)
                    val to = getInt("q_to", 7)
                    "Quiet hours ${SettingsSchema.formatHour(from)} to ${SettingsSchema.formatHour(to)}"
                } else {
                    "Quiet hours off"
                }
            }
            "data" -> {
                val auto = getBoolean("d_auto", false)
                if (auto) "Weekly auto-backup on" else "Export, import and reset"
            }
            else -> ""
        }
    }

    fun refreshStorageStats(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dbPath = context.getDatabasePath("reflex.db")
                var totalBytes = 0L
                if (dbPath.exists()) totalBytes += dbPath.length()
                val shm = File(dbPath.parentFile, "reflex.db-shm")
                if (shm.exists()) totalBytes += shm.length()
                val wal = File(dbPath.parentFile, "reflex.db-wal")
                if (wal.exists()) totalBytes += wal.length()

                val avatarFile = File(context.filesDir, "profile_avatar.jpg")
                if (avatarFile.exists()) totalBytes += avatarFile.length()

                val formatted = android.text.format.Formatter.formatShortFileSize(context, totalBytes.coerceAtLeast(1024L))

                val db = com.reflex.app.data.ReflexDatabase.getDatabase(context)
                val routines = try { db.routineDao().getAllRoutines().first().size } catch (_: Exception) { 0 }
                val tasks = try { db.taskDao().getAllTasks().first().size } catch (_: Exception) { 0 }
                val habits = try { db.habitDao().getAllHabits().first().size } catch (_: Exception) { 0 }
                val focusSessions = try { db.focusSessionDao().getAllSessions().first().size } catch (_: Exception) { 0 }

                _storageStats.value = AppStorageStats(
                    formattedStorageSize = formatted,
                    databaseSizeBytes = totalBytes,
                    routineCount = routines,
                    taskCount = tasks,
                    habitCount = habits,
                    focusSessionCount = focusSessions
                )
            } catch (e: Exception) {
                AppLog.e(TAG, "Error calculating storage stats", e)
            }
        }
    }

    fun getDeviceStorageSize(context: Context): String {
        val cached = _storageStats.value?.formattedStorageSize
        if (cached != null) return cached
        refreshStorageStats(context)
        val dbPath = context.getDatabasePath("reflex.db")
        var bytes = 0L
        if (dbPath.exists()) bytes += dbPath.length()
        return android.text.format.Formatter.formatShortFileSize(context, bytes.coerceAtLeast(1024L))
    }

    fun getEnabledCalendarsCount(context: Context): String {
        return try {
            if (!CalendarProviderHelper.hasReadPermission(context)) return "None"
            val all = CalendarProviderHelper.getAvailableCalendars(context)
            val enabledIds = CalendarPreferenceRepository.preferences.value.enabledCalendarIds
            val enabledCount = if (enabledIds == null) all.size else all.count { enabledIds.contains(it.id) }
            "$enabledCount of ${all.size}"
        } catch (_: Exception) {
            "None"
        }
    }

    private const val KEY_AUTO_BACKUP_TREE_URI = "auto_backup_tree_uri"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_BACKUP_STATUS = "last_backup_status"

    fun getAutoBackupUri(context: Context): android.net.Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uriStr = prefs.getString(KEY_AUTO_BACKUP_TREE_URI, null) ?: return null
        return android.net.Uri.parse(uriStr)
    }

    fun setAutoBackupUri(context: Context, uri: android.net.Uri?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (uri != null) {
            prefs.edit().putString(KEY_AUTO_BACKUP_TREE_URI, uri.toString()).apply()
        } else {
            prefs.edit().remove(KEY_AUTO_BACKUP_TREE_URI).apply()
        }
    }

    fun getLastBackupTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
    }

    fun setLastBackupTime(context: Context, timeMillis: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_BACKUP_TIME, timeMillis).apply()
    }

    fun getLastBackupStatus(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_BACKUP_STATUS, null)
    }

    fun setLastBackupStatus(context: Context, status: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (status != null) {
            prefs.edit().putString(KEY_LAST_BACKUP_STATUS, status).apply()
        } else {
            prefs.edit().remove(KEY_LAST_BACKUP_STATUS).apply()
        }
    }

    const val KEY_FOCUS_DAILY_GOAL_MINS = "focus_daily_goal_mins"
    const val DEFAULT_FOCUS_DAILY_GOAL_MINS = 120
    const val MIN_FOCUS_DAILY_GOAL_MINS = 30
    const val MAX_FOCUS_DAILY_GOAL_MINS = 480
    const val STEP_FOCUS_DAILY_GOAL_MINS = 15

    fun getFocusGoalMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_FOCUS_DAILY_GOAL_MINS, DEFAULT_FOCUS_DAILY_GOAL_MINS)
            .coerceIn(MIN_FOCUS_DAILY_GOAL_MINS, MAX_FOCUS_DAILY_GOAL_MINS)
    }

    fun setFocusGoalMinutes(context: Context, minutes: Int) {
        val clamped = minutes.coerceIn(MIN_FOCUS_DAILY_GOAL_MINS, MAX_FOCUS_DAILY_GOAL_MINS)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_FOCUS_DAILY_GOAL_MINS, clamped).apply()
        _values.value = _values.value + (KEY_FOCUS_DAILY_GOAL_MINS to clamped)
        try {
            val prof = UserProfileRepository.profile.value
            if (prof.focusGoalMinutes != clamped) {
                UserProfileRepository.updateProfile(context, prof.copy(focusGoalMinutes = clamped))
            }
        } catch (_: Exception) {}
    }

    fun stepFocusGoal(context: Context, deltaSteps: Int): Int {
        val current = getFocusGoalMinutes(context)
        val next = (current + deltaSteps * STEP_FOCUS_DAILY_GOAL_MINS).coerceIn(MIN_FOCUS_DAILY_GOAL_MINS, MAX_FOCUS_DAILY_GOAL_MINS)
        setFocusGoalMinutes(context, next)
        return next
    }
}


