package com.reflex.app.util

import android.content.Context
import com.reflex.app.data.FocusPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object FocusPresetRepository {

    private const val PREFS_NAME = "reflex_focus_presets"
    private const val KEY_PRESETS_JSON = "presets_json"
    private const val KEY_ACTIVE_PRESET_ID = "active_preset_id"

    private val defaultPresets = listOf(
        FocusPreset(
            id = "preset_standard",
            name = "Standard Pomodoro",
            description = "Classic 25m work with 5m short break and 15m long break",
            workDurationMin = 25,
            shortBreakMin = 5,
            longBreakMin = 15,
            sessionsBeforeLongBreak = 4,
            autoStartNextPhase = false,
            isDefault = true
        ),
        FocusPreset(
            id = "preset_deep_work",
            name = "Deep Focus",
            description = "Intense 50m focus blocks with 10m recharge breaks",
            workDurationMin = 50,
            shortBreakMin = 10,
            longBreakMin = 30,
            sessionsBeforeLongBreak = 3,
            autoStartNextPhase = false,
            isDefault = false
        ),
        FocusPreset(
            id = "preset_quick_sprint",
            name = "Quick Sprint",
            description = "Fast 15m high-energy sprints with 3m auto-advancing breaks",
            workDurationMin = 15,
            shortBreakMin = 3,
            longBreakMin = 10,
            sessionsBeforeLongBreak = 4,
            autoStartNextPhase = true,
            isDefault = false
        ),
        FocusPreset(
            id = "preset_study_block",
            name = "Study Block",
            description = "Balanced 45m study session with 15m breaks",
            workDurationMin = 45,
            shortBreakMin = 15,
            longBreakMin = 20,
            sessionsBeforeLongBreak = 4,
            autoStartNextPhase = false,
            isDefault = false
        )
    )

    private val _presets = MutableStateFlow<List<FocusPreset>>(defaultPresets)
    val presets: StateFlow<List<FocusPreset>> = _presets.asStateFlow()

    private val _activePreset = MutableStateFlow<FocusPreset>(defaultPresets.first())
    val activePreset: StateFlow<FocusPreset> = _activePreset.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PRESETS_JSON, null)
        val list = if (!json.isNullOrBlank()) {
            try {
                val array = JSONArray(json)
                val parsed = mutableListOf<FocusPreset>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    parsed.add(
                        FocusPreset(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            workDurationMin = obj.optInt("workDurationMin", 25),
                            shortBreakMin = obj.optInt("shortBreakMin", 5),
                            longBreakMin = obj.optInt("longBreakMin", 15),
                            sessionsBeforeLongBreak = obj.optInt("sessionsBeforeLongBreak", 4),
                            autoStartNextPhase = obj.optBoolean("autoStartNextPhase", false),
                            isDefault = obj.optBoolean("isDefault", false)
                        )
                    )
                }
                if (parsed.isEmpty()) defaultPresets else parsed
            } catch (e: Exception) {
                AppLog.w("FocusPresetRepository", "Failed to parse stored presets JSON", e)
                defaultPresets
            }
        } else {
            defaultPresets
        }
        _presets.value = list

        val activeId = prefs.getString(KEY_ACTIVE_PRESET_ID, list.first().id)
        _activePreset.value = list.find { it.id == activeId } ?: list.first()
    }

    fun selectPreset(context: Context, presetId: String) {
        val preset = _presets.value.find { it.id == presetId } ?: return
        _activePreset.value = preset
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_PRESET_ID, presetId).apply()
    }

    fun savePreset(context: Context, preset: FocusPreset) {
        val current = _presets.value.toMutableList()
        val index = current.indexOfFirst { it.id == preset.id }
        if (index >= 0) {
            current[index] = preset
        } else {
            current.add(preset)
        }
        _presets.value = current
        _activePreset.value = preset
        persist(context, current, preset.id)
    }

    fun deletePreset(context: Context, presetId: String) {
        val current = _presets.value.filter { it.id != presetId }.toMutableList()
        if (current.isEmpty()) {
            current.addAll(defaultPresets)
        }
        _presets.value = current
        val newActive = current.first()
        _activePreset.value = newActive
        persist(context, current, newActive.id)
    }

    private fun persist(context: Context, list: List<FocusPreset>, activeId: String) {
        try {
            val array = JSONArray()
            list.forEach { p ->
                val obj = JSONObject().apply {
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
                array.put(obj)
            }
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putString(KEY_PRESETS_JSON, array.toString())
                .putString(KEY_ACTIVE_PRESET_ID, activeId)
                .apply()
        } catch (e: Exception) {
            AppLog.w("FocusPresetRepository", "Failed to persist focus presets", e)
        }
    }
}
