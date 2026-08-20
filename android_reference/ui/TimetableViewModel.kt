package com.must.timetable.features.timetable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.timetable.features.timetable.data.SyncResult
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TimetableUiState(
    val isLoading: Boolean = false,
    val entries: List<TimetableEntry> = emptyList(),
    val selectedDay: String = "Monday",
    val selectedProgramme: String = "MBR I",
    val isSyncing: Boolean = false,
    val syncStatus: String = "",
    val draftVersion: String = ""
)

class TimetableViewModel(
    private val repository: TimetableRepository
) : ViewModel() {

    private val _selectedDay = MutableStateFlow("Monday")
    private val _selectedProgramme = MutableStateFlow("MBR I")
    private val _syncStatus = MutableStateFlow("")
    private val _isSyncing = MutableStateFlow(false)
    private val _draftVersion = MutableStateFlow("")

    val uiState: StateFlow<TimetableUiState> = combine(
        repository.getLocalSchedule("MBR I"),
        _selectedDay,
        _syncStatus,
        _isSyncing,
        _draftVersion
    ) { entries, day, syncStatus, isSyncing, draft ->
        TimetableUiState(
            isLoading = false,
            entries = entries.filter { it.dayOfWeek == day },
            selectedDay = day,
            syncStatus = syncStatus,
            isSyncing = isSyncing,
            draftVersion = draft
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        TimetableUiState()
    )

    fun selectDay(day: String) { _selectedDay.value = day }

    fun syncSchedule() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val result = repository.syncRemoteSchedule("MBR I")) {
                is SyncResult.Success -> {
                    _syncStatus.value = "Synced (${result.draftVersion})"
                    _draftVersion.value = result.draftVersion
                }
                SyncResult.NotModified -> _syncStatus.value = "Up to date"
                SyncResult.Offline -> _syncStatus.value = "Offline - using cached"
                is SyncResult.Error -> _syncStatus.value = "Sync failed"
            }
            _isSyncing.value = false
        }
    }
}