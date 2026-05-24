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

@Composable
fun LineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    coverline: Float? = null,
    xLabels: List<String> = emptyList(),
    lineColor: Color = MaterialTheme.colorScheme.primary,
    coverlineColor: Color = MaterialTheme.colorScheme.secondary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (values.size < 2) {
        Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall)
        return
    }
    val allVals = values + listOfNotNull(coverline)
    val minV = allVals.min()
    val maxV = allVals.max()
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f
    val labelArgb = labelColor.toArgb()
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val gutter = 84f
        val padTop = 20f
        val padBottom = if (xLabels.isEmpty()) 20f else 46f
        val padRight = 16f
        fun x(i: Int) = gutter + (w - gutter - padRight) * (i.toFloat() / (values.size - 1))
        fun y(v: Float) = h - padBottom - (h - padTop - padBottom) * ((v - minV) / range)

        coverline?.let { cl ->
            val cy = y(cl)
            drawLine(
                coverlineColor, Offset(gutter, cy), Offset(w - padRight, cy), strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
            )
        }
        for (i in 0 until values.size - 1) {
            drawLine(lineColor, Offset(x(i), y(values[i])), Offset(x(i + 1), y(values[i + 1])), strokeWidth = 6f)
        }
        values.forEachIndexed { i, v -> drawCircle(lineColor, radius = 6f, center = Offset(x(i), y(v))) }

        val yPaint = android.graphics.Paint().apply { color = labelArgb; textSize = 26f; isAntiAlias = true }
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", maxV), 6f, padTop + 9f, yPaint)
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", minV), 6f, y(minV) + 9f, yPaint)

        if (xLabels.isNotEmpty()) {
            val xPaint = android.graphics.Paint().apply { color = labelArgb; textSize = 24f; isAntiAlias = true }
            val n = values.size
            listOf(0, n / 2, n - 1).distinct().forEach { i ->
                if (i < xLabels.size) {
                    xPaint.textAlign = when (i) {
                        0 -> android.graphics.Paint.Align.LEFT
                        n - 1 -> android.graphics.Paint.Align.RIGHT
                        else -> android.graphics.Paint.Align.CENTER
                    }
                    drawContext.canvas.nativeCanvas.drawText(xLabels[i], x(i), h - 10f, xPaint)
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
    Canvas(modifier = modifier.fillMaxWidth().height(180.dp)) {
        val w = size.width
        val h = size.height
        val pad = 8f
        val topPad = 34f
        val botPad = if (xLabels.isEmpty()) 8f else 36f
        val slot = (w - 2 * pad) / values.size
        val barW = slot * 0.55f
        val valuePaint = android.graphics.Paint().apply {
            color = labelArgb; textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        }
        val xPaint = android.graphics.Paint().apply {
            color = labelArgb; textSize = 24f
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        }
        values.forEachIndexed { i, v ->
            val bh = (h - topPad - botPad) * (v.toFloat() / maxV)
            val cx = pad + i * slot + slot / 2
            val left = cx - barW / 2
            val top = h - botPad - bh
            drawRoundRect(
                barColor, topLeft = Offset(left, top), size = Size(barW, bh),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawContext.canvas.nativeCanvas.drawText("$v", cx, top - 12f, valuePaint)
            if (i < xLabels.size) drawContext.canvas.nativeCanvas.drawText(xLabels[i], cx, h - 10f, xPaint)
        }
    }
}
