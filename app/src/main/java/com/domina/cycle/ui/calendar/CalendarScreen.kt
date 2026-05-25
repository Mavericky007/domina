package com.domina.cycle.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private const val BASE_PAGE = 6000
private const val PAGE_COUNT = 12001
private val CAL_ROW_HEIGHT = 46.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    onOpenDay: (LocalDate) -> Unit = {},
    vm: CalendarViewModel = hiltViewModel(),
) {
    val data by vm.data.collectAsStateWithLifecycle()
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val anchor = remember { YearMonth.now() }
    val pager = rememberPagerState(initialPage = BASE_PAGE) { PAGE_COUNT }

    fun monthOf(page: Int): YearMonth = anchor.plusMonths((page - BASE_PAGE).toLong())
    val currentMonth by remember { derivedStateOf { monthOf(pager.currentPage) } }
    val currentState = remember(currentMonth, data) {
        CalendarViewModel.buildState(currentMonth, data.logs, data.today, data.mode, data.dueDate)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // ── Month header ───────────────────────────────────────────────
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous")
            }
            Text(
                "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                Modifier.weight(1f), textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next")
            }
        }

        // ── Weekday header (fixed) ─────────────────────────────────────
        Row(Modifier.fillMaxWidth()) {
            CalendarMonth.weekdayLabels.forEach { d ->
                Text(d, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(4.dp))

        // ── Swipeable month grid (smooth paging; taps open the day) ────
        // Height follows the visible month's week count so 5-week months don't leave a gap.
        val weeks = remember(currentState) { (currentState.cells.size + 6) / 7 }
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxWidth().height(CAL_ROW_HEIGHT * weeks),
            verticalAlignment = Alignment.Top,
        ) { page ->
            val st = remember(page, data) { CalendarViewModel.buildState(monthOf(page), data.logs, data.today, data.mode, data.dueDate) }
            MonthGrid(st, cs, onOpenDay)
        }

        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (currentState.isPregnancy) {
                LegendDot("Trimester 1", cs.tertiaryContainer, filled = true)
                LegendDot("Trimester 2", cs.secondaryContainer, filled = true)
                LegendDot("Trimester 3", cs.primaryContainer, filled = true)
                LegendHeart("Intimacy")
                LegendDot("Today", cs.primary, filled = false)
            } else {
                LegendDot("Period", cs.secondaryContainer, filled = true)
                LegendDot("Predicted", cs.secondary, filled = false)
                LegendDot("Fertile", cs.tertiaryContainer, filled = true)
                LegendDot("Ovulation", cs.tertiary, filled = true)
                LegendHeart("Intimacy")
                LegendDot("Today", cs.primary, filled = false)
            }
        }

        Spacer(Modifier.height(10.dp))
        HorizontalDivider(color = cs.outlineVariant)

        // ── Scrollable activity list for the visible month ─────────────
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
            if (currentState.events.isEmpty()) {
                item {
                    Text(
                        "Tap any day to log how she's feeling 💛",
                        style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp).fillMaxWidth(), textAlign = TextAlign.Center,
                    )
                }
            } else {
                items(currentState.events) { ev ->
                    if (ev.kind == CalEventKind.HEADER) SectionHeader(ev.title, cs)
                    else EventRow(ev, cs, onClick = { ev.date?.let(onOpenDay) })
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(s: CalendarUiState, cs: ColorScheme, onOpenDay: (LocalDate) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        s.cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().height(CAL_ROW_HEIGHT)) {
                week.forEach { day ->
                    Box(
                        Modifier.weight(1f).fillMaxHeight().padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (day != null) DayCell(day, s, cs) { onOpenDay(s.month.atDay(day)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, s: CalendarUiState, cs: ColorScheme, onClick: () -> Unit) {
    val isPeriod = day in s.periodDays
    val isOvulation = day in s.ovulationDays
    val isFertile = day in s.fertileDays
    val isPredicted = day in s.predictedPeriodDays
    val isToday = day == s.today

    val triTint = s.trimesterDays[day]?.let { tri ->
        when (tri) {
            1 -> cs.tertiaryContainer.copy(alpha = 0.35f)
            2 -> cs.secondaryContainer.copy(alpha = 0.40f)
            else -> cs.primaryContainer.copy(alpha = 0.40f)
        }
    }
    val fill = when {
        triTint != null -> triTint
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
        Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onClick).background(fill).then(borderMod),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$day", style = MaterialTheme.typography.bodyMedium, color = onFill,
            fontWeight = if (isToday || isPeriod || isOvulation) FontWeight.Bold else FontWeight.Normal,
        )
        if (day in s.intimacyDays) {
            Text("❤️", fontSize = 8.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String, cs: ColorScheme) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
        color = cs.onSurfaceVariant,
        modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

@Composable
private fun EventRow(ev: CalendarEvent, cs: ColorScheme, onClick: () -> Unit) {
    val badgeTint = when (ev.kind) {
        CalEventKind.PERIOD -> cs.secondaryContainer
        CalEventKind.PREDICTED_PERIOD -> cs.secondaryContainer
        CalEventKind.FERTILE -> cs.tertiaryContainer
        CalEventKind.OVULATION -> cs.tertiary
        CalEventKind.TODAY -> cs.primaryContainer
        CalEventKind.ALERT -> cs.tertiaryContainer
        else -> cs.surfaceContainerHigh
    }
    val isToday = ev.kind == CalEventKind.TODAY
    val clickable = ev.date != null
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (isToday) Modifier.background(cs.primaryContainer.copy(alpha = 0.4f)) else Modifier)
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = if (isToday) 8.dp else 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(badgeTint),
            contentAlignment = Alignment.Center,
        ) {
            Text(ev.icon, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(ev.title, style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium)
            Text(ev.detail, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        if (clickable) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = cs.onSurfaceVariant,
            )
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

@Composable
private fun LegendHeart(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("❤️", fontSize = 10.sp)
        Text(" $label", style = MaterialTheme.typography.bodySmall)
    }
}
