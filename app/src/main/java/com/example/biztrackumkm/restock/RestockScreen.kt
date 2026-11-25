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
import androidx.compose.ui.text.font.FontWeight
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
                    Text(stringResource(R.string.restock_priority_title))
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

                Text(
                    text = stringResource(
                        R.string.restock_priority_subtitle
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

    val priority = product.restockPriority()
    val isOutOfStock = product.stock == 0

    val colors = MaterialTheme.colorScheme

    val priorityColor = when (priority) {
        RestockPriority.HIGH -> colors.error
        RestockPriority.MEDIUM -> colors.tertiary
        RestockPriority.LOW -> colors.primary
    }

    val priorityContainer = when (priority) {
        RestockPriority.HIGH -> colors.errorContainer
        RestockPriority.MEDIUM -> colors.tertiaryContainer
        RestockPriority.LOW -> colors.primaryContainer
    }

    val priorityContentColor = when (priority) {
        RestockPriority.HIGH -> colors.onErrorContainer
        RestockPriority.MEDIUM -> colors.onTertiaryContainer
        RestockPriority.LOW -> colors.onPrimaryContainer
    }

    val priorityLabel = when (priority) {
        RestockPriority.HIGH -> R.string.restock_priority_high
        RestockPriority.MEDIUM -> R.string.restock_priority_medium
        RestockPriority.LOW -> R.string.restock_priority_low
    }

    val priorityHint = when (priority) {
        RestockPriority.HIGH -> R.string.restock_priority_high_hint
        RestockPriority.MEDIUM -> R.string.restock_priority_medium_hint
        RestockPriority.LOW -> R.string.restock_priority_low_hint
    }

    val stockProgress = product.restockRatio()
        .coerceIn(0.0, 1.0)
        .toFloat()

    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Surface(
                color = priorityContainer,
                contentColor = priorityContentColor,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = stringResource(priorityLabel),
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Text(
                text = stringResource(
                    if (isOutOfStock) {
                        R.string.restock_status_empty
                    } else {
                        R.string.restock_status_low
                    }
                ),
                color = priorityColor,
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

            LinearProgressIndicator(
                progress = { stockProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = priorityColor,
                trackColor = colors.surfaceVariant
            )

            Text(
                text = stringResource(priorityHint),
                color = priorityColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
