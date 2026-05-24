package com.domina.cycle.ui.tools.checklist

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
import com.domina.cycle.domain.pregnancy.ChecklistDefaults

@Composable
fun ChecklistScreen(vm: ChecklistViewModel = hiltViewModel()) {
    var tab by remember { mutableStateOf(0) }
    val category = if (tab == 0) ChecklistDefaults.BAG else ChecklistDefaults.BIRTH_PLAN
    val items by vm.items(category).collectAsStateWithLifecycle()
    var newItem by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Hospital bag") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Birth plan") })
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(newItem, { newItem = it }, label = { Text("Add item") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Button(onClick = { vm.add(category, newItem); newItem = "" }, enabled = newItem.isNotBlank()) { Text("Add") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn {
            items(items, key = { it.id }) { item ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = item.checked, onCheckedChange = { vm.toggle(item.id, it) })
                    Text(item.text, Modifier.weight(1f))
                    IconButton(onClick = { vm.delete(item.id) }) { Icon(Icons.Filled.Delete, "Delete") }
                }
            }
        }
    }
}
