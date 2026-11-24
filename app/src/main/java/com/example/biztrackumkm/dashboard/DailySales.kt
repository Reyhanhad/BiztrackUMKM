package com.example.biztrackumkm.dashboard

import com.example.biztrackumkm.cashier.SaleDao
import java.math.BigInteger
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

internal data class DailySales(val date: LocalDate, val revenue: BigInteger, val count: Int)

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeDailySales(sales: SaleDao): Flow<DailySales> = flow {
    while (true) {
        val now = ZonedDateTime.now()
        val date = now.toLocalDate()
        emit(date to now.zone)
        val midnight = date.plusDays(1).atStartOfDay(now.zone).toInstant()
        // Recheck the device zone/clock while open, and switch dates at midnight.
        delay((midnight.toEpochMilli() - System.currentTimeMillis()).coerceIn(1, 60_000))
    }
}.distinctUntilChanged().flatMapLatest { (date, zone) ->
    salesForDay(sales, date, zone)
}

internal fun salesForDay(sales: SaleDao, date: LocalDate, zone: ZoneId): Flow<DailySales> {
    val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return sales.observeTotals(start, end).map { totals ->
        DailySales(date, totals.sumOf { it.toBigInteger() }, totals.size)
    }.flowOn(Dispatchers.Default)
}
