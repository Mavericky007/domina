package com.domina.cycle.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    projected: List<Float> = emptyList(),
    coverline: Float? = null,
    xLabels: List<String> = emptyList(),   // labels for observed + projected combined
    decimals: Int = 1,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    coverlineColor: Color = MaterialTheme.colorScheme.secondary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (values.size < 2) {
        Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall)
        return
    }
    val all = values + projected + listOfNotNull(coverline)
    val minV = all.min()
    val maxV = all.max()
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f
    val total = values.size + projected.size
    val labelArgb = labelColor.toArgb()
    val projArgb = lineColor.copy(alpha = 0.5f).toArgb()
    val projColor = lineColor.copy(alpha = 0.5f)
    val fmt = "%.${decimals}f"

    Canvas(modifier = modifier.fillMaxWidth().height(210.dp)) {
        val w = size.width
        val h = size.height
        val valPx = 12.sp.toPx()
        val xPx = 12.sp.toPx()
        val padL = 14f
        val padR = 14f
        val padTop = valPx + 14f
        val padBottom = (if (xLabels.isEmpty()) 0f else xPx + 14f) + valPx + 12f
        fun x(i: Int) = padL + (w - padL - padR) * (i.toFloat() / (total - 1).coerceAtLeast(1))
        fun y(v: Float) = h - padBottom - (h - padTop - padBottom) * ((v - minV) / range)
        fun valAt(i: Int) = if (i < values.size) values[i] else projected[i - values.size]

        // coverline (ovulation reference)
        coverline?.let { cl ->
            val cy = y(cl)
            drawLine(coverlineColor, Offset(padL, cy), Offset(w - padR, cy), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
        }

        // observed line (solid)
        for (i in 0 until values.size - 1) {
            drawLine(lineColor, Offset(x(i), y(values[i])), Offset(x(i + 1), y(values[i + 1])), strokeWidth = 6f)
        }
        // projected line (dashed), bridged from the last observed point
        if (projected.isNotEmpty()) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
            var prev = values.size - 1
            for (j in projected.indices) {
                val cur = values.size + j
                drawLine(projColor, Offset(x(prev), y(valAt(prev))), Offset(x(cur), y(valAt(cur))),
                    strokeWidth = 5f, pathEffect = dash)
                prev = cur
            }
        }

        // points
        values.forEachIndexed { i, v -> drawCircle(lineColor, radius = 7f, center = Offset(x(i), y(v))) }
        projected.forEachIndexed { j, v -> drawCircle(projColor, radius = 5f, center = Offset(x(values.size + j), y(v))) }

        // per-point value labels — greedy, non-overlapping, placed above peaks / below troughs
        val vp = android.graphics.Paint().apply {
            textSize = valPx; textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        }
        var lastAbove = -1e9f; var lastBelow = -1e9f
        val gap = 10f
        for (i in 0 until total) {
            val v = valAt(i)
            val prev = if (i > 0) valAt(i - 1) else v
            val next = if (i < total - 1) valAt(i + 1) else v
            val high = v >= (prev + next) / 2f
            val cx = x(i)
            val s = String.format(fmt, v)
            val tw = vp.measureText(s)
            val left = cx - tw / 2f; val rightEdge = cx + tw / 2f
            vp.color = if (i < values.size) labelArgb else projArgb
            if (high) {
                if (left > lastAbove + gap) {
                    drawContext.canvas.nativeCanvas.drawText(s, cx, y(v) - 12f, vp); lastAbove = rightEdge
                }
            } else {
                if (left > lastBelow + gap) {
                    drawContext.canvas.nativeCanvas.drawText(s, cx, y(v) + valPx + 10f, vp); lastBelow = rightEdge
                }
            }
        }

        // x-axis time labels (start / mid / end across the whole range)
        if (xLabels.isNotEmpty()) {
            val xp = android.graphics.Paint().apply { color = labelArgb; textSize = xPx; isAntiAlias = true }
            listOf(0, total / 2, total - 1).distinct().forEach { i ->
                if (i < xLabels.size) {
                    xp.textAlign = when (i) {
                        0 -> android.graphics.Paint.Align.LEFT
                        total - 1 -> android.graphics.Paint.Align.RIGHT
                        else -> android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(xLabels[i], x(i), h - 10f, xp)
                }
            }
        }
    }
}

@Composable
fun BarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    xLabels: List<String> = emptyList(),
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (values.isEmpty()) {
        Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall)
        return
    }
    val maxV = values.max().coerceAtLeast(1)
    val labelArgb = labelColor.toArgb()
    Canvas(modifier = modifier.fillMaxWidth().height(200.dp)) {
        val w = size.width
        val h = size.height
        val valuePx = 16.sp.toPx()
        val xPx = 13.sp.toPx()
        val pad = 10f
        val topPad = valuePx + 12f
        val botPad = if (xLabels.isEmpty()) 10f else xPx + 18f
        val slot = (w - 2 * pad) / values.size
        val barW = slot * 0.55f
        val valuePaint = android.graphics.Paint().apply {
            color = labelArgb; textSize = valuePx
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        }
        val xPaint = android.graphics.Paint().apply {
            color = labelArgb; textSize = xPx
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        }
        values.forEachIndexed { i, v ->
            val bh = (h - topPad - botPad) * (v.toFloat() / maxV)
            val cx = pad + i * slot + slot / 2
            val left = cx - barW / 2
            val top = h - botPad - bh
            drawRoundRect(
                barColor, topLeft = Offset(left, top), size = Size(barW, bh),
                cornerRadius = CornerRadius(12f, 12f),
            )
            drawContext.canvas.nativeCanvas.drawText("$v", cx, top - 14f, valuePaint)
            if (i < xLabels.size) drawContext.canvas.nativeCanvas.drawText(xLabels[i], cx, h - 12f, xPaint)
        }
    }
}
