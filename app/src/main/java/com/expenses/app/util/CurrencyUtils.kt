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
}
