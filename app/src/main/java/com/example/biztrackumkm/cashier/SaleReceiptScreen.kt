package com.example.biztrackumkm.cashier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

private val receiptLocale = Locale.forLanguageTag("id-ID")

@Composable
internal fun SaleHistoryScreen(
    checkout: CheckoutViewModel,
    onOpenReceipt: (Long) -> Unit,
    onBack: () -> Unit,
) {
    val history by checkout.history.collectAsStateWithLifecycle<List<Sale>?>(initialValue = null)
    val rupiah = remember { NumberFormat.getIntegerInstance(receiptLocale) }
    val dateTime = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, receiptLocale) }
    ReceiptLayout(R.string.receipt_history_title, onBack) {
        val sales = history
        when {
            sales == null -> item { CircularProgressIndicator() }
            sales.isEmpty() -> item { Text(stringResource(R.string.receipt_history_empty)) }
            else -> items(sales, key = { it.id }) { sale ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        SaleHeading(sale, dateTime)
                        Text(
                            stringResource(R.string.cashier_amount, rupiah.formatStored(sale.totalRupiah)),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        TextButton(onClick = { onOpenReceipt(sale.id) }) {
                            Text(stringResource(R.string.receipt_history_open))
                        }
                    }
                }
            }
        }
    }
}

private sealed interface ReceiptLoad {
    data object Loading : ReceiptLoad
    data object Missing : ReceiptLoad
    data class Found(val receipt: SaleReceipt) : ReceiptLoad
}

@Composable
internal fun SaleReceiptScreen(checkout: CheckoutViewModel, saleId: Long, onBack: () -> Unit) {
    val state by produceState<ReceiptLoad>(ReceiptLoad.Loading, checkout, saleId) {
        value = checkout.findReceipt(saleId)?.let { ReceiptLoad.Found(it) } ?: ReceiptLoad.Missing
    }
    val rupiah = remember { NumberFormat.getIntegerInstance(receiptLocale) }
    val dateTime = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, receiptLocale) }
    ReceiptLayout(R.string.receipt_title, onBack) {
        when (val loaded = state) {
            ReceiptLoad.Loading -> item { CircularProgressIndicator() }
            ReceiptLoad.Missing -> item { Text(stringResource(R.string.receipt_not_found)) }
            is ReceiptLoad.Found -> {
                val (sale, lines) = loaded.receipt
                item { SaleHeading(sale, dateTime) }
                item { HorizontalDivider() }
                items(lines, key = { it.productId }) { line ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(line.productName, style = MaterialTheme.typography.titleMedium)
                            val unitPrice = stringResource(R.string.cashier_amount, rupiah.format(line.priceRupiah))
                            Text(
                                stringResource(R.string.receipt_unit_price, line.quantity, unitPrice),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(stringResource(R.string.cashier_amount, rupiah.formatStored(line.subtotalRupiah)))
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(R.string.receipt_total), Modifier.weight(1f))
                            Text(
                                stringResource(R.string.cashier_amount, rupiah.formatStored(sale.totalRupiah)),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SaleHeading(sale: Sale, dateTime: DateFormat) {
    Text(stringResource(R.string.receipt_number, sale.id), style = MaterialTheme.typography.titleMedium)
    Text(
        dateTime.format(Date(sale.soldAtMillis)),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun NumberFormat.formatStored(amount: String): String =
    format(amount.toLongOrNull() ?: amount.toBigInteger())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptLayout(title: Int, onBack: () -> Unit, content: LazyListScope.() -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.cashier_back)) }
                },
            )
        },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.padding(insets).consumeWindowInsets(insets).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
