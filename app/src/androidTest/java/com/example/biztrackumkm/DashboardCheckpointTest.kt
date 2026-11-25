package com.example.biztrackumkm

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biztrackumkm.cashier.CartLine
import com.example.biztrackumkm.cashier.CheckoutViewModel
import com.example.biztrackumkm.dashboard.salesForDay
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.Product
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.navigation.OwnerNavigation
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardCheckpointTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: ProductDatabase

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(compose.activity, ProductDatabase::class.java).build()
    }

    @After fun close() = db.close()

    private fun seedSale(time: Long, total: String) {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO sales(soldAtMillis, totalRupiah) VALUES (?, ?)", arrayOf<Any>(time, total),
        )
    }

    private fun shown(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(text).assertIsDisplayed()
    }

    @Test fun dashboardMatchesLocalDayAndRespondsToSalesAndStockChanges() {
        val zone = ZoneId.systemDefault()
        val date = LocalDate.now(zone)
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        seedSale(start - 1, "99999")
        seedSale(end, "77777")
        val inventory = InventoryViewModel(db.products())
        val checkout = CheckoutViewModel(db.sales())
        val product = Product("Gula", 14000, 3, 2, null)
        val stored = runBlocking { product.copy(id = db.products().insert(product)) }
        compose.setContent {
            BiztrackUMKMTheme {
                OwnerNavigation(inventory, checkout, db.sales(), onAddCashier = null, onLogout = {})
            }
        }
        shown("Rp 0")
        shown("0")
        shown("0 produk")
        compose.onNodeWithText("Buka kasir").performClick()
        shown("Transaksi kasir")
        compose.onNodeWithText("Kembali").performClick()
        compose.onNodeWithText("Inventaris").performClick()
        shown("Katalog & stok")
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        shown("Dashboard harian")
        runBlocking { db.sales().saveSale(listOf(CartLine(stored, 1))) }
        shown("Rp 14.000")
        shown("1")
        shown("1 produk")
        compose.onNodeWithText("Lihat peringatan").performClick()
        shown("Gula")
        compose.onNodeWithText("Kembali").performClick()
        runBlocking { db.products().update(stored.copy(priceRupiah = 18000, stock = 5)) }
        shown("0 produk")
        shown("Rp 14.000")
    }

    @Test fun midnightBoundariesAndTotalsBeyondLongAreExact() = runBlocking {
        val date = LocalDate.of(2026, 11, 24)
        val zone = ZoneId.of("Asia/Jakarta")
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        seedSale(start - 1, "9000")
        seedSale(start, "9223372036854775808")
        seedSale(end - 1, "12000")
        seedSale(end, "8000")
        val result = salesForDay(db.sales(), date, zone).first()
        assertEquals(2, result.count)
        assertEquals("9223372036854787808", result.revenue.toString())
    }
}
