package com.reflex.app.data

import android.content.Context
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.PriorityTaskOverlayManager
import com.reflex.app.util.RecurrenceCalculator
import kotlinx.coroutines.flow.Flow

class ReflexRepository(
    private val routineDao: RoutineDao,
    private val stepDao: StepDao,
    private val completionLogDao: CompletionLogDao,
    private val taskDao: TaskDao,
    private val focusSettingsDao: FocusSettingsDao? = null,
    private val focusSessionDao: FocusSessionDao? = null,
    private val habitDao: HabitDao? = null,
    private val focusTagDao: FocusTagDao? = null,
    private val blockedAppEventDao: BlockedAppEventDao? = null
) {
    // --- Focus Settings, Sessions & Tags ---

    fun getFocusSettings(): Flow<FocusSettings?> =
        focusSettingsDao?.getFocusSettings() ?: kotlinx.coroutines.flow.flowOf(FocusSettings())

    suspend fun getFocusSettingsSync(): FocusSettings =
        focusSettingsDao?.getFocusSettingsSync() ?: FocusSettings()

    suspend fun saveFocusSettings(settings: FocusSettings) {
        focusSettingsDao?.insertOrUpdateSettings(settings)
    }

    suspend fun saveFocusSession(session: FocusSession): Long {
        return focusSessionDao?.insertSession(session) ?: 0L
    }

    fun getAllFocusSessions(): Flow<List<FocusSession>> =
        focusSessionDao?.getAllSessions() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getAllSessions(): Flow<List<FocusSession>> = getAllFocusSessions()

    fun getAllFocusTags(): Flow<List<FocusTag>> =
        focusTagDao?.getAllTags() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getAllFocusTagsSync(): List<FocusTag> =
        focusTagDao?.getAllTagsSync() ?: emptyList()

    suspend fun getFocusTagById(id: Long): FocusTag? =
        focusTagDao?.getTagById(id)

    suspend fun getFocusTagByName(name: String): FocusTag? =
        focusTagDao?.getTagByName(name)

    suspend fun insertFocusTag(tag: FocusTag): Long =
        focusTagDao?.insertTag(tag) ?: 0L

    suspend fun updateFocusTag(tag: FocusTag) {
        focusTagDao?.updateTag(tag)
    }

    suspend fun deleteFocusTag(tagId: Long) {
        focusSessionDao?.clearTagFromSessions(tagId)
        focusTagDao?.deleteTagById(tagId)
    }

    fun getFocusSessionsByTag(tagId: Long): Flow<List<FocusSession>> =
        focusSessionDao?.getSessionsByTag(tagId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getFocusSessionsInDateRangeWithTag(startDate: Long, endDate: Long, tagId: Long?): Flow<List<FocusSession>> =
        focusSessionDao?.getSessionsInDateRangeWithTag(startDate, endDate, tagId) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getFocusTimeByTagInRange(startDate: Long, endDate: Long): Flow<List<TagFocusStat>> =
        focusSessionDao?.getFocusTimeByTagInRange(startDate, endDate) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getTotalFocusTimeByTag(): Flow<List<TagFocusStat>> =
        focusSessionDao?.getTotalFocusTimeByTag() ?: kotlinx.coroutines.flow.flowOf(emptyList())


    // --- Routines ---

    fun getActiveRoutines(): Flow<List<Routine>> = routineDao.getActiveRoutines()

    fun getAllRoutines(): Flow<List<Routine>> = routineDao.getAllRoutines()

    fun getRoutineById(id: Long): Flow<Routine?> = routineDao.getRoutineById(id)

    fun getRoutineWithSteps(id: Long): Flow<RoutineWithSteps?> = routineDao.getRoutineWithSteps(id)

    fun getActiveRoutinesWithSteps(): Flow<List<RoutineWithSteps>> = routineDao.getActiveRoutinesWithSteps()

    suspend fun insertRoutine(routine: Routine): Long = routineDao.insertRoutine(routine)

    suspend fun updateRoutine(routine: Routine) = routineDao.updateRoutine(routine)

    suspend fun deleteRoutine(routine: Routine, context: Context? = null) {
        if (context != null) {
            AlarmScheduler.cancelRoutineReminder(context, routine.id)
        }
        completionLogDao.deleteLogsForRoutine(routine.id)
        routineDao.deleteRoutine(routine)
    }

    suspend fun archiveRoutine(routineId: Long) = routineDao.setArchived(routineId, true)

    suspend fun unarchiveRoutine(routineId: Long) = routineDao.setArchived(routineId, false)

    suspend fun saveRoutineWithSteps(routine: Routine, steps: List<Step>): Long {
        val routineId = if (routine.id == 0L) {
            routineDao.insertRoutine(routine)
        } else {
            routineDao.updateRoutine(routine)
            stepDao.deleteStepsForRoutine(routine.id)
            routine.id
        }
        val reorderedSteps = steps.mapIndexed { index, step ->
            step.copy(id = 0, routineId = routineId, orderIndex = index)
        }
        stepDao.insertSteps(reorderedSteps)
        return routineId
    }

    // --- Steps ---

    fun getStepsForRoutine(routineId: Long): Flow<List<Step>> = stepDao.getStepsForRoutine(routineId)

    suspend fun getStepById(stepId: Long): Step? = stepDao.getStepById(stepId)

    suspend fun insertStep(step: Step): Long = stepDao.insertStep(step)

    suspend fun insertSteps(steps: List<Step>) = stepDao.insertSteps(steps)

    suspend fun updateStep(step: Step) = stepDao.updateStep(step)

    suspend fun deleteStep(step: Step) = stepDao.deleteStep(step)

    suspend fun deleteStepsForRoutine(routineId: Long) = stepDao.deleteStepsForRoutine(routineId)

    // --- Completion Logs ---

    fun getLogsForRoutine(routineId: Long): Flow<List<CompletionLog>> =
        completionLogDao.getLogsForRoutine(routineId)

    fun getLogsInDateRange(startDate: Long, endDate: Long): Flow<List<CompletionLog>> =
        completionLogDao.getLogsInDateRange(startDate, endDate)

    fun getLogsForRoutineInDateRange(
        routineId: Long,
        startDate: Long,
        endDate: Long
    ): Flow<List<CompletionLog>> =
        completionLogDao.getLogsForRoutineInDateRange(routineId, startDate, endDate)

    fun getAllLogs(): Flow<List<CompletionLog>> = completionLogDao.getAllLogs()

    fun getCompletionCountForRoutine(routineId: Long): Flow<Int> =
        completionLogDao.getCompletionCountForRoutine(routineId)

    suspend fun insertLog(log: CompletionLog): Long = completionLogDao.insertLog(log)

    suspend fun deleteLog(log: CompletionLog) = completionLogDao.deleteLog(log)

    // --- Tasks ---

    fun getIncompleteTasks(): Flow<List<Task>> = taskDao.getIncompleteTasks()

    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()

    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTaskById(taskId: Long): Flow<Task?> = taskDao.getTaskById(taskId)

    suspend fun getTaskByIdSync(taskId: Long): Task? = taskDao.getTaskByIdSync(taskId)

    fun getTasksForDate(date: Long): Flow<List<Task>> = taskDao.getTasksForDate(date)

    fun getTasksInDateRange(startDate: Long, endDate: Long): Flow<List<Task>> =
        taskDao.getTasksInDateRange(startDate, endDate)

    fun getTasksWithNoDate(): Flow<List<Task>> = taskDao.getTasksWithNoDate()

    suspend fun insertTask(task: Task): Long = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task, context: Context? = null) {
        if (context != null) {
            AlarmScheduler.cancelTaskReminder(context, task.id)
            PriorityTaskOverlayManager.hide()
        }
        taskDao.deleteTask(task)
    }

    suspend fun toggleTaskCompleted(task: Task, context: Context? = null) {
        val newCompleted = !task.isCompleted
        val completedAt = if (newCompleted) System.currentTimeMillis() else null
        taskDao.setCompleted(task.id, newCompleted, completedAt)

        if (newCompleted) {
            if (context != null) {
                AlarmScheduler.cancelTaskReminder(context, task.id)
            }

            // Automatic next occurrence generation and alarm scheduling for repeating tasks
            if (task.recurrenceFrequency != RecurrenceFrequency.NONE) {
                val nextOccurrence = RecurrenceCalculator.computeNextOccurrence(task)
                if (nextOccurrence != null) {
                    val nextId = taskDao.insertTask(nextOccurrence)
                    val nextTaskWithId = nextOccurrence.copy(id = nextId)
                    if (context != null && nextTaskWithId.reminderTime != null && nextTaskWithId.reminderTime > System.currentTimeMillis()) {
                        AlarmScheduler.scheduleTaskReminder(context, nextTaskWithId)
                    }
                }
            }
        }
    }

    suspend fun deleteAllCompletedTasks() = taskDao.deleteAllCompletedTasks()

    suspend fun clearHistory() {
        completionLogDao.deleteAllLogs()
        focusSessionDao?.deleteAllSessions()
        habitDao?.deleteAllLogs()
    }

    suspend fun wipeAllData() {
        completionLogDao.deleteAllLogs()
        focusSessionDao?.deleteAllSessions()
        focusTagDao?.deleteAllTags()
        habitDao?.deleteAllLogs()
        habitDao?.deleteAllHabits()
        stepDao.deleteAllSteps()
        routineDao.deleteAllRoutines()
        taskDao.deleteAllTasks()
    }

    // --- Habits & Habit Logs ---

    fun getAllHabits(): Flow<List<Habit>> =
        habitDao?.getAllHabits() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getAllHabitsSync(): List<Habit> =
        habitDao?.getAllHabitsSync() ?: emptyList()

    suspend fun getHabitById(id: Long): Habit? =
        habitDao?.getHabitById(id)

    suspend fun saveHabit(habit: Habit): Long {
        return if (habit.id == 0L) {
            habitDao?.insertHabit(habit) ?: 0L
        } else {
            habitDao?.updateHabit(habit)
            habit.id
        }
    }

    suspend fun insertHabits(habits: List<Habit>): List<Long> =
        habitDao?.insertHabits(habits) ?: emptyList()

    suspend fun updateHabits(habits: List<Habit>) {
        habitDao?.updateHabits(habits)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao?.deleteLogsForHabit(habit.id)
        habitDao?.deleteHabit(habit)
    }

    fun getLogsForDay(epochDay: Long): Flow<List<HabitLog>> =
        habitDao?.getLogsForDay(epochDay) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getLogsForDaySync(epochDay: Long): List<HabitLog> =
        habitDao?.getLogsForDaySync(epochDay) ?: emptyList()

    fun getLogsInRange(startEpochDay: Long, endEpochDay: Long): Flow<List<HabitLog>> =
        habitDao?.getLogsInRange(startEpochDay, endEpochDay) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getAllHabitLogs(): Flow<List<HabitLog>> =
        habitDao?.getAllLogs() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun getAllHabitLogsSync(): List<HabitLog> =
        habitDao?.getAllLogsSync() ?: emptyList()

    suspend fun getHabitLog(habitId: Long, epochDay: Long): HabitLog? =
        habitDao?.getLog(habitId, epochDay)

    suspend fun setHabitLogValue(habitId: Long, epochDay: Long, value: Double) {
        habitDao?.upsertLog(HabitLog(habitId = habitId, epochDay = epochDay, value = value))
    }

    suspend fun insertHabitLogs(logs: List<HabitLog>) {
        habitDao?.insertLogs(logs)
    }

    suspend fun deleteHabitLog(habitId: Long, epochDay: Long) {
        habitDao?.deleteLog(habitId, epochDay)
    }

    suspend fun wipeHabits() {
        habitDao?.deleteAllLogs()
        habitDao?.deleteAllHabits()
    }

    // --- Blocked App Events ---

    suspend fun recordBlockedAppEvent(packageName: String, timestamp: Long = System.currentTimeMillis()) {
        blockedAppEventDao?.insertEvent(BlockedAppEvent(packageName = packageName, timestamp = timestamp))
    }

    fun getBlockedAppEventsSince(sinceMs: Long): Flow<List<BlockedAppEvent>> =
        blockedAppEventDao?.getEventsSince(sinceMs) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    fun getBlockedAppEventsInRange(startMs: Long, endMs: Long): Flow<List<BlockedAppEvent>> =
        blockedAppEventDao?.getEventsInRange(startMs, endMs) ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun hasAnyBlockedEvents(): Boolean =
        (blockedAppEventDao?.getEventCount() ?: 0) > 0

    suspend fun clearAllBlockedEvents() {
        blockedAppEventDao?.deleteAllEvents()
    }
}

