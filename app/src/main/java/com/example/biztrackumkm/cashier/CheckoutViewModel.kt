package com.example.biztrackumkm.cashier

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed interface CheckoutState {
    data object Idle : CheckoutState
    data object Saving : CheckoutState
    data class Saved(val total: Number) : CheckoutState
    data class Rejected(val reason: SaleRejection) : CheckoutState
    data object StorageError : CheckoutState
}

internal class CheckoutViewModel(private val sales: SaleDao) : ViewModel() {
    private val _state = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val state = _state.asStateFlow()

    fun save(lines: List<CartLine>) {
        if (lines.isEmpty() || _state.value != CheckoutState.Idle) return
        _state.value = CheckoutState.Saving
        viewModelScope.launch {
            _state.value = try {
                CheckoutState.Saved(sales.saveSale(lines))
            } catch (error: SaleRejected) {
                CheckoutState.Rejected(error.reason)
            } catch (error: SQLiteException) {
                Log.e("Checkout", "Gagal menyimpan penjualan", error)
                CheckoutState.StorageError
            }
        }
    }

    fun dismissResult() {
        if (_state.value != CheckoutState.Saving) _state.value = CheckoutState.Idle
    }
}
