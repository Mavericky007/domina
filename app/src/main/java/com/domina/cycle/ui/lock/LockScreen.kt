package com.domina.cycle.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val PIN_LENGTH = 4

@Composable
fun LockScreen(
    hasPin: Boolean,
    error: Boolean,
    onPinEntered: (String) -> Unit,
    onErrorShown: () -> Unit,
    onUseBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    // Auto-submit the moment the PIN is complete — no button to tap.
    LaunchedEffect(pin) {
        if (pin.length == PIN_LENGTH) onPinEntered(pin)
    }

    // Wrong PIN: shake the slots, clear them, show a message, then ack the error.
    LaunchedEffect(error) {
        if (error) {
            shake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    -28f at 40; 28f at 80; -22f at 130; 22f at 180; -12f at 240; 12f at 290; 0f at 350
                },
            )
            pin = ""
            showError = true
            onErrorShown()
        }
    }

    fun onDigit(d: Int) {
        if (pin.length < PIN_LENGTH) {
            pin += d.toString()
            showError = false
        }
    }
    fun onBackspace() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Text("🌙", fontSize = 48.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Domina",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasPin) "Welcome back 💛" else "Create a 4-digit PIN to keep your data private 💛",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(40.dp))
        Row(
            modifier = Modifier.graphicsLayer { translationX = shake.value },
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            repeat(PIN_LENGTH) { i -> PinSlot(filled = i < pin.length) }
        }
        Box(Modifier.height(28.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
            if (showError) {
                Text(
                    "Incorrect PIN. Try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // In-app number pad — no system keyboard, so nothing can cover the slots.
        Column(
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
                KeyButton(onClick = ::onBackspace) {
                    Text("⌫", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (hasPin) {
            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onUseBiometric) { Text("Use fingerprint / face") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PinSlot(filled: Boolean) {
    Box(Modifier.size(width = 30.dp, height = 38.dp), contentAlignment = Alignment.Center) {
        if (filled) {
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            )
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
