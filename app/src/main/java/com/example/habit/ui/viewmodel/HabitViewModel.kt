package com.example.habit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.habit.data.AiScanResult
import com.example.habit.data.HabitRepository
import com.example.habit.data.LeaderboardRow
import com.example.habit.data.local.entity.PendingAiHabitEntity
import com.example.habit.data.local.entity.TrackedHabitEntity
import com.example.habit.data.local.entity.UserProfileEntity
import com.example.habit.data.model.CheckInOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HabitViewModel(
    private val repository: HabitRepository,
) : ViewModel() {

    val profile: StateFlow<UserProfileEntity?> = repository.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val trackedHabits: StateFlow<List<TrackedHabitEntity>> = repository.trackedHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pendingAi: StateFlow<List<PendingAiHabitEntity>> = repository.pendingAi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _leaderboard = MutableStateFlow<Pair<List<LeaderboardRow>, LeaderboardRow>?>(null)
    val leaderboard: StateFlow<Pair<List<LeaderboardRow>, LeaderboardRow>?> = _leaderboard.asStateFlow()

    private val _scanFeedback = MutableStateFlow<AiScanResult?>(null)
    val scanFeedback: StateFlow<AiScanResult?> = _scanFeedback.asStateFlow()

    fun consumeScanFeedback() {
        _scanFeedback.value = null
    }

    init {
        refreshLeaderboard()
    }

    fun refreshLeaderboard() {
        viewModelScope.launch {
            _leaderboard.value = repository.buildLeaderboard()
        }
    }

    fun ensureProfile() {
        viewModelScope.launch { repository.ensureProfile() }
    }

    fun completeOnboarding(selectedLibraryIds: List<String>) {
        viewModelScope.launch {
            repository.completeOnboarding(selectedLibraryIds)
            refreshLeaderboard()
        }
    }

    fun submitCheckIn(habitId: String, outcome: CheckInOutcome) {
        viewModelScope.launch {
            repository.submitCheckIn(habitId, outcome)
            refreshLeaderboard()
        }
    }

    fun trackLibraryHabit(libraryId: String) {
        viewModelScope.launch {
            repository.addTrackedHabitFromLibrary(libraryId)
            refreshLeaderboard()
        }
    }

    fun runAiScan(text: String, isWeekly: Boolean) {
        viewModelScope.launch {
            _scanFeedback.value = repository.runAiScan(text, isWeekly)
            refreshLeaderboard()
        }
    }

    fun acceptAiDetection(id: String) {
        viewModelScope.launch {
            repository.acceptPendingAi(id)
            refreshLeaderboard()
        }
    }

    fun dismissAiDetection(id: String) {
        viewModelScope.launch {
            repository.dismissPendingAi(id)
        }
    }

    companion object {
        fun factory(repository: HabitRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HabitViewModel(repository) as T
                }
            }
    }
}
