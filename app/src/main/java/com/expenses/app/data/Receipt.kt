package com.expenses.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val receiptDate: Long,
    val merchant: String?,
    val totalAmount: Double,
    val vatAmount: Double?,
    val currency: String,
    val category: String,
    val description: String?,
    val notes: String?,
    val tags: List<String> = emptyList(),
    val ocrRawText: String?,
    val ocrConfidence: Float?,
    val originalUri: String?,
    val storedUri: String?,
    val renamedFileName: String?,
    val exportFolderUri: String?,
    val exportStatus: ExportStatus = ExportStatus.EXPORTED,
    val lastExportAttemptAt: Long? = null
)
