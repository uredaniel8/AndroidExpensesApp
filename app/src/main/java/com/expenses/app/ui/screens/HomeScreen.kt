package com.expenses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.rememberAsyncImagePainter
import com.expenses.app.data.Receipt
import com.expenses.app.data.ExportStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    receipts: List<Receipt>,
    onAddReceipt: () -> Unit,
    onUploadReceipt: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onViewReports: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses & Receipts") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = onViewReports) {
                        Icon(Icons.Default.Assessment, contentDescription = "View Reports")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = onAddReceipt,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Add Receipt")
                }
                FloatingActionButton(
                    onClick = onUploadReceipt
                ) {
                    Icon(Icons.Default.Upload, contentDescription = "Upload Receipt")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (receipts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No receipts yet. Add one to get started!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(receipts) { receipt ->
                        ReceiptCard(
                            receipt = receipt,
                            onClick = { onReceiptClick(receipt.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptCard(receipt: Receipt, onClick: () -> Unit) {
    var showImageDialog by remember { mutableStateOf(false) }
    
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = receipt.merchant ?: "Unknown Merchant",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatCurrency(receipt.totalAmount, receipt.currency),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = receipt.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = formatDate(receipt.receiptDate),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusChip(status = receipt.exportStatus)
                
                // View Image Button
                if (receipt.originalUri != null || receipt.storedUri != null) {
                    TextButton(onClick = { showImageDialog = true }) {
                        Icon(Icons.Default.Image, contentDescription = "View Receipt Image")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Image")
                    }
                }
            }
        }
    }
    
    // Image Dialog
    if (showImageDialog) {
        ImageViewerDialog(
            imageUri = receipt.storedUri ?: receipt.originalUri,
            onDismiss = { showImageDialog = false }
        )
    }
}

@Composable
fun StatusChip(status: ExportStatus) {
    val color = when (status) {
        ExportStatus.NOT_EXPORTED -> MaterialTheme.colorScheme.secondary
        ExportStatus.EXPORTED -> MaterialTheme.colorScheme.primary
        ExportStatus.FAILED -> MaterialTheme.colorScheme.error
    }
    
    val text = when (status) {
        ExportStatus.NOT_EXPORTED -> "Needs Review"
        ExportStatus.EXPORTED -> "Exported"
        ExportStatus.FAILED -> "Export Failed"
    }
    
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

fun formatCurrency(amount: Double, currency: String): String {
    val symbol = when (currency) {
        "GBP" -> "£"
        "USD" -> "$"
        "EUR" -> "€"
        "CHF" -> "CHF"
        else -> currency
    }
    return "$symbol${String.format("%.2f", amount)}"
}

@Composable
fun ImageViewerDialog(imageUri: String?, onDismiss: () -> Unit) {
    if (imageUri == null) {
        onDismiss()
        return
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt Image",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Image(
                    painter = rememberAsyncImagePainter(imageUri),
                    contentDescription = "Receipt Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}
