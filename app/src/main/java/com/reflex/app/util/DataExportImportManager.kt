package com.reflex.app.util

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.reflex.app.data.BlockingMode
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.data.FocusSettings
import com.reflex.app.data.FocusTag
import com.reflex.app.data.Habit
import com.reflex.app.data.HabitLog
import com.reflex.app.data.MonthlyMode
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceBasis
import com.reflex.app.data.RecurrenceEndType
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.RecurrenceUnit
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.Routine
import com.reflex.app.data.Step
import com.reflex.app.data.StepType
import com.reflex.app.data.Task
import com.reflex.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DataExportImportManager {

    private const val BACKUP_VERSION = 4

    suspend fun exportDataToUri(context: Context, repository: ReflexRepository, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = exportDataToJson(context)
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(json.toByteArray())
            }
            true
        } catch (e: Exception) {
            AppLog.e("DataExportImportManager", "Failed to export data to URI", e)
            false
        }
    }

    suspend fun importDataFromUri(context: Context, repository: ReflexRepository, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return@withContext false

            val result = importDataFromJson(context, json)
            result.isSuccess
        } catch (e: Exception) {
            AppLog.e("DataExportImportManager", "Failed to import data from URI", e)
            false
        }
    }

    suspend fun exportDataToJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = ReflexDatabase.getDatabase(context)

        val root = JSONObject()
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("user_name", SettingsRepository.getUserName(context))
        root.put("user_bio", UserProfileRepository.profile.value.bio)
        root.put("theme", ThemePreferenceRepository.currentTheme.value.name)
        root.put("focus_daily_goal_mins", SettingsRepository.getFocusGoalMinutes(context))

        val routines = db.routineDao().getAllRoutines().first()
        val routinesArray = JSONArray()
        routines.forEach { r ->
            val obj = JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("icon", r.icon)
                put("createdAt", r.createdAt)
                put("isArchived", r.isArchived)
                put("restBetweenStepsEnabled", r.restBetweenStepsEnabled)
                put("restDurationSeconds", r.restDurationSeconds)
                put("scheduledDays", r.scheduledDays)
                put("reminderTime", r.reminderTime)
                put("reminderEnabled", r.reminderEnabled)
            }
            routinesArray.put(obj)
        }
        root.put("routines", routinesArray)

        val stepsArray = JSONArray()
        routines.forEach { r ->
            val steps = db.stepDao().getStepsForRoutine(r.id).first()
            steps.forEach { s ->
                val obj = JSONObject().apply {
                    put("id", s.id)
                    put("routineId", s.routineId)
                    put("name", s.name)
                    put("stepType", s.stepType.name)
                    put("durationSeconds", s.durationSeconds)
                    put("targetCount", s.targetCount)
                    put("notes", s.notes)
                    put("orderIndex", s.orderIndex)
                    put("emoji", s.emoji)
                    if (s.restDurationSeconds != null) {
                        put("restDurationSeconds", s.restDurationSeconds)
                    }
                }
                stepsArray.put(obj)
            }
        }
        root.put("steps", stepsArray)

        val tasks = db.taskDao().getAllTasks().first()
        val tasksArray = JSONArray()
        tasks.forEach { t ->
            val obj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("notes", t.notes)
                put("dueDate", t.dueDate)
                put("dueTime", t.dueTime)
                put("priority", t.priority.name)
                put("isCompleted", t.isCompleted)
                put("completedAt", t.completedAt)
                put("createdAt", t.createdAt)
                put("reminderTime", t.reminderTime)
                put("recurrenceFrequency", t.recurrenceFrequency.name)
                put("recurrenceInterval", t.recurrenceInterval)
                put("recurrenceUnit", t.recurrenceUnit.name)
                put("recurrenceDaysOfWeek", t.recurrenceDaysOfWeek)
                put("recurrenceEndType", t.recurrenceEndType.name)
                put("recurrenceEndOccurrences", t.recurrenceEndOccurrences)
                put("recurrenceBasis", t.recurrenceBasis.name)
                put("recurrenceMonthlyMode", t.recurrenceMonthlyMode?.name)
            }
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        // User Profile
        val userProfile = UserProfileRepository.profile.value
        val profileObj = JSONObject().apply {
            put("name", userProfile.name)
            put("avatarEmoji", userProfile.avatarEmoji)
            put("userEmail", userProfile.userEmail)
            put("focusGoalMinutes", userProfile.focusGoalMinutes)
            put("bio", userProfile.bio)
            if (!userProfile.avatarBase64.isNullOrBlank()) {
                put("avatarBase64", userProfile.avatarBase64)
            }
        }
        root.put("user_profile", profileObj)

        // Focus Presets
        val presetsList = FocusPresetRepository.presets.value
        val presetsArray = JSONArray()
        presetsList.forEach { p ->
            val pObj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("workDurationMin", p.workDurationMin)
                put("shortBreakMin", p.shortBreakMin)
                put("longBreakMin", p.longBreakMin)
                put("sessionsBeforeLongBreak", p.sessionsBeforeLongBreak)
                put("autoStartNextPhase", p.autoStartNextPhase)
                put("isDefault", p.isDefault)
            }
            presetsArray.put(pObj)
        }
        root.put("focus_presets", presetsArray)

        // Calendar Preferences
        val calPrefs = CalendarPreferenceRepository.preferences.value
        val calObj = JSONObject().apply {
            put("firstDayOfWeek", calPrefs.firstDayOfWeek.name)
            put("defaultLandingView", calPrefs.defaultLandingView)
            put("excludeRepeatingTasks", calPrefs.excludeRepeatingTasks)
            put("excludeCompletedTasks", calPrefs.excludeCompletedTasks)
            put("showRoutineReminders", calPrefs.showRoutineReminders)
            put("agendaRangeDays", calPrefs.agendaRangeDays)
            val overridesObj = JSONObject()
            calPrefs.calendarDisplayNameOverrides.forEach { (k, v) -> overridesObj.put(k, v) }
            put("calendarDisplayNameOverrides", overridesObj)
        }
        root.put("calendar_preferences", calObj)

        val logs = db.completionLogDao().getAllLogs().first()
        val logsArray = JSONArray()
        logs.forEach { l ->
            val obj = JSONObject().apply {
                put("id", l.id)
                put("routineId", l.routineId)
                put("dateCompleted", l.dateCompleted)
                put("totalTimeTakenSeconds", l.totalTimeTakenSeconds)
                put("stepsCompletedCount", l.stepsCompletedCount)
                put("isCompleted", l.isCompleted)
                put("isSkipped", l.isSkipped)
                put("totalStepsCount", l.totalStepsCount)
                if (l.stepLogsJson != null) {
                    put("stepLogsJson", l.stepLogsJson)
                }
            }
            logsArray.put(obj)
        }
        root.put("completion_logs", logsArray)

        val settings = db.focusSettingsDao().getFocusSettingsSync() ?: FocusSettings()
        val settingsObj = JSONObject().apply {
            put("id", settings.id)
            put("workDurationMin", settings.workDurationMin)
            put("shortBreakMin", settings.shortBreakMin)
            put("longBreakMin", settings.longBreakMin)
            put("sessionsBeforeLongBreak", settings.sessionsBeforeLongBreak)
            put("autoStartNextPhase", settings.autoStartNextPhase)
            put("soundEnabled", settings.soundEnabled)
            put("vibrationEnabled", settings.vibrationEnabled)
            put("blockingMode", settings.blockingMode.name)
            put("selectedPackages", settings.selectedPackages)
        }
        root.put("focus_settings", settingsObj)

        // Focus Tags
        val focusTags = db.focusTagDao().getAllTagsSync()
        val tagsArray = JSONArray()
        val tagIdToNameMap = mutableMapOf<Long, String>()
        focusTags.forEach { ft ->
            tagIdToNameMap[ft.id] = ft.name
            val obj = JSONObject().apply {
                put("id", ft.id)
                put("name", ft.name)
                put("createdAt", ft.createdAt)
            }
            tagsArray.put(obj)
        }
        root.put("focus_tags", tagsArray)

        val focusSessions = db.focusSessionDao().getAllSessions().first()
        val sessionsArray = JSONArray()
        focusSessions.forEach { fs ->
            val obj = JSONObject().apply {
                put("id", fs.id)
                put("mode", fs.mode.name)
                put("startTime", fs.startTime)
                put("endTime", fs.endTime)
                if (fs.plannedDurationSeconds != null) {
                    put("plannedDurationSeconds", fs.plannedDurationSeconds)
                }
                put("actualDurationSeconds", fs.actualDurationSeconds)
                put("completedCycles", fs.completedCycles)
                put("completed", fs.completed)
                put("blockedAttemptCount", fs.blockedAttemptCount)
                put("sessionTitle", fs.sessionTitle)
                put("checklistJson", fs.checklistJson)
                put("endReason", fs.endReason)
                if (fs.tagId != null && tagIdToNameMap.containsKey(fs.tagId)) {
                    put("tagName", tagIdToNameMap[fs.tagId])
                }
            }
            sessionsArray.put(obj)
        }
        root.put("focus_sessions", sessionsArray)

        // Habits
        val habits = db.habitDao().getAllHabitsSync()
        val habitsArray = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject().apply {
                put("id", h.id)
                put("name", h.name)
                put("type", h.type)
                put("target", h.target)
                put("unit", h.unit)
                put("step", h.step)
                put("startEpochDay", h.startEpochDay)
                put("sortOrder", h.sortOrder)
                put("frequencyType", h.frequencyType)
                put("frequencyDays", h.frequencyDays)
                put("frequencyTargetPerWeek", h.frequencyTargetPerWeek)
                put("reminderEnabled", h.reminderEnabled)
                put("reminderTimes", h.reminderTimes)
                if (h.endEpochDay != null) put("endEpochDay", h.endEpochDay)
                if (h.colorHex != null) put("colorHex", h.colorHex)
                if (h.iconKey != null) put("iconKey", h.iconKey)
                if (h.notes != null) put("notes", h.notes)
            }
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        // Habit Logs
        val habitLogs = db.habitDao().getAllLogsSync()
        val habitLogsArray = JSONArray()
        habitLogs.forEach { hl ->
            val obj = JSONObject().apply {
                put("id", hl.id)
                put("habitId", hl.habitId)
                put("epochDay", hl.epochDay)
                put("value", hl.value)
            }
            habitLogsArray.put(obj)
        }
        root.put("habit_logs", habitLogsArray)

        // SharedPreferences
        val prefsObj = JSONObject()
        val sp = context.getSharedPreferences("reflex_app_settings", Context.MODE_PRIVATE)
        sp.all.forEach { (k, v) ->
            if (v != null) {
                prefsObj.put(k, v)
            }
        }
        root.put("preferences", prefsObj)

        root.toString(2)
    }

    suspend fun importDataFromJson(context: Context, jsonStr: String): Result<String> = withContext(Dispatchers.IO) {
        val root = try {
            JSONObject(jsonStr)
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        }

        // Full structure validation before any database write
        if (!root.has("version") || !root.has("routines") || !root.has("steps") || !root.has("tasks")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        }

        val routinesArray = root.optJSONArray("routines")
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        val stepsArray = root.optJSONArray("steps")
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        val tasksArray = root.optJSONArray("tasks")
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))

        // Validate routines
        val parsedRoutines = mutableListOf<Routine>()
        try {
            for (i in 0 until routinesArray.length()) {
                val obj = routinesArray.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                if (!obj.has("name")) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                }
                parsedRoutines.add(
                    Routine(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        icon = obj.optString("icon", "BOLT"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        isArchived = obj.optBoolean("isArchived", false),
                        restBetweenStepsEnabled = obj.optBoolean("restBetweenStepsEnabled", false),
                        restDurationSeconds = obj.optInt("restDurationSeconds", 15),
                        scheduledDays = obj.optString("scheduledDays", ""),
                        reminderTime = if (obj.has("reminderTime") && !obj.isNull("reminderTime")) obj.getString("reminderTime") else null,
                        reminderEnabled = obj.optBoolean("reminderEnabled", false)
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        }

        // Validate steps
        val parsedSteps = mutableListOf<Step>()
        try {
            for (i in 0 until stepsArray.length()) {
                val obj = stepsArray.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                if (!obj.has("routineId") || !obj.has("name") || !obj.has("stepType")) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                }
                val stepType = try {
                    StepType.valueOf(obj.getString("stepType"))
                } catch (_: Exception) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                }
                parsedSteps.add(
                    Step(
                        id = obj.optLong("id", 0L),
                        routineId = obj.getLong("routineId"),
                        name = obj.getString("name"),
                        stepType = stepType,
                        durationSeconds = obj.optInt("durationSeconds", 60),
                        targetCount = obj.optInt("targetCount", 10),
                        notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                        orderIndex = obj.optInt("orderIndex", 0),
                        emoji = if (obj.has("emoji") && !obj.isNull("emoji")) obj.getString("emoji") else null,
                        restDurationSeconds = if (obj.has("restDurationSeconds") && !obj.isNull("restDurationSeconds")) obj.getInt("restDurationSeconds") else null
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        }

        // Validate tasks
        val parsedTasks = mutableListOf<Task>()
        try {
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                if (!obj.has("title")) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                }
                val priority = try {
                    Priority.valueOf(obj.optString("priority", "NONE"))
                } catch (_: Exception) {
                    Priority.NONE
                }
                val recurrenceFrequency = try {
                    RecurrenceFrequency.valueOf(obj.optString("recurrenceFrequency", "NONE"))
                } catch (_: Exception) {
                    RecurrenceFrequency.NONE
                }
                val recurrenceUnit = try {
                    RecurrenceUnit.valueOf(obj.optString("recurrenceUnit", "DAY"))
                } catch (_: Exception) {
                    RecurrenceUnit.DAY
                }
                val recurrenceEndType = try {
                    RecurrenceEndType.valueOf(obj.optString("recurrenceEndType", "NEVER"))
                } catch (_: Exception) {
                    RecurrenceEndType.NEVER
                }
                val recurrenceBasis = try {
                    RecurrenceBasis.valueOf(obj.optString("recurrenceBasis", "FROM_DUE_DATE"))
                } catch (_: Exception) {
                    RecurrenceBasis.FROM_DUE_DATE
                }
                val recurrenceMonthlyMode = if (obj.has("recurrenceMonthlyMode") && !obj.isNull("recurrenceMonthlyMode")) {
                    try {
                        MonthlyMode.valueOf(obj.getString("recurrenceMonthlyMode"))
                    } catch (_: Exception) {
                        MonthlyMode.SAME_DATE
                    }
                } else MonthlyMode.SAME_DATE

                parsedTasks.add(
                    Task(
                        id = obj.optLong("id", 0L),
                        title = obj.getString("title"),
                        notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null,
                        dueDate = if (obj.has("dueDate") && !obj.isNull("dueDate")) obj.getLong("dueDate") else null,
                        dueTime = if (obj.has("dueTime") && !obj.isNull("dueTime")) obj.getLong("dueTime") else null,
                        priority = priority,
                        isCompleted = obj.optBoolean("isCompleted", false),
                        completedAt = if (obj.has("completedAt") && !obj.isNull("completedAt")) obj.getLong("completedAt") else null,
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        reminderTime = if (obj.has("reminderTime") && !obj.isNull("reminderTime")) obj.getLong("reminderTime") else null,
                        recurrenceFrequency = recurrenceFrequency,
                        recurrenceInterval = obj.optInt("recurrenceInterval", 1),
                        recurrenceUnit = recurrenceUnit,
                        recurrenceDaysOfWeek = if (obj.has("recurrenceDaysOfWeek") && !obj.isNull("recurrenceDaysOfWeek")) obj.getString("recurrenceDaysOfWeek") else null,
                        recurrenceEndType = recurrenceEndType,
                        recurrenceEndOccurrences = obj.optInt("recurrenceEndOccurrences", 0),
                        recurrenceBasis = recurrenceBasis,
                        recurrenceMonthlyMode = recurrenceMonthlyMode
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
        }

        // Validate optional completion_logs
        val parsedLogs = mutableListOf<CompletionLog>()
        if (root.has("completion_logs")) {
            val arr = root.optJSONArray("completion_logs")
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                parsedLogs.add(
                    CompletionLog(
                        id = obj.optLong("id", 0L),
                        routineId = obj.optLong("routineId", 0L),
                        dateCompleted = obj.optLong("dateCompleted", 0L),
                        totalTimeTakenSeconds = obj.optInt("totalTimeTakenSeconds", 0),
                        stepsCompletedCount = obj.optInt("stepsCompletedCount", 0),
                        isCompleted = obj.optBoolean("isCompleted", true),
                        isSkipped = obj.optBoolean("isSkipped", false),
                        totalStepsCount = obj.optInt("totalStepsCount", 0),
                        stepLogsJson = if (obj.has("stepLogsJson") && !obj.isNull("stepLogsJson")) obj.getString("stepLogsJson") else null
                    )
                )
            }
        }

        // Validate optional focus_settings
        var parsedFocusSettings: FocusSettings? = null
        if (root.has("focus_settings")) {
            val obj = root.optJSONObject("focus_settings")
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
            val blockingMode = try {
                BlockingMode.valueOf(obj.optString("blockingMode", "OFF"))
            } catch (_: Exception) {
                BlockingMode.OFF
            }
            parsedFocusSettings = FocusSettings(
                id = obj.optInt("id", 1),
                workDurationMin = obj.optInt("workDurationMin", 25),
                shortBreakMin = obj.optInt("shortBreakMin", 5),
                longBreakMin = obj.optInt("longBreakMin", 15),
                sessionsBeforeLongBreak = obj.optInt("sessionsBeforeLongBreak", 4),
                autoStartNextPhase = obj.optBoolean("autoStartNextPhase", false),
                soundEnabled = obj.optBoolean("soundEnabled", true),
                vibrationEnabled = obj.optBoolean("vibrationEnabled", true),
                blockingMode = blockingMode,
                selectedPackages = obj.optString("selectedPackages", "")
            )
        }

        // Validate optional focus_tags
        val parsedTags = mutableListOf<FocusTag>()
        if (root.has("focus_tags")) {
            val arr = root.optJSONArray("focus_tags")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    parsedTags.add(
                        FocusTag(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        // Validate optional focus_sessions
        val parsedSessions = mutableListOf<Pair<FocusSession, String?>>()
        if (root.has("focus_sessions")) {
            val arr = root.optJSONArray("focus_sessions")
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                val mode = try {
                    FocusMode.valueOf(obj.optString("mode", "CLASSIC_POMODORO"))
                } catch (_: Exception) {
                    FocusMode.CLASSIC_POMODORO
                }
                val tagName = if (obj.has("tagName") && !obj.isNull("tagName")) obj.getString("tagName") else null
                parsedSessions.add(
                    Pair(
                        FocusSession(
                            id = obj.optLong("id", 0L),
                            mode = mode,
                            startTime = obj.optLong("startTime", 0L),
                            endTime = obj.optLong("endTime", 0L),
                            plannedDurationSeconds = if (obj.has("plannedDurationSeconds") && !obj.isNull("plannedDurationSeconds")) obj.getInt("plannedDurationSeconds") else null,
                            actualDurationSeconds = obj.optInt("actualDurationSeconds", 0),
                            completedCycles = obj.optInt("completedCycles", 0),
                            completed = obj.optBoolean("completed", true),
                            blockedAttemptCount = obj.optInt("blockedAttemptCount", 0),
                            sessionTitle = if (obj.has("sessionTitle") && !obj.isNull("sessionTitle")) obj.getString("sessionTitle") else null,
                            checklistJson = if (obj.has("checklistJson") && !obj.isNull("checklistJson")) obj.getString("checklistJson") else null,
                            endReason = obj.optString("endReason", if (obj.optBoolean("completed", true)) "completed" else "stopped_early")
                        ),
                        tagName
                    )
                )
            }
        }

        // Validate optional habits
        val parsedHabits = mutableListOf<Habit>()
        if (root.has("habits")) {
            val arr = root.optJSONArray("habits")
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                parsedHabits.add(
                    Habit(
                        id = obj.optLong("id", 0L),
                        name = obj.getString("name"),
                        type = obj.optString("type", "CHECK_OFF"),
                        target = obj.optDouble("target", 1.0),
                        unit = obj.optString("unit", ""),
                        step = obj.optDouble("step", 1.0),
                        startEpochDay = obj.optLong("startEpochDay", java.time.LocalDate.now().toEpochDay()),
                        sortOrder = obj.optInt("sortOrder", 0),
                        frequencyType = obj.optString("frequencyType", "DAILY"),
                        frequencyDays = obj.optString("frequencyDays", ""),
                        frequencyTargetPerWeek = obj.optInt("frequencyTargetPerWeek", 0),
                        reminderEnabled = obj.optBoolean("reminderEnabled", false),
                        reminderTimes = obj.optString("reminderTimes", ""),
                        endEpochDay = if (obj.has("endEpochDay") && !obj.isNull("endEpochDay")) obj.getLong("endEpochDay") else null,
                        colorHex = if (obj.has("colorHex") && !obj.isNull("colorHex")) obj.getString("colorHex") else null,
                        iconKey = if (obj.has("iconKey") && !obj.isNull("iconKey")) obj.getString("iconKey") else null,
                        notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null
                    )
                )
            }
        }

        // Validate optional habit_logs
        val parsedHabitLogs = mutableListOf<HabitLog>()
        if (root.has("habit_logs")) {
            val arr = root.optJSONArray("habit_logs")
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid Reflex backup file format"))
                parsedHabitLogs.add(
                    HabitLog(
                        id = obj.optLong("id", 0L),
                        habitId = obj.getLong("habitId"),
                        epochDay = obj.getLong("epochDay"),
                        value = obj.getDouble("value")
                    )
                )
            }
        }

        // 1. Cancel existing alarms before clearing data
        AlarmScheduler.cancelAllAlarms(context)

        // 2. Perform all database writes inside db.withTransaction { ... }
        val db = ReflexDatabase.getDatabase(context)
        try {
            db.withTransaction {
                // Clear existing data (default import mode: REPLACE)
                db.completionLogDao().deleteAllLogs()
                db.focusSessionDao().deleteAllSessions()
                db.focusTagDao().deleteAllTags()
                db.habitDao().deleteAllLogs()
                db.habitDao().deleteAllHabits()
                db.stepDao().deleteAllSteps()
                db.routineDao().deleteAllRoutines()
                db.taskDao().deleteAllTasks()

                val tagNameToIdMap = mutableMapOf<String, Long>()
                parsedTags.forEach { t ->
                    val insertedId = db.focusTagDao().insertTag(t.copy(id = 0L))
                    tagNameToIdMap[t.name.lowercase().trim()] = insertedId
                }

                // Insert all imported entities
                parsedRoutines.forEach { db.routineDao().insertRoutine(it) }
                parsedSteps.forEach { db.stepDao().insertStep(it) }
                parsedTasks.forEach { db.taskDao().insertTask(it) }
                parsedLogs.forEach { db.completionLogDao().insertLog(it) }
                if (parsedFocusSettings != null) {
                    db.focusSettingsDao().insertOrUpdateSettings(parsedFocusSettings)
                }
                parsedSessions.forEach { (session, tagName) ->
                    val resolvedTagId = if (!tagName.isNullOrBlank()) {
                        val key = tagName.lowercase().trim()
                        tagNameToIdMap[key] ?: run {
                            val newId = db.focusTagDao().insertTag(FocusTag(name = tagName.trim()))
                            tagNameToIdMap[key] = newId
                            newId
                        }
                    } else {
                        null
                    }
                    db.focusSessionDao().insertSession(session.copy(tagId = resolvedTagId))
                }
                parsedHabits.forEach { db.habitDao().insertHabit(it) }
                parsedHabitLogs.forEach { db.habitDao().upsertLog(it) }
            }
        } catch (e: Exception) {
            AppLog.e("DataExportImportManager", "Failed to restore database records from JSON", e)
            return@withContext Result.failure(e)
        }

        // 3. Immediately after the import transaction commits, reschedule all alarms
        AlarmScheduler.rescheduleAllAlarms(context)

        // 4. Restore preferences
        if (root.has("preferences")) {
            val prefsObj = root.optJSONObject("preferences")
            if (prefsObj != null) {
                val sp = context.getSharedPreferences("reflex_app_settings", Context.MODE_PRIVATE)
                val editor = sp.edit()
                val keys = prefsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    when (val value = prefsObj.get(k)) {
                        is Boolean -> editor.putBoolean(k, value)
                        is Int -> editor.putInt(k, value)
                        is Long -> editor.putLong(k, value)
                        is Float -> editor.putFloat(k, value)
                        is Double -> editor.putFloat(k, value.toFloat())
                        is String -> editor.putString(k, value)
                    }
                }
                editor.apply()
            }
        }

        // 5. Restore profile name, bio, and avatar base64
        val pObj = root.optJSONObject("user_profile")
        val restoredName = when {
            root.has("user_name") -> root.optString("user_name", "")
            pObj?.has("name") == true -> pObj.optString("name", "")
            else -> ""
        }
        val restoredBio = when {
            root.has("user_bio") -> root.optString("user_bio", "")
            pObj?.has("bio") == true -> pObj.optString("bio", "")
            else -> ""
        }

        val avatarBase64 = pObj?.optString("avatarBase64")?.takeIf { it.isNotBlank() }
            ?: root.optString("avatarBase64").takeIf { it.isNotBlank() }
        var avatarPath: String? = null
        if (!avatarBase64.isNullOrBlank()) {
            try {
                val bytes = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT)
                val avatarFile = File(context.filesDir, "profile_avatar.jpg")
                avatarFile.writeBytes(bytes)
                avatarPath = avatarFile.absolutePath
            } catch (e: Exception) {
                AppLog.w("DataExportImportManager", "Failed to restore profile avatar file from base64", e)
            }
        }

        val currentProfile = UserProfileRepository.profile.value
        val finalProfile = currentProfile.copy(
            name = if (restoredName.isNotBlank()) restoredName else currentProfile.name,
            bio = if (restoredBio.isNotBlank()) restoredBio else currentProfile.bio,
            avatarEmoji = pObj?.optString("avatarEmoji", currentProfile.avatarEmoji) ?: currentProfile.avatarEmoji,
            userEmail = pObj?.optString("userEmail", currentProfile.userEmail) ?: currentProfile.userEmail,
            focusGoalMinutes = pObj?.optInt("focusGoalMinutes", currentProfile.focusGoalMinutes) ?: currentProfile.focusGoalMinutes,
            avatarPath = avatarPath ?: currentProfile.avatarPath,
            avatarBase64 = avatarBase64 ?: currentProfile.avatarBase64
        )
        UserProfileRepository.updateProfile(context, finalProfile)
        if (restoredName.isNotBlank()) {
            SettingsRepository.setUserName(context, restoredName)
        }
        if (root.has("focus_daily_goal_mins")) {
            SettingsRepository.setFocusGoalMinutes(context, root.getInt("focus_daily_goal_mins"))
        } else if (pObj?.has("focusGoalMinutes") == true) {
            SettingsRepository.setFocusGoalMinutes(context, pObj.getInt("focusGoalMinutes"))
        }

        // 6. Restore Focus presets
        if (root.has("focus_presets")) {
            val presetsArray = root.getJSONArray("focus_presets")
            for (i in 0 until presetsArray.length()) {
                val itemObj = presetsArray.getJSONObject(i)
                FocusPresetRepository.savePreset(
                    context,
                    com.reflex.app.data.FocusPreset(
                        id = itemObj.getString("id"),
                        name = itemObj.getString("name"),
                        description = itemObj.optString("description", ""),
                        workDurationMin = itemObj.optInt("workDurationMin", 25),
                        shortBreakMin = itemObj.optInt("shortBreakMin", 5),
                        longBreakMin = itemObj.optInt("longBreakMin", 15),
                        sessionsBeforeLongBreak = itemObj.optInt("sessionsBeforeLongBreak", 4),
                        autoStartNextPhase = itemObj.optBoolean("autoStartNextPhase", false),
                        isDefault = itemObj.optBoolean("isDefault", false)
                    )
                )
            }
        }

        // 7. Restore Calendar preferences
        if (root.has("calendar_preferences")) {
            val calObj = root.getJSONObject("calendar_preferences")
            if (calObj.has("firstDayOfWeek")) {
                try {
                    CalendarPreferenceRepository.setFirstDayOfWeek(
                        context,
                        java.time.DayOfWeek.valueOf(calObj.getString("firstDayOfWeek"))
                    )
                } catch (_: Exception) {}
            }
            if (calObj.has("defaultLandingView")) {
                CalendarPreferenceRepository.setDefaultLandingView(
                    context,
                    calObj.getString("defaultLandingView")
                )
            }
            if (calObj.has("excludeRepeatingTasks")) {
                CalendarPreferenceRepository.setExcludeRepeatingTasks(
                    context,
                    calObj.getBoolean("excludeRepeatingTasks")
                )
            }
            if (calObj.has("excludeCompletedTasks")) {
                CalendarPreferenceRepository.setExcludeCompletedTasks(
                    context,
                    calObj.getBoolean("excludeCompletedTasks")
                )
            }
            if (calObj.has("showRoutineReminders")) {
                CalendarPreferenceRepository.setShowRoutineReminders(
                    context,
                    calObj.getBoolean("showRoutineReminders")
                )
            }
            if (calObj.has("agendaRangeDays")) {
                CalendarPreferenceRepository.setAgendaRangeDays(
                    context,
                    calObj.getInt("agendaRangeDays")
                )
            }
            if (calObj.has("calendarDisplayNameOverrides")) {
                val overridesObj = calObj.getJSONObject("calendarDisplayNameOverrides")
                val map = mutableMapOf<String, String>()
                val keys = overridesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = overridesObj.getString(key)
                }
                CalendarPreferenceRepository.setAllCalendarDisplayNameOverrides(context, map)
            }
        }

        // 8. Restore theme if present
        if (root.has("theme")) {
            try {
                val themeStr = root.getString("theme")
                ThemePreferenceRepository.setTheme(context, AppTheme.fromString(themeStr))
            } catch (_: Exception) {}
        }

        // 9. Refresh theme and settings repositories
        ThemePreferenceRepository.init(context)
        SettingsRepository.init(context)

        Result.success("Data backup successfully restored!")
    }
}
