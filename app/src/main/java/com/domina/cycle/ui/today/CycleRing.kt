package com.domina.cycle.ui.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domina.cycle.domain.prediction.CyclePhase

private val MenstrualColor = Color(0xFFFF7A8A)
private val FollicularColor = Color(0xFF1FB6A6)
private val OvulationColor = Color(0xFFFFC857)
private val LutealColor = Color(0xFF7C5CBF)

/**
 * Circular cycle ring: four phase arcs sized by their day-spans, with a marker at the
 * current cycle day and the day/phase in the center.
 */
@Composable
fun CycleRing(
    cycleDay: Int?,
    cycleLength: Int,
    periodLength: Int,
    phase: CyclePhase?,
    phaseLabel: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val stroke = 26.dp.toPx()
            val inset = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(inset, inset)
            val ovulationDay = (cycleLength - 14).coerceAtLeast(periodLength + 1)

            // day-of-cycle spans for each phase (1-based, inclusive)
            val spans = listOf(
                MenstrualColor to (1..periodLength),
                FollicularColor to ((periodLength + 1) until (ovulationDay - 1)),
                OvulationColor to ((ovulationDay - 1)..(ovulationDay + 1)),
                LutealColor to ((ovulationDay + 2)..cycleLength),
            )
            val degPerDay = 360f / cycleLength
            spans.forEach { (color, range) ->
                if (!range.isEmpty()) {
                    val startAngle = -90f + (range.first - 1) * degPerDay
                    val sweep = (range.last - range.first + 1) * degPerDay
                    drawArc(
                        color = color, startAngle = startAngle, sweepAngle = sweep,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    )
                }
            }
            // current-day marker
            if (cycleDay != null) {
                val clamped = cycleDay.coerceIn(1, cycleLength)
                val angle = Math.toRadians((-90f + (clamped - 0.5f) * degPerDay).toDouble())
                val r = (arcSize.width / 2)
                val cx = size.width / 2 + r * kotlin.math.cos(angle).toFloat()
                val cy = size.height / 2 + r * kotlin.math.sin(angle).toFloat()
                drawCircle(color = Color.White, radius = stroke * 0.55f, center = Offset(cx, cy))
                drawCircle(color = Color(0xFF333333), radius = stroke * 0.30f, center = Offset(cx, cy))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (cycleDay != null) "CYCLE DAY" else "WELCOME", style = MaterialTheme.typography.labelSmall)
            Text(
                text = cycleDay?.toString() ?: "—",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
            Text(phaseLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}
