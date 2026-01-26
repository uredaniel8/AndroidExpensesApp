package com.expenses.app.ui

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AddReceipt : Screen("add_receipt")
    object EditReceipt : Screen("edit_receipt/{receiptId}") {
        fun createRoute(receiptId: String) = "edit_receipt/$receiptId"
    }
    object Reports : Screen("reports")
}
