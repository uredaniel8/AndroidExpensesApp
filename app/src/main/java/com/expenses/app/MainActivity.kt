package com.expenses.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.expenses.app.ui.ReceiptViewModel
import com.expenses.app.ui.Screen
import com.expenses.app.ui.screens.*
import com.expenses.app.ui.theme.AndroidExpensesAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidExpensesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ExpensesApp()
                }
            }
        }
    }
}

@Composable
fun ExpensesApp() {
    val navController = rememberNavController()
    val viewModel: ReceiptViewModel = viewModel()
    val receipts by viewModel.receipts.collectAsState()
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                receipts = receipts,
                onAddReceipt = {
                    navController.navigate(Screen.AddReceipt.route)
                },
                onUploadReceipt = {
                    navController.navigate(Screen.AddReceipt.route)
                },
                onReceiptClick = { receiptId ->
                    navController.navigate(Screen.EditReceipt.createRoute(receiptId))
                },
                onViewReports = {
                    navController.navigate(Screen.Reports.route)
                }
            )
        }

        composable(Screen.AddReceipt.route) {
            AddReceiptScreen(
                onBack = { navController.popBackStack() },
                onReceiptCaptured = { uri ->
                    viewModel.processReceipt(uri)
                }
            )
        }

        composable(
            route = Screen.EditReceipt.route,
            arguments = listOf(navArgument("receiptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val receiptId = backStackEntry.arguments?.getString("receiptId")
            val receipt = receipts.firstOrNull { it.id == receiptId }
            
            EditReceiptScreen(
                receipt = receipt,
                onBack = { navController.popBackStack() },
                onSave = { updatedReceipt ->
                    scope.launch {
                        viewModel.updateReceipt(updatedReceipt)
                    }
                }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                receiptsFlow = viewModel.getReceiptsByDateRange(
                    startDate = getStartOfMonth(),
                    endDate = System.currentTimeMillis()
                ),
                onBack = { navController.popBackStack() }
            )
        }
    }
}
