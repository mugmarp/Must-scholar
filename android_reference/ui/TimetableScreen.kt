package com.must.timetable.features.timetable.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.must.timetable.features.timetable.domain.TimetableEntry

@Composable
fun TimetableScreen(
    viewModel: TimetableViewModel,
    onLectureClick: (TimetableEntry) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TimetableHeader(
            draftVersion = state.draftVersion,
            syncStatus = state.syncStatus,
            isSyncing = state.isSyncing,
            onSyncClick = { viewModel.syncSchedule() }
        )
        Spacer(modifier = Modifier.height(12.dp))
        NextUpCard(entries = state.entries)
        Spacer(modifier = Modifier.height(16.dp))
        DaySelector(
            selectedDay = state.selectedDay,
            onDaySelected = { viewModel.selectDay(it) }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OverlapTimeline(entries = state.entries, onLectureClick = onLectureClick)
    }
}

@Composable
fun TimetableHeader(
    draftVersion: String, syncStatus: String, isSyncing: Boolean, onSyncClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Good Morning, Student",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (draftVersion.isNotEmpty()) {
                    Text("Academic Week - $draftVersion",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(syncStatus.ifEmpty { "Tap to sync" }, style = MaterialTheme.typography.labelSmall)
                TextButton(onClick = onSyncClick, enabled = !isSyncing) {
                    Text(if (isSyncing) "Syncing..." else "Sync Now")
                }
            }
        }
    }
}

@Composable
fun DaySelector(selectedDay: String, onDaySelected: (String) -> Unit) {
    val days = listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(days) { day ->
            FilterChip(selected = day == selectedDay, onClick = { onDaySelected(day) },
                label = { Text(day.take(3)) })
        }
    }
}