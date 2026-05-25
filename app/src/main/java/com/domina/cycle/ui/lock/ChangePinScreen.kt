package com.domina.cycle.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChangePinScreen(
    onDone: () -> Unit,
    vm: ChangePinViewModel = hiltViewModel(),
) {
    var stage by remember { mutableStateOf(0) } // 0 = enter new, 1 = confirm
    var first by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }

    LaunchedEffect(mismatch) {
        if (mismatch) {
            shake.animateTo(0f, pinShakeSpec())
            pin = ""; first = ""; stage = 0
            mismatch = false
        }
    }

    fun process(completed: String) {
        if (stage == 0) {
            first = completed
            pin = ""
            stage = 1
        } else {
            if (completed == first) vm.save(completed, onDone) else mismatch = true
        }
    }
    fun onDigit(d: Int) {
        if (pin.length < PIN_LENGTH) {
            val next = pin + d
            pin = next
            if (next.length == PIN_LENGTH) process(next)
        }
    }
    fun onBackspace() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("🔐", fontSize = 40.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            if (stage == 0) "Choose a new PIN" else "Confirm your PIN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (stage == 0) "Enter a new 4-digit PIN" else "Re-enter it to confirm",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(36.dp))
        PinSlots(filledCount = pin.length, translationX = shake.value)
        Box(Modifier.height(28.dp).padding(top = 8.dp), contentAlignment = Alignment.Center) {
            if (mismatch) {
                Text(
                    "PINs didn't match. Try again.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        NumberPad(onDigit = ::onDigit, onBackspace = ::onBackspace)
        Spacer(Modifier.height(16.dp))
    }
}
