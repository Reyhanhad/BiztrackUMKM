package com.example.biztrackumkm.inventory

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class NewProductForm(
    val name: String = "",
    val price: String = "",
    val stock: String = "",
    val minimumStock: String = "",
    val barcode: String = "",
    val problem: ProductProblem? = null,
    val saving: Boolean = false,
)

internal enum class ProductProblem { Name, Price, Stock, MinimumStock, DuplicateBarcode, Storage }

internal class InventoryViewModel(private val dao: ProductDao) : ViewModel() {
    val products = dao.observeProducts()

    private val _form = MutableStateFlow<NewProductForm?>(null)
    val form = _form.asStateFlow()

    fun openForm() {
        if (_form.value == null) _form.value = NewProductForm()
    }

    fun closeForm() {
        if (_form.value?.saving != true) _form.value = null
    }

    fun changeForm(change: NewProductForm.() -> NewProductForm) {
        val current = _form.value ?: return
        if (!current.saving) _form.value = current.change().copy(problem = null)
    }

    fun saveProduct() {
        val current = _form.value ?: return
        if (current.saving) return
        val name = current.name.trim()
        val price = current.price.trim().toLongOrNull()
        val stock = current.stock.trim().toIntOrNull()
        val minimumStock = current.minimumStock.trim().toIntOrNull()
        val problem = when {
            name.isEmpty() -> ProductProblem.Name
            price == null || price <= 0 -> ProductProblem.Price
            stock == null || stock < 0 -> ProductProblem.Stock
            minimumStock == null || minimumStock < 0 -> ProductProblem.MinimumStock
            else -> null
        }
        if (problem != null) {
            _form.value = current.copy(problem = problem)
            return
        }

        val product = Product(
            name, checkNotNull(price), checkNotNull(stock), checkNotNull(minimumStock),
            current.barcode.trim().ifEmpty { null },
        )
        _form.value = current.copy(problem = null, saving = true)
        viewModelScope.launch {
            _form.value = try {
                if (dao.insert(product) == -1L) current.copy(problem = ProductProblem.DuplicateBarcode) else null
            } catch (error: SQLiteException) {
                Log.e("Inventory", "Gagal menyimpan produk", error)
                current.copy(problem = ProductProblem.Storage)
            }
        }
    }
}
