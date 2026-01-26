package com.expenses.app.util

data class OcrResult(
    val merchant: String? = null,
    val date: Long? = null,
    val totalAmount: Double? = null,
    val vatAmount: Double? = null,
    val currency: String? = null,
    val rawText: String = "",
    val confidence: Float = 0f
)
