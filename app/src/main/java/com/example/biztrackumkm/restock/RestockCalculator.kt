package com.example.biztrackumkm.restock

import kotlin.math.ceil

internal sealed interface RestockRecommendationResult {
    data class Success(
        val recommendedQuantity: Int,
        val requiredQuantity: Int,
        val averageDailySales: Double
    ) : RestockRecommendationResult

    data class InsufficientHistory(
        val reason: String = "Riwayat transaksi belum mencukupi."
    ) : RestockRecommendationResult
}
internal object RestockCalculator {

    fun calculate(
        currentStock: Int,
        totalUnitsSold: Int,
        periodDays: Int,
        leadTimeDays: Int,
        safetyStock: Int,
        hasEnoughHistory: Boolean = true
    ): RestockRecommendationResult {

        if (!hasEnoughHistory || periodDays <= 0) {
            return RestockRecommendationResult.InsufficientHistory(
                reason = "Riwayat transaksi belum mencukupi untuk menghitung rekomendasi."
            )
        }

        val averageDailySales = totalUnitsSold.toDouble() / periodDays.toDouble()
        val rawRequired = (averageDailySales * leadTimeDays) + safetyStock
        val requiredQuantity = ceil(rawRequired).toInt()
        val recommendedQuantity = maxOf(0, requiredQuantity - currentStock)

        return RestockRecommendationResult.Success(
            recommendedQuantity = recommendedQuantity,
            requiredQuantity = requiredQuantity,
            averageDailySales = averageDailySales
        )
    }
}
