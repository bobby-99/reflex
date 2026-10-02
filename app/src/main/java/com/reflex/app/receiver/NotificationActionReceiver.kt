package com.reflex.app.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.reflex.app.ReflexApplication
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val app = context.applicationContext as ReflexApplication
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cancel notification right away on action tap
        notificationManager.cancel(taskId.toInt())

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_MARK_COMPLETE -> {
                        val task = app.repository.getTaskByIdSync(taskId)
                        if (task != null) {
                            app.repository.toggleTaskCompleted(task, context)
                        }
                    }
                    ACTION_SNOOZE -> {
                        val task = app.repository.getTaskByIdSync(taskId)
                        if (task != null) {
                            val snoozedReminderTime = System.currentTimeMillis() + (10 * 60 * 1000L) // 10 mins
                            val snoozedTask = task.copy(reminderTime = snoozedReminderTime)
                            app.repository.updateTask(snoozedTask)
                            AlarmScheduler.scheduleTaskReminder(context, snoozedTask)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLog.e(TAG, "Error processing notification action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
        const val ACTION_MARK_COMPLETE = "com.reflex.productivity.ACTION_MARK_COMPLETE"
        const val ACTION_SNOOZE = "com.reflex.productivity.ACTION_SNOOZE"
        const val EXTRA_TASK_ID = "extra_task_id"
    }
}
