package com.example.biztrackumkm

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme
import java.util.UUID
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryCheckpointTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun savedProductsSurviveReopeningAndInvalidEntriesAreRejected() {
        val context = compose.activity.applicationContext
        val filename = "inventory-checkpoint-${UUID.randomUUID()}.db"
        fun openDatabase() = Room.databaseBuilder(context, ProductDatabase::class.java, filename).build()
        var database = openDatabase()
        val model = mutableStateOf<InventoryViewModel?>(InventoryViewModel(database.products()))
        fun awaitText(text: String) = compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        fun add() = compose.onNodeWithText("Tambah barang").performClick()
        fun input(label: String, value: String) {
            compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextInput(value)
        }
        fun fill(name: String, stock: String = "7", barcode: String? = null) {
            input("Nama produk", name)
            input("Harga jual (Rp)", "12500")
            input("Stok awal", stock)
            input("Batas minimum stok", "3")
            if (barcode != null) input("Barcode (opsional)", barcode)
        }
        fun save() = compose.onNodeWithText("Simpan barang").performScrollTo().performClick()
        fun backToCatalog() = compose.onNodeWithText("Kembali").performClick()

        compose.setContent { BiztrackUMKMTheme { model.value?.let { InventoryScreen(it) } } }
        try {
            awaitText("Belum ada produk")
            add()
            save()
            compose.onNodeWithText("Isi nama produk.").assertIsDisplayed()
            fill("Stok negatif", stock = "-1")
            save()
            compose.onNodeWithText("Masukkan stok awal berupa bilangan bulat, nol atau lebih.").assertIsDisplayed()
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

            compose.runOnIdle { model.value = null }
            compose.waitForIdle()
            database.close()
            database = openDatabase()
            compose.runOnIdle { model.value = InventoryViewModel(database.products()) }
            awaitText("Minyak Goreng")
            for (text in listOf("Beras", "Gula Pasir", "Barcode: 001234")) {
                compose.onNodeWithText(text).assertIsDisplayed()
            }
            compose.onNodeWithText("Barcode bentrok").assertDoesNotExist()
            compose.onNodeWithText("Stok negatif").assertDoesNotExist()
        } finally {
            compose.runOnIdle { model.value = null }
            compose.waitForIdle()
            database.close()
            context.deleteDatabase(filename)
        }
    }
}
