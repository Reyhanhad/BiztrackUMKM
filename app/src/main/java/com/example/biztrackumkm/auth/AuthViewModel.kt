package com.example.biztrackumkm.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AuthScreenState {

    data object Loading : AuthScreenState

    data object Register : AuthScreenState

    data object Login : AuthScreenState

    data object Authenticated : AuthScreenState
}

data class AuthUiState(
    val screen: AuthScreenState = AuthScreenState.Loading,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = AuthRepository(
        AuthDatabase.getDatabase(application).ownerDao()
    )

    private val preferences =
        application.getSharedPreferences(
            "biztrack_owner_session",
            Context.MODE_PRIVATE
        )

    private val _uiState = MutableStateFlow(AuthUiState())

    val uiState: StateFlow<AuthUiState> =
        _uiState.asStateFlow()

    init {
        checkSession()
    }

    // PERIKSA AKUN DAN SESI
    private fun checkSession() {

        viewModelScope.launch {

            try {
                val hasOwner = withContext(Dispatchers.IO) {
                    repository.hasOwner()
                }

                val loggedIn = preferences.getBoolean(
                    "is_logged_in",
                    false
                )

                val destination = when {
                    !hasOwner -> AuthScreenState.Register
                    loggedIn -> AuthScreenState.Authenticated
                    else -> AuthScreenState.Login
                }

                _uiState.value = AuthUiState(
                    screen = destination
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                _uiState.value = AuthUiState(
                    screen = AuthScreenState.Loading,
                    errorMessage = e.message
                        ?: "Gagal memuat data akun."
                )
            }
        }
    }

    // REGISTRASI PEMILIK
    fun register(
        username: String,
        password: String,
        confirmPassword: String
    ) {

        if (_uiState.value.isProcessing) return

        if (_uiState.value.screen != AuthScreenState.Register) {
            return
        }

        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Konfirmasi password tidak sama."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            errorMessage = null
        )

        viewModelScope.launch {

            try {

                val result = withContext(Dispatchers.IO) {
                    repository.registerOwner(
                        username = username,
                        password = password
                    )
                }

                result.fold(
                    onSuccess = {
                        preferences.edit()
                            .putBoolean("is_logged_in", false)
                            .apply()

                        _uiState.value = AuthUiState(
                            screen = AuthScreenState.Login
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = error.message
                                ?: "Registrasi gagal."
                        )
                    }
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message
                        ?: "Terjadi kesalahan saat registrasi."
                )
            }
        }
    }

    // LOGIN PEMILIK
    fun login(
        username: String,
        password: String
    ) {

        if (_uiState.value.isProcessing) return

        if (_uiState.value.screen != AuthScreenState.Login) {
            return
        }

        _uiState.value = _uiState.value.copy(
            isProcessing = true,
            errorMessage = null
        )

        viewModelScope.launch {

            try {

                val result = withContext(Dispatchers.IO) {
                    repository.login(
                        username = username,
                        password = password
                    )
                }

                result.fold(
                    onSuccess = {

                        val saved = preferences.edit()
                            .putBoolean("is_logged_in", true)
                            .commit()

                        if (saved) {
                            _uiState.value = AuthUiState(
                                screen = AuthScreenState.Authenticated
                            )
                        } else {
                            _uiState.value = AuthUiState(
                                screen = AuthScreenState.Login,
                                errorMessage = "Gagal menyimpan sesi login."
                            )
                        }
                    },
                    onFailure = { error ->

                        _uiState.value = _uiState.value.copy(
                            isProcessing = false,
                            errorMessage = error.message
                                ?: "Login gagal."
                        )
                    }
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    errorMessage = e.message
                        ?: "Terjadi kesalahan saat login."
                )
            }
        }
    }

    // LOGOUT PEMILIK
    fun logout() {

        if (_uiState.value.screen != AuthScreenState.Authenticated) {
            return
        }

        val saved = preferences.edit()
            .putBoolean("is_logged_in", false)
            .commit()

        if (saved) {
            _uiState.value = AuthUiState(
                screen = AuthScreenState.Login
            )
        } else {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Gagal menghapus sesi login."
            )
        }
    }

    // HAPUS PESAN ERROR
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null
        )
    }

    // COBA ULANG PEMERIKSAAN SESI
    fun retry() {
        if (!_uiState.value.isProcessing) {
            _uiState.value = AuthUiState(
                screen = AuthScreenState.Loading
            )
            checkSession()
        }
    }
}
