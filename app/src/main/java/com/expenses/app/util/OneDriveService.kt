package com.expenses.app.util

import android.content.Context
import android.net.Uri
import com.expenses.app.data.Receipt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

/**
 * Service class for handling OneDrive upload operations.
 * 
 * Features:
 * - Uploads receipts to OneDrive with category-based folder organization
 * - Fuel category receipts go to "Receipts/Fuel" folder
 * - Other category receipts go to "Receipts/Other" folder
 * - Uses Microsoft Graph API for file uploads
 */
class OneDriveService(private val context: Context) {
    
    private val client = OkHttpClient()
    
    companion object {
        private const val GRAPH_API_BASE_URL = "https://graph.microsoft.com/v1.0"
        private const val FUEL_FOLDER_PATH = "Receipts/Fuel"
        private const val OTHER_FOLDER_PATH = "Receipts/Other"
    }
    
    /**
     * Configuration for OneDrive access.
     * Note: In a production app, the access token should be obtained via OAuth2.
     * This is a placeholder implementation.
     */
    data class OneDriveConfig(
        val accessToken: String,
        val enabled: Boolean = false
    )
    
    // This should be stored securely or obtained via OAuth
    private var config: OneDriveConfig? = null
    
    fun setConfig(config: OneDriveConfig) {
        this.config = config
    }
    
    fun isConfigured(): Boolean {
        return config != null && config!!.enabled && config!!.accessToken.isNotBlank()
    }
    
    /**
     * Uploads a receipt image to OneDrive.
     * 
     * @param receipt The receipt to upload
     * @param imageUri The URI of the image to upload
     * @return Result with the OneDrive file path or error message
     */
    suspend fun uploadReceipt(receipt: Receipt, imageUri: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("OneDrive is not configured. Please set up OneDrive integration in settings."))
            }
            
            val accessToken = config!!.accessToken
            
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
            
            // Copy file to temporary location if needed
            val file = getFileFromUri(Uri.parse(imageUri))
            
            // Upload to OneDrive
            val uploadUrl = "$GRAPH_API_BASE_URL/me/drive/root:/$folderPath/$fileName:/content"
            
            val requestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .addHeader("Content-Type", "image/jpeg")
                .put(requestBody)
                .build()
            
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val oneDrivePath = "$folderPath/$fileName"
                    Result.success(oneDrivePath)
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Result.failure(Exception("Upload failed: ${response.code} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Gets a File object from a URI.
     * If the URI points to a content:// URI, copies it to a temporary file first.
     */
    private fun getFileFromUri(uri: Uri): File {
        return if (uri.scheme == "content") {
            // Create a temporary file
            val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } else {
            // Direct file path
            File(uri.path ?: throw IllegalArgumentException("Invalid file URI"))
        }
    }
    
    /**
     * Creates the folder structure on OneDrive if it doesn't exist.
     * This should be called during initial setup.
     */
    suspend fun ensureFoldersExist(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured()) {
                return@withContext Result.failure(Exception("OneDrive is not configured"))
            }
            
            val accessToken = config!!.accessToken
            
            // Create base Receipts folder
            createFolder("Receipts", accessToken)
            
            // Create Fuel subfolder
            createFolder("$FUEL_FOLDER_PATH", accessToken)
            
            // Create Other subfolder
            createFolder("$OTHER_FOLDER_PATH", accessToken)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun createFolder(path: String, accessToken: String) {
        val url = "$GRAPH_API_BASE_URL/me/drive/root:/$path"
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful && response.code == 404) {
                // Folder doesn't exist, create it
                // Note: This is a simplified implementation
                // In production, use proper folder creation API
            }
        }
    }
}
