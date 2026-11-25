package com.example.biztrackumkm.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import com.example.biztrackumkm.restock.RestockViewModel
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    dailySales: Flow<DailySales>,
    restock: RestockViewModel,
    onScanProduct: () -> Unit,
    onAddProduct: () -> Unit,
    onRestock: () -> Unit,
    onLogout: () -> Unit,
    onAddCashier: (() -> Unit)? = null,
) {
    val sales by dailySales.collectAsStateWithLifecycle(initialValue = null)
    val lowStock by restock.lowStockProducts.collectAsStateWithLifecycle(initialValue = null)
    val locale = remember { Locale.forLanguageTag("id-ID") }
    val rupiah = remember { NumberFormat.getIntegerInstance(locale) }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale) }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.dashboard_title)) },
            actions = { TextButton(onClick = onLogout) { Text(stringResource(R.string.owner_logout)) } },
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.dashboard_revenue), style = MaterialTheme.typography.titleMedium)
                    val today = sales
                    if (today == null) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            stringResource(R.string.cashier_amount, rupiah.format(today.revenue)),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(today.date.format(dateFormat), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.dashboard_transactions))
                            Text(today.count.toString(), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onScanProduct, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.barcode_scan_open))
                }
                OutlinedButton(onClick = onAddProduct, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.inventory_add))
                }
            }

            if (onAddCashier != null) {
                OutlinedButton(onClick = onAddCashier, modifier = Modifier.fillMaxWidth()) {
                    Text("Tambah Akun Kasir")
                }
            }

            OutlinedCard(onClick = onRestock, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.restock_title), style = MaterialTheme.typography.titleMedium)
                    val products = lowStock
                    if (products == null) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            stringResource(R.string.dashboard_stock_count, products.size),
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (products.isEmpty()) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        stringResource(R.string.dashboard_alerts),
                        modifier = Modifier.align(Alignment.End),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
