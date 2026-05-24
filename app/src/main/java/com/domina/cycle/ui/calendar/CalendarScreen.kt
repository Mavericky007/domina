package com.domina.cycle.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(vm: CalendarViewModel = hiltViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = vm::prevMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous") }
            Text(
                "${s.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${s.month.year}",
                Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = vm::nextMonth) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            CalendarMonth.weekdayLabels.forEach { d ->
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(7)) {
            items(s.cells) { day ->
                Box(Modifier.aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (day != null) {
                        val isPeriod = day in s.periodDays
                        val isToday = day == s.today
                        Box(
                            Modifier.size(38.dp).clip(CircleShape)
                                .background(if (isPeriod) cs.secondaryContainer else cs.surfaceContainerHigh)
                                .then(if (isToday) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$day", style = MaterialTheme.typography.bodyMedium,
                                color = if (isPeriod) cs.onSecondaryContainer else cs.onSurface,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(cs.secondaryContainer))
            Text("  Period", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(16.dp))
            Box(Modifier.size(14.dp).clip(CircleShape).border(2.dp, cs.primary, CircleShape))
            Text("  Today", style = MaterialTheme.typography.bodySmall)
        }
    }
}
