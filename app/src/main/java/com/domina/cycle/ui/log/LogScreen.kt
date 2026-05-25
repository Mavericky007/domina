package com.domina.cycle.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domina.cycle.data.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val moodEmoji = mapOf(
    Mood.HAPPY to "😊", Mood.CALM to "😌", Mood.SENSITIVE to "🥺",
    Mood.SAD to "😢", Mood.IRRITABLE to "😤", Mood.ANXIOUS to "😰",
    Mood.ENERGETIC to "⚡", Mood.TIRED to "😴",
)
private val commonSymptoms = listOf(
    "cramps", "headache", "bloating", "tender breasts", "acne",
    "nausea", "backache", "fatigue", "cravings", "mood swings",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(date: LocalDate, onSaved: () -> Unit, vm: LogViewModel = hiltViewModel()) {
    LaunchedEffect(date) { vm.load(date) }
    val state by vm.state.collectAsState()
    var bbtText by remember(state.date) { mutableStateOf(state.bbt?.toString() ?: "") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(state.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Section("How are you feeling?") {
            Mood.entries.forEach { m ->
                FilterChip(selected = state.mood == m, onClick = { vm.setMood(m) },
                    label = { Text("${moodEmoji[m]} ${m.name.lowercase()}") })
            }
        }
        Section("Energy") {
            Energy.entries.forEach { e ->
                FilterChip(selected = state.energy == e, onClick = { vm.setEnergy(e) },
                    label = { Text(e.name.lowercase()) })
            }
        }
        Section("Flow") {
            FlowIntensity.entries.forEach { f ->
                FilterChip(selected = state.flow == f, onClick = { vm.setFlow(f) },
                    label = { Text(f.name.lowercase()) })
            }
        }
        Section("Symptoms") {
            commonSymptoms.forEach { s ->
                FilterChip(selected = s in state.symptoms, onClick = { vm.toggleSymptom(s) },
                    label = { Text(s) })
            }
        }
        Section("Cervical mucus") {
            CervicalMucus.entries.forEach { c ->
                FilterChip(selected = state.cervicalMucus == c, onClick = { vm.setCervicalMucus(c) },
                    label = { Text(c.name.lowercase().replace('_', ' ')) })
            }
        }
        Section("Ovulation (LH) test") {
            LhResult.entries.forEach { l ->
                FilterChip(selected = state.lhResult == l, onClick = { vm.setLhResult(l) },
                    label = { Text(l.name.lowercase().replace('_', ' ')) })
            }
        }

        Section("Intimacy") {
            FilterChip(selected = state.intimacy == Intimacy.PROTECTED,
                onClick = { vm.setIntimacy(Intimacy.PROTECTED) },
                label = { Text("🛡️ protected") })
            FilterChip(selected = state.intimacy == Intimacy.UNPROTECTED,
                onClick = { vm.setIntimacy(Intimacy.UNPROTECTED) },
                label = { Text("💗 unprotected") })
        }
        Section("Contraception") {
            FilterChip(selected = state.emergencyContraception,
                onClick = { vm.setEmergencyContraception(!state.emergencyContraception) },
                label = { Text("💊 Morning-after pill") })
        }
        Section("Pregnancy test") {
            FilterChip(selected = state.pregnancyTest == PregnancyTest.NEGATIVE,
                onClick = { vm.setPregnancyTest(PregnancyTest.NEGATIVE) },
                label = { Text("negative") })
            FilterChip(selected = state.pregnancyTest == PregnancyTest.POSITIVE,
                onClick = { vm.setPregnancyTest(PregnancyTest.POSITIVE) },
                label = { Text("🤰 positive") })
        }

        Text("Basal body temperature (°C)", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = bbtText,
            onValueChange = { v -> bbtText = v.filter { it.isDigit() || it == '.' }; vm.setBbt(bbtText.toDoubleOrNull()) },
            placeholder = { Text("e.g. 36.6") }, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))

        Text("Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = state.note, onValueChange = vm::setNote,
            placeholder = { Text("Anything else?") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(24.dp))

        Button(onClick = { vm.save(); onSaved() }, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save") }
        Spacer(Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Section(title: String, content: @Composable FlowRowScope.() -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
    Spacer(Modifier.height(16.dp))
}
