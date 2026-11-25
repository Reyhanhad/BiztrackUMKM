package com.example.biztrackumkm.restock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestockCalculatorTest {

    @Test
    fun `calculate recommendation matches spec example`() {
        val result = RestockCalculator.calculate(
            currentStock = 5,
            totalUnitsSold = 21,
            periodDays = 7,
            leadTimeDays = 4,
            safetyStock = 2,
            hasEnoughHistory = true
        )

        assertTrue(result is RestockRecommendationResult.Success)
        val success = result as RestockRecommendationResult.Success
        assertEquals(9, success.recommendedQuantity)
        assertEquals(14, success.requiredQuantity)
        assertEquals(3.0, success.averageDailySales, 0.001)
    }

    @Test
    fun `returns zero recommendation when current stock covers required amount`() {
        val result = RestockCalculator.calculate(
            currentStock = 15,
            totalUnitsSold = 21,
            periodDays = 7,
            leadTimeDays = 4,
            safetyStock = 2,
            hasEnoughHistory = true
        )

        assertTrue(result is RestockRecommendationResult.Success)
        val success = result as RestockRecommendationResult.Success
        assertEquals(0, success.recommendedQuantity)
    }

    @Test
    fun `returns InsufficientHistory when history is not enough`() {
        val result = RestockCalculator.calculate(
            currentStock = 5,
            totalUnitsSold = 0,
            periodDays = 0,
            leadTimeDays = 4,
            safetyStock = 2,
            hasEnoughHistory = false
        )

        assertTrue(result is RestockRecommendationResult.InsufficientHistory)
    }

    @Test
    fun `rounds up fractional required quantity correctly`() {
        val result = RestockCalculator.calculate(
            currentStock = 2,
            totalUnitsSold = 5,
            periodDays = 2,
            leadTimeDays = 3,
            safetyStock = 1,
            hasEnoughHistory = true
        )

        assertTrue(result is RestockRecommendationResult.Success)
        val success = result as RestockRecommendationResult.Success
        assertEquals(7, success.recommendedQuantity)
        assertEquals(9, success.requiredQuantity)
    }
}
