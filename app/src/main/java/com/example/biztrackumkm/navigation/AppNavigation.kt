package com.example.biztrackumkm.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.biztrackumkm.auth.AuthScreenState
import com.example.biztrackumkm.auth.AuthViewModel
import com.example.biztrackumkm.auth.LoginScreen
import com.example.biztrackumkm.auth.RegisterScreen
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel

@Composable
internal fun AppNavigation(
    authViewModel: AuthViewModel,
    inventoryViewModel: InventoryViewModel
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

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
            InventoryWithLogout(
                inventoryViewModel = inventoryViewModel,
                onLogout = authViewModel::logout
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InventoryWithLogout(
    inventoryViewModel: InventoryViewModel,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("BizTrack UMKM")
                },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            InventoryScreen(inventoryViewModel)
        }
    }
}
