package com.domina.cycle.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domina.cycle.data.model.FlowIntensity
import com.domina.cycle.data.model.Mood
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(date: LocalDate, onSaved: () -> Unit, vm: LogViewModel = hiltViewModel()) {
    LaunchedEffect(date) { vm.load(date) }
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Log for $date", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("Mood")
        Row {
            Mood.entries.take(4).forEach { m ->
                FilterChip(selected = state.mood == m, onClick = { vm.setMood(m) }, label = { Text(m.name.lowercase()) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Flow")
        Row {
            FlowIntensity.entries.forEach { f ->
                FilterChip(selected = state.flow == f, onClick = { vm.setFlow(f) }, label = { Text(f.name.lowercase()) }, modifier = Modifier.padding(end = 4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = state.note, onValueChange = vm::setNote, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { vm.save(); onSaved() }) { Text("Save") }
    }
}
