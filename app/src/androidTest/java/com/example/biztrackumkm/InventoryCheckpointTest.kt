package com.example.biztrackumkm

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.Product
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
        compose.setContent { BiztrackUMKMTheme { model.value?.let { InventoryScreen(it) } } }
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

    @Test fun savedProductsSurviveReopeningAndInvalidEntriesAreRejected() {
        fun add() = compose.onNodeWithText("Tambah barang").performClick()
        fun input(label: String, value: String) {
            field(label).performTextInput(value)
        }
        fun fill(name: String, stock: String = "7", barcode: String? = null) {
            input("Nama produk", name)
            input("Harga jual (Rp)", "12500")
            input("Stok awal", stock)
            input("Batas minimum stok", "3")
            if (barcode != null) input("Barcode (opsional)", barcode)
        }

        awaitText("Belum ada produk")
        add()
        save()
        compose.onNodeWithText("Isi nama produk.").assertIsDisplayed()
        fill("Stok negatif", stock = "-1")
        save()
        compose.onNodeWithText("Masukkan stok berupa bilangan bulat, nol atau lebih.").assertIsDisplayed()
        backToCatalog()
        awaitText("Belum ada produk")

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

        field("Harga jual (Rp)").performTextReplacement("25000")
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
}
