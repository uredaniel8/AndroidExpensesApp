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
    val categories by viewModel.categories.collectAsState()
    val fuelFolderUri by viewModel.fuelFolderUri.collectAsState()
    val otherFolderUri by viewModel.otherFolderUri.collectAsState()
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
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.AddReceipt.route) {
            AddReceiptScreen(
                onBack = { navController.popBackStack() },
                onReceiptCaptured = { uri ->
                    scope.launch {
                        viewModel.processReceipt(uri)
                        // Wait a bit for OCR processing, then navigate to edit the receipt
                        kotlinx.coroutines.delay(1000)
                        val latestReceipt = receipts.firstOrNull()
                        latestReceipt?.let { 
                            navController.navigate(Screen.EditReceipt.createRoute(it.id)) {
                                popUpTo(Screen.Home.route)
                            }
                        }
                    }
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
                categories = categories.map { it.name },
                onBack = { navController.popBackStack() },
                onSave = { updatedReceipt ->
                    scope.launch {
                        viewModel.updateReceipt(updatedReceipt)
                    }
                },
                onDelete = { receiptToDelete ->
                    scope.launch {
                        viewModel.deleteReceipt(receiptToDelete)
                    }
                },
                onUploadToProtonDrive = { receiptToUpload ->
                    viewModel.uploadToProtonDrive(receiptToUpload)
                },
                onAddCategory = { categoryName ->
                    viewModel.addCategory(categoryName)
                },
                onDeleteCategory = { categoryName ->
                    val category = categories.firstOrNull { it.name == categoryName }
                    category?.let { viewModel.deleteCategory(it) }
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
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onConfigureProtonDrive = { accessToken, enabled ->
                    viewModel.configureProtonDrive(accessToken, enabled)
                },
                onFuelFolderSelected = { uri ->
                    viewModel.setFuelFolder(uri)
                },
                onOtherFolderSelected = { uri ->
                    viewModel.setOtherFolder(uri)
                },
                fuelFolderUri = fuelFolderUri,
                otherFolderUri = otherFolderUri
            )
        }
    }
}
