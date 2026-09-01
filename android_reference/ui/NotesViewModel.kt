package com.must.timetable.features.notes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.domain.LectureNote
import com.must.timetable.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.flow.*

data class NoteGroup(val courseCode: String, val entries: List<TimetableEntry>)
data class NotesUiState(val isLoading: Boolean = true, val groups: List<NoteGroup> = emptyList())

class NotesViewModel(repo: TimetableRepository) : ViewModel() {
    val uiState: StateFlow<NotesUiState> = repo.getLocalSchedule(repo.getProgrammePref())
        .map { entries ->
            val grouped = entries.groupBy { it.courseCode }
                .map { NoteGroup(it.key, it.value.sortedBy { e -> e.dayOfWeek }) }
                .sortedBy { it.courseCode }
            NotesUiState(false, grouped)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NotesUiState())
}