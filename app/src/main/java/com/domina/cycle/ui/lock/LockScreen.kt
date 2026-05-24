package com.domina.cycle.ui.lock

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LockScreen(
    hasPin: Boolean,
    onPinEntered: (String) -> Unit,
    onUseBiometric: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌙", fontSize = 56.sp)
        Spacer(Modifier.height(8.dp))
        Text("Domina", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasPin) "Welcome back 💛" else "Set a PIN to keep your data private 💛",
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { if (it.length <= 8) pin = it.filter(Char::isDigit) },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(0.7f),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onPinEntered(pin); pin = "" },
            enabled = pin.length >= 4,
            modifier = Modifier.fillMaxWidth(0.7f).height(52.dp),
        ) { Text(if (hasPin) "Unlock" else "Save PIN") }
        if (hasPin) {
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onUseBiometric) { Text("Use fingerprint / face") }
        }
    }
}
