package com.domina.cycle.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domina.cycle.data.prefs.ThemePreference

@Composable
fun SettingsScreen(
    onOpenMeds: () -> Unit = {},
    onOpenAppointments: () -> Unit = {},
    vm: SettingsViewModel = hiltViewModel(),
) {
    val theme by vm.theme.collectAsStateWithLifecycle()
    val rem by vm.reminders.collectAsStateWithLifecycle()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var backupMode by remember { mutableStateOf("") } // "backup" or "restore"
    var passphrase by remember { mutableStateOf("") }
    val message by vm.message.collectAsStateWithLifecycle()

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> if (uri != null) { pendingUri = uri; backupMode = "backup" } }

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) { pendingUri = uri; backupMode = "restore" } }

    val createPdf = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri -> if (uri != null) vm.exportReport(uri) }

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
        Spacer(Modifier.height(16.dp))
        Text("Appointments", style = MaterialTheme.typography.titleMedium)
        ListItem(
            headlineContent = { Text("Manage appointments") },
            supportingContent = { Text("Add appointment reminders (24h before)") },
            trailingContent = { TextButton(onClick = onOpenAppointments) { Text("Open") } },
        )
        Spacer(Modifier.height(16.dp))
        Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
        Text(
            "Your data stays on your phone. A backup is an encrypted file you save wherever you like.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { createDoc.launch("domina-backup.dom") }) { Text("Back up (encrypted)") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { openDoc.launch(arrayOf("application/octet-stream", "*/*")) }) {
            Text("Restore from backup")
        }
        Spacer(Modifier.height(16.dp))
        Text("Doctor report", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { createPdf.launch("domina-cycle-report.pdf") }) { Text("Export PDF for my doctor") }
        message?.let { msg ->
            LaunchedEffect(msg) {}
            Spacer(Modifier.height(8.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium)
        }
    }

    if (pendingUri != null) {
        AlertDialog(
            onDismissRequest = { pendingUri = null; passphrase = "" },
            title = { Text(if (backupMode == "backup") "Choose a passphrase" else "Enter your passphrase") },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = passphrase.length >= 4,
                    onClick = {
                        val u = pendingUri!!
                        val p = passphrase
                        if (backupMode == "backup") vm.backupTo(u, p) else vm.restoreFrom(u, p)
                        pendingUri = null
                        passphrase = ""
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pendingUri = null; passphrase = "" }) { Text("Cancel") }
            },
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
