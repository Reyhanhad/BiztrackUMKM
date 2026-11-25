package com.example.biztrackumkm.inventory

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class ProductForm(
    val name: String = "",
    val price: String = "",
    val stock: String = "",
    val minimumStock: String = "",
    val barcode: String = "",
    val problem: ProductProblem? = null,
    val saving: Boolean = false,
    val productId: Long? = null,
)

internal enum class ProductProblem { Name, Price, Stock, MinimumStock, DuplicateBarcode, NotFound, Storage }

internal enum class BarcodeLookupResult { Opened, Archived, Ignored }

internal class InventoryViewModel(private val dao: ProductDao) : ViewModel() {
    val products = dao.observeProducts()
    val archivedProducts = dao.observeArchivedProducts()

    suspend fun restoreArchivedProduct(id: Long) = dao.restoreArchivedProduct(id)

    private val _form = MutableStateFlow<ProductForm?>(null)
    val form = _form.asStateFlow()

    fun openForm() {
        if (_form.value == null) _form.value = ProductForm()
    }

    fun openEditForm(product: Product) {
        if (_form.value != null || product.archived) return
        _form.value = ProductForm(
            name = product.name,
            price = product.priceRupiah.toString(),
            stock = product.stock.toString(),
            minimumStock = product.minimumStock.toString(),
            barcode = product.barcode.orEmpty(),
            productId = product.id,
        )
    }

    suspend fun openBarcode(barcode: String): BarcodeLookupResult {
        val code = barcode.trim()
        if (code.isEmpty() || _form.value != null) return BarcodeLookupResult.Ignored
        val product = dao.findByBarcode(code)
        if (_form.value != null) return BarcodeLookupResult.Ignored
        if (product?.archived == true) return BarcodeLookupResult.Archived
        if (product != null) openEditForm(product) else _form.value = ProductForm(barcode = code)
        return BarcodeLookupResult.Opened
    }

    fun closeForm() {
        if (_form.value?.saving != true) _form.value = null
    }

    fun changeForm(change: ProductForm.() -> ProductForm) {
        val current = _form.value ?: return
        if (!current.saving) _form.value = current.change().copy(problem = null)
    }

    fun archiveProduct() {
        val current = _form.value ?: return
        val id = current.productId ?: return
        if (current.saving) return
        _form.value = current.copy(problem = null, saving = true)
        viewModelScope.launch {
            val problem = try {
                if (dao.archive(id) == 0) ProductProblem.NotFound else null
            } catch (error: SQLiteException) {
                Log.e("Inventory", "Gagal mengarsipkan produk", error)
                ProductProblem.Storage
            }
            _form.value = problem?.let { current.copy(problem = it) }
        }
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
            name = name,
            priceRupiah = checkNotNull(price),
            stock = checkNotNull(stock),
            minimumStock = checkNotNull(minimumStock),
            barcode = current.barcode.trim().ifEmpty { null },
            id = current.productId ?: 0,
        )
        _form.value = current.copy(problem = null, saving = true)
        viewModelScope.launch {
            val saveProblem = try {
                if (current.productId == null) {
                    if (dao.insert(product) == -1L) ProductProblem.DuplicateBarcode else null
                } else {
                    if (dao.update(product) == 0) ProductProblem.NotFound else null
                }
            } catch (_: SQLiteConstraintException) {
                ProductProblem.DuplicateBarcode
            } catch (error: SQLiteException) {
                Log.e("Inventory", "Gagal menyimpan produk", error)
                ProductProblem.Storage
            }
            _form.value = saveProblem?.let { current.copy(problem = it) }
        }
    }
}
