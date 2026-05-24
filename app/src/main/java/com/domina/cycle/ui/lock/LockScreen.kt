package com.domina.cycle.ui.lock

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LockScreen(
    hasPin: Boolean,
    onPinEntered: (String) -> Unit,
    onUseBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (hasPin) "Welcome back" else "Set a PIN to begin", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) }, label = { Text("PIN") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onPinEntered(pin); pin = "" }, enabled = pin.length >= 4) {
            Text(if (hasPin) "Unlock" else "Save PIN")
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onUseBiometric) { Text("Use fingerprint / face") }
    }
}
