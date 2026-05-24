package com.domina.cycle.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domina.cycle.ui.calendar.CalendarScreen
import com.domina.cycle.ui.log.LogScreen
import com.domina.cycle.ui.today.TodayScreen
import java.time.LocalDate

@Composable
fun AppNav() {
    val nav = rememberNavController()
    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = true, onClick = { nav.navigate(Destinations.TODAY) },
                icon = { Icon(Icons.Filled.Home, null) }, label = { Text("Today") })
            NavigationBarItem(selected = false, onClick = { nav.navigate(Destinations.CALENDAR) },
                icon = { Icon(Icons.Filled.CalendarMonth, null) }, label = { Text("Calendar") })
        }
    }) { padding ->
        NavHost(nav, startDestination = Destinations.TODAY, modifier = Modifier.padding(padding)) {
            composable(Destinations.TODAY) { TodayScreen(onLogToday = { nav.navigate(Destinations.LOG) }) }
            composable(Destinations.CALENDAR) { CalendarScreen() }
            composable(Destinations.LOG) { LogScreen(date = LocalDate.now(), onSaved = { nav.popBackStack() }) }
        }
    }
}
