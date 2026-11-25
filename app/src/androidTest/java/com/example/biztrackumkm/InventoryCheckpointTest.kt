package com.example.biztrackumkm

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biztrackumkm.cashier.CartLine
import com.example.biztrackumkm.cashier.SaleRejected
import com.example.biztrackumkm.cashier.SaleRejection
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.Product
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.restock.RestockViewModel
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryCheckpointTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val context get() = compose.activity.applicationContext
    private val filename = "inventory-checkpoint-${UUID.randomUUID()}.db"
    private lateinit var database: ProductDatabase
    private val model = mutableStateOf<InventoryViewModel?>(null)

    @Before fun openCatalog() {
        openDatabase()
        compose.setContent {
            BiztrackUMKMTheme {
                model.value?.let { InventoryScreen(it, onOpenCashier = {}, onLogout = {}, onOpenRestock = {}) }
            }
        }
    }

    @After fun cleanUpDatabase() {
        closeDatabase()
        context.deleteDatabase(filename)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(context, ProductDatabase::class.java, filename).build()
        compose.runOnIdle { model.value = InventoryViewModel(database.products()) }
    }

    private fun closeDatabase() {
        compose.runOnIdle { model.value = null }
        compose.waitForIdle()
        database.close()
    }

    private fun awaitText(text: String) = compose.waitUntil(timeoutMillis = 5_000) {
        compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun field(label: String): SemanticsNodeInteraction =
        compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo()

    private fun save() = compose.onNodeWithText("Simpan barang").performScrollTo().performClick()

    private fun backToCatalog() = compose.onNodeWithText("Kembali").performClick()

    private fun enterPrice(vararg keys: String) {
        compose.onNodeWithText("Harga jual (Rp)").performScrollTo().performClick()
        for (key in keys) compose.onNodeWithText(key).performScrollTo().performClick()
        compose.onNodeWithText("Selesai").performScrollTo().performClick()
    }

    @Test fun savedProductsSurviveReopeningAndInvalidEntriesAreRejected() {
        fun add() {
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Tambah barang"))
            compose.onNodeWithText("Tambah barang").performClick()
        }
        fun input(label: String, value: String) {
            field(label).performTextInput(value)
        }
        fun fill(name: String, stock: String = "7", barcode: String? = null) {
            input("Nama produk", name)
            enterPrice("1", "2", "5", "00")
            input("Stok awal", stock)
            input("Batas minimum stok", "3")
            if (barcode != null) input("Barcode (opsional)", barcode)
        }
        fun assertEmptyCatalog() {
            runBlocking { database.products().observeProducts().first() }
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Belum ada produk"))
            compose.onNodeWithText("Belum ada produk").assertIsDisplayed()
        }

        assertEmptyCatalog()
        add()
        save()
        compose.onNodeWithText("Isi nama produk.").assertIsDisplayed()
        fill("Stok negatif", stock = "-1")
        save()
        compose.onNodeWithText("Masukkan stok berupa bilangan bulat, nol atau lebih.").assertIsDisplayed()
        backToCatalog()
        assertEmptyCatalog()

        add()
        fill("Minyak Goreng", barcode = "001234")
        save()
        awaitText("Minyak Goreng")
        add()
        fill("Barcode bentrok", barcode = "001234")
        save()
        awaitText("Barcode sudah digunakan oleh produk lain.")
        backToCatalog()
        awaitText("Minyak Goreng")
        for (text in listOf("Rp12.500", "7", "3", "Barcode: 001234")) {
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText("Barcode bentrok").assertDoesNotExist()

        for (name in listOf("Gula Pasir", "Beras")) {
            add()
            fill(name)
            save()
            awaitText(name)
        }

        closeDatabase()
        openDatabase()
        awaitText("Minyak Goreng")
        for (text in listOf("Beras", "Gula Pasir", "Barcode: 001234")) {
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText("Barcode bentrok").assertDoesNotExist()
        compose.onNodeWithText("Stok negatif").assertDoesNotExist()
    }

    @Test fun editingPreservesProductIdentityAndRejectsChangesAtomically() {
        fun insert(product: Product) = product.copy(id = runBlocking { database.products().insert(product) })
        fun allProducts() = runBlocking { database.products().observeProducts().first() }
        val other = insert(Product("Gula Pasir", 14_000, 6, 2, "001111"))
        val original = insert(Product("Minyak Goreng", 12_500, 7, 3, "001234"))
        val unchanged = listOf(original, other)

        awaitText(original.name)
        compose.onNodeWithText(original.name).performClick()
        compose.onNodeWithText("Ubah barang").assertIsDisplayed()
        field("Nama produk").assert(hasText(original.name))
        field("Stok saat ini").assert(hasText("7"))
        field("Nama produk").performTextReplacement("Minyak Goreng 2 L")
        field("Stok saat ini").performTextReplacement("-1")
        save()
        compose.onNodeWithText("Masukkan stok berupa bilangan bulat, nol atau lebih.").assertIsDisplayed()
        assertEquals(unchanged, allProducts())

        enterPrice("Hapus semua", "2", "4", "Hapus digit", "5", "000")
        field("Stok saat ini").performTextReplacement("12")
        field("Batas minimum stok").performTextReplacement("5")
        field("Barcode (opsional)").performTextReplacement(other.barcode!!)
        save()
        awaitText("Barcode sudah digunakan oleh produk lain.")
        assertEquals(unchanged, allProducts())

        field("Barcode (opsional)").performTextReplacement(original.barcode!!)
        save()
        val updated = original.copy(name = "Minyak Goreng 2 L", priceRupiah = 25_000, stock = 12, minimumStock = 5)
        awaitText(updated.name)
        assertEquals(listOf(updated, other), allProducts())

        closeDatabase()
        openDatabase()
        awaitText(updated.name)
        for (text in listOf("Rp25.000", "12", "5", "Barcode: 001234")) {
            compose.onNodeWithText(text).assertIsDisplayed()
        }
        compose.onNodeWithText(updated.name).performClick()
        field("Batas minimum stok").assert(hasText("5"))
        field("Stok saat ini").assert(hasText("12"))
    }

    @Test fun manualBarcodeLookupOpensExistingProductOrPrefillsNewProduct() {
        runBlocking {
            database.products().insert(Product("Kode tanpa nol", 10_000, 8, 2, "1234"))
            database.products().insert(Product("Kode dengan nol", 12_500, 7, 3, "001234"))
        }

        fun lookup(code: String) {
            awaitText("Cari barcode produk")
            field("Cari barcode produk").performTextInput(code)
            compose.onNodeWithText("Cari").performClick()
        }

        lookup(" 001234 ")
        awaitText("Ubah barang")
        field("Nama produk").assert(hasText("Kode dengan nol"))
        field("Barcode (opsional)").assert(hasText("001234"))
        backToCatalog()

        lookup("000456")
        awaitText("Isi data barang dan stok awal.")
        field("Barcode (opsional)").assert(hasText("000456"))
        val empty = SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        field("Nama produk").assert(empty)
        compose.onNodeWithText("Masukkan harga").assertIsDisplayed()
        field("Nama produk").performTextInput("Oatmeal")
        enterPrice("2", "3", "000")
        field("Stok awal").performTextInput("7")
        field("Batas minimum stok").performTextInput("1")
        save()

        lookup("000456")
        awaitText("Ubah barang")
        field("Nama produk").assert(hasText("Oatmeal"))
        compose.onNodeWithText("Rp23.000").assertIsDisplayed()
    }

    @Test fun archivedProductCanBeRestoredWithoutLosingProductOrReceipt() {
        val original = Product("Beras arsip", 10_000, 3, 3, "000555")
        val other = Product("Kopi aktif", 12_000, 8, 2, "000666")
        val archivedProduct = runBlocking { original.copy(id = database.products().insert(original)) }
        val activeProduct = runBlocking { other.copy(id = database.products().insert(other)) }
        val sale = runBlocking { database.sales().saveSale(listOf(CartLine(archivedProduct, 1))) }

        awaitText(archivedProduct.name)
        compose.onNodeWithText(archivedProduct.name).performClick()
        compose.onNodeWithText("Arsipkan barang").performScrollTo().performClick()
        compose.onNodeWithText("Arsipkan").performClick()
        awaitText("Katalog & stok")

        closeDatabase()
        openDatabase()
        awaitText(activeProduct.name)
        compose.onNodeWithText(archivedProduct.name).assertDoesNotExist()
        field("Cari barcode produk").performTextInput("000555")
        compose.onNodeWithText("Cari").performClick()
        awaitText("Barcode milik barang arsip")
        compose.onNodeWithText("Isi data barang dan stok awal.").assertDoesNotExist()
        compose.onNodeWithText("Mengerti").performClick()

        val rejection = assertThrows(SaleRejected::class.java) {
            runBlocking {
                database.sales().saveSale(listOf(CartLine(activeProduct, 1), CartLine(archivedProduct, 1)))
            }
        }
        assertEquals(SaleRejection.ProductUnavailable, rejection.reason)
        runBlocking {
            val products = database.products()
            assertEquals(listOf(activeProduct), products.observeProducts().first())
            assertEquals(emptyList<Product>(), RestockViewModel(products.observeProducts()).lowStockProducts.first())
            assertEquals(
                archivedProduct.copy(stock = 2, archived = true),
                products.findByBarcode("000555"),
            )
            assertEquals(-1L, products.insert(original))

            val sales = database.sales()
            assertEquals(listOf(sale.id), sales.observeSales().first().map { it.id })
            assertEquals(listOf(sale.id), sales.observeReportSales(0L, Long.MAX_VALUE).first().map { it.id })
            val item = checkNotNull(sales.findReceipt(sale.id)).items.single()
            assertEquals(archivedProduct.id, item.productId)
            assertEquals("Beras arsip", item.productName)
            assertEquals(10_000L, item.priceRupiah)
            assertEquals(1, item.quantity)
        }

        compose.onNodeWithText("Arsip").performClick()
        awaitText("Barang arsip")
        awaitText(archivedProduct.name)
        compose.onNodeWithText("Barcode: 000555").assertIsDisplayed()
        compose.onNodeWithText("Pulihkan").performClick()
        awaitText("Tidak ada barang yang diarsipkan.")
        backToCatalog()
        awaitText(archivedProduct.name)
        compose.onNodeWithText(archivedProduct.name).assertIsDisplayed()

        val restored = archivedProduct.copy(stock = 2)
        runBlocking {
            val products = database.products()
            assertEquals(listOf(activeProduct, restored), products.observeProducts().first())
            assertEquals(restored, products.findByBarcode("000555"))
            assertEquals(listOf(restored), RestockViewModel(products.observeProducts()).lowStockProducts.first())
            val receipt = checkNotNull(database.sales().findReceipt(sale.id))
            assertEquals(archivedProduct.id, receipt.items.single().productId)
            assertEquals("10000", receipt.sale.totalRupiah)
        }
    }
}
