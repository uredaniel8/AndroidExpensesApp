package com.expenses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.expenses.app.data.Receipt
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReceiptScreen(
    receipt: Receipt?,
    onBack: () -> Unit,
    onSave: (Receipt) -> Unit
) {
    var merchant by remember { mutableStateOf(receipt?.merchant ?: "") }
    var totalAmount by remember { mutableStateOf(receipt?.totalAmount?.toString() ?: "") }
    var vatAmount by remember { mutableStateOf(receipt?.vatAmount?.toString() ?: "") }
    var currency by remember { mutableStateOf(receipt?.currency ?: "USD") }
    var category by remember { mutableStateOf(receipt?.category ?: "Uncategorized") }
    var notes by remember { mutableStateOf(receipt?.notes ?: "") }
    
    val categories = listOf(
        "Uncategorized", "Fuel", "Lunch", "Dinner", 
        "Hotel", "Transport", "Office Supplies", "Entertainment"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (receipt == null) "New Receipt" else "Edit Receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            receipt?.let {
                                val updatedReceipt = it.copy(
                                    merchant = merchant.takeIf { it.isNotBlank() },
                                    totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                                    vatAmount = vatAmount.toDoubleOrNull(),
                                    currency = currency,
                                    category = category,
                                    notes = notes.takeIf { it.isNotBlank() }
                                )
                                onSave(updatedReceipt)
                                onBack()
                            }
                        },
                        enabled = receipt != null
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
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
            // Merchant
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text("Merchant") },
                modifier = Modifier.fillMaxWidth()
            )

            // Date (display only for now)
            receipt?.let {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                Text(
                    text = "Date: ${dateFormat.format(Date(it.receiptDate))}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Total Amount
            OutlinedTextField(
                value = totalAmount,
                onValueChange = { totalAmount = it },
                label = { Text("Total Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // VAT Amount
            OutlinedTextField(
                value = vatAmount,
                onValueChange = { vatAmount = it },
                label = { Text("VAT Amount (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            // Currency
            var currencyExpanded by remember { mutableStateOf(false) }
            val currencies = listOf("USD", "EUR", "GBP", "CHF")
            
            ExposedDropdownMenuBox(
                expanded = currencyExpanded,
                onExpandedChange = { currencyExpanded = it }
            ) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Currency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = currencyExpanded,
                    onDismissRequest = { currencyExpanded = false }
                ) {
                    currencies.forEach { curr ->
                        DropdownMenuItem(
                            text = { Text(curr) },
                            onClick = {
                                currency = curr
                                currencyExpanded = false
                            }
                        )
                    }
                }
            }

            // Category
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.chunked(2).forEach { rowCategories ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5
            )

            // OCR Confidence
            receipt?.ocrConfidence?.let { confidence ->
                if (confidence < 0.5f) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = "⚠️ Low OCR confidence. Please verify the data.",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}
