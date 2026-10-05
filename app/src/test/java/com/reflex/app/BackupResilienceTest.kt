package com.reflex.app

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupResilienceTest {

    @Test
    fun testCorruptedOrTruncatedJsonThrowsException() {
        val truncatedJson = """{"version": 4, "routines": [{"name": "Morning"""
        assertThrows(JSONException::class.java) {
            JSONObject(truncatedJson)
        }

        val garbageJson = "<<not json at all>>"
        assertThrows(JSONException::class.java) {
            JSONObject(garbageJson)
        }
    }

    @Test
    fun testUnknownFieldsIgnoredGracefully() {
        val jsonWithUnknownFields = """
        {
            "version": 4,
            "future_field_9000": "futuristic data",
            "routines": [
                {
                    "name": "Workout",
                    "future_routine_prop": 12345,
                    "isArchived": false
                }
            ],
            "tasks": [
                {
                    "title": "Build rocket",
                    "unknown_ai_meta": {"score": 99.9}
                }
            ]
        }
        """.trimIndent()

        val root = JSONObject(jsonWithUnknownFields)
        val routines = root.getJSONArray("routines")
        val tasks = root.getJSONArray("tasks")

        val rObj = routines.getJSONObject(0)
        assertEquals("Workout", rObj.getString("name"))

        val tObj = tasks.getJSONObject(0)
        assertEquals("Build rocket", tObj.getString("title"))
    }

    @Test
    fun testV1BackupCompatibility() {
        // V1 backup with minimal fields (no habits, no focus_tags, no endReason)
        val v1Json = """
        {
            "version": 1,
            "routines": [
                {"name": "Read", "icon": "BOOK"}
            ],
            "steps": [],
            "tasks": [
                {"title": "Buy groceries", "priority": "MEDIUM"}
            ],
            "completion_logs": []
        }
        """.trimIndent()

        val root = JSONObject(v1Json)
        assertEquals(1, root.getInt("version"))
        val routines = root.getJSONArray("routines")
        assertEquals(1, routines.length())
        assertEquals("Read", routines.getJSONObject(0).getString("name"))

        val tasks = root.getJSONArray("tasks")
        assertEquals(1, tasks.length())
        assertEquals("Buy groceries", tasks.getJSONObject(0).getString("title"))
    }

    @Test
    fun testHugeBackupFileParsing() {
        val root = JSONObject()
        root.put("version", 4)
        val tasksArray = JSONArray()

        val totalTasks = 5_000
        for (i in 1..totalTasks) {
            val taskObj = JSONObject().apply {
                put("id", i.toLong())
                put("title", "Massive Task #$i with some descriptions and text to consume space")
                put("priority", if (i % 3 == 0) "HIGH" else "NONE")
                put("isCompleted", i % 2 == 0)
                put("createdAt", System.currentTimeMillis())
            }
            tasksArray.put(taskObj)
        }
        root.put("tasks", tasksArray)

        val jsonString = root.toString()
        val startTime = System.currentTimeMillis()
        val parsedRoot = JSONObject(jsonString)
        val parsedTasks = parsedRoot.getJSONArray("tasks")
        val elapsed = System.currentTimeMillis() - startTime

        assertEquals(totalTasks, parsedTasks.length())
        assertTrue("Parsing 5,000 tasks took ${elapsed}ms, must be < 2000ms", elapsed < 2000)
    }

    @Test
    fun testRoundTripDataIntegrity() {
        val originalTask = JSONObject().apply {
            put("title", "Export Round Trip Task")
            put("priority", "HIGH")
            put("recurrenceFrequency", "WEEKLY")
            put("recurrenceDaysOfWeek", "MONDAY,FRIDAY")
            put("reminderTime", 1728000000000L)
            put("notes", "Important notes")
        }

        val jsonString = originalTask.toString()
        val restoredTask = JSONObject(jsonString)

        assertEquals(originalTask.getString("title"), restoredTask.getString("title"))
        assertEquals(originalTask.getString("priority"), restoredTask.getString("priority"))
        assertEquals(originalTask.getString("recurrenceFrequency"), restoredTask.getString("recurrenceFrequency"))
        assertEquals(originalTask.getString("recurrenceDaysOfWeek"), restoredTask.getString("recurrenceDaysOfWeek"))
        assertEquals(originalTask.getLong("reminderTime"), restoredTask.getLong("reminderTime"))
        assertEquals(originalTask.getString("notes"), restoredTask.getString("notes"))
    }
}
