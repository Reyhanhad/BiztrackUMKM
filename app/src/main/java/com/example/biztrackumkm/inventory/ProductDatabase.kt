package com.example.biztrackumkm.inventory

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.biztrackumkm.cashier.Sale
import com.example.biztrackumkm.cashier.SaleDao
import com.example.biztrackumkm.cashier.SaleItem
import kotlinx.coroutines.flow.Flow

/** Harga dalam rupiah utuh; barcode kosong disimpan sebagai null. */
@Entity(tableName = "products", indices = [Index(value = ["barcode"], unique = true)])
internal data class Product(
    val name: String,
    val priceRupiah: Long,
    val stock: Int,
    val minimumStock: Int,
    val barcode: String?,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(defaultValue = "0") val archived: Boolean = false,
)

@Dao
internal interface ProductDao {
    @Query("SELECT * FROM products WHERE archived = 0 ORDER BY id DESC")
    fun observeProducts(): Flow<List<Product>>

    // Lookup inventaris juga mengenali kode yang dicadangkan oleh produk arsip.
    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Product?

    /** Produk baru memakai ID otomatis. Nilai -1 berarti barcode sudah terdaftar. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: Product): Long

    suspend fun update(product: Product): Int = updateActiveFields(
        product.id, product.name, product.priceRupiah, product.stock, product.minimumStock, product.barcode,
    )

    @Query("""
        UPDATE products
        SET name = :name, priceRupiah = :priceRupiah, stock = :stock,
            minimumStock = :minimumStock, barcode = :barcode
        WHERE id = :id AND archived = 0
    """)
    suspend fun updateActiveFields(
        id: Long, name: String, priceRupiah: Long, stock: Int, minimumStock: Int, barcode: String?,
    ): Int

    @Query("UPDATE products SET archived = 1 WHERE id = :id AND archived = 0")
    suspend fun archive(id: Long): Int
}

@Database(
    entities = [Product::class, Sale::class, SaleItem::class],
    version = 3,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
internal abstract class ProductDatabase : RoomDatabase() {
    abstract fun products(): ProductDao
    abstract fun sales(): SaleDao

    companion object {
        @Volatile private var instance: ProductDatabase? = null

        fun get(context: Context): ProductDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                ProductDatabase::class.java,
                "inventory.db",
            ).build().also { instance = it }
        }
    }
}
