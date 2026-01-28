package com.expenses.app.util

import android.content.Context
import android.net.Uri
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
 * 
 * Note: Category matching is case-insensitive, so "Fuel", "FUEL", "fuel" all match.
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
    
    fun setConfig(config: ProtonDriveConfig) {
        this.config = config
    }
    
    fun isConfigured(): Boolean {
        return config != null && config!!.enabled
    }
    
    /**
     * Saves a receipt image to local storage.
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
            
            // Determine the folder based on category
            val folderPath = if (receipt.category.equals("Fuel", ignoreCase = true)) {
                FUEL_FOLDER_PATH
            } else {
                OTHER_FOLDER_PATH
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
            
            // Create the destination folder
            val baseFolder = File(context.getExternalFilesDir(null), folderPath)
            if (!baseFolder.exists()) {
                baseFolder.mkdirs()
            }
            
            // Copy file to local storage
            val destFile = File(baseFolder, fileName)
            val sourceUri = Uri.parse(imageUri)
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
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
                baseFolder.mkdirs()
            }
            
            // Create Fuel subfolder
            val fuelFolder = File(context.getExternalFilesDir(null), FUEL_FOLDER_PATH)
            if (!fuelFolder.exists()) {
                fuelFolder.mkdirs()
            }
            
            // Create Other subfolder
            val otherFolder = File(context.getExternalFilesDir(null), OTHER_FOLDER_PATH)
            if (!otherFolder.exists()) {
                otherFolder.mkdirs()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
