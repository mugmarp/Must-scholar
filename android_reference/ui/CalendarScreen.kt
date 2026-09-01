package com.must.timetable.features.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.must.timetable.AppGraph
import com.must.timetable.core.util.TimeUtil
import java.util.Calendar
import java.util.Date

@Composable
fun CalendarRoute() {
    val context = LocalContext.current
    val vm: CalendarViewModel = viewModel(factory = AppGraph.factory(context))
    val state by vm.uiState.collectAsStateWithLifecycle()
    var cursor by remember { mutableStateOf(Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }

    val classesByDay = remember(state.entries) {
        state.entries.groupBy { it.dayOfWeek }
    }
    val eventsByDay = remember(state.events) {
        state.events.groupBy { it.dayOfWeek }
    }
    val deadlinesByDay = remember(state.assignments) {
        state.assignments.filter { it.dueAtMillis != null }
            .groupBy { TimeUtil.DAYS[(Calendar.getInstance().apply { time = Date(it.dueAtMillis!!) }.get(Calendar.DAY_OF_WEEK) + 5) % 7] }
    }

    val cells = remember(cursor) {
        val year = cursor.get(Calendar.YEAR)
        val month = cursor.get(Calendar.MONTH)
        val firstDow = (Calendar.getInstance().apply { set(year, month, 1) }.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val daysInMonth = Calendar.getInstance().apply { set(year, month + 1, 0) }.get(Calendar.DAY_OF_MONTH)
        val list = mutableListOf<Date?>()
        repeat(firstDow) { list.add(null) }
        for (d in 1..daysInMonth) list.add(Calendar.getInstance().apply { set(year, month, d) }.time)
        list
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text("Calendar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = { cursor = Calendar.getInstance().apply { set(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH) - 1, 1) } }) { Text("‹") }
                Text("${cursor.getDisplayName(Calendar.MONTH, java.util.Calendar.LONG, java.util.Locale.getDefault())} ${cursor.get(Calendar.YEAR)}", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { cursor = Calendar.getInstance().apply { set(cursor.get(Calendar.YEAR), cursor.get(Calendar.MONTH) + 1, 1) } }) { Text("›") }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            TimeUtil.DAYS.forEach { Text(it.take(2), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall) }
        }
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(cells) { date ->
                if (date == null) { Spacer(Modifier.size(48.dp)) } else {
                    val cal = Calendar.getInstance().apply { time = date }
                    val dayName = TimeUtil.DAYS[(cal.get(Calendar.DAY_OF_WEEK) + 5) % 7]
                    Surface(
                        onClick = { selectedDate = date },
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("${cal.get(Calendar.DAY_OF_MONTH)}", style = MaterialTheme.typography.labelMedium)
                            Row {
                                if (classesByDay[dayName]?.isNotEmpty() == true) Box(Modifier.size(4.dp).background(Color(0xFF3B82F6), RoundedCornerShape(50)))
                                if (eventsByDay[dayName]?.isNotEmpty() == true) Box(Modifier.size(4.dp).background(Color(0xFFA855F7), RoundedCornerShape(50)))
                                if (deadlinesByDay[dayName]?.isNotEmpty() == true) Box(Modifier.size(4.dp).background(Color(0xFFEF4444), RoundedCornerShape(50)))
                            }
                        }
                    }
                }
            }
        }
    }
}