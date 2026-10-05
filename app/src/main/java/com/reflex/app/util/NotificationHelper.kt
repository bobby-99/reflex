package com.reflex.app.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.reflex.app.MainActivity
import com.reflex.app.R
import com.reflex.app.data.Task
import com.reflex.app.receiver.NotificationActionReceiver

object NotificationHelper {

    const val CHANNEL_TASK_REMINDERS = "channel_task_reminders"
    const val CHANNEL_ROUTINE_REMINDERS = "channel_routine_reminders"
    const val CHANNEL_FOCUS_ALERTS = "channel_focus_alerts"
    const val CHANNEL_HABIT_NUDGES = "channel_habit_nudges"
    const val CHANNEL_TIMER_SERVICE = "channel_timer_service"
    const val CHANNEL_FOCUS_TIMER = "channel_focus_timer"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority exact alerts for due tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true)
            }

            val routineChannel = NotificationChannel(
                CHANNEL_ROUTINE_REMINDERS,
                "Routine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for scheduled routines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                enableLights(true)
            }

            val focusAlertsChannel = NotificationChannel(
                CHANNEL_FOCUS_ALERTS,
                "Focus Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when focus or break intervals end"
                enableVibration(true)
            }

            val habitNudgesChannel = NotificationChannel(
                CHANNEL_HABIT_NUDGES,
                "Habit Nudges",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Evening habit check-ins and streak reminders"
                enableVibration(true)
            }

            val timerChannel = NotificationChannel(
                CHANNEL_TIMER_SERVICE,
                "Running Routine Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent controls while a routine timer is active"
            }

            val focusChannel = NotificationChannel(
                CHANNEL_FOCUS_TIMER,
                "Focus & Pomodoro Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent controls for active Focus sessions"
                enableVibration(false)
                setSound(null, null)
            }

            manager.createNotificationChannel(taskChannel)
            manager.createNotificationChannel(routineChannel)
            manager.createNotificationChannel(focusAlertsChannel)
            manager.createNotificationChannel(habitNudgesChannel)
            manager.createNotificationChannel(timerChannel)
            manager.createNotificationChannel(focusChannel)
        }
    }

    fun showTaskReminderNotification(context: Context, task: Task) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, "tasks")
            putExtra(EXTRA_TASK_ID, task.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Complete Action
        val completeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_MARK_COMPLETE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze 10 Min Action
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_SNOOZE
            putExtra(NotificationActionReceiver.EXTRA_TASK_ID, task.id)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (task.id * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle("TASK DUE: ${task.title}")
            .setContentText(task.notes ?: "Tap to open Reflex tasks")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "MARK COMPLETE", completePendingIntent)
            .addAction(android.R.drawable.ic_menu_recent_history, "SNOOZE 10 MIN", snoozePendingIntent)

        if (task.priority == com.reflex.app.data.Priority.HIGH || task.priority == com.reflex.app.data.Priority.MEDIUM) {
            val fullScreenIntent = Intent(context, com.reflex.app.ui.PriorityTaskAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(com.reflex.app.ui.PriorityTaskAlertActivity.EXTRA_TASK_ID, task.id)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                (task.id * 10 + 3).toInt(),
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setFullScreenIntent(fullScreenPendingIntent, true)
        }

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(task.id.toInt(), builder.build())
        } catch (e: SecurityException) {
            AppLog.w("NotificationHelper", "Notification permission not granted for task reminder", e)
        }
    }

    fun showRoutineReminderNotification(context: Context, routine: com.reflex.app.data.Routine) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, "routines")
            putExtra(EXTRA_START_ROUTINE_ID, routine.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (routine.id + 50000).toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ROUTINE_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle("Routine reminder: ${routine.name}")
            .setContentText("Tap to start your routine now")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify((routine.id + 50000).toInt(), builder.build())
        } catch (e: SecurityException) {
            AppLog.w("NotificationHelper", "Notification permission not granted for routine reminder", e)
        }
    }

    fun showTestNotification(context: Context) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO, "tasks")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle("REFLEX TEST NOTIFICATION")
            .setContentText("Background alarm & notification delivery verified successfully!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify(9999, builder.build())
        } catch (e: SecurityException) {
            AppLog.w("NotificationHelper", "Notification permission not granted for test notification", e)
        }
    }

    fun showHabitReminderNotification(context: Context, habit: com.reflex.app.data.Habit) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NAVIGATE_TO, "habits")
            putExtra(EXTRA_HABIT_ID, habit.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            (habit.id + 70000).toInt(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_HABIT_NUDGES)
            .setSmallIcon(R.drawable.ic_stat_reflex)
            .setColor(0xFFD9A184.toInt())
            .setContentTitle("Habit reminder: ${habit.name}")
            .setContentText(if (!habit.notes.isNullOrBlank()) habit.notes else "Time to make progress on your habit!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        val manager = NotificationManagerCompat.from(context)
        try {
            manager.notify((habit.id + 70000).toInt(), builder.build())
        } catch (e: SecurityException) {
            AppLog.w("NotificationHelper", "Notification permission not granted for habit reminder", e)
        }
    }

    const val EXTRA_START_ROUTINE_ID = "extra_start_routine_id"
    const val EXTRA_NAVIGATE_TO = "extra_navigate_to"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_HABIT_ID = "extra_habit_id"
}
