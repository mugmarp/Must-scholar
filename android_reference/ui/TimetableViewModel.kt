package com.must.timetable.features.timetable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.data.SyncResult
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class DayItem {
    data class Lecture(val entry: TimetableEntry) : DayItem()
    data class Event(val event: CustomEvent) : DayItem()
}

data class TimetableUiState(
    val isLoading: Boolean = true,
    val programmes: List<String> = emptyList(),
    val selectedProgramme: String = "MBR I",
    val selectedDay: String = "",
    val today: String = "",
    val dayItems: List<DayItem> = emptyList(),
    val allEntries: List<TimetableEntry> = emptyList(),
    val allEvents: List<CustomEvent> = emptyList(),
    val isSyncing: Boolean = false,
    val syncStatus: String = "",
    val draftVersion: String = ""
)

class TimetableViewModel(private val repo: TimetableRepository) : ViewModel() {

    private val _selectedDay = MutableStateFlow("")
    private val _selectedProgramme = MutableStateFlow(repo.getProgrammePref())
    private val _syncStatus = MutableStateFlow("")
    private val _isSyncing = MutableStateFlow(false)
    private val _draftVersion = MutableStateFlow("")

    private val entriesFlow = _selectedProgramme.flatMapLatest { repo.getLocalSchedule(it) }

    val uiState: StateFlow<TimetableUiState> = combine(
        repo.getAllProgrammeGroups(),
        entriesFlow,
        repo.getCustomEvents(),
        _selectedDay,
        _selectedProgramme
    ) { programmes, entries, events, day, programme ->
        val today = TimeUtil.todayName()
        val effDay = if (day.isEmpty()) today else day
        val lectures = entries.filter { it.dayOfWeek == effDay }.map { DayItem.Lecture(it) }
        val evs = events.filter { it.dayOfWeek == effDay }.map { DayItem.Event(it) }
        val items = (lectures + evs).sortedBy { item ->
            TimeUtil.toMinutes(
                when (item) {
                    is DayItem.Lecture -> item.entry.startTime
                    is DayItem.Event -> item.event.startTime
                }
            )
        }
        TimetableUiState(
            isLoading = false,
            programmes = programmes,
            selectedProgramme = programme,
            selectedDay = effDay,
            today = today,
            dayItems = items,
            allEntries = entries,
            allEvents = events
        )
    }.combine(_syncStatus, _isSyncing, _draftVersion) { state, status, syncing, draft ->
        state.copy(syncStatus = status, isSyncing = syncing, draftVersion = draft)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimetableUiState())

    fun selectDay(day: String) { _selectedDay.value = day }

    fun selectProgramme(programme: String) {
        repo.setProgrammePref(programme)
        _selectedProgramme.value = programme
    }

    fun syncSchedule() {
        viewModelScope.launch {
            _isSyncing.value = true
            when (val r = repo.syncRemoteSchedule(_selectedProgramme.value)) {
                is SyncResult.Success -> { _syncStatus.value = "Synced"; _draftVersion.value = r.draftVersion }
                SyncResult.NotModified -> _syncStatus.value = "Up to date"
                SyncResult.Offline -> _syncStatus.value = "Offline"
                is SyncResult.Error -> _syncStatus.value = "Sync failed"
            }
            _isSyncing.value = false
        }
    }
}