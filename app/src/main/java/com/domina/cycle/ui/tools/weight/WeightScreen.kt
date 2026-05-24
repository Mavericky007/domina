package com.domina.cycle.ui.tools.weight

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

@Composable
fun WeightScreen(vm: WeightViewModel = hiltViewModel()) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    var kg by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Weight", style = MaterialTheme.typography.titleLarge)
        Row {
            OutlinedTextField(kg, { kg = it.filter { ch -> ch.isDigit() || ch == '.' } },
                label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { kg.toDoubleOrNull()?.let { vm.add(it); kg = "" } }, enabled = kg.toDoubleOrNull() != null) {
                Text("Add")
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(entries, key = { it.id }) { e ->
                ListItem(
                    headlineContent = { Text("%.1f kg".format(e.weightKg)) },
                    supportingContent = { Text(LocalDate.ofEpochDay(e.dateEpochDay).toString()) },
                    trailingContent = { IconButton(onClick = { vm.delete(e.id) }) { Icon(Icons.Filled.Delete, "Delete") } },
                )
                HorizontalDivider()
            }
        }
    }
}
