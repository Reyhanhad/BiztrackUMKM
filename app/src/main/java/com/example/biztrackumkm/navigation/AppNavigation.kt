package com.example.biztrackumkm.navigation

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
import com.example.biztrackumkm.auth.RegisterScreen
import com.example.biztrackumkm.cashier.CashierScreen
import com.example.biztrackumkm.cashier.CheckoutViewModel
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.restock.RestockScreen
import com.example.biztrackumkm.restock.RestockViewModel

@Composable
internal fun AppNavigation(
    authViewModel: AuthViewModel,
    inventoryViewModel: InventoryViewModel,
    checkoutViewModel: CheckoutViewModel,
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val restockViewModel = remember(inventoryViewModel) {
        RestockViewModel(inventoryViewModel.products)
    }

    when (uiState.screen) {

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

        AuthScreenState.Authenticated -> {

            var currentScreen by rememberSaveable {
                mutableStateOf("inventory")
            }

            when (currentScreen) {

                "cashier" -> {
                    CashierScreen(
                        inventoryViewModel.products,
                        checkoutViewModel,
                        onBack = {
                            currentScreen = "inventory"
                        }
                    )
                }

                "restock" -> {
                    RestockScreen(
                        viewModel = restockViewModel,
                        onBack = {
                            currentScreen = "inventory"
                        }
                    )
                }

                else -> {
                    InventoryScreen(
                        viewModel = inventoryViewModel,
                        onOpenCashier = {
                            currentScreen = "cashier"
                        },
                        onOpenRestock = {
                            currentScreen = "restock"
                        },
                        onLogout = {
                            currentScreen = "inventory"
                            authViewModel.logout()
                        }
                    )
                }
            }
        }
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
