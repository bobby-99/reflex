package com.reflex.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.RecurrenceFrequency
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.Task
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.RecurrenceCalculator
import com.reflex.app.util.TaskParser
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class GroupedTasksState(
    val todayTasks: List<Task> = emptyList(),
    val tomorrowTasks: List<Task> = emptyList(),
    val upcomingTasks: List<Task> = emptyList(),
    val noDateTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList()
)

class TasksViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    val tasksState: StateFlow<GroupedTasksState> = combine(
        repository.getIncompleteTasks(),
        repository.getCompletedTasks()
    ) { incompleteList, completedList ->
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val todayTasks = mutableListOf<Task>()
        val tomorrowTasks = mutableListOf<Task>()
        val upcomingTasks = mutableListOf<Task>()
        val noDateTasks = mutableListOf<Task>()

        incompleteList.forEach { task ->
            if (task.dueDate == null) {
                noDateTasks.add(task)
            } else {
                val taskDate = Instant.ofEpochMilli(task.dueDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                when {
                    taskDate.isEqual(today) || taskDate.isBefore(today) -> todayTasks.add(task)
                    taskDate.isEqual(tomorrow) -> tomorrowTasks.add(task)
                    else -> upcomingTasks.add(task)
                }
            }
        }

        GroupedTasksState(
            todayTasks = todayTasks,
            tomorrowTasks = tomorrowTasks,
            upcomingTasks = upcomingTasks,
            noDateTasks = noDateTasks,
            completedTasks = completedList
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GroupedTasksState()
    )

    fun addTaskFromNaturalLanguage(input: String, context: Context? = null) {
        if (input.isBlank()) return
        val parsed = TaskParser.parse(input)
        val reminderToUse = parsed.dueTime ?: (if (parsed.dueDate != null) parsed.dueDate + (9 * 3600000L) else null)
        val newTask = Task(
            title = parsed.title,
            dueDate = parsed.dueDate,
            dueTime = parsed.dueTime,
            priority = parsed.priority,
            reminderTime = reminderToUse,
            recurrenceFrequency = parsed.recurrenceFrequency,
            recurrenceInterval = parsed.recurrenceInterval,
            recurrenceUnit = parsed.recurrenceUnit,
            recurrenceDaysOfWeek = parsed.recurrenceDaysOfWeek,
            recurrenceMonthlyMode = parsed.recurrenceMonthlyMode
        )
        viewModelScope.launch {
            val insertedId = repository.insertTask(newTask)
            if (context != null && reminderToUse != null && reminderToUse > System.currentTimeMillis()) {
                AlarmScheduler.scheduleTaskReminder(context, newTask.copy(id = insertedId))
            }
        }
    }

    fun toggleTaskComplete(task: Task, context: Context? = null) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task, context)
        }
    }

    fun updateTask(task: Task, context: Context? = null) {
        val now = System.currentTimeMillis()
        val reminderToUse = when {
            task.dueTime != null && task.dueTime > now && (task.reminderTime == null || task.reminderTime <= now) -> task.dueTime
            task.reminderTime != null -> task.reminderTime
            task.dueTime != null -> task.dueTime
            task.dueDate != null -> task.dueDate + (9 * 3600000L)
            else -> null
        }
        val updatedTask = task.copy(reminderTime = reminderToUse)

        viewModelScope.launch {
            repository.updateTask(updatedTask)
            if (context != null) {
                if (!updatedTask.isCompleted && reminderToUse != null && reminderToUse > System.currentTimeMillis()) {
                    AlarmScheduler.scheduleTaskReminder(context, updatedTask)
                } else {
                    AlarmScheduler.cancelTaskReminder(context, updatedTask.id)
                }
            }
        }
    }

    fun clearCompletedTasks(context: Context? = null) {
        viewModelScope.launch {
            repository.deleteAllCompletedTasks()
        }
    }

    fun deleteTask(task: Task, context: Context? = null) {
        viewModelScope.launch {
            repository.deleteTask(task, context)
            if (context != null) {
                AlarmScheduler.cancelTaskReminder(context, task.id)
            }
        }
    }

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TasksViewModel(repository) as T
        }
    }
}
