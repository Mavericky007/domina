package com.domina.cycle.ui.appointments

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun AppointmentsScreen(vm: AppointmentsViewModel = hiltViewModel()) {
    val appts by vm.appointments.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }   // YYYY-MM-DD
    var time by remember { mutableStateOf("10:00") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Appointments", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(title, { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
        Row {
            OutlinedTextField(date, { date = it }, label = { Text("Date YYYY-MM-DD") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(time, { time = it }, label = { Text("HH:MM") }, modifier = Modifier.width(110.dp))
        }
        Button(onClick = {
            val parsed = runCatching {
                LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }.getOrNull()
            if (parsed != null) { vm.add(title, parsed, 24); title = ""; date = "" }
        }, enabled = title.isNotBlank()) { Text("Add appointment (reminds 24h before)") }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(appts, key = { it.id }) { a ->
                val whenText = java.time.Instant.ofEpochMilli(a.atEpochMillis)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime().toString().replace('T', ' ')
                ListItem(
                    headlineContent = { Text(a.title) },
                    supportingContent = { Text(whenText) },
                    trailingContent = { IconButton(onClick = { vm.delete(a.id) }) { Icon(Icons.Filled.Delete, "Delete") } },
                )
                HorizontalDivider()
            }
        }
    }
}
