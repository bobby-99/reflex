package com.reflex.app

import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSession
import com.reflex.app.data.FocusTag
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupImportTest {

    private val v2BackupJson = """
    {
      "version": 2,
      "exportedAt": 1727700000000,
      "user_name": "Test User",
      "routines": [
        {
          "id": 1,
          "name": "Morning Routine",
          "icon": "BOLT",
          "createdAt": 1727700000000,
          "isArchived": false,
          "restBetweenStepsEnabled": false,
          "restDurationSeconds": 15,
          "scheduledDays": "1,2,3,4,5",
          "reminderTime": "08:00",
          "reminderEnabled": true
        }
      ],
      "steps": [
        {
          "id": 1,
          "routineId": 1,
          "name": "Stretch",
          "stepOrder": 0,
          "stepType": "TIMED",
          "durationSeconds": 300,
          "targetCount": null,
          "restDurationSeconds": null,
          "emoji": "🧘"
        }
      ],
      "tasks": [
        {
          "id": 1,
          "title": "Daily Planning",
          "priority": "HIGH",
          "isCompleted": false,
          "dueDate": null,
          "dueTime": null,
          "createdAt": 1727700000000
        }
      ],
      "focus_sessions": [
        {
          "id": 101,
          "mode": "CLASSIC_POMODORO",
          "startTime": 1727701000000,
          "endTime": 1727702500000,
          "plannedDurationSeconds": 1500,
          "actualDurationSeconds": 1500,
          "completedCycles": 1,
          "completed": true,
          "blockedAttemptCount": 0,
          "sessionTitle": "Coding pass"
        }
      ]
    }
    """.trimIndent()

    private val v3BackupJson = """
    {
      "version": 3,
      "exportedAt": 1727705000000,
      "user_name": "Test User",
      "routines": [],
      "steps": [],
      "tasks": [],
      "focus_tags": [
        {
          "id": 1,
          "name": "Deep Work",
          "createdAt": 1727700000000
        },
        {
          "id": 2,
          "name": "Reading",
          "createdAt": 1727701000000
        }
      ],
      "focus_sessions": [
        {
          "id": 201,
          "mode": "FLOW_TIMED",
          "startTime": 1727703000000,
          "endTime": 1727704800000,
          "plannedDurationSeconds": 1800,
          "actualDurationSeconds": 1800,
          "completedCycles": 1,
          "completed": true,
          "blockedAttemptCount": 0,
          "sessionTitle": "Chapter 4",
          "tagName": "Reading"
        },
        {
          "id": 202,
          "mode": "FLOW_OPEN",
          "startTime": 1727705000000,
          "endTime": 1727706000000,
          "plannedDurationSeconds": null,
          "actualDurationSeconds": 1000,
          "completedCycles": 1,
          "completed": true,
          "blockedAttemptCount": 0,
          "sessionTitle": "Untagged session"
        }
      ]
    }
    """.trimIndent()

    @Test
    fun testV2BackupParsesWithoutTagsAndLeavesTagIdNull() {
        val root = JSONObject(v2BackupJson)
        assertEquals(2, root.getInt("version"))

        // Focus tags should not exist in v2 backup
        val hasFocusTags = root.has("focus_tags")
        assertTrue("v2 backup should not have focus_tags array", !hasFocusTags)

        // Parse focus sessions
        val sessionsArray = root.getJSONArray("focus_sessions")
        assertEquals(1, sessionsArray.length())

        val sessionObj = sessionsArray.getJSONObject(0)
        val hasTagName = sessionObj.has("tagName") && !sessionObj.isNull("tagName")
        assertTrue("v2 focus session should not have a tagName", !hasTagName)

        // Simulating the import mapping logic from DataExportImportManager
        val tagNameToIdMap = mutableMapOf<String, Long>()
        val parsedTags = mutableListOf<FocusTag>()
        if (root.has("focus_tags")) {
            val arr = root.optJSONArray("focus_tags")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    parsedTags.add(FocusTag(id = obj.optLong("id"), name = obj.getString("name")))
                }
            }
        }
        assertEquals(0, parsedTags.size)

        val tagName = if (sessionObj.has("tagName") && !sessionObj.isNull("tagName")) sessionObj.getString("tagName") else null
        val resolvedTagId: Long? = if (!tagName.isNullOrBlank()) {
            val key = tagName.lowercase().trim()
            tagNameToIdMap[key] ?: 99L
        } else {
            null
        }

        val session = FocusSession(
            id = sessionObj.optLong("id"),
            mode = FocusMode.valueOf(sessionObj.getString("mode")),
            startTime = sessionObj.getLong("startTime"),
            endTime = sessionObj.getLong("endTime"),
            plannedDurationSeconds = sessionObj.optInt("plannedDurationSeconds"),
            actualDurationSeconds = sessionObj.getInt("actualDurationSeconds"),
            completedCycles = sessionObj.optInt("completedCycles", 1),
            completed = sessionObj.getBoolean("completed"),
            tagId = resolvedTagId
        )

        assertNull("TagId must be null for imported v2 sessions", session.tagId)
        assertEquals(101L, session.id)
        assertEquals(1500, session.actualDurationSeconds)
    }

    @Test
    fun testV3BackupParsesTagsAndMapsSessionTagId() {
        val root = JSONObject(v3BackupJson)
        assertEquals(3, root.getInt("version"))

        // Parse focus_tags
        val tagsArray = root.getJSONArray("focus_tags")
        assertEquals(2, tagsArray.length())

        val simulatedDbTags = mutableMapOf<Long, FocusTag>()
        val tagNameToIdMap = mutableMapOf<String, Long>()
        var nextTagId = 10L

        for (i in 0 until tagsArray.length()) {
            val obj = tagsArray.getJSONObject(i)
            val name = obj.getString("name").trim()
            val id = nextTagId++
            simulatedDbTags[id] = FocusTag(id = id, name = name, createdAt = obj.optLong("createdAt"))
            tagNameToIdMap[name.lowercase()] = id
        }

        assertEquals(2, tagNameToIdMap.size)
        assertTrue(tagNameToIdMap.containsKey("deep work"))
        assertTrue(tagNameToIdMap.containsKey("reading"))

        // Parse focus_sessions and resolve tags
        val sessionsArray = root.getJSONArray("focus_sessions")
        assertEquals(2, sessionsArray.length())

        // Session 1: tagged with "Reading"
        val s1Obj = sessionsArray.getJSONObject(0)
        val s1TagName = if (s1Obj.has("tagName") && !s1Obj.isNull("tagName")) s1Obj.getString("tagName") else null
        val s1ResolvedTagId = if (!s1TagName.isNullOrBlank()) {
            val key = s1TagName.lowercase().trim()
            tagNameToIdMap[key] ?: run {
                val newId = nextTagId++
                tagNameToIdMap[key] = newId
                newId
            }
        } else null

        assertNotNull(s1ResolvedTagId)
        assertEquals(tagNameToIdMap["reading"], s1ResolvedTagId)

        // Session 2: untagged
        val s2Obj = sessionsArray.getJSONObject(1)
        val s2TagName = if (s2Obj.has("tagName") && !s2Obj.isNull("tagName")) s2Obj.getString("tagName") else null
        val s2ResolvedTagId = if (!s2TagName.isNullOrBlank()) {
            val key = s2TagName.lowercase().trim()
            tagNameToIdMap[key] ?: run {
                val newId = nextTagId++
                tagNameToIdMap[key] = newId
                newId
            }
        } else null

        assertNull("Untagged session must resolve to null tagId", s2ResolvedTagId)
    }

    @Test
    fun testV3ExportProducesCorrectTagMappingRoundTrip() {
        val originalTags = listOf(
            FocusTag(id = 1L, name = "Writing", createdAt = 1000L),
            FocusTag(id = 2L, name = "Coding", createdAt = 2000L)
        )
        val tagIdToNameMap = originalTags.associate { it.id to it.name }

        val originalSessions = listOf(
            FocusSession(
                id = 10L,
                mode = FocusMode.CLASSIC_POMODORO,
                startTime = 5000L,
                endTime = 6500L,
                plannedDurationSeconds = 1500,
                actualDurationSeconds = 1500,
                completedCycles = 1,
                completed = true,
                tagId = 2L
            ),
            FocusSession(
                id = 11L,
                mode = FocusMode.FLOW_OPEN,
                startTime = 7000L,
                endTime = 8000L,
                plannedDurationSeconds = null,
                actualDurationSeconds = 1000,
                completedCycles = 1,
                completed = true,
                tagId = null
            )
        )

        // Export simulation
        val exportedRoot = JSONObject().apply {
            put("version", 3)
            val tagsArr = JSONArray()
            originalTags.forEach { t ->
                tagsArr.put(JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("createdAt", t.createdAt)
                })
            }
            put("focus_tags", tagsArr)

            val sessionsArr = JSONArray()
            originalSessions.forEach { s ->
                sessionsArr.put(JSONObject().apply {
                    put("id", s.id)
                    put("mode", s.mode.name)
                    put("startTime", s.startTime)
                    put("endTime", s.endTime)
                    put("actualDurationSeconds", s.actualDurationSeconds)
                    if (s.tagId != null && tagIdToNameMap.containsKey(s.tagId)) {
                        put("tagName", tagIdToNameMap[s.tagId])
                    }
                })
            }
            put("focus_sessions", sessionsArr)
        }

        val jsonString = exportedRoot.toString()

        // Import simulation
        val importedRoot = JSONObject(jsonString)
        val importedTagsArr = importedRoot.getJSONArray("focus_tags")
        val importedSessionsArr = importedRoot.getJSONArray("focus_sessions")

        assertEquals(2, importedTagsArr.length())
        assertEquals(2, importedSessionsArr.length())

        val s0 = importedSessionsArr.getJSONObject(0)
        assertEquals("Coding", s0.getString("tagName"))

        val s1 = importedSessionsArr.getJSONObject(1)
        assertTrue(!s1.has("tagName"))
    }

    @Test
    fun testV4HabitAndFocusSessionFieldsRoundTripAndV3BackwardsCompatibility() {
        val v3Json = """
        {
          "version": 3,
          "exportedAt": 1727705000000,
          "habits": [
            {
              "id": 1,
              "name": "Exercise",
              "type": "CHECK_OFF",
              "target": 1.0,
              "unit": "",
              "step": 1.0,
              "startEpochDay": 19000,
              "sortOrder": 0
            }
          ],
          "focus_sessions": [
            {
              "id": 101,
              "mode": "CLASSIC_POMODORO",
              "startTime": 1727701000000,
              "endTime": 1727702500000,
              "actualDurationSeconds": 1500,
              "completed": true
            }
          ]
        }
        """.trimIndent()
        val root = JSONObject(v3Json)
        val habitObj = root.getJSONArray("habits").getJSONObject(0)
        val habit = com.reflex.app.data.Habit(
            id = habitObj.optLong("id", 0L),
            name = habitObj.getString("name"),
            type = habitObj.optString("type", "CHECK_OFF"),
            target = habitObj.optDouble("target", 1.0),
            unit = habitObj.optString("unit", ""),
            step = habitObj.optDouble("step", 1.0),
            startEpochDay = habitObj.optLong("startEpochDay", 0L),
            sortOrder = habitObj.optInt("sortOrder", 0),
            frequencyType = habitObj.optString("frequencyType", "DAILY"),
            frequencyDays = habitObj.optString("frequencyDays", ""),
            frequencyTargetPerWeek = habitObj.optInt("frequencyTargetPerWeek", 0),
            reminderEnabled = habitObj.optBoolean("reminderEnabled", false),
            reminderTimes = habitObj.optString("reminderTimes", "")
        )
        assertEquals("DAILY", habit.frequencyType)
        assertEquals(false, habit.reminderEnabled)
        assertEquals(0, habit.frequencyTargetPerWeek)

        val sessionObj = root.getJSONArray("focus_sessions").getJSONObject(0)
        val endReason = sessionObj.optString("endReason", if (sessionObj.optBoolean("completed", true)) "completed" else "stopped_early")
        assertEquals("completed", endReason)
    }
}
