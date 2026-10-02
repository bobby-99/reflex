package com.reflex.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.Serializable

data class FocusChecklistItem(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
) : Serializable {

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("isCompleted", isCompleted)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): FocusChecklistItem {
            return FocusChecklistItem(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                title = obj.optString("title", ""),
                isCompleted = obj.optBoolean("isCompleted", false)
            )
        }

        fun toJsonArrayString(items: List<FocusChecklistItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJsonObject()) }
            return array.toString()
        }

        fun listFromJsonArrayString(jsonStr: String?): List<FocusChecklistItem> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<FocusChecklistItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJsonObject(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
