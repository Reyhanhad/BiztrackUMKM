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

    fun calculateRecommendation(
        product: Product,
        totalUnitsSold: Int,
        periodDays: Int,
        leadTimeDays: Int,
        safetyStock: Int,
        hasEnoughHistory: Boolean = true
    ): RestockRecommendationResult {
        return RestockCalculator.calculate(
            currentStock = product.stock,
            totalUnitsSold = totalUnitsSold,
            periodDays = periodDays,
            leadTimeDays = leadTimeDays,
            safetyStock = safetyStock,
            hasEnoughHistory = hasEnoughHistory
        )
    }
}
