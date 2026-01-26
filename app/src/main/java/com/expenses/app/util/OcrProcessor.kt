package com.expenses.app.util

import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(imageUri: Uri, contentResolver: android.content.ContentResolver): OcrResult {
        return try {
            val image = InputImage.fromFilePath(contentResolver, imageUri)
            val visionText = recognizer.process(image).await()
            
            val rawText = visionText.text
            val blocks = visionText.textBlocks
            
            // Extract information from text
            val merchant = extractMerchant(blocks.firstOrNull()?.text)
            val date = extractDate(rawText)
            val totalAmount = extractTotal(rawText)
            val vatAmount = extractVat(rawText)
            val currency = extractCurrency(rawText)
            
            // Calculate average confidence
            val confidence = if (blocks.isNotEmpty()) {
                blocks.flatMap { it.lines }
                    .flatMap { it.elements }
                    .map { it.confidence ?: 0f }
                    .average()
                    .toFloat()
            } else 0f

            OcrResult(
                merchant = merchant,
                date = date,
                totalAmount = totalAmount,
                vatAmount = vatAmount,
                currency = currency,
                rawText = rawText,
                confidence = confidence
            )
        } catch (e: Exception) {
            OcrResult(rawText = "", confidence = 0f)
        }
    }

    private fun extractMerchant(firstLine: String?): String? {
        return firstLine?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractDate(text: String): Long? {
        // Look for common date patterns
        val datePatterns = listOf(
            "\\d{4}-\\d{2}-\\d{2}",  // YYYY-MM-DD
            "\\d{2}/\\d{2}/\\d{4}",  // DD/MM/YYYY
            "\\d{2}-\\d{2}-\\d{4}",  // DD-MM-YYYY
            "\\d{2}\\.\\d{2}\\.\\d{4}" // DD.MM.YYYY
        )

        for (pattern in datePatterns) {
            val regex = Regex(pattern)
            val match = regex.find(text)
            if (match != null) {
                return parseDate(match.value, pattern)
            }
        }
        return null
    }

    private fun parseDate(dateStr: String, pattern: String): Long? {
        val formats = mapOf(
            "\\d{4}-\\d{2}-\\d{2}" to "yyyy-MM-dd",
            "\\d{2}/\\d{2}/\\d{4}" to "dd/MM/yyyy",
            "\\d{2}-\\d{2}-\\d{4}" to "dd-MM-yyyy",
            "\\d{2}\\.\\d{2}\\.\\d{4}" to "dd.MM.yyyy"
        )
        
        val format = formats[pattern] ?: return null
        return try {
            SimpleDateFormat(format, Locale.getDefault()).parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    private fun extractTotal(text: String): Double? {
        // Look for "Total", "TOTAL", "Amount", etc. followed by numbers
        val patterns = listOf(
            "(?i)total[:\\s]*([\\d.,]+)",
            "(?i)amount[:\\s]*([\\d.,]+)",
            "(?i)sum[:\\s]*([\\d.,]+)",
            "([\\d.,]+)\\s*(?i)(eur|usd|gbp|\\$|€|£)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(",", ".")
                return numStr.toDoubleOrNull()
            }
        }
        
        // Fallback: find largest number
        val numbers = Regex("[\\d.,]+").findAll(text)
            .mapNotNull { it.value.replace(",", ".").toDoubleOrNull() }
            .filter { it > 0 }
            .toList()
        
        return numbers.maxOrNull()
    }

    private fun extractVat(text: String): Double? {
        val patterns = listOf(
            "(?i)vat[:\\s]*([\\d.,]+)",
            "(?i)tax[:\\s]*([\\d.,]+)",
            "(?i)mwst[:\\s]*([\\d.,]+)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern)
            val match = regex.find(text)
            if (match != null) {
                val numStr = match.groupValues[1].replace(",", ".")
                return numStr.toDoubleOrNull()
            }
        }
        return null
    }

    private fun extractCurrency(text: String): String? {
        val currencies = listOf("EUR", "USD", "GBP", "CHF", "€", "$", "£")
        for (currency in currencies) {
            if (text.contains(currency, ignoreCase = true)) {
                return when (currency) {
                    "€" -> "EUR"
                    "$" -> "USD"
                    "£" -> "GBP"
                    else -> currency
                }
            }
        }
        // Default currency based on device locale
        return try {
            java.util.Currency.getInstance(java.util.Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "USD" // Fallback if locale currency cannot be determined
        }
    }

    fun release() {
        recognizer.close()
    }
}
