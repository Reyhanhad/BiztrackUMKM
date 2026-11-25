package com.example.biztrackumkm.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.auth.AuthScreenState
import com.example.biztrackumkm.auth.AuthViewModel
import com.example.biztrackumkm.auth.LoginScreen
import com.example.biztrackumkm.auth.RegisterCashierDialog
import com.example.biztrackumkm.auth.RegisterScreen
import com.example.biztrackumkm.auth.UserRole
import com.example.biztrackumkm.cashier.CashierScreen
import com.example.biztrackumkm.cashier.CheckoutViewModel
import com.example.biztrackumkm.cashier.SaleDao
import com.example.biztrackumkm.dashboard.observeDailySales
import com.example.biztrackumkm.dashboard.DashboardScreen
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.restock.RestockScreen
import com.example.biztrackumkm.restock.RestockViewModel
import com.example.biztrackumkm.report.SalesReportScreen

@Composable
internal fun AppNavigation(
    authViewModel: AuthViewModel,
    inventoryViewModel: InventoryViewModel,
    checkoutViewModel: CheckoutViewModel,
    sales: SaleDao,
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    when (val screen = uiState.screen) {

        AuthScreenState.Loading -> {
            LoadingScreen(
                errorMessage = uiState.errorMessage,
                onRetry = authViewModel::retry
            )
        }

        AuthScreenState.Register -> {
            RegisterScreen(
                uiState = uiState,
                onRegister = authViewModel::register,
                onClearError = authViewModel::clearError
            )
        }

        AuthScreenState.Login -> {
            LoginScreen(
                uiState = uiState,
                onLogin = authViewModel::login,
                onClearError = authViewModel::clearError
            )
        }

        is AuthScreenState.Authenticated -> {
            if (screen.role == UserRole.CASHIER) {
                CashierOnlyNavigation(
                    inventory = inventoryViewModel,
                    checkout = checkoutViewModel,
                    onLogout = authViewModel::logout,
                )
            } else {
                var showRegisterCashier by rememberSaveable { mutableStateOf(false) }
                if (showRegisterCashier) {
                    RegisterCashierDialog(
                        uiState = uiState,
                        onRegisterCashier = authViewModel::registerCashier,
                        onDismiss = {
                            showRegisterCashier = false
                            authViewModel.clearCashierMessage()
                        },
                        onClearMessage = authViewModel::clearCashierMessage,
                    )
                }
                OwnerNavigation(
                    inventory = inventoryViewModel,
                    checkout = checkoutViewModel,
                    sales = sales,
                    onAddCashier = { showRegisterCashier = true },
                    onLogout = authViewModel::logout,
                )
            }
        }
    }
}

@Composable
internal fun CashierOnlyNavigation(
    inventory: InventoryViewModel,
    checkout: CheckoutViewModel,
    onLogout: () -> Unit,
) {
    CashierScreen(
        products = inventory.products,
        checkout = checkout,
        onBack = onLogout
    )
}

@Composable
internal fun OwnerNavigation(
    inventory: InventoryViewModel,
    checkout: CheckoutViewModel,
    sales: SaleDao,
    onAddCashier: (() -> Unit)?,
    onLogout: () -> Unit,
) {
    val restock = remember(inventory) { RestockViewModel(inventory.products) }
    var screen by rememberSaveable { mutableStateOf("dashboard") }
    var returnScreen by rememberSaveable { mutableStateOf("dashboard") }

    fun openFromHere(destination: String) {
        returnScreen = screen
        screen = destination
    }

    BackHandler(enabled = screen == "inventory") { screen = "dashboard" }
    when (screen) {
        "dashboard" -> DashboardScreen(
            dailySales = remember(sales) { observeDailySales(sales) },
            onReport = { screen = "report" },
            restock = restock,
            onCashier = { openFromHere("cashier") },
            onInventory = { screen = "inventory" },
            onRestock = { openFromHere("restock") },
            onLogout = onLogout,
            onAddCashier = onAddCashier,
        )
        "report" -> SalesReportScreen(sales, onBack = { screen = "dashboard" })
        "cashier" -> CashierScreen(inventory.products, checkout, onBack = { screen = returnScreen })
        "restock" -> RestockScreen(restock, onBack = { screen = returnScreen })
        "inventory" -> InventoryScreen(
            viewModel = inventory,
            onOpenCashier = { openFromHere("cashier") },
            onOpenRestock = { openFromHere("restock") },
            onLogout = onLogout,
        )
    }
}

@Composable
private fun LoadingScreen(
    errorMessage: String?,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (errorMessage == null) {
            CircularProgressIndicator()
        } else {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )

                Button(onClick = onRetry) {
                    Text("Coba Lagi")
                }
            }
        }
    }
}
