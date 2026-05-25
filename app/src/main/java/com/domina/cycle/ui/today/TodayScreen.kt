package com.domina.cycle.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domina.cycle.domain.guidance.PhaseGuidance
import com.domina.cycle.domain.prediction.Confidence
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(
    onLogToday: () -> Unit,
    onOpenKick: () -> Unit = {},
    onOpenContractions: () -> Unit = {},
    onOpenWeight: () -> Unit = {},
    onOpenChecklist: () -> Unit = {},
    vm: TodayViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pregnancy by vm.pregnancy.collectAsStateWithLifecycle()
    val p = state.prediction

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val greeting = when (java.time.LocalTime.now().hour) {
            in 5..11 -> "Good morning"; in 12..17 -> "Good afternoon"; else -> "Good evening"
        }
        val name by vm.userName.collectAsStateWithLifecycle()
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (name.isNullOrBlank()) "$greeting 💛" else "$greeting, $name 💛",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(
                state.today.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d")),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

        val preg = pregnancy
        if (preg != null) {
            PregnancyDashboard(
                preg,
                onOpenKick = onOpenKick,
                onOpenContractions = onOpenContractions,
                onOpenWeight = onOpenWeight,
                onOpenChecklist = onOpenChecklist,
            )
        } else {
            CycleRing(
                cycleDay = p.cycleDay,
                cycleLength = p.averageCycleLength,
                periodLength = p.averagePeriodLength,
                phase = p.phase,
                phaseLabel = state.guidance?.title?.substringAfter(' ')?.substringBefore(" —") ?: "Let's begin",
            )

            Spacer(Modifier.height(12.dp))

            if (p.confidence == Confidence.NONE) {
                Text(
                    "Log a few periods and I'll start predicting your cycle & phases 💛",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    p.nextPeriodDate?.let { ChipInfo("🩸 Period", daysLabel(state.today, it)) }
                    p.fertileWindowStart?.let { ChipInfo("🌸 Fertile", daysLabel(state.today, it)) }
                }
                if (p.confidence == Confidence.LOW) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Still learning your rhythm — predictions get sharper as you log.",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            state.guidance?.let { g ->
                Spacer(Modifier.height(16.dp))
                GuidanceCard(g)
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(onClick = onLogToday) { Text(if (state.todayLog == null) "Log today" else "Edit today's log") }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ChipInfo(label: String, value: String) {
    AssistChip(onClick = {}, label = { Text("$label · $value") })
}

@Composable
private fun GuidanceCard(g: PhaseGuidance) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(g.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(g.moodForecast, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(10.dp))
            GuidanceRow("🏃‍♀️ Move", g.move)
            GuidanceRow("🥗 Eat", g.eat)
            GuidanceRow("✨ Plan", g.plan)
            Spacer(Modifier.height(8.dp))
            Text("Gentle guidance, not medical advice 💛", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun GuidanceRow(label: String, body: String) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}

private fun daysLabel(today: LocalDate, target: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, target).toInt()
    return when {
        days < 0 -> "${-days}d ago"
        days == 0 -> "today"
        days == 1 -> "tomorrow"
        else -> "in ${days}d"
    }
}
