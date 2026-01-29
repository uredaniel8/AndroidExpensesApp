package com.expenses.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onConfigureProtonDrive: (String, Boolean) -> Unit, // kept for compatibility with your app
    onFuelFolderSelected: (Uri?) -> Unit = {},
    onOtherFolderSelected: (Uri?) -> Unit = {},
    fuelFolderUri: Uri? = null,
    otherFolderUri: Uri? = null
) {
    var showInfo by remember { mutableStateOf(false) }
    var showFolderInfo by remember { mutableStateOf(false) }

    // Folder picker launchers
    val fuelFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onFuelFolderSelected(it) }
    }

    val otherFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { onOtherFolderSelected(it) }
    }

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
                text = "Local Storage",
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
                        text = "Receipts are automatically saved to local storage by category:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "• Fuel receipts → Documents/Fuel Receipts folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "• Other receipts → Documents/Expenses Receipts folder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Files are stored in the app's external storage directory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Custom Folder Selection",
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
                        text = "Choose custom folders for storing receipts. By default, receipts are saved to the app's internal storage.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // ✅ FIX: Divider() is available across more Material3 versions than HorizontalDivider()
                    Divider()

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fuel Receipts Folder",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (fuelFolderUri != null) {
                                        Text(
                                            text = "Custom folder selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Using default folder",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { fuelFolderPicker.launch(null) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Select Folder")
                                }

                                if (fuelFolderUri != null) {
                                    OutlinedButton(
                                        onClick = { onFuelFolderSelected(null) }
                                    ) {
                                        Text("Reset")
                                    }
                                }
                            }
                        }
                    }

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Other Receipts Folder",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (otherFolderUri != null) {
                                        Text(
                                            text = "Custom folder selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Text(
                                            text = "Using default folder",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = "Folder",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { otherFolderPicker.launch(null) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Select Folder")
                                }

                                if (otherFolderUri != null) {
                                    OutlinedButton(
                                        onClick = { onOtherFolderSelected(null) }
                                    ) {
                                        Text("Reset")
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showFolderInfo = true }
                    ) {
                        Text("Why choose custom folders?")
                    }
                }
            }

            TextButton(
                onClick = { showInfo = true }
            ) {
                Text("Where are files saved?")
            }
        }
    }

    if (showInfo) {
        AlertDialog(
            onDismissRequest = { showInfo = false },
            title = { Text("Local Storage Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Receipt images are saved to:")
                    Text("")
                    Text("Android/data/com.expenses.app/files/Documents/")
                    Text("")
                    Text("Files are organized by category:")
                    Text("• Fuel receipts → Documents/Fuel Receipts")
                    Text("• Other receipts → Documents/Expenses Receipts")
                    Text("")
                    Text(
                        "These files are stored in your device's external storage and are accessible only to this app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfo = false }) { Text("OK") }
            }
        )
    }

    if (showFolderInfo) {
        AlertDialog(
            onDismissRequest = { showFolderInfo = false },
            title = { Text("Custom Folder Selection") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Custom folders allow you to:", style = MaterialTheme.typography.titleSmall)
                    Text("• Choose where receipts are stored on your device")
                    Text("• Easily access receipts from other apps like file managers")
                    Text("• Back up receipts to cloud storage more easily")
                    Text("• Organize receipts with your own folder structure")
                    Text("")
                    Text("Note:", style = MaterialTheme.typography.titleSmall)
                    Text("• The app needs permission to write to the selected folder")
                    Text("• If a folder is deleted, the app will fall back to default storage")
                    Text("• Custom folders persist across app restarts")
                }
            },
            confirmButton = {
                TextButton(onClick = { showFolderInfo = false }) { Text("OK") }
            }
        )
    }
}
