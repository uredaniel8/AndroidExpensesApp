package com.expenses.app.util

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrProcessor {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun processImage(imageUri: Uri, context: Context): OcrResult {
        return try {
            // ✅ Correct signature: fromFilePath(Context, Uri)
            val image = InputImage.fromFilePath(context, imageUri)

            // ✅ No kotlinx-coroutines-play-services needed
            val visionText = recognizer.process(image).awaitResult()

            val rawText = visionText.text
            val blocks = visionText.textBlocks

            val merchant = extractMerchant(blocks.firstOrNull()?.text)
            val date = extractDate(rawText)
            val totalAmount = extractTotal(rawText)
            val vatAmount = extractVat(rawText)
            val currency = extractCurrency(rawText)

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
            OcrResult(
                merchant = null,
                date = null,
                totalAmount = null,
                vatAmount = null,
                currency = CurrencyUtils.getDefaultCurrency(),
                rawText = "",
                confidence = 0f
            )
        }
    }

    private fun extractMerchant(firstLine: String?): String? =
        firstLine?.trim()?.takeIf { it.isNotEmpty() }

    private fun extractDate(text: String): Long? {
        val datePatterns = listOf(
            "\\d{4}-\\d{2}-\\d{2}",        // YYYY-MM-DD
            "\\d{2}/\\d{2}/\\d{4}",        // DD/MM/YYYY
            "\\d{2}-\\d{2}-\\d{4}",        // DD-MM-YYYY
            "\\d{2}\\.\\d{2}\\.\\d{4}"     // DD.MM.YYYY
        )

        for (pattern in datePatterns) {
            val match = Regex(pattern).find(text) ?: continue
            return parseDate(match.value, pattern)
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
        } catch (_: Exception) {
            null
        }
    }

    private fun extractTotal(text: String): Double? {
        val patterns = listOf(
            "(?i)total[:\\s]*([\\d.,]+)",
            "(?i)amount[:\\s]*([\\d.,]+)",
            "(?i)sum[:\\s]*([\\d.,]+)",
            "([\\d.,]+)\\s*(?i)(eur|usd|gbp|\\$|€|£)"
        )

        for (pattern in patterns) {
            val match = Regex(pattern).find(text) ?: continue
            val numStr = match.groupValues[1].replace(",", ".")
            return numStr.toDoubleOrNull()
        }

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
            val match = Regex(pattern).find(text) ?: continue
            val numStr = match.groupValues[1].replace(",", ".")
            return numStr.toDoubleOrNull()
        }
        return null
    }

    private fun extractCurrency(text: String): String {
        val candidates = listOf("EUR", "USD", "GBP", "CHF", "€", "$", "£")
        for (c in candidates) {
            if (text.contains(c, ignoreCase = true)) {
                return when (c) {
                    "€" -> "EUR"
                    "$" -> "USD"
                    "£" -> "GBP"
                    else -> c
                }
            }
        }
        // fallback locale currency
        return try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (_: Exception) {
            CurrencyUtils.getDefaultCurrency()
        }
    }

    fun release() {
        recognizer.close()
    }
}

/**
 * Coroutine bridge for Google Tasks without kotlinx-coroutines-play-services.
 */
private suspend fun <T> Task<T>.awaitResult(): T =
    suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result -> cont.resume(result) }
        addOnFailureListener { e -> cont.resumeWithException(e) }
        addOnCanceledListener { cont.cancel() }
    }
