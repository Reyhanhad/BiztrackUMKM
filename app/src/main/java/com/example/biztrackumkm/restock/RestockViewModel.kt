package com.example.biztrackumkm.restock

import com.example.biztrackumkm.inventory.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal enum class RestockPriority {
    HIGH,
    MEDIUM,
    LOW
}

internal fun Product.restockRatio(): Double {
    return if (minimumStock > 0) {
        stock.toDouble() / minimumStock.toDouble()
    } else {
        if (stock == 0) 0.0 else Double.POSITIVE_INFINITY
    }
}

internal fun Product.restockPriority(): RestockPriority {
    return when {
        stock == 0 -> RestockPriority.HIGH
        restockRatio() <= 0.5 -> RestockPriority.MEDIUM
        else -> RestockPriority.LOW
    }
}

internal class RestockViewModel(
    products: Flow<List<Product>>
) {

    val lowStockProducts: Flow<List<Product>> =
        products.map { productList ->
            productList
                .filter { product ->
                    product.stock <= product.minimumStock
                }
                .sortedWith(
                    compareBy<Product> {
                        it.restockPriority().ordinal
                    }.thenBy {
                        it.restockRatio()
                    }.thenBy {
                        it.stock
                    }.thenBy {
                        it.name
                    }
                )
        }
}
