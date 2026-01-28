package com.expenses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expenses.app.data.Receipt
import com.expenses.app.data.ExportStatus
import com.expenses.app.util.CurrencyUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    receipts: List<Receipt>,
    onAddReceipt: () -> Unit,
    onUploadReceipt: () -> Unit,
    onReceiptClick: (String) -> Unit,
    onViewReports: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses & Receipts") },
                actions = {
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
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(1f)
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
                        text = CurrencyUtils.formatCurrency(receipt.totalAmount, receipt.currency),
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
                
                Spacer(modifier = Modifier.height(4.dp))
                
                StatusChip(status = receipt.exportStatus)
            }
            
            // Image indicator
            if (receipt.originalUri != null || receipt.storedUri != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Default.Image,
                    contentDescription = "Has Image",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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
        ExportStatus.NOT_EXPORTED -> "Draft"
        ExportStatus.EXPORTED -> "Committed"
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
