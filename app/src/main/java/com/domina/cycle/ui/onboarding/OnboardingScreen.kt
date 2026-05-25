package com.domina.cycle.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val dateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy")

@Composable
fun OnboardingScreen(
    isEdit: Boolean = false,
    onDone: () -> Unit,
    vm: OnboardingViewModel = hiltViewModel(),
) {
    val prefill by vm.prefill.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf<LocalDate?>(null) }
    var heightText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf("") }
    var lastPeriod by remember { mutableStateOf<LocalDate?>(null) }
    var prefilled by remember { mutableStateOf(false) }

    LaunchedEffect(prefill) {
        if (!prefilled && (prefill.name.isNotEmpty() || prefill.birthDate != null || prefill.heightCm != null)) {
            name = prefill.name
            dob = prefill.birthDate
            heightText = prefill.heightCm?.toString() ?: ""
            prefilled = true
        }
    }

    var step by remember { mutableStateOf(0) }
    val lastStep = if (isEdit) 0 else 2
    fun finish() = vm.save(name, dob, heightText.toIntOrNull(), weightText.toDoubleOrNull(), lastPeriod, isEdit, onDone)

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        if (!isEdit) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = ::finish) { Text("Skip") }
            }
        }

        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (isEdit) {
                StepHeader("Your profile", "Update how Domina knows you.")
                NameField(name) { name = it }
                Spacer(Modifier.height(12.dp))
                DateField("Date of birth", dob, age(dob)) { dob = it }
                Spacer(Modifier.height(12.dp))
                NumberField("Height (cm)", heightText, decimal = false) { heightText = it }
            } else when (step) {
                0 -> {
                    Spacer(Modifier.height(24.dp))
                    Text("🌙", fontSize = 64.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text("Welcome to Domina", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Everything you log stays only on this phone — always private 💛",
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(28.dp))
                    Text("What should we call you?", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    NameField(name) { name = it }
                }
                1 -> {
                    StepHeader("A little about you 💛", "Optional — it helps personalise your tips. Skip anything you like.")
                    DateField("Date of birth", dob, age(dob)) { dob = it }
                    Spacer(Modifier.height(12.dp))
                    NumberField("Height (cm)", heightText, decimal = false) { heightText = it }
                    Spacer(Modifier.height(12.dp))
                    NumberField("Current weight (kg)", weightText, decimal = true) { weightText = it }
                }
                else -> {
                    StepHeader("Your cycle", "When did your last period start? This lets me predict your cycle right away. (Optional)")
                    DateField("Last period start", lastPeriod, null) { lastPeriod = it }
                }
            }
        }

        // step dots
        if (!isEdit) {
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center) {
                repeat(3) { i ->
                    val active = i == step
                    Box(
                        Modifier.padding(horizontal = 4.dp).size(if (active) 10.dp else 8.dp)
                            .then(Modifier).background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                                androidx.compose.foundation.shape.CircleShape,
                            ),
                    )
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!isEdit && step > 0) {
                OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f).height(52.dp)) { Text("Back") }
            }
            Button(
                onClick = { if (step < lastStep) step++ else finish() },
                enabled = !(step == 0 && !isEdit && name.isBlank()),
                modifier = Modifier.weight(1f).height(52.dp),
            ) {
                Text(
                    when {
                        isEdit -> "Save"
                        step < lastStep -> "Continue"
                        else -> "Get started 🌙"
                    },
                )
            }
        }
    }
}

@Composable
private fun StepHeader(title: String, subtitle: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun NameField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = { if (it.length <= 30) onChange(it) },
        label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun NumberField(label: String, value: String, decimal: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || (decimal && it == '.') }
            if (filtered.count { it == '.' } <= 1 && filtered.length <= 6) onChange(filtered)
        },
        label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: LocalDate?, ageYears: Int?, onPick: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val display = value?.let { it.format(dateFmt) + (ageYears?.let { a -> "  ·  $a yrs" } ?: "") } ?: ""
    Box {
        OutlinedTextField(
            value = display, onValueChange = {}, readOnly = true, label = { Text(label) },
            placeholder = { Text("Tap to choose") },
            trailingIcon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.matchParentSize().clickable { show = true })
    }
    if (show) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = value?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= System.currentTimeMillis()
                override fun isSelectableYear(year: Int) = year <= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }
}

private fun age(dob: LocalDate?): Int? = dob?.let { Period.between(it, LocalDate.now()).years }
