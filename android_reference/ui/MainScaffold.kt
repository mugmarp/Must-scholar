package com.must.timetable.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.must.timetable.AppGraph
import com.must.timetable.core.util.RequestNotificationPermission
import com.must.timetable.features.calendar.ui.CalendarRoute
import com.must.timetable.features.notes.ui.NotesRoute
import com.must.timetable.features.onboarding.ui.WelcomeScreen
import com.must.timetable.features.settings.ui.SettingsRoute
import com.must.timetable.features.tasks.ui.TasksRoute
import com.must.timetable.features.timetable.ui.TimetableRoute

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab("timetable", "Timetable", Icons.Default.Today),
    Tab("calendar", "Calendar", Icons.Default.CalendarMonth),
    Tab("notes", "Notes", Icons.Default.Edit),
    Tab("tasks", "Tasks", Icons.Default.CheckCircle),
    Tab("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun MainScaffold() {
    val context = LocalContext.current
    var welcomed by remember { mutableStateOf(AppGraph.repository(context).welcomed) }
    if (!welcomed) {
        WelcomeScreen(onDone = { welcomed = true })
        return
    }
    RequestNotificationPermission()

    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination

    Scaffold(bottomBar = {
        NavigationBar {
            TABS.forEach { tab ->
                NavigationBarItem(
                    selected = current?.hierarchy?.any { it.route == tab.route } == true,
                    onClick = {
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                    label = { Text(tab.label) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = "timetable", modifier = Modifier.padding(padding)) {
            composable("timetable") { TimetableRoute() }
            composable("calendar") { CalendarRoute() }
            composable("notes") { NotesRoute() }
            composable("tasks") { TasksRoute() }
            composable("settings") { SettingsRoute() }
        }
    }
}