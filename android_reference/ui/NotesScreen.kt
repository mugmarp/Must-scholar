package com.must.timetable.features.notes.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph

@Composable
fun NotesRoute() {
    val context = LocalContext.current
    val vm: NotesViewModel = viewModel(factory = AppGraph.factory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (state.groups.isEmpty()) {
            Text("No lectures yet. Sync your timetable to start taking notes.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.groups) { group ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(group.courseCode, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        group.entries.forEach { e ->
                            Text("• ${e.dayOfWeek} ${e.startTime} — ${e.courseTitle}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}