package com.domina.cycle.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun InsightsScreen(vm: InsightsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall)

        Spacer(Modifier.height(12.dp))
        SectionCard("Cycle length history") {
            BarChart(values = s.cycleLengths, xLabels = s.cycleLabels)
            if (s.cycleLengths.isNotEmpty()) {
                Text("Average ${s.averageCycle} days · ${s.shortest}–${s.longest} day range",
                    style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard("Basal body temperature") {
            LineChart(values = s.bbtSeries, projected = s.bbtProjected,
                coverline = s.coverline, xLabels = s.bbtLabels, decimals = 2)
            if (s.coverline != null) {
                Text("Coverline detected — a temperature shift suggests ovulation has passed.",
                    style = MaterialTheme.typography.bodySmall)
            }
            if (s.bbtProjected.isNotEmpty()) {
                Text("Dotted line = projected from your phases — temperature tends to rise after ovulation (luteal phase).",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard("Weight") {
            LineChart(values = s.weightSeries, projected = s.weightProjected, xLabels = s.weightLabels)
            if (s.weightProjected.isNotEmpty()) {
                Text("Dotted line = projected by cycle phase (weight often nudges up in the luteal phase).",
                    style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard("Patterns") {
            if (s.insights.isEmpty()) Text("Keep logging and I'll spot patterns for you 💛",
                style = MaterialTheme.typography.bodyMedium)
            else s.insights.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}
