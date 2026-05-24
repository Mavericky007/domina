package com.domina.cycle.ui.meds

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun MedsScreen(vm: MedsViewModel = hiltViewModel()) {
    val meds by vm.meds.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf("9") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Medications", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(name, { name = it }, label = { Text("Name") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, label = { Text("Hr") }, modifier = Modifier.width(72.dp))
        }
        Button(onClick = {
            val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 9
            vm.add(name, h * 60); name = ""
        }, enabled = name.isNotBlank()) { Text("Add medication") }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(meds, key = { it.id }) { m ->
                ListItem(
                    headlineContent = { Text(m.name) },
                    supportingContent = { Text("Daily at %02d:%02d".format(m.timeMinutes / 60, m.timeMinutes % 60)) },
                    trailingContent = {
                        IconButton(onClick = { vm.delete(m.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
