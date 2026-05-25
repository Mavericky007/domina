package com.domina.cycle.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * iOS-style "slot machine" picker. Scroll the column; the value under the centred highlight band is
 * the selection. [selectedIndex] sets the initial/external position (e.g. after a unit switch) and
 * should stay stable while the user scrolls — see the keyed `remember` in callers.
 */
@Composable
fun WheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleCount: Int = 5,
    itemHeight: Dp = 44.dp,
) {
    if (items.isEmpty()) return
    val start = selectedIndex.coerceIn(0, items.lastIndex)
    val state = rememberLazyListState(initialFirstVisibleItemIndex = start)
    val fling = rememberSnapFlingBehavior(lazyListState = state)

    val centered by remember {
        derivedStateOf {
            val info = state.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) start
            else {
                val mid = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2f) - mid) }?.index ?: start
            }
        }
    }

    // Report user scrolling.
    LaunchedEffect(Unit) {
        snapshotFlow { centered }.distinctUntilChanged().collect { if (it in items.indices) onSelectedIndexChange(it) }
    }
    // React to an external position change (unit toggle / prefill).
    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && selectedIndex != centered && !state.isScrollInProgress) {
            state.scrollToItem(selectedIndex)
        }
    }

    Box(modifier.height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        Box(
            Modifier.fillMaxWidth().height(itemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleCount / 2)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            itemsIndexed(items) { i, label ->
                val isSel = i == centered
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        textAlign = TextAlign.Center,
                        style = if (isSel) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSel) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

@Composable
fun UnitToggle(left: String, right: String, leftSelected: Boolean, onPick: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = leftSelected, onClick = { onPick(true) }, label = { Text(left) })
        FilterChip(selected = !leftSelected, onClick = { onPick(false) }, label = { Text(right) })
    }
}

// ── Weight wheel (kg 30–150 by 0.5, or lb 66–330) — shared by onboarding & daily log ──
private const val W_MIN_KG = 30
private const val W_MAX_KG = 150
private const val W_MIN_LB = 66
private const val W_MAX_LB = 330
private fun kgToLb(kg: Double) = kg / 0.45359237
private fun lbToKg(lb: Int) = lb * 0.45359237

@Composable
fun WeightField(initialKg: Double?, onChange: (Double) -> Unit) {
    var kgUnit by remember { mutableStateOf(true) }
    var kg by remember { mutableStateOf(initialKg ?: 60.0) }
    var adopted by remember { mutableStateOf(initialKg != null) }
    LaunchedEffect(initialKg) { if (initialKg != null && !adopted) { kg = initialKg; adopted = true } }

    UnitToggle("kg", "lb", kgUnit) { kgUnit = it }
    Spacer(Modifier.height(8.dp))

    val items = remember(kgUnit) {
        if (kgUnit) (0..((W_MAX_KG - W_MIN_KG) * 2)).map { String.format("%.1f kg", W_MIN_KG + it * 0.5) }
        else (W_MIN_LB..W_MAX_LB).map { "$it lb" }
    }
    val index = remember(kgUnit, adopted) {
        if (kgUnit) Math.round((kg.coerceIn(W_MIN_KG.toDouble(), W_MAX_KG.toDouble()) - W_MIN_KG) / 0.5).toInt()
        else (Math.round(kgToLb(kg)).toInt().coerceIn(W_MIN_LB, W_MAX_LB) - W_MIN_LB)
    }
    WheelPicker(items, index, onSelectedIndexChange = { i ->
        kg = if (kgUnit) W_MIN_KG + i * 0.5 else lbToKg(W_MIN_LB + i)
        onChange(kg)
    })
}
