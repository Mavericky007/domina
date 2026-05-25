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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
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
                        val isOvulation = day in s.ovulationDays
                        val isFertile = day in s.fertileDays
                        val isPredicted = day in s.predictedPeriodDays
                        val isToday = day == s.today

                        val fill = when {
                            isPeriod -> cs.secondaryContainer
                            isOvulation -> cs.tertiary
                            isFertile -> cs.tertiaryContainer
                            else -> cs.surfaceContainerHigh
                        }
                        val onFill = when {
                            isPeriod -> cs.onSecondaryContainer
                            isOvulation -> cs.onTertiary
                            isFertile -> cs.onTertiaryContainer
                            else -> cs.onSurface
                        }
                        val borderMod = when {
                            isToday -> Modifier.border(2.dp, cs.primary, CircleShape)
                            isPredicted -> Modifier.border(2.dp, cs.secondary, CircleShape)
                            else -> Modifier
                        }
                        Box(
                            Modifier.size(38.dp).clip(CircleShape).background(fill).then(borderMod),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day", style = MaterialTheme.typography.bodyMedium, color = onFill,
                                fontWeight = if (isToday || isPeriod || isOvulation) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            LegendDot("Period", cs.secondaryContainer, filled = true)
            LegendDot("Predicted", cs.secondary, filled = false)
            LegendDot("Fertile", cs.tertiaryContainer, filled = true)
            LegendDot("Ovulation", cs.tertiary, filled = true)
            LegendDot("Today", cs.primary, filled = false)
        }
    }
}

@Composable
private fun LegendDot(label: String, color: Color, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(14.dp).clip(CircleShape)
                .then(if (filled) Modifier.background(color) else Modifier.border(2.dp, color, CircleShape)),
        )
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}
