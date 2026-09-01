package com.must.timetable.features.tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph
import com.must.timetable.core.alarm.ReminderScheduler
import com.must.timetable.core.util.TimeUtil
import com.must.timetable.features.timetable.domain.Assignment
import com.must.timetable.features.timetable.domain.Priority
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TasksRoute() {
    val context = LocalContext.current
    val vm: TasksViewModel = viewModel(factory = AppGraph.factory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Assignment?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = Assignment() }) {
                Icon(Icons.Default.Add, contentDescription = "Add task")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.query, onValueChange = { vm.setQuery(it) },
                    placeholder = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(selected = state.hideDone, onClick = { vm.toggleHideDone() }, label = { Text("Hide done") })
            }
            Spacer(Modifier.height(8.dp))
            if (state.tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.tasks) { a ->
                        TaskRow(a, onClick = { editing = a }, onToggle = { vm.toggleComplete(a) })
                    }
                }
            }
        }
    }

    editing?.let { a ->
        AssignmentSheet(
            initial = a,
            onDismiss = { editing = null },
            onConfirm = {
                vm.save(it)
                ReminderScheduler(context).rescheduleAllAsync()
                editing = null
            },
            onDelete = { vm.delete(it); ReminderScheduler(context).rescheduleAllAsync(); editing = null }
        )
    }
}

@Composable
private fun TaskRow(a: Assignment, onClick: () -> Unit, onToggle: () -> Unit) {
    val dueLabel = a.dueAtMillis?.let {
        val now = System.currentTimeMillis()
        val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
        if (it < now && !a.completed) "Overdue • ${fmt.format(Date(it))}" else fmt.format(Date(it))
    }
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    if (a.completed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (a.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(a.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {}, label = { Text(a.priority.name.lowercase()) })
                }
                if (a.courseCode != null || dueLabel != null) {
                    Text(
                        listOfNotNull(a.courseCode, dueLabel).joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}