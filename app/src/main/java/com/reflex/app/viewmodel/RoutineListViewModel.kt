package com.reflex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.RoutineWithSteps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class RoutineListUiState(
    val activeRoutines: List<RoutineWithSteps> = emptyList(),
    val archivedRoutines: List<RoutineWithSteps> = emptyList(),
    val showArchived: Boolean = false,
    val isLoading: Boolean = true
)

class RoutineListViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()

    private val _uiState = MutableStateFlow(RoutineListUiState())
    val uiState: StateFlow<RoutineListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.getAllRoutines(),
                repository.getActiveRoutinesWithSteps(),
                _showArchived
            ) { allRoutines, activeRoutinesWithSteps, showArchived ->
                val activeIds = activeRoutinesWithSteps.map { it.routine.id }.toSet()

                // Active routines with steps
                val activeList = activeRoutinesWithSteps

                // Archived routines with steps
                val archivedRoutines = allRoutines.filter { it.isArchived }
                val archivedWithSteps = archivedRoutines.map { routine ->
                    // Fetch steps for archived if any
                    RoutineWithSteps(routine = routine, steps = emptyList())
                }

                RoutineListUiState(
                    activeRoutines = activeList,
                    archivedRoutines = archivedWithSteps,
                    showArchived = showArchived,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }

    fun archiveRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.archiveRoutine(routineId)
        }
    }

    fun unarchiveRoutine(routineId: Long) {
        viewModelScope.launch {
            repository.unarchiveRoutine(routineId)
        }
    }

    fun deleteRoutine(routineWithSteps: RoutineWithSteps) {
        viewModelScope.launch {
            repository.deleteRoutine(routineWithSteps.routine)
        }
    }

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoutineListViewModel(repository) as T
        }
    }
}
