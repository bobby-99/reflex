package com.reflex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.reflex.app.data.ReflexRepository

class CompletionSummaryViewModel(
    private val repository: ReflexRepository,
    private val routineId: Long,
    private val logId: Long
) : ViewModel() {

    // Completion summary logic will be implemented in a later build phase

    class Factory(
        private val repository: ReflexRepository,
        private val routineId: Long,
        private val logId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CompletionSummaryViewModel(repository, routineId, logId) as T
        }
    }
}
