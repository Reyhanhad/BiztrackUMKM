package com.example.biztrackumkm.restock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
                        LowStockProductCard(product, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockProductCard(
    product: Product,
    viewModel: RestockViewModel
) {
    var leadTimeText by rememberSaveable { mutableStateOf("4") }
    var safetyStockText by rememberSaveable { mutableStateOf("2") }
    var totalSoldText by rememberSaveable { mutableStateOf("21") }
    var periodDaysText by rememberSaveable { mutableStateOf("7") }

    val isOutOfStock = product.stock == 0

    val statusColor = if (isOutOfStock) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val leadTime = leadTimeText.toIntOrNull() ?: 0
    val safetyStock = safetyStockText.toIntOrNull() ?: 0
    val totalSold = totalSoldText.toIntOrNull() ?: 0
    val periodDays = periodDaysText.toIntOrNull() ?: 0

    val recommendation = viewModel.calculateRecommendation(
        product = product,
        totalUnitsSold = totalSold,
        periodDays = periodDays,
        leadTimeDays = leadTime,
        safetyStock = safetyStock,
        hasEnoughHistory = periodDays > 0 && totalSold >= 0
    )

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

            HorizontalDivider()

            Text(
                text = stringResource(R.string.restock_recommendation_title),
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = leadTimeText,
                    onValueChange = { leadTimeText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.restock_lead_time_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = safetyStockText,
                    onValueChange = { safetyStockText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.restock_safety_stock_label)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            when (recommendation) {
                is RestockRecommendationResult.Success -> {
                    Text(
                        text = stringResource(
                            R.string.restock_recommendation_result,
                            recommendation.recommendedQuantity,
                            recommendation.requiredQuantity
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is RestockRecommendationResult.InsufficientHistory -> {
                    Text(
                        text = stringResource(R.string.restock_insufficient_history),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
