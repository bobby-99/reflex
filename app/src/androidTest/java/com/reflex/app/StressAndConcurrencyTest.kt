package com.reflex.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reflex.app.data.Priority
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.Routine
import com.reflex.app.data.Task
import com.reflex.app.util.RecurrenceCalculator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StressAndConcurrencyTest {

    @Test
    fun testDatabaseStressWithMassiveData() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ReflexDatabase.getDatabase(context)

        val taskDao = db.taskDao()
        val routineDao = db.routineDao()

        // Measure batch insertion of 1,000 tasks
        val startInsert = System.currentTimeMillis()
        val tasks = (1..1000).map { i ->
            Task(
                title = "Performance Benchmark Task #$i",
                priority = if (i % 3 == 0) Priority.HIGH else Priority.LOW,
                dueDate = System.currentTimeMillis() + (i * 86400000L),
                isCompleted = i % 2 == 0
            )
        }

        tasks.forEach { taskDao.insertTask(it) }
        val insertElapsed = System.currentTimeMillis() - startInsert
        assertTrue("Inserting 1,000 tasks took ${insertElapsed}ms, must be < 5000ms", insertElapsed < 5000)

        // Measure query cold latency
        val startQuery = System.currentTimeMillis()
        val incomplete = taskDao.getIncompleteTasks().first()
        val queryElapsed = System.currentTimeMillis() - startQuery

        assertTrue("Querying incomplete tasks took ${queryElapsed}ms, must be < 500ms", queryElapsed < 500)
        assertTrue("Incomplete tasks must contain inserted items", incomplete.isNotEmpty())

        // Cleanup inserted test tasks
        taskDao.deleteAllCompletedTasks()
    }

    @Test
    fun testRapidTapConcurrentCompletionRaces() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ReflexDatabase.getDatabase(context)
        val taskDao = db.taskDao()

        val repeatingTask = Task(
            title = "Race Condition Habit",
            recurrenceFrequency = RecurrenceFrequency.DAILY,
            dueDate = System.currentTimeMillis()
        )
        val taskId = taskDao.insertTask(repeatingTask)
        val savedTask = repeatingTask.copy(id = taskId)

        val app = context.applicationContext as com.reflex.app.ReflexApplication
        val repo = app.repository

        // Simulate 10 rapid concurrent clicks attempting to toggle and complete the task via repository
        (1..10).map {
            async {
                repo.toggleTaskCompleted(savedTask, context)
            }
        }.awaitAll()

        val allOccurrences = taskDao.getAllTasks().first().filter { it.title == "Race Condition Habit" }
        // Exactly 2 tasks should exist: the completed task and 1 new recurring occurrence!
        assertEquals("Exactly 1 new occurrence should be generated despite concurrent taps", 2, allOccurrences.size)

        // Cleanup
        allOccurrences.forEach { taskDao.deleteTask(it) }
    }
}
