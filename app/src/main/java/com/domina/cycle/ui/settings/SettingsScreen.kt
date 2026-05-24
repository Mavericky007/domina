package com.domina.cycle.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domina.cycle.data.prefs.ThemePreference

@Composable
fun SettingsScreen(vm: SettingsViewModel = hiltViewModel()) {
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
