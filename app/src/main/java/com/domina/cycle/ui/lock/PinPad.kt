package com.domina.cycle.ui.lock

import androidx.compose.animation.core.KeyframesSpec
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val PIN_LENGTH = 4

/** Side-to-side shake used to signal a wrong / mismatched PIN. */
fun pinShakeSpec(): KeyframesSpec<Float> = keyframes {
    durationMillis = 350
    -28f at 40; 28f at 80; -22f at 130; 22f at 180; -12f at 240; 12f at 290; 0f at 350
}

/** Row of [total] slots; the first [filledCount] show a dot, the rest an underscore. */
@Composable
fun PinSlots(
    filledCount: Int,
    modifier: Modifier = Modifier,
    total: Int = PIN_LENGTH,
    translationX: Float = 0f,
) {
    Row(
        modifier = modifier.graphicsLayer { this.translationX = translationX },
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        repeat(total) { i -> PinSlot(filled = i < filledCount) }
    }
}

/** In-app number pad — 1–9, then 0 and backspace. No system keyboard. */
@Composable
fun NumberPad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { d -> KeyButton(onClick = { onDigit(d) }) { KeyText("$d") } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(Modifier.size(72.dp))
            KeyButton(onClick = { onDigit(0) }) { KeyText("0") }
            KeyButton(onClick = onBackspace) {
                Text("⌫", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PinSlot(filled: Boolean) {
    Box(Modifier.size(width = 30.dp, height = 38.dp), contentAlignment = Alignment.Center) {
        if (filled) {
            Box(Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        } else {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(width = 22.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
    }
}

@Composable
private fun KeyText(text: String) {
    Text(text, fontSize = 28.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun KeyButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(72.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
    }
}
