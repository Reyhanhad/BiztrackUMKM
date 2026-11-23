package com.example.biztrackumkm.inventory

import android.content.Context
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
)

@Dao
internal interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun observeProducts(): Flow<List<Product>>

    /** Produk baru memakai ID otomatis. Nilai -1 berarti barcode sudah terdaftar. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(product: Product): Long
}

@Database(entities = [Product::class], version = 1)
internal abstract class ProductDatabase : RoomDatabase() {
    abstract fun products(): ProductDao

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
