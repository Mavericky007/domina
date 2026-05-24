package com.domina.cycle.ui.today

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PregnancyDashboard(state: PregnancyUiState, modifier: Modifier = Modifier) {
    val w = state.week
    val p = state.progress
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Week ${p.weeksCompleted} · Trimester ${p.trimester}", style = MaterialTheme.typography.labelLarge)
        Text(w.emoji, fontSize = 72.sp)
        Text("Size of a ${w.fruit}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (w.lengthCm > 0) {
            Text("~${w.lengthCm} cm" + if (w.weightG > 0) " · ${w.weightG} g" else "",
                style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(w.development, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                Text("💛 ${w.funFact}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(12.dp))
        val countdown = when {
            p.daysRemaining > 0 -> "${p.daysRemaining} days to go 💛"
            p.daysRemaining == 0 -> "Due today! 💛"
            else -> "${-p.daysRemaining} days past due — any moment now 💛"
        }
        AssistChip(onClick = {}, label = { Text(countdown) })
    }
}
