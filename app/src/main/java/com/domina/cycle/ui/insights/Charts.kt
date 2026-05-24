package com.domina.cycle.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    coverline: Float? = null,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    coverlineColor: Color = MaterialTheme.colorScheme.secondary,
) {
    if (values.size < 2) { Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall); return }
    val allVals = values + listOfNotNull(coverline)
    val minV = allVals.min(); val maxV = allVals.max(); val range = (maxV - minV).takeIf { it > 0f } ?: 1f
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width; val h = size.height; val pad = 8f
        fun x(i: Int) = pad + (w - 2 * pad) * (i.toFloat() / (values.size - 1))
        fun y(v: Float) = h - pad - (h - 2 * pad) * ((v - minV) / range)
        coverline?.let { cl ->
            val cy = y(cl)
            drawLine(coverlineColor, Offset(pad, cy), Offset(w - pad, cy), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)))
        }
        for (i in 0 until values.size - 1) {
            drawLine(lineColor, Offset(x(i), y(values[i])), Offset(x(i + 1), y(values[i + 1])), strokeWidth = 6f)
        }
        values.forEachIndexed { i, v -> drawCircle(lineColor, radius = 7f, center = Offset(x(i), y(v))) }
        val axisPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.GRAY; textSize = 24f; isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", maxV), 4f, y(maxV) + 8f, axisPaint)
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", minV), 4f, y(minV) + 8f, axisPaint)
    }
}

@Composable
fun BarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (values.isEmpty()) { Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall); return }
    val maxV = (values.max()).coerceAtLeast(1)
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width; val h = size.height; val pad = 8f
        val slot = (w - 2 * pad) / values.size
        val barW = slot * 0.6f
        values.forEachIndexed { i, v ->
            val bh = (h - 2 * pad) * (v.toFloat() / maxV)
            val left = pad + i * slot + (slot - barW) / 2
            drawRect(barColor, topLeft = Offset(left, h - pad - bh), size = androidx.compose.ui.geometry.Size(barW, bh))
        }
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.DKGRAY; textSize = 28f; textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        values.forEachIndexed { i, v ->
            val cx = pad + i * slot + slot / 2
            drawContext.canvas.nativeCanvas.drawText("$v", cx, h - pad - (h - 2 * pad) * (v.toFloat() / maxV) - 10f, labelPaint)
        }
    }
}
