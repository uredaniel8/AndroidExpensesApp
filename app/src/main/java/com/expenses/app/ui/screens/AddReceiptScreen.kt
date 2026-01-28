package com.expenses.app.ui.screens

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File

private const val RECEIPT_FILENAME_PREFIX = "receipt_"
private const val RECEIPT_FILE_EXTENSION = ".jpg"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddReceiptScreen(
    onBack: () -> Unit,
    onReceiptCaptured: (Uri) -> Unit
) {
    val context = LocalContext.current
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            onReceiptCaptured(photoUri!!)
            onBack()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onReceiptCaptured(it)
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Receipt") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val granted = cameraPermissionState.status is PermissionStatus.Granted
                    if (granted) {
                        val file = createTempReceiptFile(context)
                        photoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            file
                        )
                        cameraLauncher.launch(photoUri)
                    } else {
                        cameraPermissionState.launchPermissionRequest()
                    }
                }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Take Photo")
            }

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { galleryLauncher.launch("image/*") }
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Choose from Gallery")
            }
        }
    }
}

private fun createTempReceiptFile(context: Context): File {
    return File.createTempFile(
        RECEIPT_FILENAME_PREFIX,
        RECEIPT_FILE_EXTENSION,
        context.cacheDir
    )
}
