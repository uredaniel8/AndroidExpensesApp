package com.expenses.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expenses.app.data.*
import com.expenses.app.util.OcrProcessor
import com.expenses.app.util.FileUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
                    currency = ocrResult.currency ?: "USD",
                    category = "Uncategorized",
                    notes = null,
                    ocrRawText = ocrResult.rawText,
                    ocrConfidence = ocrResult.confidence,
                    originalUri = imageUri.toString()
                )

                repository.insertReceipt(receipt)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error processing receipt"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateReceipt(receipt: Receipt) {
        viewModelScope.launch {
            try {
                repository.updateReceipt(receipt)
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
