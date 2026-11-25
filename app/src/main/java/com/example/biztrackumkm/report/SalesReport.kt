package com.example.biztrackumkm.report

import androidx.room.Relation
import com.example.biztrackumkm.cashier.SaleDao
import com.example.biztrackumkm.cashier.SaleItem
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal data class SoldItem(val productId: Long, val productName: String, val quantity: Int)

internal data class ReportSale(
    val id: Long,
    val totalRupiah: String,
    @Relation(
        parentColumn = "id", entityColumn = "saleId", entity = SaleItem::class,
        projection = ["productId", "productName", "quantity"],
    )
    val items: List<SoldItem>,
)

internal data class RankedProduct(val id: Long, val name: String, val units: Long)

internal data class SalesReport(
    val revenue: BigInteger,
    val transactionCount: Int,
    val ranking: List<RankedProduct>,
)

internal fun observeSalesReport(
    dao: SaleDao,
    first: LocalDate,
    last: LocalDate,
    zone: ZoneId,
): Flow<SalesReport> {
    require(!last.isBefore(first))
    val start = first.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = last.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return dao.observeReportSales(start, end).map { sales ->
        val byProduct = mutableMapOf<Long, RankedProduct>()
        for (sale in sales) {
            for (item in sale.items) {
                val previous = byProduct[item.productId]
                // Sales are newest first. Keep the latest recorded name in this period.
                byProduct[item.productId] = RankedProduct(
                    item.productId, previous?.name ?: item.productName,
                    (previous?.units ?: 0L) + item.quantity,
                )
            }
        }
        SalesReport(
            revenue = sales.sumOf { it.totalRupiah.toBigInteger() },
            transactionCount = sales.size,
            ranking = byProduct.values.sortedWith(
                compareByDescending<RankedProduct> { it.units }.thenBy { it.id },
            ),
        )
    }.flowOn(Dispatchers.Default)
}
