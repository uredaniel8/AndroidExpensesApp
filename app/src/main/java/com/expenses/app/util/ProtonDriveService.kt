package com.expenses.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.expenses.app.data.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Service class for handling local storage operations.
 * 
 * Features:
 * - Saves receipts to local device storage with category-based folder organization
 * - Fuel category receipts go to "Receipts/Fuel" folder (case-insensitive match)
 * - Other category receipts go to "Receipts/Other" folder
 * - Stores files in the app's external files directory
 * - Supports custom folder selection via Storage Access Framework
 * 
 * Note: 
 * - Category matching is case-insensitive, so "Fuel", "FUEL", "fuel" all match.
 * - Uses getExternalFilesDir which doesn't require WRITE_EXTERNAL_STORAGE permission
 *   as it's the app-specific external storage directory.
 * - Custom folders require persistent URI permissions.
 */
class ProtonDriveService(private val context: Context) {
    
    companion object {
        private const val FUEL_FOLDER_PATH = "Receipts/Fuel"
        private const val OTHER_FOLDER_PATH = "Receipts/Other"
    }
    
    /**
     * Configuration for local storage.
     */
    data class ProtonDriveConfig(
        val accessToken: String = "",
        val enabled: Boolean = false
    )
    
    // This should be stored securely in Android Keystore in production
    @Volatile
    private var config: ProtonDriveConfig? = null
    
    @Volatile
    private var customFuelFolderUri: Uri? = null
    
    @Volatile
    private var customOtherFolderUri: Uri? = null
    
    fun setConfig(config: ProtonDriveConfig) {
        this.config = config
    }
    
    fun isConfigured(): Boolean {
        return config != null && config!!.enabled
    }
    
    /**
     * Sets the custom folder URI for fuel receipts.
     */
    fun setCustomFuelFolder(uri: Uri?) {
        customFuelFolderUri = uri
    }
    
    /**
     * Sets the custom folder URI for other receipts.
     */
    fun setCustomOtherFolder(uri: Uri?) {
        customOtherFolderUri = uri
    }
    
    /**
     * Saves a receipt image to local storage.
     * Uses custom folders if set, otherwise falls back to default folders.
     * 
     * @param receipt The receipt to save
     * @param imageUri The URI of the image to save
     * @return Result with the local file path or error message
     */
    suspend fun uploadReceipt(receipt: Receipt, imageUri: String): Result<String> = withContext(Dispatchers.IO) {
        val currentConfig = config
        
        try {
            if (currentConfig == null || !currentConfig.enabled) {
                return@withContext Result.failure(Exception("Local storage is not enabled. Please enable local storage in settings."))
            }
            
            // Get the file name from receipt or generate one
            val fileName = receipt.renamedFileName ?: FileUtils.generateFileName(
                date = receipt.receiptDate,
                merchant = receipt.merchant,
                category = receipt.category,
                total = receipt.totalAmount,
                extension = "jpg",
                description = receipt.description
            )
            
            val sourceUri = Uri.parse(imageUri)
            val isFuelReceipt = receipt.category.equals("Fuel", ignoreCase = true)
            
            // Make a local copy of the custom folder URI to ensure thread safety
            val customFolderUri = if (isFuelReceipt) customFuelFolderUri else customOtherFolderUri
            
            if (customFolderUri != null) {
                // Use custom folder via SAF
                return@withContext saveToCustomFolder(sourceUri, customFolderUri, fileName, isFuelReceipt)
            } else {
                // Use default folder
                return@withContext saveToDefaultFolder(receipt, sourceUri, fileName, isFuelReceipt)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Saves receipt to custom folder using Storage Access Framework.
     */
    private suspend fun saveToCustomFolder(
        sourceUri: Uri,
        customFolderUri: Uri,
        fileName: String,
        isFuelReceipt: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Get DocumentFile for the custom folder
            val folder = DocumentFile.fromTreeUri(context, customFolderUri)
            
            if (folder == null || !folder.exists()) {
                return@withContext Result.failure(
                    Exception("Custom folder is no longer accessible. Please select a new folder in settings.")
                )
            }
            
            if (!folder.canWrite()) {
                return@withContext Result.failure(
                    Exception("No write permission for custom folder. Please select a different folder.")
                )
            }
            
            // Check if file already exists
            val existingFile = folder.findFile(fileName)
            val destFile = existingFile ?: folder.createFile("image/jpeg", fileName)
            
            if (destFile == null) {
                return@withContext Result.failure(Exception("Failed to create file in custom folder"))
            }
            
            // Copy file to custom folder
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                return@withContext Result.failure(Exception("Failed to open source image file"))
            }
            
            inputStream.use { input ->
                val outputStream = context.contentResolver.openOutputStream(destFile.uri)
                if (outputStream == null) {
                    return@withContext Result.failure(Exception("Failed to create output stream for custom folder"))
                }
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            
            val categoryName = if (isFuelReceipt) "Fuel" else "Other"
            Result.success("Custom/$categoryName/$fileName")
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied. Please select the folder again in settings."))
        } catch (e: Exception) {
            Result.failure(Exception("Failed to save to custom folder: ${e.message}"))
        }
    }
    
    /**
     * Saves receipt to default app folder.
     */
    private suspend fun saveToDefaultFolder(
        receipt: Receipt,
        sourceUri: Uri,
        fileName: String,
        isFuelReceipt: Boolean
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Determine the folder based on category
            val folderPath = if (isFuelReceipt) {
                FUEL_FOLDER_PATH
            } else {
                OTHER_FOLDER_PATH
            }
            
            // Create the destination folder
            val baseFolder = File(context.getExternalFilesDir(null), folderPath)
            if (!baseFolder.exists()) {
                val created = baseFolder.mkdirs()
                if (!created) {
                    return@withContext Result.failure(Exception("Failed to create storage directory. Please check storage permissions."))
                }
            }
            
            // Copy file to local storage
            val destFile = File(baseFolder, fileName)
            
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                return@withContext Result.failure(Exception("Failed to open image file. The file may be inaccessible."))
            }
            
            inputStream.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            if (destFile.exists()) {
                val localPath = "$folderPath/$fileName"
                Result.success(localPath)
            } else {
                Result.failure(Exception("Failed to save file to local storage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Ensures that the folder structure exists in local storage.
     * This should be called during initial setup.
     */
    suspend fun ensureFoldersExist(): Result<Unit> = withContext(Dispatchers.IO) {
        val currentConfig = config
        
        try {
            if (currentConfig == null || !currentConfig.enabled) {
                return@withContext Result.failure(Exception("Local storage is not enabled"))
            }
            
            // Create base Receipts folder
            val baseFolder = File(context.getExternalFilesDir(null), "Receipts")
            if (!baseFolder.exists()) {
                val created = baseFolder.mkdirs()
                if (!created) {
                    return@withContext Result.failure(Exception("Failed to create Receipts directory"))
                }
            }
            
            // Create Fuel subfolder
            val fuelFolder = File(context.getExternalFilesDir(null), FUEL_FOLDER_PATH)
            if (!fuelFolder.exists()) {
                val created = fuelFolder.mkdirs()
                if (!created) {
                    return@withContext Result.failure(Exception("Failed to create Fuel directory"))
                }
            }
            
            // Create Other subfolder
            val otherFolder = File(context.getExternalFilesDir(null), OTHER_FOLDER_PATH)
            if (!otherFolder.exists()) {
                val created = otherFolder.mkdirs()
                if (!created) {
                    return@withContext Result.failure(Exception("Failed to create Other directory"))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
