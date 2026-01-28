package com.expenses.app.util

import android.content.Context
import android.net.Uri
import com.expenses.app.data.Receipt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    // Store custom folder URIs
    @Volatile
    private var customFuelFolderUri: Uri? = null
    
    @Volatile
    private var customOtherFolderUri: Uri? = null
    
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
            File(baseFolder, "Fuel")
        } else {
            File(baseFolder, "Other")
        }
        if (!categoryFolder.exists()) {
            categoryFolder.mkdirs()
        }
        return categoryFolder
    }

    fun saveReceiptImage(
        context: Context,
        sourceUri: Uri,
        receipt: Receipt
    ): Pair<String?, String?> {
        return try {
            val extension = getFileExtension(sourceUri, context)
            val fileName = generateFileName(
                date = receipt.receiptDate,
                merchant = receipt.merchant,
                category = receipt.category,
                total = receipt.totalAmount,
                extension = extension,
                description = receipt.description
            )
            
            val categoryFolder = getCategoryFolder(context, receipt.category)
            val destFile = File(categoryFolder, fileName)
            
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Pair(destFile.absolutePath, fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }
}
