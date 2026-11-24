package com.example.biztrackumkm.restock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import com.example.biztrackumkm.inventory.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RestockScreen(
    viewModel: RestockViewModel,
    onBack: () -> Unit
) {
    val products by viewModel.lowStockProducts
        .collectAsStateWithLifecycle(initialValue = null)

    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.restock_title))
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text(stringResource(R.string.restock_back))
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val lowStockProducts = products

            if (lowStockProducts == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (lowStockProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.restock_empty_title
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            text = stringResource(
                                R.string.restock_empty_hint
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(
                        R.string.restock_product_count,
                        lowStockProducts.size
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = lowStockProducts,
                        key = { it.id }
                    ) { product ->
                        LowStockProductCard(product)
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockProductCard(product: Product) {

    val isOutOfStock = product.stock == 0

    val statusColor = if (isOutOfStock) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = stringResource(
                    if (isOutOfStock) {
                        R.string.restock_status_empty
                    } else {
                        R.string.restock_status_low
                    }
                ),
                color = statusColor,
                style = MaterialTheme.typography.labelLarge
            )

            HorizontalDivider()

            Text(
                text = stringResource(
                    R.string.restock_current_stock,
                    product.stock
                )
            )

            Text(
                text = stringResource(
                    R.string.restock_minimum_stock,
                    product.minimumStock
                )
            )

            Text(
                text = stringResource(
                    R.string.restock_action_hint
                ),
                color = statusColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
