package com.must.timetable.features.calendar.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.domain.Assignment
import com.must.timetable.features.timetable.domain.CustomEvent
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.*

data class CalendarUiState(
    val isLoading: Boolean = true,
    val entries: List<TimetableEntry> = emptyList(),
    val events: List<CustomEvent> = emptyList(),
    val assignments: List<Assignment> = emptyList()
)

class CalendarViewModel(repo: TimetableRepository) : ViewModel() {
    val uiState: StateFlow<CalendarUiState> = combine(
        repo.getLocalSchedule(repo.getProgrammePref()),
        repo.getCustomEvents(),
        repo.getAssignments()
    ) { entries, events, assignments ->
        CalendarUiState(false, entries, events, assignments.filter { !it.completed })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())
}