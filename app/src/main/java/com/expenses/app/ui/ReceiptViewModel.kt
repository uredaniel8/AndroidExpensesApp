package com.expenses.app.ui

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.expenses.app.data.Category
import com.expenses.app.data.CategoryRepository
import com.expenses.app.data.ExportStatus
import com.expenses.app.data.Receipt
import com.expenses.app.data.ReceiptDatabase
import com.expenses.app.data.ReceiptRepository
import com.expenses.app.util.CurrencyUtils
import com.expenses.app.util.FileUtils
import com.expenses.app.util.FolderPreferences
import com.expenses.app.util.OcrProcessor
import kotlinx.coroutines.delay
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
    private val folderPreferences = FolderPreferences(application)

    // Local-storage enabled flag (since Proton is removed)
    private val _localStorageEnabled = MutableStateFlow(false)
    val localStorageEnabled: StateFlow<Boolean> = _localStorageEnabled.asStateFlow()

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
        viewModelScope.launch {
            categoryRepository.initializeDefaultCategories()
        }

        // Load saved folder preferences
        _fuelFolderUri.value = folderPreferences.getFuelFolderUri()
        _otherFolderUri.value = folderPreferences.getOtherFolderUri()
    }

    /**
     * Sets the custom folder for fuel receipts.
     * Persists the URI and requests persistent permissions.
     */
    fun setFuelFolder(uri: Uri?) {
        viewModelScope.launch {
            try {
                // Release old permission if resetting
                if (uri == null) {
                    val oldUri = _fuelFolderUri.value
                    if (oldUri != null) {
                        try {
                            val contentResolver = getApplication<Application>().contentResolver
                            contentResolver.releasePersistableUriPermission(
                                oldUri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                            // Ignore if permission already released
                        }
                    }
                } else {
                    // Take persistent permission for the new URI
                    val contentResolver = getApplication<Application>().contentResolver
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        _error.value =
                            "Cannot access selected folder. Please try selecting a different folder."
                        return@launch
                    } catch (e: UnsupportedOperationException) {
                        _error.value =
                            "Selected folder does not support persistent access. Please choose a folder from your device storage."
                        return@launch
                    } catch (e: Exception) {
                        _error.value = "Failed to obtain folder access: ${e.message}"
                        return@launch
                    }
                }

                folderPreferences.setFuelFolderUri(uri)
                _fuelFolderUri.value = uri
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
                // Release old permission if resetting
                if (uri == null) {
                    val oldUri = _otherFolderUri.value
                    if (oldUri != null) {
                        try {
                            val contentResolver = getApplication<Application>().contentResolver
                            contentResolver.releasePersistableUriPermission(
                                oldUri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                            // Ignore if permission already released
                        }
                    }
                } else {
                    // Take persistent permission for the new URI
                    val contentResolver = getApplication<Application>().contentResolver
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    } catch (e: SecurityException) {
                        _error.value =
                            "Cannot access selected folder. Please try selecting a different folder."
                        return@launch
                    } catch (e: UnsupportedOperationException) {
                        _error.value =
                            "Selected folder does not support persistent access. Please choose a folder from your device storage."
                        return@launch
                    } catch (e: Exception) {
                        _error.value = "Failed to obtain folder access: ${e.message}"
                        return@launch
                    }
                }

                folderPreferences.setOtherFolderUri(uri)
                _otherFolderUri.value = uri
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

                val ocrResult = ocrProcessor.processImage(
                    imageUri = imageUri,
                    context = getApplication()
                )

                val receipt = Receipt(
                    receiptDate = System.currentTimeMillis(),
                    merchant = null,
                    totalAmount = 0.0,
                    vatAmount = null,
                    currency = ocrResult.currency ?: CurrencyUtils.getDefaultCurrency(),
                    category = "Uncategorized",
                    notes = null,
                    tags = emptyList(),
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

    /**
     * Determines the appropriate custom folder URI based on the receipt category.
     * 
     * @param category Receipt category (e.g., "Fuel", "Other")
     * @return Custom folder URI for fuel if category is "Fuel", otherwise returns other folder URI
     */
    private fun getCustomFolderForCategory(category: String): Uri? {
        val folderUri = if (category.equals("Fuel", ignoreCase = true)) {
            Log.d("ReceiptViewModel", "Category '$category' matched as Fuel - checking for custom Fuel folder")
            _fuelFolderUri.value
        } else {
            Log.d("ReceiptViewModel", "Category '$category' is not Fuel - checking for custom Other folder")
            _otherFolderUri.value
        }
        
        if (folderUri != null) {
            Log.d("ReceiptViewModel", "Custom folder found for category '$category': $folderUri")
        } else {
            Log.d("ReceiptViewModel", "No custom folder set for category '$category', will use default")
        }
        
        return folderUri
    }

    fun updateReceipt(receipt: Receipt) {
        viewModelScope.launch {
            try {
                // Save image with category-based storage if originalUri exists
                val updatedReceipt =
                    if (receipt.originalUri != null && receipt.storedUri == null) {
                        val imageUri = Uri.parse(receipt.originalUri)
                        val customFolderUri = getCustomFolderForCategory(receipt.category)
                        
                        val (storedPath, fileName) = FileUtils.saveReceiptImage(
                            context = getApplication(),
                            sourceUri = imageUri,
                            receipt = receipt,
                            customFolderUri = customFolderUri
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
                receipt.storedUri?.let { storedUri ->
                    try {
                        val file = java.io.File(storedUri)
                        if (file.exists()) file.delete()
                    } catch (e: Exception) {
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
     * Kept name/signature so the rest of your app doesn't break.
     * This now simply toggles LOCAL storage on/off (no Proton).
     *
     * @param accessToken Unused parameter (kept for backward compatibility)
     * @param enabled Whether local storage is enabled
     */
    fun configureProtonDrive(accessToken: String, enabled: Boolean) {
        _localStorageEnabled.value = enabled
        _uploadStatus.value = if (enabled) "Local storage enabled" else "Local storage disabled"

        viewModelScope.launch {
            delay(2000)
            _uploadStatus.value = null
        }
    }

    /**
     * Kept name so existing UI buttons keep working.
     * This now marks a receipt as "exported to local storage".
     *
     * If the receipt hasn't been saved locally yet, it will attempt to save it first.
     */
    fun uploadToProtonDrive(receipt: Receipt) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null
                _uploadStatus.value = "Saving locally..."

                if (!_localStorageEnabled.value) {
                    _error.value =
                        "Local storage is not enabled. Enable it in Settings first."
                    _uploadStatus.value = null
                    return@launch
                }

                // Ensure we have a local stored file path
                val ensuredReceipt = if (receipt.storedUri == null && receipt.originalUri != null) {
                    val imageUri = Uri.parse(receipt.originalUri)
                    val customFolderUri = getCustomFolderForCategory(receipt.category)
                    
                    val (storedPath, fileName) = FileUtils.saveReceiptImage(
                        context = getApplication(),
                        sourceUri = imageUri,
                        receipt = receipt,
                        customFolderUri = customFolderUri
                    )
                    val updated = receipt.copy(
                        storedUri = storedPath,
                        renamedFileName = fileName
                    )
                    repository.updateReceipt(updated)
                    updated
                } else {
                    receipt
                }

                val localPath = ensuredReceipt.storedUri
                if (localPath == null) {
                    _error.value = "No image found for this receipt"
                    _uploadStatus.value = null
                    return@launch
                }

                // Mark as exported (local)
                val updatedReceipt = ensuredReceipt.copy(
                    exportFolderUri = localPath,
                    exportStatus = ExportStatus.EXPORTED,
                    lastExportAttemptAt = System.currentTimeMillis()
                )
                repository.updateReceipt(updatedReceipt)

                _uploadStatus.value = "Saved to local storage"
                delay(2500)
                _uploadStatus.value = null
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
