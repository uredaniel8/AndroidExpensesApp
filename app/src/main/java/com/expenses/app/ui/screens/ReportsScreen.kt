package com.expenses.app.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.expenses.app.data.Receipt
import com.expenses.app.util.CsvExporter
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    receiptsFlow: Flow<List<Receipt>>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(getStartOfMonth()) }
    var endDate by remember { mutableStateOf(System.currentTimeMillis()) }
    
    val receipts by receiptsFlow.collectAsState(initial = emptyList())
    
    // File picker for CSV export
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            try {
                val tempFile = File.createTempFile("receipts_", ".csv", context.cacheDir)
                if (CsvExporter.exportToCsv(receipts, tempFile)) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        tempFile.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    Toast.makeText(context, "CSV exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to export CSV", Toast.LENGTH_SHORT).show()
                }
                tempFile.delete()
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val fileName = "receipts_${dateFormat.format(Date())}.csv"
                        csvLauncher.launch(fileName)
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Summary",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val totalAmount = receipts.sumOf { it.totalAmount }
                    Text(
                        text = "Total: %.2f".format(totalAmount),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Receipts: ${receipts.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    // Breakdown by category
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "By Category:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    val byCategory = receipts.groupBy { it.category }
                        .mapValues { it.value.sumOf { receipt -> receipt.totalAmount } }
                        .toList()
                        .sortedByDescending { it.second }
                    
                    byCategory.forEach { (category, amount) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = category)
                            Text(text = "%.2f".format(amount))
                        }
                    }
                }
            }

            // Receipt List
            Text(
                text = "Recent Receipts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(receipts) { receipt ->
                    ReceiptCard(
                        receipt = receipt,
                        onClick = { }
                    )
                }
            }
        }
    }
}

fun getStartOfMonth(): Long {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
