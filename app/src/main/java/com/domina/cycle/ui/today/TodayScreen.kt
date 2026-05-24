package com.domina.cycle.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun TodayScreen(onLogToday: () -> Unit, vm: TodayViewModel = hiltViewModel()) {
    val log by vm.log.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hello 💛", style = MaterialTheme.typography.headlineMedium)
        Text("Today: ${vm.today}", style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Today's log", style = MaterialTheme.typography.titleMedium)
                Text(if (log == null || log!!.isEmpty()) "Nothing logged yet" else "Mood: ${log!!.mood ?: "—"} · Flow: ${log!!.flow}")
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogToday) { Text("Log today") }
    }
}
