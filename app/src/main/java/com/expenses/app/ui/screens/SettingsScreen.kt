package com.expenses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onConfigureOneDrive: (String, Boolean) -> Unit
) {
    var accessToken by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }

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
                text = "OneDrive Integration",
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
                        text = "Configure OneDrive to automatically upload receipts:",
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
                Text("Enable OneDrive Integration")
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it }
                )
            }

            if (isEnabled) {
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = { accessToken = it },
                    label = { Text("OneDrive Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste your OneDrive access token here") },
                    minLines = 3
                )

                TextButton(
                    onClick = { showInfo = true }
                ) {
                    Text("How to get an access token?")
                }

                Button(
                    onClick = {
                        onConfigureOneDrive(accessToken, isEnabled)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accessToken.isNotBlank()
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
            title = { Text("Getting OneDrive Access Token") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("To get a OneDrive access token:")
                    Text("1. Visit https://portal.azure.com")
                    Text("2. Register an application in Azure AD")
                    Text("3. Configure Microsoft Graph API permissions")
                    Text("4. Generate an access token")
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
