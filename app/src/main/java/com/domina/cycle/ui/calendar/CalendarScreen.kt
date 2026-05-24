package com.domina.cycle.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val month by vm.visibleMonth.collectAsState()
    val logs by vm.logsThisMonth.collectAsState()
    val loggedDays = logs.map { it.date.dayOfMonth }.toSet()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            TextButton(onClick = vm::prevMonth) { Text("‹") }
            Text("$month", Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = vm::nextMonth) { Text("›") }
        }
        val days = (1..month.lengthOfMonth()).toList()
        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            items(days) { d ->
                Box(Modifier.padding(4.dp)) {
                    Surface(
                        color = if (d in loggedDays) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ) { Text("$d", Modifier.padding(10.dp)) }
                }
            }
        }
    }
}
