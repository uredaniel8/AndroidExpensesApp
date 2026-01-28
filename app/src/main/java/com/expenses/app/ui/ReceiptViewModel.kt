package com.expenses.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expenses.app.data.Receipt
import com.expenses.app.data.ReceiptDatabase
import com.expenses.app.data.ReceiptRepository
import com.expenses.app.data.Category
import com.expenses.app.data.CategoryRepository
import com.expenses.app.util.CurrencyUtils
import com.expenses.app.util.OcrProcessor
import com.expenses.app.util.FileUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ReceiptDatabase.getDatabase(application)
    private val repository = ReceiptRepository(database.receiptDao())
    private val categoryRepository = CategoryRepository(database.categoryDao())
    private val ocrProcessor = OcrProcessor()

    val receipts: StateFlow<List<Receipt>> = repository.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        // Initialize default categories
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
        }
    }

    fun processReceipt(imageUri: Uri) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null

                // ✅ OcrProcessor now takes Context (not ContentResolver)
                val ocrResult = ocrProcessor.processImage(
                    imageUri = imageUri,
                    context = getApplication()
                )

                val receipt = Receipt(
                    receiptDate = ocrResult.date ?: System.currentTimeMillis(),
                    merchant = ocrResult.merchant,
                    totalAmount = ocrResult.totalAmount ?: 0.0,
                    vatAmount = ocrResult.vatAmount,
                    currency = ocrResult.currency ?: CurrencyUtils.getDefaultCurrency(),
                    category = "Uncategorized",
                    notes = null,
                    tags = emptyList(),
                    ocrRawText = ocrResult.rawText,
                    ocrConfidence = ocrResult.confidence,
                    originalUri = imageUri.toString()
                    // storedUri / renamedFileName / exportFolderUri default to null now ✅
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
                // Save image with category-based storage if originalUri exists
                val updatedReceipt = if (receipt.originalUri != null && receipt.storedUri == null) {
                    val imageUri = Uri.parse(receipt.originalUri)
                    val (storedPath, fileName) = FileUtils.saveReceiptImage(
                        context = getApplication(),
                        sourceUri = imageUri,
                        receipt = receipt
                    )
                    receipt.copy(
                        storedUri = storedPath,
                        renamedFileName = fileName
                    )
                } else {
                    receipt
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

    fun addCategory(categoryName: String) {
        viewModelScope.launch {
            try {
                if (!categoryRepository.categoryExists(categoryName)) {
                    categoryRepository.insertCategory(Category(categoryName, isDefault = false))
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error adding category"
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            try {
                if (!category.isDefault) {
                    categoryRepository.deleteCategory(category)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting category"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.release()
    }
}
