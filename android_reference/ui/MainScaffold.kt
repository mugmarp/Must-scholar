package com.must.timetable.ui

/**
 * Mirrors src/components/BottomNav.jsx: pill-style bottom bar —
 * active tab gets a primary/15 pill behind the icon plus tiny 10sp label.
 */
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHost
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

// Icon mapping mirrors the web TABS (Calendar, CalendarDays, StickyNote, CheckCircle2, Settings)
private val TABS = listOf(
    Tab("timetable", "Timetable", Icons.Default.DateRange),
    Tab("calendar", "Calendar", Icons.Default.CalendarMonth),
    Tab("notes", "Notes", Icons.Default.Edit),
    Tab("tasks", "Tasks", Icons.Default.CheckCircle),
    Tab("settings", "Settings", Icons.Default.Settings)
)

@Composable
fun MainScaffold() {
    AppTheme {
        val context = LocalContext.current
        var welcomed by remember { mutableStateOf(AppGraph.repository(context).welcomed) }
        if (!welcomed) {
            WelcomeScreen(onDone = { welcomed = true })
            return@AppTheme
        }
        RequestNotificationPermission()

        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val current = backStack?.destination

        Scaffold(bottomBar = { BottomBar(current) { route -> navigate(navController, route) } }) { padding ->
            NavHost(navController, startDestination = "timetable", modifier = Modifier.padding(padding)) {
                composable("timetable") { TimetableRoute() }
                composable("calendar") { CalendarRoute() }
                composable("notes") { NotesRoute() }
                composable("tasks") { TasksRoute() }
                composable("settings") { SettingsRoute() }
            }
        }
    }
}

private fun navigate(navController: androidx.navigation.NavController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun BottomBar(current: NavDestination?, onTab: (String) -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            TABS.forEach { tab ->
                val active = current?.hierarchy?.any { it.route == tab.route } == true
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTab(tab.route) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // icon pill: w-11 h-8 rounded-full, primary/15 when active
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            tab.icon, tab.label,
                            modifier = Modifier.size(20.dp),
                            tint = if (active) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        tab.label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (active) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}