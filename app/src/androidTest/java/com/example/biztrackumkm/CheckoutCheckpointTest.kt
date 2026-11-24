package com.example.biztrackumkm

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biztrackumkm.cashier.CashierScreen
import com.example.biztrackumkm.cashier.CheckoutViewModel
import com.example.biztrackumkm.inventory.Product
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CheckoutCheckpointTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var database: ProductDatabase

    @Before fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(compose.activity, ProductDatabase::class.java).build()
    }

    @After fun closeDatabase() = database.close()

    @Test fun stockChangedDuringConfirmationRollsBackAllItemsThenAdjustedCartCanBeSaved() {
        val products = database.products()
        val (sugar, coffee) = runBlocking {
            // The product list is newest first; the valid item is processed before the short one.
            val sugar = Product("Gula", 14_000, 3, 1, null)
            val coffee = Product("Kopi", 8_500, 5, 3, null)
            sugar.copy(id = products.insert(sugar)) to coffee.copy(id = products.insert(coffee))
        }
        val checkout = CheckoutViewModel(database.sales())
        compose.setContent {
            BiztrackUMKMTheme { CashierScreen(products.observeProducts(), checkout, onBack = {}) }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("Cari produk") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        fun click(text: String) = compose.onNodeWithText(text).performClick()
        fun action(description: String) = compose.onNodeWithContentDescription(description).performClick()
        fun shown(text: String) {
            compose.waitUntil(5_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        for (product in listOf(coffee, sugar)) {
            click("Cari produk")
            action("Tambah ${product.name} ke keranjang")
            action("Tambah jumlah ${product.name}")
        }
        click("Simpan penjualan")
        compose.onNodeWithText("Konfirmasi penjualan").assertIsDisplayed()
        runBlocking { products.update(sugar.copy(stock = 1)) }
        click("Konfirmasi")
        shown("Penjualan tidak tersimpan")
        shown("Stok Gula tersisa 1 pcs. Kurangi jumlah atau hapus produk dari keranjang.")
        assertEquals(mapOf(sugar.id to 1, coffee.id to 5), stocks())
        val sqlite = database.openHelper.readableDatabase
        for (table in listOf("sales", "sale_items")) {
            sqlite.query("SELECT COUNT(*) FROM $table").use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        }
        click("OK")
        action("Kurangi jumlah Gula")
        click("Simpan penjualan")
        click("Konfirmasi")
        shown("Penjualan senilai Rp 31.000 tersimpan dan stok sudah diperbarui.")
        click("OK")
        compose.onNodeWithText("0 produk · 0 pcs").assertIsDisplayed()
        assertEquals(mapOf(sugar.id to 0, coffee.id to 3), stocks())
        val saleId = sqlite.query("SELECT id, soldAtMillis, totalRupiah FROM sales").use {
            assertTrue(it.moveToFirst())
            assertTrue(it.getLong(1) > 0)
            assertEquals("31000", it.getString(2))
            val id = it.getLong(0)
            assertFalse(it.moveToNext())
            id
        }
        runBlocking { products.update(coffee.copy(name = "Kopi Baru", priceRupiah = 12_000, stock = 3)) }
        val details = "saleId, productId, productName, priceRupiah, quantity, subtotalRupiah"
        sqlite.query("SELECT $details FROM sale_items ORDER BY productId").use { cursor ->
            val actual = buildList {
                while (cursor.moveToNext()) {
                    add(List(cursor.columnCount) { cursor.getString(it) })
                }
            }
            assertEquals(
                listOf(
                    listOf("$saleId", "${sugar.id}", "Gula", "14000", "1", "14000"),
                    listOf("$saleId", "${coffee.id}", "Kopi", "8500", "2", "17000"),
                ),
                actual,
            )
        }
    }

    private fun stocks() = runBlocking {
        database.products().observeProducts().first().associate { it.id to it.stock }
    }
}
