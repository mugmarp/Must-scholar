package com.must.timetable.features.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.must.timetable.features.timetable.data.TimetableRepository
import com.must.timetable.features.timetable.domain.Assignment
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TasksUiState(
    val isLoading: Boolean = true,
    val tasks: List<Assignment> = emptyList(),
    val query: String = "",
    val hideDone: Boolean = false
)

class TasksViewModel(private val repo: TimetableRepository) : ViewModel() {
    private val _query = MutableStateFlow("")
    private val _hideDone = MutableStateFlow(false)

    val uiState: StateFlow<TasksUiState> = combine(
        repo.getAssignments(), _query, _hideDone
    ) { tasks, q, hide ->
        val filtered = tasks
            .filter { (!hide || !it.completed) && matchesQuery(it, q) }
            .sortedWith(compareBy({ it.completed }, { it.dueAtMillis ?: Long.MAX_VALUE }))
        TasksUiState(false, filtered, q, hide)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TasksUiState())

    private fun matchesQuery(a: Assignment, q: String): Boolean =
        q.isBlank() || a.title.contains(q, true) || (a.courseCode ?: "").contains(q, true)

    fun setQuery(q: String) { _query.value = q }
    fun toggleHideDone() { _hideDone.value = !_hideDone.value }

    fun toggleComplete(a: Assignment) {
        viewModelScope.launch { repo.saveAssignment(a.copy(completed = !a.completed)) }
    }

    fun save(a: Assignment) { viewModelScope.launch { repo.saveAssignment(a) } }
    fun delete(id: Long) { viewModelScope.launch { repo.deleteAssignment(id) } }
}