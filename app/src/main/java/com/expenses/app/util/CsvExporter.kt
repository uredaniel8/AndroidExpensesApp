package com.expenses.app.util

import com.expenses.app.data.Receipt
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    
    fun exportToCsv(receipts: List<Receipt>, outputFile: File): Boolean {
        return try {
            outputFile.bufferedWriter().use { writer ->
                // Write header
                writer.write("Date,Merchant,Category,Total Amount,VAT Amount,Currency,Notes,Tags,Export Status\n")
                
                // Write data
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                receipts.forEach { receipt ->
                    val date = dateFormat.format(Date(receipt.receiptDate))
                    val merchant = receipt.merchant ?: ""
                    val category = receipt.category
                    val total = receipt.totalAmount
                    val vat = receipt.vatAmount ?: 0.0
                    val currency = receipt.currency
                    val notes = receipt.notes?.replace("\"", "\"\"") ?: ""
                    val tags = receipt.tags.joinToString(";")
                    val status = receipt.exportStatus.name
                    
                    writer.write("$date,\"$merchant\",$category,$total,$vat,$currency,\"$notes\",\"$tags\",$status\n")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
