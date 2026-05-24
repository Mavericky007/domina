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
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val gutter = 84f      // reserved space on the left for y-axis labels
        val padTop = 20f
        val padBottom = 20f
        val padRight = 12f
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

        // y-axis labels live in the gutter — left-aligned, never clipped, never over the line
        val paint = android.graphics.Paint().apply {
            color = labelArgb; textSize = 26f; isAntiAlias = true
        }
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", maxV), 6f, padTop + 9f, paint)
        drawContext.canvas.nativeCanvas.drawText(String.format("%.1f", minV), 6f, h - padBottom + 9f, paint)
    }
}

@Composable
fun BarChart(
    values: List<Int>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (values.isEmpty()) {
        Text("Not enough data yet 💛", style = MaterialTheme.typography.bodySmall)
        return
    }
    val maxV = values.max().coerceAtLeast(1)
    val labelArgb = labelColor.toArgb()
    Canvas(modifier = modifier.fillMaxWidth().height(160.dp)) {
        val w = size.width
        val h = size.height
        val pad = 8f
        val topPad = 34f      // room for the value label above each bar
        val slot = (w - 2 * pad) / values.size
        val barW = slot * 0.55f
        val paint = android.graphics.Paint().apply {
            color = labelArgb; textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER; isAntiAlias = true
        }
        values.forEachIndexed { i, v ->
            val bh = (h - topPad - pad) * (v.toFloat() / maxV)
            val left = pad + i * slot + (slot - barW) / 2
            val top = h - pad - bh
            drawRoundRect(
                barColor, topLeft = Offset(left, top), size = Size(barW, bh),
                cornerRadius = CornerRadius(10f, 10f),
            )
            drawContext.canvas.nativeCanvas.drawText("$v", left + barW / 2, top - 12f, paint)
        }
    }
}
