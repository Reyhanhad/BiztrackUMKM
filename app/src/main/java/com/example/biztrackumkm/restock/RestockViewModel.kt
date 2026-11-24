package com.example.biztrackumkm.restock

import com.example.biztrackumkm.inventory.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RestockViewModel(
    products: Flow<List<Product>>
) {

    val lowStockProducts: Flow<List<Product>> =
        products.map { productList ->
            productList.filter { product ->
                product.stock <= product.minimumStock
            }
        }
}
