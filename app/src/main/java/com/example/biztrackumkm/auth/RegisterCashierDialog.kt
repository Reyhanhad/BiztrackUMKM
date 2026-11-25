package com.example.biztrackumkm.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterCashierDialog(
    uiState: AuthUiState,
    onRegisterCashier: (username: String, password: String, confirm: String) -> Unit,
    onDismiss: () -> Unit,
    onClearMessage: () -> Unit
) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Tambah Akun Kasir",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Buat akun untuk kasir agar dapat melayani transaksi penjualan.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        onClearMessage()
                    },
                    label = { Text("Username Kasir") },
                    singleLine = true,
                    enabled = !uiState.isRegisteringCashier,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onClearMessage()
                    },
                    label = { Text("Password") },
                    singleLine = true,
                    enabled = !uiState.isRegisteringCashier,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        onClearMessage()
                    },
                    label = { Text("Konfirmasi Password") },
                    singleLine = true,
                    enabled = !uiState.isRegisteringCashier,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )

                uiState.cashierMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = if (msg.contains("berhasil", ignoreCase = true)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onRegisterCashier(username.trim(), password, confirmPassword)
                },
                enabled = !uiState.isRegisteringCashier &&
                        username.isNotBlank() &&
                        password.isNotBlank() &&
                        confirmPassword.isNotBlank()
            ) {
                if (uiState.isRegisteringCashier) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Buat Akun")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !uiState.isRegisteringCashier
            ) {
                Text("Tutup")
            }
        }
    )
}
