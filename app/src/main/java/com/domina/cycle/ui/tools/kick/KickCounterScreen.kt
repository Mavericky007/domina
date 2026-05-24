package com.domina.cycle.ui.tools.kick

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId

@Composable
fun KickCounterScreen(vm: KickViewModel = hiltViewModel()) {
    val count by vm.count.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Kick counter 👶", style = MaterialTheme.typography.titleLarge)
        Text("Tap each time you feel a kick. Ten in a session is a lovely sign 💛",
            style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))
        Text("$count", fontSize = 72.sp, fontWeight = FontWeight.Bold)
        Button(onClick = vm::kick, modifier = Modifier.fillMaxWidth().height(72.dp)) { Text("I felt a kick!") }
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedButton(onClick = vm::reset, modifier = Modifier.weight(1f)) { Text("Reset") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = vm::save, enabled = count > 0, modifier = Modifier.weight(1f)) { Text("Save session") }
        }
        Spacer(Modifier.height(16.dp))
        Text("Past sessions", style = MaterialTheme.typography.titleMedium)
        LazyColumn {
            items(sessions, key = { it.id }) { s ->
                val date = Instant.ofEpochMilli(s.startMillis).atZone(ZoneId.systemDefault()).toLocalDate()
                val mins = ((s.endMillis - s.startMillis) / 60000).coerceAtLeast(0)
                ListItem(headlineContent = { Text("${s.count} kicks") },
                    supportingContent = { Text("$date · ${mins} min") })
                HorizontalDivider()
            }
        }
    }
}
