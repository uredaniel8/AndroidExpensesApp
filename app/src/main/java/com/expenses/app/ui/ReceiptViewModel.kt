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
import com.expenses.app.data.ExportStatus
import com.expenses.app.util.CurrencyUtils
import com.expenses.app.util.OcrProcessor
import com.expenses.app.util.FileUtils
import com.expenses.app.util.ProtonDriveService
import com.expenses.app.util.FolderPreferences
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
    private val protonDriveService = ProtonDriveService(application)
    private val folderPreferences = FolderPreferences(application)

    val receipts: StateFlow<List<Receipt>> = repository.getAllReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _uploadStatus = MutableStateFlow<String?>(null)
    val uploadStatus: StateFlow<String?> = _uploadStatus.asStateFlow()
    
    private val _fuelFolderUri = MutableStateFlow<Uri?>(null)
    val fuelFolderUri: StateFlow<Uri?> = _fuelFolderUri.asStateFlow()
    
    private val _otherFolderUri = MutableStateFlow<Uri?>(null)
    val otherFolderUri: StateFlow<Uri?> = _otherFolderUri.asStateFlow()

    init {
        // Initialize default categories
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
        }
        
        // Load saved folder preferences
        _fuelFolderUri.value = folderPreferences.getFuelFolderUri()
        _otherFolderUri.value = folderPreferences.getOtherFolderUri()
        
        // Set loaded URIs in services
        protonDriveService.setCustomFuelFolder(_fuelFolderUri.value)
        protonDriveService.setCustomOtherFolder(_otherFolderUri.value)
        FileUtils.setCustomFuelFolder(_fuelFolderUri.value)
        FileUtils.setCustomOtherFolder(_otherFolderUri.value)
    }
    
    /**
     * Sets the custom folder for fuel receipts.
     * Persists the URI and requests persistent permissions.
     */
    fun setFuelFolder(uri: Uri?) {
        viewModelScope.launch {
            try {
                if (uri != null) {
                    // Take persistent permission for the URI
                    val contentResolver = getApplication<Application>().contentResolver
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        _error.value = "Failed to obtain persistent folder access: ${e.message}"
                        return@launch
                    }
                }
                folderPreferences.setFuelFolderUri(uri)
                _fuelFolderUri.value = uri
                protonDriveService.setCustomFuelFolder(uri)
                FileUtils.setCustomFuelFolder(uri)
            } catch (e: Exception) {
                _error.value = "Failed to set fuel folder: ${e.message}"
            }
        }
    }
    
    /**
     * Sets the custom folder for other receipts.
     * Persists the URI and requests persistent permissions.
     */
    fun setOtherFolder(uri: Uri?) {
        viewModelScope.launch {
            try {
                if (uri != null) {
                    // Take persistent permission for the URI
                    val contentResolver = getApplication<Application>().contentResolver
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        _error.value = "Failed to obtain persistent folder access: ${e.message}"
                        return@launch
                    }
                }
                folderPreferences.setOtherFolderUri(uri)
                _otherFolderUri.value = uri
                protonDriveService.setCustomOtherFolder(uri)
                FileUtils.setCustomOtherFolder(uri)
            } catch (e: Exception) {
                _error.value = "Failed to set other folder: ${e.message}"
            }
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
                // Delete associated image files if they exist
                receipt.storedUri?.let { storedUri ->
                    try {
                        val file = java.io.File(storedUri)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        // Log but don't fail the delete operation
                        e.printStackTrace()
                    }
                }
                
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
    
    /**
     * Configures local storage.
     * 
     * @param accessToken Unused parameter (kept for backward compatibility)
     * @param enabled Whether local storage is enabled
     */
    fun configureProtonDrive(accessToken: String, enabled: Boolean) {
        protonDriveService.setConfig(
            ProtonDriveService.ProtonDriveConfig(
                accessToken = "",
                enabled = enabled
            )
        )
        
        if (enabled) {
            viewModelScope.launch {
                try {
                    protonDriveService.ensureFoldersExist()
                    _uploadStatus.value = "Local storage configured successfully"
                } catch (e: Exception) {
                    _error.value = "Failed to configure local storage: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Saves a receipt to local storage.
     * 
     * @param receipt The receipt to save
     */
    fun uploadToProtonDrive(receipt: Receipt) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _uploadStatus.value = "Saving to local storage..."
                
                if (!protonDriveService.isConfigured()) {
                    _error.value = "Local storage is not enabled. Please enable local storage in settings."
                    _uploadStatus.value = null
                    _isProcessing.value = false
                    return@launch
                }
                
                // Use stored URI or original URI
                val imageUri = receipt.storedUri ?: receipt.originalUri
                if (imageUri == null) {
                    _error.value = "No image found for this receipt"
                    _uploadStatus.value = null
                    _isProcessing.value = false
                    return@launch
                }
                
                val result = protonDriveService.uploadReceipt(receipt, imageUri)
                
                if (result.isSuccess) {
                    val localPath = result.getOrNull()
                    _uploadStatus.value = "Successfully saved to local storage"
                    
                    // Update receipt with local storage path and export status
                    val updatedReceipt = receipt.copy(
                        exportFolderUri = localPath,
                        exportStatus = ExportStatus.EXPORTED,
                        lastExportAttemptAt = System.currentTimeMillis()
                    )
                    repository.updateReceipt(updatedReceipt)
                    
                    // Clear status after a delay
                    kotlinx.coroutines.delay(3000)
                    _uploadStatus.value = null
                } else {
                    val error = result.exceptionOrNull()
                    _error.value = "Save failed: ${error?.message}"
                    _uploadStatus.value = null
                    
                    // Update receipt with failed status
                    val updatedReceipt = receipt.copy(
                        exportStatus = ExportStatus.FAILED,
                        lastExportAttemptAt = System.currentTimeMillis()
                    )
                    repository.updateReceipt(updatedReceipt)
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error saving to local storage"
                _uploadStatus.value = null
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun clearUploadStatus() {
        _uploadStatus.value = null
    }

    override fun onCleared() {
        super.onCleared()
        ocrProcessor.release()
    }
}
