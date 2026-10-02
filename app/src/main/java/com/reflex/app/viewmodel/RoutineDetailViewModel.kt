package com.reflex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.ReflexRepository
import com.reflex.app.data.RoutineWithSteps
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RoutineDetailViewModel(
    private val repository: ReflexRepository,
    private val routineId: Long
) : ViewModel() {

    val routineWithSteps: StateFlow<RoutineWithSteps?> = repository.getRoutineWithSteps(routineId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    class Factory(
        private val repository: ReflexRepository,
        private val routineId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RoutineDetailViewModel(repository, routineId) as T
        }
    }
}
