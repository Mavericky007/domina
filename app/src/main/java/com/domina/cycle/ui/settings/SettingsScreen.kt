package com.domina.cycle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domina.cycle.data.prefs.ThemePreference

@Composable
fun SettingsScreen(
    onOpenMeds: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val rem by vm.reminders.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Theme", style = MaterialTheme.typography.titleMedium)
        ThemePreference.entries.forEach { t ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = theme == t, onClick = { vm.setTheme(t) })
                Text(t.displayName)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Period & cycle alerts", rem.periodAlerts) { vm.updateReminders(rem.copy(periodAlerts = it)) }
        ToggleRow("Fertility alerts", rem.fertilityAlerts) { vm.updateReminders(rem.copy(fertilityAlerts = it)) }
        ToggleRow("Daily logging nudge (8 PM)", rem.dailyNudge) { vm.updateReminders(rem.copy(dailyNudge = it)) }
        Spacer(Modifier.height(16.dp))
        Text("Mode", style = MaterialTheme.typography.titleMedium)
        val mode by vm.mode.collectAsStateWithLifecycle()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.domina.cycle.domain.pregnancy.AppMode.entries.forEach { m ->
                FilterChip(selected = mode == m, onClick = { vm.setMode(m) },
                    label = { Text(when (m) {
                        com.domina.cycle.domain.pregnancy.AppMode.CYCLE -> "Cycle"
                        com.domina.cycle.domain.pregnancy.AppMode.TTC -> "Trying"
                        com.domina.cycle.domain.pregnancy.AppMode.PREGNANCY -> "Pregnancy" }) })
            }
        }
        if (mode == com.domina.cycle.domain.pregnancy.AppMode.PREGNANCY) {
            val due by vm.dueDate.collectAsStateWithLifecycle()
            var text by remember(due) { mutableStateOf(due?.toString() ?: "") }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it; runCatching { java.time.LocalDate.parse(it) }.getOrNull()?.let(vm::setDueDate) },
                label = { Text("Due date (YYYY-MM-DD)") },
                singleLine = true,
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Medications", style = MaterialTheme.typography.titleMedium)
        ListItem(
            headlineContent = { Text("Manage medications") },
            supportingContent = { Text("Add daily medication reminders") },
            trailingContent = { TextButton(onClick = onOpenMeds) { Text("Open") } },
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
