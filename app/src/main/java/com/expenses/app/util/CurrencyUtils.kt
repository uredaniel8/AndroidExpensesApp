package com.expenses.app.util

import java.util.*

object CurrencyUtils {
    /**
     * Gets the default currency based on the device locale.
     * Falls back to USD if the locale currency cannot be determined.
     */
    fun getDefaultCurrency(): String {
        return try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: IllegalArgumentException) {
            "USD" // Fallback if locale doesn't have a currency
        }
    }
    
    /**
     * Formats currency display with proper symbols
     */
    fun formatCurrency(amount: Double, currency: String): String {
        val symbol = when (currency) {
            "GBP" -> "£"
            "USD" -> "$"
            "EUR" -> "€"
            "CHF" -> "CHF"
            else -> currency
        }
        return "$symbol${String.format("%.2f", amount)}"
    }
    
    /**
     * Gets the currency symbol for display
     */
    fun getCurrencySymbol(currency: String): String {
        return when (currency) {
            "GBP" -> "£"
            "USD" -> "$"
            "EUR" -> "€"
            "CHF" -> "CHF"
            else -> currency
        }
    }
}
