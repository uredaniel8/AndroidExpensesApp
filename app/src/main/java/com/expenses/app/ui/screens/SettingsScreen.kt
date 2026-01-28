package com.expenses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onConfigureProtonDrive: (String, Boolean) -> Unit
) {
    var accessToken by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    var showToken by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "ProtonDrive Integration",
                style = MaterialTheme.typography.headlineSmall
            )

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Configure ProtonDrive to automatically upload receipts:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "• Fuel receipts → Receipts/Fuel folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "• Other receipts → Receipts/Other folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enable ProtonDrive Integration")
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }

            if (isEnabled) {
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    label = { Text("ProtonDrive Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste your ProtonDrive access token here") },
                    minLines = 3,
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showToken = !showToken }) {
                            Icon(
                                if (showToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showToken) "Hide token" else "Show token"
                            )
                        }
                    }
                )

                TextButton(
                    onClick = { showInfo = true }
                ) {
                    Text("How to get an access token?")
                }

                Button(
                    onClick = {
                        onConfigureProtonDrive(accessToken.trim(), isEnabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accessToken.trim().length >= 20 // Basic validation for minimum token length
                ) {
                    Text("Save Configuration")
                }
            }
        }
    }

    // Info Dialog
    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Getting ProtonDrive Access Token") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To get a ProtonDrive access token:")
                    Text("1. Visit https://account.proton.me")
                    Text("2. Navigate to Account Settings")
                    Text("3. Go to Security → API Access")
                    Text("4. Generate an access token for ProtonDrive")
                    Text("")
                    Text(
                        "Note: This is a simplified setup. In production, you should use OAuth2 authentication flow.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) {
                    Text("OK")
                }
            }
        )
    }
}
