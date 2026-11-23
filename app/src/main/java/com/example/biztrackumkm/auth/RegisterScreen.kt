package com.example.biztrackumkm.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    uiState: AuthUiState,
    onRegister: (
        username: String,
        password: String,
        confirmPassword: String
    ) -> Unit,
    onClearError: () -> Unit
) {

    var username by rememberSaveable {
        mutableStateOf("")
    }

    var password by rememberSaveable {
        mutableStateOf("")
    }

    var confirmPassword by rememberSaveable {
        mutableStateOf("")
    }

    var showPassword by rememberSaveable {
        mutableStateOf(false)
    }

    var showConfirmPassword by rememberSaveable {
        mutableStateOf(false)
    }

    val isProcessing = uiState.isProcessing

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {

                Text(
                    text = "BizTrack UMKM",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Daftarkan Akun Pemilik",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "Buat akun pemilik pertama untuk mengakses dan mengelola inventaris usaha Anda.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(28.dp)
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        onClearError()
                    },
                    label = {
                        Text("Username")
                    },
                    placeholder = {
                        Text("Minimal 3 karakter")
                    },
                    singleLine = true,
                    enabled = !isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onClearError()
                    },
                    label = {
                        Text("Password")
                    },
                    placeholder = {
                        Text("Minimal 8 karakter")
                    },
                    singleLine = true,
                    enabled = !isProcessing,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showPassword = !showPassword
                            }
                        ) {
                            Text(
                                if (showPassword) "Sembunyikan"
                                else "Lihat"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        onClearError()
                    },
                    label = {
                        Text("Konfirmasi Password")
                    },
                    singleLine = true,
                    enabled = !isProcessing,
                    visualTransformation = if (showConfirmPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                showConfirmPassword =
                                    !showConfirmPassword
                            }
                        ) {
                            Text(
                                if (showConfirmPassword) {
                                    "Sembunyikan"
                                } else {
                                    "Lihat"
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.errorMessage != null) {

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(
                    modifier = Modifier.height(24.dp)
                )

                Button(
                    onClick = {
                        onRegister(
                            username.trim(),
                            password,
                            confirmPassword
                        )
                    },
                    enabled = !isProcessing &&
                            username.isNotBlank() &&
                            password.isNotBlank() &&
                            confirmPassword.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {

                    if (isProcessing) {

                        CircularProgressIndicator(
                            modifier = Modifier.height(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )

                    } else {

                        Text(
                            text = "Daftar Akun Pemilik"
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Text(
                    text = "Registrasi hanya tersedia untuk pemilik pertama. Setelah akun dibuat, gunakan halaman login untuk masuk.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
