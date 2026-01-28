package com.expenses.app.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileUtils {
    
    fun generateFileName(
        date: Long,
        description: String?,
        amount: Double,
        extension: String
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        val dateStr = dateFormat.format(Date(date))
        val descStr = description?.replace(Regex("[^a-zA-Z0-9\\s]"), "")?.trim()?.replace(Regex("\\s+"), " ") ?: "NoDescription"
        val amountStr = String.format(Locale.US, "%.2f", amount)
        
        return "${dateStr} - ${descStr} - ${amountStr}.${extension}"
    }

    fun getCategoryFolder(context: Context, category: String): File {
        val baseFolder = File(context.getExternalFilesDir(null), "Receipts")
        val folder = if (category.equals("Fuel", ignoreCase = true)) {
            File(baseFolder, "Fuel")
        } else {
            File(baseFolder, "Others")
        }
        if (!folder.exists()) {
            val created = folder.mkdirs()
            if (!created && !folder.exists()) {
                throw IllegalStateException("Failed to create directory: ${folder.absolutePath}")
            }
        }
        return folder
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
}
