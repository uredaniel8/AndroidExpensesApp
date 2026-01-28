package com.expenses.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expenses.app.data.*
import com.expenses.app.util.CurrencyUtils
import com.expenses.app.util.OcrProcessor
import com.expenses.app.util.FileUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ReceiptDatabase.getDatabase(application)
    private val repository = ReceiptRepository(database.receiptDao())
    private val ocrProcessor = OcrProcessor()

    val receipts: StateFlow<List<Receipt>> = repository.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastProcessedReceiptId = MutableStateFlow<String?>(null)
    val lastProcessedReceiptId: StateFlow<String?> = _lastProcessedReceiptId.asStateFlow()

    fun processReceipt(imageUri: Uri) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null
                
                val ocrResult = ocrProcessor.processImage(
                    imageUri,
                    getApplication<Application>().contentResolver
                )

                val receipt = Receipt(
                    receiptDate = ocrResult.date ?: System.currentTimeMillis(),
                    merchant = ocrResult.merchant,
                    totalAmount = ocrResult.totalAmount ?: 0.0,
                    vatAmount = ocrResult.vatAmount,
                    currency = ocrResult.currency ?: CurrencyUtils.getDefaultCurrency(),
                    category = "Uncategorized",
                    description = null,
                    notes = null,
                    ocrRawText = ocrResult.rawText,
                    ocrConfidence = ocrResult.confidence,
                    originalUri = imageUri.toString(),
                    exportStatus = ExportStatus.NOT_EXPORTED  // Start as NOT_EXPORTED until user saves
                )

                repository.insertReceipt(receipt)
                _lastProcessedReceiptId.value = receipt.id
            } catch (e: Exception) {
                _error.value = e.message ?: "Error processing receipt"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun clearLastProcessedReceiptId() {
        _lastProcessedReceiptId.value = null
    }

    fun updateReceipt(receipt: Receipt) {
        viewModelScope.launch {
            try {
                // Save and rename the file based on category and new naming convention
                val context = getApplication<Application>()
                val updatedReceipt = if (receipt.originalUri != null || receipt.storedUri != null) {
                    val sourceUri = Uri.parse(receipt.storedUri ?: receipt.originalUri)
                    val extension = FileUtils.getFileExtension(sourceUri, context)
                    val newFileName = FileUtils.generateFileName(
                        receipt.receiptDate,
                        receipt.description,
                        receipt.totalAmount,
                        extension
                    )
                    
                    val categoryFolder = FileUtils.getCategoryFolder(context, receipt.category)
                    val destFile = FileUtils.copyToExportFolder(
                        context,
                        sourceUri,
                        categoryFolder.absolutePath,
                        newFileName
                    )
                    
                    // Delete old file if it exists and is different from the new one
                    receipt.storedUri?.let { oldUri ->
                        try {
                            val oldFile = File(Uri.parse(oldUri).path ?: "")
                            if (oldFile.exists() && oldFile.absolutePath != destFile?.absolutePath) {
                                oldFile.delete()
                            }
                        } catch (e: Exception) {
                            // Log but don't fail the update
                            e.printStackTrace()
                        }
                    }
                    
                    receipt.copy(
                        storedUri = destFile?.let { Uri.fromFile(it).toString() },
                        renamedFileName = newFileName,
                        exportStatus = ExportStatus.EXPORTED  // Mark as committed when saved
                    )
                } else {
                    receipt.copy(exportStatus = ExportStatus.EXPORTED)
                }
                
                repository.updateReceipt(updatedReceipt)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error updating receipt"
            }
        }
    }

    fun deleteReceipt(receipt: Receipt) {
        viewModelScope.launch {
            try {
                repository.deleteReceipt(receipt)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting receipt"
            }
        }
    }

    fun getReceiptsByDateRange(startDate: Long, endDate: Long): Flow<List<Receipt>> {
        return repository.getReceiptsByDateRange(startDate, endDate)
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.release()
    }
}
