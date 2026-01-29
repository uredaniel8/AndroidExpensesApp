package com.expenses.app.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.expenses.app.data.Receipt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    fun generateFileName(
        date: Long,
        merchant: String?,
        category: String,
        total: Double,
        extension: String,
        description: String? = null
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(date))
        val descriptionStr = description?.replace(Regex("[^a-zA-Z0-9 ]"), "")?.trim() ?: merchant?.replace(Regex("[^a-zA-Z0-9 ]"), "")?.trim() ?: "Unknown"
        val totalStr = String.format("%.2f", total)
        
        return "${dateStr} - ${descriptionStr} - ${totalStr}.${extension}"
    }

    fun copyToExportFolder(
        context: Context,
        sourceUri: Uri,
        exportFolderPath: String,
        newFileName: String
    ): File? {
        return try {
            val exportFolder = File(exportFolderPath)
            if (!exportFolder.exists()) {
                exportFolder.mkdirs()
            }
            
            val destFile = File(exportFolder, newFileName)
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileExtension(uri: Uri, context: Context): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "application/pdf" -> "pdf"
            else -> {
                // Try to get from URI
                val path = uri.path ?: ""
                path.substringAfterLast('.', "jpg")
            }
        }
    }

    fun getCategoryFolder(context: Context, category: String): File {
        val baseFolder = File(context.getExternalFilesDir(null), "Receipts")
        val categoryFolder = if (category.equals("Fuel", ignoreCase = true)) {
            android.util.Log.d("FileUtils", "Category '$category' matched as Fuel - using Receipts/Fuel folder")
            File(baseFolder, "Fuel")
        } else {
            android.util.Log.d("FileUtils", "Category '$category' matched as Other - using Receipts/Other folder")
            File(baseFolder, "Other")
        }
        if (!categoryFolder.exists()) {
            val created = categoryFolder.mkdirs()
            android.util.Log.d("FileUtils", "Created folder ${categoryFolder.absolutePath}: $created")
        }
        return categoryFolder
    }

    /**
     * Saves a receipt image to either a custom folder (if provided) or the default app folder.
     * 
     * When a custom folder is provided, the image is saved using the Storage Access Framework (DocumentFile API).
     * The returned URI will be a content:// URI in this case.
     * 
     * When the default folder is used (or as fallback), the image is saved to app's external files directory.
     * The returned URI will be a file path string in this case.
     * 
     * Note: The stored URI format differs based on storage method (content:// vs file path).
     * This is by design to support both SAF and traditional file storage.
     * 
     * @param context Application context
     * @param sourceUri URI of the source image to save
     * @param receipt Receipt object containing metadata for file naming
     * @param customFolderUri Optional custom folder URI selected by user (from ACTION_OPEN_DOCUMENT_TREE)
     * @return Pair of (file path/URI string, file name) or (null, null) on error
     */
    fun saveReceiptImage(
        context: Context,
        sourceUri: Uri,
        receipt: Receipt,
        customFolderUri: Uri? = null
    ): Pair<String?, String?> {
        return try {
            android.util.Log.d("FileUtils", "Starting saveReceiptImage for category: ${receipt.category}")
            
            val extension = getFileExtension(sourceUri, context)
            val fileName = generateFileName(
                date = receipt.receiptDate,
                merchant = receipt.merchant,
                category = receipt.category,
                total = receipt.totalAmount,
                extension = extension,
                description = receipt.description
            )
            
            android.util.Log.d("FileUtils", "Generated filename: $fileName")
            
            // Try custom folder first if provided
            if (customFolderUri != null) {
                android.util.Log.d("FileUtils", "Attempting to save to custom folder: $customFolderUri")
                val result = saveToCustomFolder(context, sourceUri, customFolderUri, fileName)
                if (result != null) {
                    android.util.Log.i("FileUtils", "Successfully saved to custom folder")
                    return result
                }
                // If custom folder fails, fall through to default folder
                android.util.Log.w("FileUtils", "Failed to save to custom folder, falling back to default")
            }
            
            // Use default app folder
            val categoryFolder = getCategoryFolder(context, receipt.category)
            android.util.Log.d("FileUtils", "Using default category folder: ${categoryFolder.absolutePath}")
            val destFile = File(categoryFolder, fileName)
            
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                android.util.Log.e("FileUtils", "Failed to open input stream for source URI")
                return Pair(null, null)
            }
            
            inputStream.use { input ->
                destFile.outputStream().use { output ->
                    val bytesCopied = input.copyTo(output)
                    if (bytesCopied == 0L) {
                        android.util.Log.w("FileUtils", "No bytes copied from source")
                    } else {
                        android.util.Log.i("FileUtils", "Successfully saved $bytesCopied bytes to: ${destFile.absolutePath}")
                    }
                }
            }
            
            Pair(destFile.absolutePath, fileName)
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "Error saving receipt image for category ${receipt.category}", e)
            e.printStackTrace()
            Pair(null, null)
        }
    }
    
    /**
     * Saves a file to a custom folder using Storage Access Framework (DocumentFile API).
     * 
     * @param context Application context
     * @param sourceUri URI of the source file to save
     * @param customFolderUri URI of the custom folder (from OpenDocumentTree)
     * @param fileName Name for the destination file
     * @return Pair of (document URI string, file name) or null on error
     */
    private fun saveToCustomFolder(
        context: Context,
        sourceUri: Uri,
        customFolderUri: Uri,
        fileName: String
    ): Pair<String?, String?>? {
        return try {
            // Get DocumentFile for the custom folder
            val folderDoc = DocumentFile.fromTreeUri(context, customFolderUri)
            if (folderDoc == null || !folderDoc.exists() || !folderDoc.isDirectory) {
                android.util.Log.e("FileUtils", "Custom folder is not accessible")
                return null
            }
            
            // Get MIME type from source
            val mimeType = context.contentResolver.getType(sourceUri) ?: "image/*"
            
            // Check if file already exists and delete it
            val existingFile = folderDoc.findFile(fileName)
            if (existingFile != null && !existingFile.delete()) {
                android.util.Log.w("FileUtils", "Failed to delete existing file: $fileName")
                // Continue anyway - createFile will handle collision by appending number
            }
            
            // Create new file in the custom folder
            val newFile = folderDoc.createFile(mimeType, fileName)
            if (newFile == null) {
                android.util.Log.e("FileUtils", "Failed to create file in custom folder")
                return null
            }
            
            // Copy file content
            val inputStream = context.contentResolver.openInputStream(sourceUri)
            if (inputStream == null) {
                android.util.Log.e("FileUtils", "Failed to open input stream for source")
                newFile.delete()  // Clean up the created file
                return null
            }
            
            inputStream.use { input ->
                val outputStream = context.contentResolver.openOutputStream(newFile.uri)
                if (outputStream == null) {
                    android.util.Log.e("FileUtils", "Failed to open output stream for destination")
                    newFile.delete()  // Clean up the created file
                    return null
                }
                
                outputStream.use { output ->
                    val bytesCopied = input.copyTo(output)
                    if (bytesCopied == 0L) {
                        android.util.Log.w("FileUtils", "No bytes copied to custom folder")
                    }
                }
            }
            
            Pair(newFile.uri.toString(), fileName)
        } catch (e: SecurityException) {
            android.util.Log.e("FileUtils", "Security exception accessing custom folder", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("FileUtils", "Error saving to custom folder", e)
            null
        }
    }
}
