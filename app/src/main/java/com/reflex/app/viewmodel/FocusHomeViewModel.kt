package com.reflex.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflex.app.data.FocusMode
import com.reflex.app.data.FocusSettings
import com.reflex.app.data.ReflexRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import com.reflex.app.data.FocusTag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

data class FocusHomeUiState(
    val settings: FocusSettings = FocusSettings(),
    val selectedMode: FocusMode = FocusMode.CLASSIC_POMODORO,
    val timedFlowDurationMin: Int = 30
)

class FocusHomeViewModel(
    private val repository: ReflexRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusHomeUiState())
    val uiState: StateFlow<FocusHomeUiState> = _uiState.asStateFlow()

    val allTags: Flow<List<FocusTag>> = repository.getAllFocusTags()

    init {
        viewModelScope.launch {
            repository.getFocusSettings().collectLatest { settings ->
                if (settings != null) {
                    _uiState.value = _uiState.value.copy(settings = settings)
                }
            }
        }
    }

    fun selectMode(mode: FocusMode) {
        _uiState.value = _uiState.value.copy(selectedMode = mode)
    }

    fun updateTimedFlowDuration(minutes: Int) {
        _uiState.value = _uiState.value.copy(timedFlowDurationMin = minutes)
    }

    fun saveSettings(settings: FocusSettings) {
        viewModelScope.launch {
            repository.saveFocusSettings(settings)
        }
    }

    suspend fun createTag(rawName: String): Result<FocusTag> {
        val trimmed = rawName.trim().take(24)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        val existing = repository.getAllFocusTagsSync()
        if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Tag already exists"))
        }
        val tag = FocusTag(name = trimmed)
        val id = repository.insertFocusTag(tag)
        return Result.success(tag.copy(id = id))
    }

    suspend fun renameTag(tag: FocusTag, rawName: String): Result<Unit> {
        val trimmed = rawName.trim().take(24)
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tag name cannot be empty"))
        }
        val existing = repository.getAllFocusTagsSync()
        if (existing.any { it.id != tag.id && it.name.equals(trimmed, ignoreCase = true) }) {
            return Result.failure(IllegalArgumentException("Another tag already has this name"))
        }
        repository.updateFocusTag(tag.copy(name = trimmed))
        return Result.success(Unit)
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            repository.deleteFocusTag(tagId)
        }
    }

    class Factory(private val repository: ReflexRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusHomeViewModel(repository) as T
        }
    }
}
