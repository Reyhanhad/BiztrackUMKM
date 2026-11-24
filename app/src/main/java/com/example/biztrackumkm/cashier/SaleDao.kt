package com.example.biztrackumkm.cashier

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.biztrackumkm.inventory.Product
import java.math.BigInteger

// Total and subtotal are exact decimal rupiah, including values beyond SQLite INTEGER range.
@Entity(tableName = "sales", indices = [Index("soldAtMillis")])
internal data class Sale(
    val soldAtMillis: Long,
    val totalRupiah: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)

@Entity(
    tableName = "sale_items",
    primaryKeys = ["saleId", "productId"],
    foreignKeys = [ForeignKey(entity = Sale::class, parentColumns = ["id"], childColumns = ["saleId"])],
)
internal data class SaleItem(
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val priceRupiah: Long,
    val quantity: Int,
    val subtotalRupiah: String,
)

internal data class CartLine(val product: Product, val quantity: Int) {
    val subtotal: Number = if (product.priceRupiah <= Long.MAX_VALUE / quantity) {
        product.priceRupiah * quantity
    } else {
        BigInteger.valueOf(product.priceRupiah) * BigInteger.valueOf(quantity.toLong())
    }
}

internal fun cartTotal(lines: List<CartLine>): Number {
    var total = 0L
    var largeTotal: BigInteger? = null
    for (line in lines) {
        val subtotal = line.subtotal
        if (largeTotal == null && subtotal is Long && subtotal <= Long.MAX_VALUE - total) {
            total += subtotal
        } else {
            largeTotal = (largeTotal ?: BigInteger.valueOf(total)) +
                (subtotal as? BigInteger ?: BigInteger.valueOf(subtotal.toLong()))
        }
    }
    return largeTotal ?: total
}

internal sealed interface SaleRejection {
    data object ProductUnavailable : SaleRejection
    data class InsufficientStock(val productName: String, val available: Int) : SaleRejection
    data class PriceChanged(val productName: String) : SaleRejection
}

internal class SaleRejected(val reason: SaleRejection) : RuntimeException()

@Dao
internal abstract class SaleDao {
    @Query("SELECT * FROM products WHERE id = :productId")
    protected abstract suspend fun findProduct(productId: Long): Product?

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    protected abstract suspend fun reduceStock(productId: Long, quantity: Int)

    @Insert
    protected abstract suspend fun insertSale(sale: Sale): Long

    @Insert
    protected abstract suspend fun insertItem(item: SaleItem)

    @Transaction
    open suspend fun saveSale(lines: List<CartLine>): Number {
        require(lines.isNotEmpty())
        val total = cartTotal(lines)
        val saleId = insertSale(Sale(System.currentTimeMillis(), total.toString()))
        for (line in lines) {
            val current = findProduct(line.product.id)
                ?: throw SaleRejected(SaleRejection.ProductUnavailable)
            if (line.quantity > current.stock) {
                throw SaleRejected(SaleRejection.InsufficientStock(current.name, current.stock))
            }
            if (line.product.priceRupiah != current.priceRupiah) {
                throw SaleRejected(SaleRejection.PriceChanged(current.name))
            }
            reduceStock(current.id, line.quantity)
            insertItem(
                SaleItem(saleId, current.id, current.name, current.priceRupiah, line.quantity, line.subtotal.toString()),
            )
        }
        return total
    }
}
