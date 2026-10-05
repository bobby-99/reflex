package com.reflex.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.reflex.app.data.ReflexDatabase
import com.reflex.app.data.Task
import com.reflex.app.receiver.AlarmReceiver
import kotlinx.coroutines.flow.first

object AlarmScheduler {

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            return alarmManager.canScheduleExactAlarms()
        }
        return true
    }

    fun scheduleTaskReminder(context: Context, task: Task) {
        val triggerTime = task.reminderTime ?: task.dueTime ?: return
        if (triggerTime <= System.currentTimeMillis()) return // Don't schedule past alarms

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TASK_REMINDER
            putExtra(AlarmReceiver.EXTRA_TASK_ID, task.id)
            putExtra(AlarmReceiver.EXTRA_TASK_TITLE, task.title)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Highest-priority exact alarm clock delivery for Doze mode immunity
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: SecurityException) {
            AppLog.w("AlarmScheduler", "Exact alarm permission not granted, falling back to inexact", e)
            // Fallback if exact alarm permission revoked
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        }
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TASK_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleTestNotification(context: Context, delayMs: Long = 10000L) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = System.currentTimeMillis() + delayMs

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_TEST_NOTIFICATION
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            AppLog.e("AlarmScheduler", "Failed to schedule test notification", e)
        }
    }

    fun scheduleRoutineReminder(context: Context, routine: com.reflex.app.data.Routine) {
        if (!routine.reminderEnabled || routine.scheduledDays.isBlank() || routine.reminderTime.isNullOrBlank()) {
            cancelRoutineReminder(context, routine.id)
            return
        }

        val (hour, minute) = try {
            val parts = routine.reminderTime.split(":")
            Pair(parts[0].toInt(), parts[1].toInt())
        } catch (e: Exception) {
            return
        }

        val nextTriggerTime = getNextRoutineTriggerMillis(routine.scheduledDaysSet, hour, minute) ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ROUTINE_REMINDER
            putExtra(AlarmReceiver.EXTRA_ROUTINE_ID, routine.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (routine.id + 50000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTriggerTime, pendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            AppLog.w("AlarmScheduler", "Failed to set exact routine reminder, falling back to inexact", e)
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
        }
    }

    fun cancelRoutineReminder(context: Context, routineId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ROUTINE_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (routineId + 50000).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun getNextRoutineTriggerMillis(scheduledDays: Set<java.time.DayOfWeek>, hour: Int, minute: Int): Long? {
        if (scheduledDays.isEmpty()) return null
        val now = java.time.LocalDateTime.now()
        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        if (candidate.isBefore(now) || !scheduledDays.contains(candidate.dayOfWeek)) {
            do {
                candidate = candidate.plusDays(1)
            } while (!scheduledDays.contains(candidate.dayOfWeek))
        }

        return candidate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    fun scheduleHabitReminders(context: Context, habit: com.reflex.app.data.Habit) {
        if (!habit.reminderEnabled || habit.reminderTimes.isBlank()) {
            cancelHabitReminders(context, habit.id)
            return
        }

        val times = habit.reminderTimes.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        times.forEachIndexed { index, timeStr ->
            val (hour, minute) = try {
                val parts = timeStr.split(":")
                Pair(parts[0].toInt(), parts[1].toInt())
            } catch (e: Exception) {
                return@forEachIndexed
            }

            val nextTriggerTime = getNextHabitTriggerMillis(habit, hour, minute) ?: return@forEachIndexed

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_HABIT_REMINDER
                putExtra(AlarmReceiver.EXTRA_HABIT_ID, habit.id)
            }

            val requestCode = (habit.id * 100 + index + 70000).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val alarmClockInfo = AlarmManager.AlarmClockInfo(nextTriggerTime, pendingIntent)
                    alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
                }
            } catch (e: Exception) {
                AppLog.w("AlarmScheduler", "Failed to set exact habit reminder, falling back to inexact", e)
                alarmManager.set(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent)
            }
        }
    }

    fun cancelHabitReminders(context: Context, habitId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_HABIT_REMINDER
        }
        for (index in 0 until 10) {
            val requestCode = (habitId * 100 + index + 70000).toInt()
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    fun getNextHabitTriggerMillis(habit: com.reflex.app.data.Habit, hour: Int, minute: Int): Long? {
        val now = java.time.LocalDateTime.now()
        var candidate = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)

        when (habit.frequencyType) {
            "SPECIFIC_DAYS" -> {
                val daysSet = habit.frequencyDays.split(",")
                    .mapNotNull { dayStr ->
                        try { java.time.DayOfWeek.valueOf(dayStr.trim().uppercase()) } catch (_: Exception) { null }
                    }.toSet()
                if (daysSet.isEmpty()) return null
                if (candidate.isBefore(now) || !daysSet.contains(candidate.dayOfWeek)) {
                    do {
                        candidate = candidate.plusDays(1)
                    } while (!daysSet.contains(candidate.dayOfWeek))
                }
            }
            else -> {
                if (candidate.isBefore(now)) {
                    candidate = candidate.plusDays(1)
                }
            }
        }

        return candidate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    suspend fun rescheduleAllAlarms(context: Context) {
        val db = ReflexDatabase.getDatabase(context)
        val incompleteTasks = db.taskDao().getIncompleteTasks().first()
        val now = System.currentTimeMillis()
        incompleteTasks.forEach { task ->
            val triggerTime = task.reminderTime ?: task.dueTime
            if (triggerTime != null && triggerTime > now) {
                scheduleTaskReminder(context, task)
            }
        }

        val routines = db.routineDao().getAllRoutines().first()
        routines.filter { it.reminderEnabled && !it.isArchived }.forEach { routine ->
            scheduleRoutineReminder(context, routine)
        }

        val habits = db.habitDao().getAllHabitsSync()
        habits.filter { it.reminderEnabled }.forEach { habit ->
            scheduleHabitReminders(context, habit)
        }
    }

    suspend fun cancelAllAlarms(context: Context) {
        val db = ReflexDatabase.getDatabase(context)
        val tasks = db.taskDao().getAllTasks().first()
        tasks.forEach { task ->
            cancelTaskReminder(context, task.id)
        }
        val routines = db.routineDao().getAllRoutines().first()
        routines.forEach { routine ->
            cancelRoutineReminder(context, routine.id)
        }
        val habits = db.habitDao().getAllHabitsSync()
        habits.forEach { habit ->
            cancelHabitReminders(context, habit.id)
        }
    }
}
