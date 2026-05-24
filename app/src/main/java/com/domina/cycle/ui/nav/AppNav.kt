package com.domina.cycle.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.domina.cycle.ui.calendar.CalendarScreen
import com.domina.cycle.ui.log.LogScreen
import com.domina.cycle.ui.meds.MedsScreen
import com.domina.cycle.ui.settings.SettingsScreen
import com.domina.cycle.ui.today.TodayScreen
import java.time.LocalDate

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NavigationBarItem(
                selected = currentRoute == Destinations.TODAY || currentRoute == null,
                onClick = { nav.navigate(Destinations.TODAY) },
                icon = { Icon(Icons.Filled.Home, null) },
                label = { Text("Today") },
                colors = navColors,
            )
            NavigationBarItem(
                selected = currentRoute == Destinations.CALENDAR,
                onClick = { nav.navigate(Destinations.CALENDAR) },
                icon = { Icon(Icons.Filled.CalendarMonth, null) },
                label = { Text("Calendar") },
                colors = navColors,
            )
            NavigationBarItem(
                selected = currentRoute == Destinations.SETTINGS,
                onClick = { nav.navigate(Destinations.SETTINGS) },
                icon = { Icon(Icons.Filled.Settings, null) },
                label = { Text("Settings") },
                colors = navColors,
            )
        }
    }) { padding ->
        NavHost(nav, startDestination = Destinations.TODAY, modifier = Modifier.padding(padding)) {
            composable(Destinations.TODAY) { TodayScreen(onLogToday = { nav.navigate(Destinations.LOG) }) }
            composable(Destinations.CALENDAR) { CalendarScreen() }
            composable(Destinations.LOG) { LogScreen(date = LocalDate.now(), onSaved = { nav.popBackStack() }) }
            composable(Destinations.SETTINGS) { SettingsScreen(onOpenMeds = { nav.navigate(Destinations.MEDS) }) }
            composable(Destinations.MEDS) { MedsScreen() }
        }
    }
}
