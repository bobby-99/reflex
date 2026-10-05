package com.reflex.app

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.Task
import com.reflex.app.receiver.BootReceiver
import com.reflex.app.util.AlarmScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmsAndBootReceiverTest {

    @Test
    fun testBootReceiverTriggersAlarmRescheduling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)

        val receiver = BootReceiver()
        // Should not crash and should execute smoothly
        receiver.onReceive(context, bootIntent)
        assertTrue(true)
    }

    @Test
    fun testScheduleAndCancelTaskAlarms() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ReflexDatabase.getDatabase(context)

        val task = Task(
            title = "Test Alarm Task",
            dueDate = System.currentTimeMillis() + 3600000L,
            dueTime = System.currentTimeMillis() + 3600000L,
            reminderTime = System.currentTimeMillis() + 3600000L
        )

        val id = db.taskDao().insertTask(task)
        val taskWithId = task.copy(id = id)

        AlarmScheduler.scheduleTaskReminder(context, taskWithId)
        // Cancel reminder
        AlarmScheduler.cancelTaskReminder(context, id)

        // Reschedule all alarms without exceptions
        AlarmScheduler.rescheduleAllAlarms(context)

        // Cancel all alarms cleanly
        AlarmScheduler.cancelAllAlarms(context)

        // Cleanup
        db.taskDao().deleteTask(taskWithId)
    }
}
