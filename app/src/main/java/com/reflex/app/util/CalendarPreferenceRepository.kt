package com.reflex.app.util

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.DayOfWeek

data class CalendarPreferences(
    val excludeRepeatingTasks: Boolean = false,
    val excludeCompletedTasks: Boolean = false,
    val showRoutineReminders: Boolean = true,
    val enabledCalendarIds: Set<Long>? = null, // null means all enabled
    val agendaRangeDays: Int = 30, // 7, 14, 30, 60
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val defaultLandingView: String = "WEEK_STRIP", // "WEEK_STRIP" or "MONTH_GRID"
    val calendarDisplayNameOverrides: Map<String, String> = emptyMap()
)

object CalendarPreferenceRepository {

    private const val PREFS_NAME = "reflex_calendar_prefs"
    private const val KEY_EXCLUDE_REPEATING = "exclude_repeating_tasks"
    private const val KEY_EXCLUDE_COMPLETED = "exclude_completed_tasks"
    private const val KEY_SHOW_ROUTINES = "show_routine_reminders"
    private const val KEY_ENABLED_CALENDARS = "enabled_calendar_ids"
    private const val KEY_AGENDA_RANGE = "agenda_range_days"
    private const val KEY_FIRST_DAY_OF_WEEK = "first_day_of_week"
    private const val KEY_DEFAULT_LANDING_VIEW = "default_landing_view"
    private const val KEY_NAME_OVERRIDES = "calendar_display_name_overrides"

    private val _preferences = MutableStateFlow(CalendarPreferences())
    val preferences: StateFlow<CalendarPreferences> = _preferences.asStateFlow()

    fun getCalendarStableKey(accountName: String?, syncId: String?, originalName: String): String {
        val acc = accountName ?: ""
        val idPart = if (!syncId.isNullOrBlank()) syncId else originalName
        return "$acc|$idPart"
    }

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val excludeRepeating = prefs.getBoolean(KEY_EXCLUDE_REPEATING, false)
        val excludeCompleted = prefs.getBoolean(KEY_EXCLUDE_COMPLETED, false)
        val showRoutines = prefs.getBoolean(KEY_SHOW_ROUTINES, true)
        val calendarIdsString = prefs.getString(KEY_ENABLED_CALENDARS, null)
        val calendarIds = calendarIdsString?.split(",")?.mapNotNull { it.trim().toLongOrNull() }?.toSet()
        val agendaRange = prefs.getInt(KEY_AGENDA_RANGE, 30)
        val firstDayName = prefs.getString(KEY_FIRST_DAY_OF_WEEK, DayOfWeek.MONDAY.name) ?: DayOfWeek.MONDAY.name
        val firstDay = try { DayOfWeek.valueOf(firstDayName) } catch (e: Exception) { DayOfWeek.MONDAY }
        val landingView = prefs.getString(KEY_DEFAULT_LANDING_VIEW, "WEEK_STRIP") ?: "WEEK_STRIP"

        val overridesJson = prefs.getString(KEY_NAME_OVERRIDES, null)
        val overrides = if (!overridesJson.isNullOrBlank()) {
            try {
                val json = org.json.JSONObject(overridesJson)
                val map = mutableMapOf<String, String>()
                json.keys().forEach { k -> map[k] = json.getString(k) }
                map
            } catch (_: Exception) {
                emptyMap()
            }
        } else {
            emptyMap()
        }

        _preferences.value = CalendarPreferences(
            excludeRepeatingTasks = excludeRepeating,
            excludeCompletedTasks = excludeCompleted,
            showRoutineReminders = showRoutines,
            enabledCalendarIds = calendarIds,
            agendaRangeDays = agendaRange,
            firstDayOfWeek = firstDay,
            defaultLandingView = landingView,
            calendarDisplayNameOverrides = overrides
        )
    }

    fun setExcludeRepeatingTasks(context: Context, exclude: Boolean) {
        _preferences.value = _preferences.value.copy(excludeRepeatingTasks = exclude)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EXCLUDE_REPEATING, exclude).apply()
    }

    fun setExcludeCompletedTasks(context: Context, exclude: Boolean) {
        _preferences.value = _preferences.value.copy(excludeCompletedTasks = exclude)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_EXCLUDE_COMPLETED, exclude).apply()
    }

    fun setShowRoutineReminders(context: Context, show: Boolean) {
        _preferences.value = _preferences.value.copy(showRoutineReminders = show)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_ROUTINES, show).apply()
    }

    fun setEnabledCalendarIds(context: Context, ids: Set<Long>?) {
        _preferences.value = _preferences.value.copy(enabledCalendarIds = ids)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val serialized = ids?.joinToString(",")
        prefs.edit().putString(KEY_ENABLED_CALENDARS, serialized).apply()
    }

    fun setAgendaRangeDays(context: Context, days: Int) {
        _preferences.value = _preferences.value.copy(agendaRangeDays = days)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_AGENDA_RANGE, days).apply()
    }

    fun setFirstDayOfWeek(context: Context, day: DayOfWeek) {
        _preferences.value = _preferences.value.copy(firstDayOfWeek = day)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_FIRST_DAY_OF_WEEK, day.name).apply()
    }

    fun setDefaultLandingView(context: Context, view: String) {
        _preferences.value = _preferences.value.copy(defaultLandingView = view)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEFAULT_LANDING_VIEW, view).apply()
    }

    private fun saveOverrides(context: Context, map: Map<String, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = org.json.JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        prefs.edit().putString(KEY_NAME_OVERRIDES, json.toString()).apply()
    }

    fun setCalendarDisplayName(context: Context, stableKey: String, customName: String?) {
        val currentMap = _preferences.value.calendarDisplayNameOverrides.toMutableMap()
        if (customName.isNullOrBlank()) {
            currentMap.remove(stableKey)
        } else {
            currentMap[stableKey] = customName.trim()
        }
        _preferences.value = _preferences.value.copy(calendarDisplayNameOverrides = currentMap)
        saveOverrides(context, currentMap)
    }

    fun setAllCalendarDisplayNameOverrides(context: Context, map: Map<String, String>) {
        _preferences.value = _preferences.value.copy(calendarDisplayNameOverrides = map)
        saveOverrides(context, map)
    }
}
