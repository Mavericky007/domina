package com.domina.cycle.ui.tools.contractions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ContractionTimerScreen(vm: ContractionsViewModel = hiltViewModel()) {
    val running by vm.running.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val list by vm.contractions.collectAsStateWithLifecycle()
    val fmt = remember { DateTimeFormatter.ofPattern("MMM d, HH:mm:ss") }
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Contraction timer", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        Button(onClick = vm::toggle, modifier = Modifier.fillMaxWidth().height(80.dp)) {
            Text(if (running) "Stop contraction" else "Start contraction")
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Averages (${stats.count})", style = MaterialTheme.typography.titleMedium)
                Text("Duration: ${stats.averageDurationSec}s · Apart: ${stats.averageIntervalSec / 60}m ${stats.averageIntervalSec % 60}s")
                Text("Tip: contractions ~5 min apart, ~1 min long, for 1 hour often means it's time to call your provider 💛",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = vm::clearAll) { Text("Clear all") }
        LazyColumn {
            items(list, key = { it.id }) { c ->
                val dur = ((c.endMillis - c.startMillis) / 1000).coerceAtLeast(0)
                val start = Instant.ofEpochMilli(c.startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime().format(fmt)
                ListItem(headlineContent = { Text("${dur}s") }, supportingContent = { Text(start) })
                HorizontalDivider()
            }
        }
    }
}
