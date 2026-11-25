package com.example.biztrackumkm

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biztrackumkm.cashier.CheckoutViewModel
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.Product
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.navigation.OwnerNavigation
import com.example.biztrackumkm.report.observeSalesReport
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
class SalesReportCheckpointTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var db: ProductDatabase
    private val first = LocalDate.of(2025, 9, 1)
    private val last = first.plusDays(2)
    private val zone = ZoneId.systemDefault()

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(compose.activity, ProductDatabase::class.java).build()
    }

    @After fun close() = db.close()

    private fun sale(id: Long, time: Long, total: String) {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO sales(id, soldAtMillis, totalRupiah) VALUES (?, ?, ?)",
            arrayOf<Any>(id, time, total),
        )
    }

    private fun item(sale: Long, product: Long, name: String, price: Long, units: Int) {
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO sale_items(saleId, productId, productName, priceRupiah, quantity, subtotalRupiah) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any>(sale, product, name, price, units, (price.toBigInteger() * units.toBigInteger()).toString()),
        )
    }

    private fun seedPeriod() {
        val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        sale(1, start - 1, "9000")
        item(1, 9, "Di luar periode", 9000, 1)
        sale(2, start, "55000")
        item(2, 1, "Teh", 1000, 5)
        item(2, 2, "Kopi lama", 50000, 1)
        sale(3, first.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(), "50000")
        item(3, 2, "Kopi", 50000, 1)
        sale(4, end - 1, "50000")
        item(4, 2, "Kopi", 50000, 1)
        sale(5, end, "8000")
        item(5, 9, "Di luar periode", 8000, 1)
    }

    @Test fun historicalReportUsesSaleBoundariesAndUnitsPerId() = runBlocking {
        seedPeriod()
        val tea = Product("Teh", 1000, 8, 2, null, id = 1)
        db.products().insert(tea)
        db.products().update(tea.copy(priceRupiah = 9000, name = "Nama katalog baru"))
        val result = observeSalesReport(db.sales(), first, last, zone).first()
        assertEquals("155000", result.revenue.toString())
        assertEquals(3, result.transactionCount)
        assertEquals(listOf(1L, 2L), result.ranking.map { it.id })
        assertEquals(listOf("Teh", "Kopi"), result.ranking.map { it.name })
        assertEquals(listOf(5L, 3L), result.ranking.map { it.units })
    }

    @Test fun separateIdsWithSameNameAndLargeTotalsStayExact() = runBlocking {
        val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
        sale(1, start, "18446744073709551614")
        item(1, 2, "Kopi", Long.MAX_VALUE, 1)
        item(1, 1, "Kopi", Long.MAX_VALUE, 1)
        val result = observeSalesReport(db.sales(), first, first, zone).first()
        assertEquals("18446744073709551614", result.revenue.toString())
        assertEquals(1, result.transactionCount)
        assertEquals(listOf(1L, 2L), result.ranking.map { it.id })
    }

    @Test fun ownerSelectsPeriodAndEmptyPeriodClearsPreviousResults() {
        seedPeriod()
        val inventory = InventoryViewModel(db.products())
        val checkout = CheckoutViewModel(db.sales())
        compose.setContent {
            BiztrackUMKMTheme {
                OwnerNavigation(inventory, checkout, db.sales(), onAddCashier = null, onLogout = {})
            }
        }
        compose.onNodeWithText("Laporan penjualan").performClick()
        fun click(text: String) = compose.onNodeWithText(text).performClick()
        fun dates(start: String, end: String) {
            compose.onNode(hasSetTextAction() and hasText("Tanggal awal")).performTextReplacement(start)
            compose.onNode(hasSetTextAction() and hasText("Tanggal akhir")).performTextReplacement(end)
        }
        fun shown(text: String) {
            compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        click("Ubah periode")
        dates("31/02/2025", "03/09/2025")
        compose.onNodeWithText("Terapkan").assertIsNotEnabled()
        dates("04/09/2025", "03/09/2025")
        compose.onNodeWithText("Terapkan").assertIsNotEnabled()
        dates("01/09/2025", "03/09/2025")
        click("Terapkan")
        shown("Rp 155.000")
        shown("3 transaksi")
        shown("Teh")
        shown("5 unit")
        shown("Kopi")
        shown("3 unit")
        click("Ubah periode")
        dates("05/09/2025", "05/09/2025")
        click("Terapkan")
        shown("Rp 0")
        shown("0 transaksi")
        shown("Belum ada transaksi pada periode ini.")
        compose.onNodeWithText("Teh").assertDoesNotExist()
        compose.onNodeWithText("Kopi").assertDoesNotExist()
        click("Kembali")
        shown("Dashboard harian")
    }
}
