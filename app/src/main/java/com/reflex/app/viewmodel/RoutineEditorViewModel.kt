package com.reflex.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.Routine
import com.reflex.app.data.RoutineTemplates
import com.reflex.app.data.Step
import com.reflex.app.util.AlarmScheduler
import com.reflex.app.util.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek

data class RoutineEditorUiState(
    val routineId: Long = 0L,
    val name: String = "",
    val iconKey: String = "BOLT",
    val steps: List<Step> = emptyList(),
    val isArchived: Boolean = false,
    val restBetweenStepsEnabled: Boolean = false,
    val restDurationSeconds: Int = 15,
    val scheduledDays: Set<DayOfWeek> = emptySet(),
    val reminderTime: String? = null,
    val reminderEnabled: Boolean = false,
    val validationError: String? = null,
    val isSaved: Boolean = false,
    val isLoading: Boolean = true
)

class RoutineEditorViewModel(
    private val repository: ReflexRepository,
    private val routineId: Long,
    private val templateId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineEditorUiState(routineId = routineId))
    val uiState: StateFlow<RoutineEditorUiState> = _uiState.asStateFlow()

    init {
        if (routineId != 0L) {
            loadExistingRoutine(routineId)
        } else if (!templateId.isNullOrBlank()) {
            loadFromTemplate(templateId)
        } else {
            val restSetting = SettingsRepository.getString("r_rest", "15s")
            val isRestEnabled = restSetting != "Off"
            val restSec = when (restSetting) {
                "10s" -> 10
                "15s" -> 15
                "20s" -> 20
                "30s" -> 30
                else -> 15
            }
            _uiState.value = _uiState.value.copy(
                restBetweenStepsEnabled = isRestEnabled,
                restDurationSeconds = restSec,
                isLoading = false
            )
        }
    }

    private fun loadExistingRoutine(id: Long) {
        viewModelScope.launch {
            val routineWithSteps = repository.getRoutineWithSteps(id).firstOrNull()
            if (routineWithSteps != null) {
                _uiState.value = RoutineEditorUiState(
                    routineId = routineWithSteps.routine.id,
                    name = routineWithSteps.routine.name,
                    iconKey = routineWithSteps.routine.icon,
                    steps = routineWithSteps.steps.sortedBy { it.orderIndex },
                    isArchived = routineWithSteps.routine.isArchived,
                    restBetweenStepsEnabled = routineWithSteps.routine.restBetweenStepsEnabled,
                    restDurationSeconds = routineWithSteps.routine.restDurationSeconds,
                    scheduledDays = routineWithSteps.routine.scheduledDaysSet,
                    reminderTime = routineWithSteps.routine.reminderTime,
                    reminderEnabled = routineWithSteps.routine.reminderEnabled,
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadFromTemplate(id: String) {
        val tmpl = RoutineTemplates.templates.find { it.id == id }
        if (tmpl != null) {
            val restSetting = SettingsRepository.getString("r_rest", "15s")
            val isRestEnabled = restSetting != "Off"
            val restSec = when (restSetting) {
                "10s" -> 10
                "15s" -> 15
                "20s" -> 20
                "30s" -> 30
                else -> 15
            }
            val mappedSteps = tmpl.steps.mapIndexed { idx, st ->
                Step(
                    routineId = 0L,
                    name = st.name,
                    stepType = st.stepType,
                    durationSeconds = st.durationSeconds,
                    targetCount = st.targetCount,
                    notes = st.notes,
                    emoji = st.emoji,
                    orderIndex = idx
                )
            }
            _uiState.value = _uiState.value.copy(
                name = tmpl.name,
                iconKey = tmpl.iconKey,
                restBetweenStepsEnabled = if (tmpl.restBetweenStepsEnabled) true else isRestEnabled,
                restDurationSeconds = if (tmpl.restBetweenStepsEnabled) tmpl.restDurationSeconds else restSec,
                steps = mappedSteps,
                isLoading = false
            )
        } else {
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun updateName(newName: String) {
        _uiState.value = _uiState.value.copy(
            name = newName,
            validationError = if (newName.isNotBlank() && _uiState.value.steps.isNotEmpty()) null else _uiState.value.validationError
        )
    }

    fun updateIcon(newIconKey: String) {
        _uiState.value = _uiState.value.copy(iconKey = newIconKey)
    }

    fun updateRestEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(restBetweenStepsEnabled = enabled)
    }

    fun updateRestDuration(seconds: Int) {
        val clamped = seconds.coerceIn(10, 30)
        _uiState.value = _uiState.value.copy(restDurationSeconds = clamped)
    }

    fun toggleScheduledDay(day: DayOfWeek) {
        val current = _uiState.value.scheduledDays.toMutableSet()
        if (current.contains(day)) {
            current.remove(day)
        } else {
            current.add(day)
        }
        _uiState.value = _uiState.value.copy(scheduledDays = current)
    }

    fun updateReminderTime(timeStr: String?) {
        _uiState.value = _uiState.value.copy(reminderTime = timeStr)
    }

    fun updateReminderEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reminderEnabled = enabled)
    }

    fun addOrUpdateStep(step: Step) {
        val currentSteps = _uiState.value.steps.toMutableList()
        val existingIndex = currentSteps.indexOfFirst { it.id == step.id && step.id != 0L }

        if (existingIndex != -1) {
            currentSteps[existingIndex] = step
        } else {
            val tempIndex = currentSteps.indexOfFirst { it.orderIndex == step.orderIndex }
            if (tempIndex != -1 && step.id == 0L) {
                currentSteps[tempIndex] = step
            } else {
                currentSteps.add(step.copy(orderIndex = currentSteps.size))
            }
        }

        val reindexed = currentSteps.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
        _uiState.value = _uiState.value.copy(
            steps = reindexed,
            validationError = null
        )
    }

    fun removeStep(step: Step) {
        val updated = _uiState.value.steps.filterNot {
            (it.id != 0L && it.id == step.id) || (it.id == 0L && it.orderIndex == step.orderIndex && it.name == step.name)
        }.mapIndexed { idx, s -> s.copy(orderIndex = idx) }

        _uiState.value = _uiState.value.copy(steps = updated)
    }

    fun moveStepUp(index: Int) {
        if (index <= 0 || index >= _uiState.value.steps.size) return
        val list = _uiState.value.steps.toMutableList()
        val temp = list[index]
        list[index] = list[index - 1]
        list[index - 1] = temp
        val reindexed = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
        _uiState.value = _uiState.value.copy(steps = reindexed)
    }

    fun moveStepDown(index: Int) {
        if (index < 0 || index >= _uiState.value.steps.size - 1) return
        val list = _uiState.value.steps.toMutableList()
        val temp = list[index]
        list[index] = list[index + 1]
        list[index + 1] = temp
        val reindexed = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
        _uiState.value = _uiState.value.copy(steps = reindexed)
    }

    fun reorderSteps(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in 0.._uiState.value.steps.lastIndex) return
        if (toIndex !in 0.._uiState.value.steps.lastIndex) return

        val list = _uiState.value.steps.toMutableList()
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        val reindexed = list.mapIndexed { idx, s -> s.copy(orderIndex = idx) }
        _uiState.value = _uiState.value.copy(steps = reindexed)
    }

    fun saveRoutine(context: Context, onSuccess: () -> Unit) {
        val name = _uiState.value.name.trim()
        val steps = _uiState.value.steps

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(validationError = "Routine name cannot be empty")
            return
        }
        if (steps.isEmpty()) {
            _uiState.value = _uiState.value.copy(validationError = "Routine must have at least 1 step")
            return
        }

        viewModelScope.launch {
            val daysStr = _uiState.value.scheduledDays.joinToString(",") { it.name }
            val routine = Routine(
                id = _uiState.value.routineId,
                name = name,
                icon = _uiState.value.iconKey,
                isArchived = _uiState.value.isArchived,
                restBetweenStepsEnabled = _uiState.value.restBetweenStepsEnabled,
                restDurationSeconds = _uiState.value.restDurationSeconds,
                scheduledDays = daysStr,
                reminderTime = _uiState.value.reminderTime,
                reminderEnabled = _uiState.value.reminderEnabled
            )
            val savedRoutineId = repository.saveRoutineWithSteps(routine, steps)
            val finalRoutine = routine.copy(id = savedRoutineId)
            AlarmScheduler.scheduleRoutineReminder(context, finalRoutine)

            _uiState.value = _uiState.value.copy(isSaved = true, validationError = null)
            onSuccess()
        }
    }

    fun deleteRoutine(context: Context, onSuccess: () -> Unit) {
        if (_uiState.value.routineId == 0L) {
            onSuccess()
            return
        }
        viewModelScope.launch {
            val routine = Routine(
                id = _uiState.value.routineId,
                name = _uiState.value.name,
                icon = _uiState.value.iconKey
            )
            AlarmScheduler.cancelRoutineReminder(context, routine.id)
            repository.deleteRoutine(routine, context)
            onSuccess()
        }
    }

    class Factory(
        private val repository: ReflexRepository,
        private val routineId: Long,
        private val templateId: String? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoutineEditorViewModel(repository, routineId, templateId) as T
        }
    }
}
