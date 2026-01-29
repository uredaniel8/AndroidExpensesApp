package com.expenses.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    categories: List<String>,
    onBack: () -> Unit,
    onSave: (Receipt) -> Unit,
    onDelete: (Receipt) -> Unit,
    onUploadToProtonDrive: (Receipt) -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit
) {
    var merchant by remember { mutableStateOf(receipt?.merchant ?: "") }
    var totalAmount by remember { mutableStateOf(receipt?.totalAmount?.toString() ?: "") }
    var vatAmount by remember { mutableStateOf(receipt?.vatAmount?.toString() ?: "") }
    var currency by remember { mutableStateOf(receipt?.currency ?: "USD") }
    var category by remember { mutableStateOf(receipt?.category ?: "Uncategorized") }
    var notes by remember { mutableStateOf(receipt?.notes ?: "") }
    var description by remember { mutableStateOf(receipt?.description ?: "") }
    var receiptDate by remember { mutableStateOf(receipt?.receiptDate ?: System.currentTimeMillis()) }
    
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteCategoryDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteReceiptDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

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
                        onClick = { showDeleteReceiptDialog = true },
                        enabled = receipt != null
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Receipt")
                    }
                    IconButton(
                        onClick = {
                            receipt?.let {
                                val updatedReceipt = it.copy(
                                    receiptDate = receiptDate,
                                    merchant = merchant.takeIf { it.isNotBlank() },
                                    totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                                    vatAmount = vatAmount.toDoubleOrNull(),
                                    currency = currency,
                                    category = category,
                                    notes = notes.takeIf { it.isNotBlank() },
                                    description = description.takeIf { it.isNotBlank() }
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

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            // Date Picker
            val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            OutlinedTextField(
                value = dateFormat.format(Date(receiptDate)),
                onValueChange = {},
                label = { Text("Receipt Date") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row {
                    TextButton(onClick = { showAddCategoryDialog = true }) {
                        Text("+ Add")
                    }
                    TextButton(onClick = { showDeleteCategoryDialog = true }) {
                        Text("- Remove")
                    }
                }
            }
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
            
            // Save Locally Button
            receipt?.let {
                Button(
                    onClick = { onUploadToProtonDrive(it) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save to Local Storage")
                }
            }

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
    
    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add New Category") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCategoryName.isNotBlank()) {
                            onAddCategory(newCategoryName.trim())
                            showAddCategoryDialog = false
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Category Dialog
    if (showDeleteCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCategoryDialog = false },
            title = { Text("Remove Category") },
            text = {
                Column {
                    Text("Select a category to remove:")
                    Spacer(modifier = Modifier.height(8.dp))
                    categories.filter { it != "Uncategorized" }.forEach { cat ->
                        TextButton(
                            onClick = {
                                categoryToDelete = cat
                            }
                        ) {
                            Text(cat)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categoryToDelete?.let { onDeleteCategory(it) }
                        showDeleteCategoryDialog = false
                        categoryToDelete = null
                    },
                    enabled = categoryToDelete != null
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showDeleteCategoryDialog = false
                    categoryToDelete = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Delete Receipt Confirmation Dialog
    if (showDeleteReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteReceiptDialog = false },
            title = { Text("Delete Receipt?") },
            text = { Text("This action cannot be undone. The receipt and its associated image will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        receipt?.let { 
                            onDelete(it)
                            onBack()
                        }
                        showDeleteReceiptDialog = false
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteReceiptDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = receiptDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDate ->
                            receiptDate = selectedDate
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
