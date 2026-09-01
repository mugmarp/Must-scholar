package com.must.timetable.features.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph
import com.must.timetable.core.alarm.ReminderScheduler
import com.must.timetable.core.util.RequestNotificationPermission
import com.must.timetable.features.timetable.ui.TimetableViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute() {
    val context = LocalContext.current
    val vm: TimetableViewModel = viewModel(factory = AppGraph.factory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var programmeMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Text("Programme", style = MaterialTheme.typography.titleSmall)
        ExposedDropdownMenuBox(expanded = programmeMenu, onExpandedChange = { programmeMenu = it }) {
            OutlinedTextField(
                value = state.selectedProgramme, onValueChange = {}, readOnly = true,
                label = { Text("Programme") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = programmeMenu) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = programmeMenu, onDismissRequest = { programmeMenu = false }) {
                state.programmes.forEach { p ->
                    DropdownMenuItem(text = { Text(p) }, onClick = { vm.selectProgramme(p); programmeMenu = false })
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Reminders", style = MaterialTheme.typography.titleSmall)
        Button(onClick = { ReminderScheduler(context).rescheduleAllAsync() }) { Text("Re-arm all reminders") }
        Spacer(Modifier.height(8.dp))
        RequestNotificationPermission()

        Spacer(Modifier.height(24.dp))
        Text("Sync", style = MaterialTheme.typography.titleSmall)
        Button(onClick = { vm.syncSchedule() }, enabled = !state.isSyncing) {
            Text(if (state.isSyncing) "Syncing…" else "Sync timetable")
        }
        if (state.syncStatus.isNotEmpty()) {
            Text(state.syncStatus, style = MaterialTheme.typography.labelSmall)
        }
    }
}