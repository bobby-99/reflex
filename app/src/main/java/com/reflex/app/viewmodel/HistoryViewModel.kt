package com.reflex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.reflex.app.data.CompletionLog
import com.reflex.app.data.ReflexRepository
import kotlinx.coroutines.flow.Flow

class HistoryViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    val allLogs: Flow<List<CompletionLog>> = repository.getAllLogs()

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
