package com.example.biztrackumkm.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InventoryScreen(viewModel: InventoryViewModel, onOpenCashier: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val current = form
    val products by viewModel.products.collectAsStateWithLifecycle<List<Product>?>(initialValue = null)
    BackHandler(enabled = current != null, onBack = viewModel::closeForm)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        current == null -> R.string.app_name
                        current.productId == null -> R.string.inventory_add
                        else -> R.string.inventory_edit
                    }
                    Text(stringResource(title))
                },
                navigationIcon = {
                    if (current != null) {
                        TextButton(onClick = viewModel::closeForm, enabled = !current.saving) {
                            Text(stringResource(R.string.inventory_back))
                        }
                    }
                },
                actions = {
                    if (current == null) {
                        TextButton(onClick = onOpenCashier) {
                            Text(stringResource(R.string.cashier_open))
                        }
                    }
                },
            )
        },
    ) { insets ->
        if (current == null) {
            Catalog(products, viewModel::openForm, viewModel::openEditForm, Modifier.padding(insets))
        } else {
            ProductEditor(
                current,
                viewModel::changeForm,
                viewModel::saveProduct,
                Modifier.padding(insets).consumeWindowInsets(insets).imePadding(),
            )
        }
    }
}

@Composable
private fun Catalog(
    products: List<Product>?,
    onAdd: () -> Unit,
    onEdit: (Product) -> Unit,
    modifier: Modifier,
) {
    val rupiah = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.inventory_title), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.inventory_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.inventory_add), modifier = Modifier.padding(vertical = 4.dp))
            }
        }
        when {
            products == null -> item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            products.isEmpty() -> item {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.inventory_empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.inventory_empty_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> items(products, key = { it.id }) { product ->
                val price = remember(product.priceRupiah) { rupiah.format(product.priceRupiah) }
                ProductCard(product, price, onEdit = { onEdit(product) })
            }
        }
    }
}

@Composable
private fun ProductCard(product: Product, price: String, onEdit: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    OutlinedCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainer),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.inventory_price_display, price), color = colors.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.inventory_stock_current), style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                    Text(product.stock.toString(), style = MaterialTheme.typography.titleLarge)
                }
            }
            if (product.barcode != null) {
                Text(stringResource(R.string.inventory_barcode_display, product.barcode), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.inventory_minimum_short), Modifier.weight(1f), color = colors.onSurfaceVariant)
                Text(product.minimumStock.toString())
                Text(stringResource(R.string.inventory_edit_action), color = colors.primary, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ProductEditor(
    form: ProductForm,
    onChange: (ProductForm.() -> ProductForm) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier,
) {
    val editing = form.productId != null
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(if (editing) R.string.inventory_edit_hint else R.string.inventory_add_hint),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ProductField(form.name, R.string.inventory_name, ProductProblem.Name, form, KeyboardType.Text) { value ->
            onChange { copy(name = value) }
        }
        ProductField(form.price, R.string.inventory_price, ProductProblem.Price, form, KeyboardType.Number) { value ->
            onChange { copy(price = value) }
        }
        val stockLabel = if (editing) R.string.inventory_stock_current else R.string.inventory_initial_stock
        ProductField(form.stock, stockLabel, ProductProblem.Stock, form, KeyboardType.Number) { value ->
            onChange { copy(stock = value) }
        }
        ProductField(form.minimumStock, R.string.inventory_minimum_stock, ProductProblem.MinimumStock, form, KeyboardType.Number) { value ->
            onChange { copy(minimumStock = value) }
        }
        ProductField(form.barcode, R.string.inventory_barcode, ProductProblem.DuplicateBarcode, form, KeyboardType.Text) { value ->
            onChange { copy(barcode = value) }
        }
        when (form.problem) {
            ProductProblem.NotFound, ProductProblem.Storage -> Text(
                stringResource(form.problem.message),
                color = MaterialTheme.colorScheme.error,
            )
            else -> Unit
        }
        Button(onClick = onSave, enabled = !form.saving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (form.saving) R.string.inventory_saving else R.string.inventory_save))
        }
    }
}

@Composable
private fun ProductField(
    value: String,
    label: Int,
    problem: ProductProblem,
    form: ProductForm,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
) {
    val invalid = form.problem == problem
    val focusError = remember { BringIntoViewRequester() }
    LaunchedEffect(invalid) {
        if (invalid) focusError.bringIntoView()
    }
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(focusError),
        enabled = !form.saving,
        singleLine = true,
        isError = invalid,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = if (invalid) { { Text(stringResource(problem.message)) } } else null,
    )
}

private val ProductProblem.message: Int
    get() = when (this) {
        ProductProblem.Name -> R.string.inventory_invalid_name
        ProductProblem.Price -> R.string.inventory_invalid_price
        ProductProblem.Stock -> R.string.inventory_invalid_stock
        ProductProblem.MinimumStock -> R.string.inventory_invalid_minimum
        ProductProblem.DuplicateBarcode -> R.string.inventory_duplicate_barcode
        ProductProblem.NotFound -> R.string.inventory_missing_product
        ProductProblem.Storage -> R.string.inventory_invalid_storage
    }
