package com.example.biztrackumkm.restock

import com.example.biztrackumkm.inventory.Product
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RestockViewModelTest {

    @Test
    fun `calculateRecommendation delegates correctly to calculator`() {
        val viewModel = RestockViewModel(flowOf(emptyList()))
        val product = Product(
            id = 1,
            name = "Minyak Goreng",
            priceRupiah = 15000,
            stock = 5,
            minimumStock = 10,
            barcode = "123456"
        )

        val result = viewModel.calculateRecommendation(
            product = product,
            totalUnitsSold = 21,
            periodDays = 7,
            leadTimeDays = 4,
            safetyStock = 2,
            hasEnoughHistory = true
        )

        assertTrue(result is RestockRecommendationResult.Success)
        val success = result as RestockRecommendationResult.Success
        assertEquals(9, success.recommendedQuantity)
    }
}
