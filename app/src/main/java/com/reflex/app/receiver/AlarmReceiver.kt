package com.reflex.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reflex.app.ReflexApplication
import com.reflex.app.util.AppLog
import com.reflex.app.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TASK_REMINDER -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId != -1L) {
                    val app = context.applicationContext as ReflexApplication
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val task = app.repository.getTaskByIdSync(taskId)
                            if (task != null && !task.isCompleted) {
                                NotificationHelper.showTaskReminderNotification(context, task)

                                // System-wide overlay and lockscreen popup for High and Medium priority tasks
                                if (task.priority == com.reflex.app.data.Priority.HIGH || task.priority == com.reflex.app.data.Priority.MEDIUM) {
                                    try {
                                        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                                        val wakeLock = powerManager?.newWakeLock(
                                            android.os.PowerManager.PARTIAL_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                                            "Reflex:PriorityTaskAlertWakeLock"
                                        )
                                        wakeLock?.acquire(10000L) // 10 seconds
                                    } catch (e: Exception) {
                                        AppLog.w(TAG, "Failed to acquire wake lock for priority alert", e)
                                    }

                                    com.reflex.app.util.PriorityTaskOverlayManager.show(context.applicationContext, task)
                                }
                            }
                        } catch (e: Exception) {
                            AppLog.e(TAG, "Error handling task reminder alarm", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_HABIT_REMINDER -> {
                val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
                if (habitId != -1L) {
                    val app = context.applicationContext as ReflexApplication
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val habit = app.repository.getHabitById(habitId)
                            if (habit != null && habit.reminderEnabled) {
                                NotificationHelper.showHabitReminderNotification(context, habit)
                                com.reflex.app.util.AlarmScheduler.scheduleHabitReminders(context, habit)
                            }
                        } catch (e: Exception) {
                            AppLog.e(TAG, "Error handling habit reminder alarm", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_ROUTINE_REMINDER -> {
                val routineId = intent.getLongExtra(EXTRA_ROUTINE_ID, -1L)
                if (routineId != -1L) {
                    val app = context.applicationContext as ReflexApplication
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val data = app.repository.getRoutineWithSteps(routineId).firstOrNull()
                            if (data != null && data.routine.reminderEnabled && !data.routine.isArchived) {
                                NotificationHelper.showRoutineReminderNotification(context, data.routine)
                                com.reflex.app.util.AlarmScheduler.scheduleRoutineReminder(context, data.routine)
                            }
                        } catch (e: Exception) {
                            AppLog.e(TAG, "Error handling routine reminder alarm", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_TEST_NOTIFICATION -> {
                NotificationHelper.showTestNotification(context)
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val ACTION_TASK_REMINDER = "com.reflex.productivity.ACTION_TASK_REMINDER"
        const val ACTION_HABIT_REMINDER = "com.reflex.productivity.ACTION_HABIT_REMINDER"
        const val ACTION_ROUTINE_REMINDER = "com.reflex.productivity.ACTION_ROUTINE_REMINDER"
        const val ACTION_TEST_NOTIFICATION = "com.reflex.productivity.ACTION_TEST_NOTIFICATION"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_TASK_TITLE = "extra_task_title"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
    }
}
