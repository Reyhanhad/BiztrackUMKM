package com.example.biztrackumkm.inventory

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InventoryScreen(
    viewModel: InventoryViewModel,
    onOpenCashier: () -> Unit,
    onLogout: () -> Unit,
    onOpenRestock: () -> Unit
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val current = form
    var scannerOpen by rememberSaveable { mutableStateOf(false) }
    var archiveOpen by rememberSaveable { mutableStateOf(false) }
    var archivedBarcodeNotice by rememberSaveable { mutableStateOf(false) }

    suspend fun lookupBarcode(barcode: String) {
        if (viewModel.openBarcode(barcode) == BarcodeLookupResult.Archived) {
            archivedBarcodeNotice = true
        }
    }

    if (scannerOpen && current == null) {
        BarcodeScannerScreen(
            onLookup = ::lookupBarcode,
            onClose = { scannerOpen = false },
        )
        return
    }

    if (archiveOpen) {
        ArchivedProductsScreen(viewModel, onBack = { archiveOpen = false })
        return
    }

    val products by viewModel.products
        .collectAsStateWithLifecycle<List<Product>?>(initialValue = null)

    BackHandler(
        enabled = current != null,
        onBack = viewModel::closeForm
    )

    if (archivedBarcodeNotice) {
        AlertDialog(
            onDismissRequest = { archivedBarcodeNotice = false },
            title = { Text(stringResource(R.string.inventory_archived_barcode_title)) },
            text = { Text(stringResource(R.string.inventory_archived_barcode_hint)) },
            confirmButton = {
                TextButton(onClick = { archivedBarcodeNotice = false }) {
                    Text(stringResource(R.string.inventory_archive_ok))
                }
            },
        )
    }

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
                        TextButton(
                            onClick = viewModel::closeForm,
                            enabled = !current.saving
                        ) {
                            Text(stringResource(R.string.inventory_back))
                        }
                    }
                },
                actions = {
                    if (current == null) {
                        TextButton(onClick = onOpenCashier) {
                            Text(stringResource(R.string.cashier_open))
                        }
                        TextButton(onClick = onLogout) {
                            Text(stringResource(R.string.owner_logout))
                        }
                    }
                }
            )
        }
    ) { insets ->
        if (current == null) {
            Catalog(
                products = products,
                onAdd = viewModel::openForm,
                onEdit = viewModel::openEditForm,
                onBarcodeLookup = ::lookupBarcode,
                onScanBarcode = { scannerOpen = true },
                onOpenRestock = onOpenRestock,
                onOpenArchive = { archiveOpen = true },
                modifier = Modifier.padding(insets)
            )
        } else {
            ProductEditor(
                current,
                viewModel::changeForm,
                viewModel::saveProduct,
                viewModel::archiveProduct,
                Modifier.padding(insets)
                    .consumeWindowInsets(insets)
                    .imePadding()
            )
        }
    }
}

@Composable
private fun Catalog(
    products: List<Product>?,
    onAdd: () -> Unit,
    onEdit: (Product) -> Unit,
    onBarcodeLookup: suspend (String) -> Unit,
    onScanBarcode: () -> Unit,
    onOpenRestock: () -> Unit,
    onOpenArchive: () -> Unit,
    modifier: Modifier
) {
    val rupiah = remember {
        NumberFormat.getIntegerInstance(
            Locale.forLanguageTag("id-ID")
        )
    }

    val lowStockCount = products?.count {
        it.stock <= it.minimumStock
    } ?: 0

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.inventory_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    TextButton(onClick = onOpenArchive) {
                        Text(stringResource(R.string.inventory_archive_open))
                    }
                }
                Text(
                    stringResource(R.string.inventory_subtitle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BarcodeSearch(onBarcodeLookup, Modifier.weight(1f))
                OutlinedButton(onClick = onScanBarcode) {
                    Text(stringResource(R.string.barcode_scan_open))
                }
            }
        }

        // GER-1: Peringatan stok rendah
        item {
            if (lowStockCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.restock_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Text(
                            text = stringResource(R.string.restock_warning_count, lowStockCount),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        Button(
                            onClick = onOpenRestock,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.restock_open))
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenRestock,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.restock_open))
                }
            }
        }

        item {
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.inventory_add),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        when {
            products == null -> item {
                Box(
                    Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            products.isEmpty() -> item {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.inventory_empty_title),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        stringResource(R.string.inventory_empty_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> items(products, key = { it.id }) { product ->
                val price = remember(product.priceRupiah) {
                    rupiah.format(product.priceRupiah)
                }

                ProductCard(
                    product,
                    price,
                    onEdit = { onEdit(product) }
                )
            }
        }
    }
}

@Composable
private fun BarcodeSearch(onLookup: suspend (String) -> Unit, modifier: Modifier) {
    var barcode by rememberSaveable { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canSearch = barcode.isNotBlank() && !searching
    fun search() {
        if (searching || barcode.isBlank()) return
        searching = true
        failed = false
        scope.launch {
            try {
                onLookup(barcode)
            } catch (error: SQLiteException) {
                Log.e("Inventory", "Gagal mencari barcode", error)
                failed = true
            } finally {
                searching = false
            }
        }
    }

    OutlinedTextField(
        value = barcode,
        onValueChange = {
            barcode = it
            failed = false
        },
        label = { Text(stringResource(R.string.inventory_search_barcode)) },
        modifier = modifier,
        enabled = !searching,
        singleLine = true,
        isError = failed,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { search() }),
        trailingIcon = {
            TextButton(onClick = ::search, enabled = canSearch) {
                Text(stringResource(R.string.inventory_search_action))
            }
        },
        supportingText = if (failed) {
            { Text(stringResource(R.string.inventory_search_error)) }
        } else {
            null
        }
    )
}

@Composable
private fun ProductCard(
    product: Product,
    price: String,
    onEdit: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isLowStock = product.stock <= product.minimumStock

    OutlinedCard(
        onClick = onEdit,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = colors.surfaceContainer
        ),
        border = BorderStroke(
            1.dp,
            if (isLowStock) colors.error else colors.outlineVariant
        )
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        stringResource(
                            R.string.inventory_price_display,
                            price
                        ),
                        color = colors.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.inventory_stock_current),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )

                    Text(
                        product.stock.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isLowStock) {
                            colors.error
                        } else {
                            colors.onSurface
                        }
                    )
                }
            }

            // GER-1: Status stok produk
            if (isLowStock) {
                Text(
                    text = if (product.stock == 0) {
                        stringResource(R.string.restock_status_empty)
                    } else {
                        stringResource(R.string.restock_status_low)
                    },
                    color = colors.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            if (product.barcode != null) {
                Text(
                    stringResource(
                        R.string.inventory_barcode_display,
                        product.barcode
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.inventory_minimum_short),
                    Modifier.weight(1f),
                    color = colors.onSurfaceVariant
                )

                Text(product.minimumStock.toString())

                Text(
                    stringResource(R.string.inventory_edit_action),
                    color = colors.primary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun ProductEditor(
    form: ProductForm,
    onChange: (ProductForm.() -> ProductForm) -> Unit,
    onSave: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier
) {
    val editing = form.productId != null
    var confirmArchive by rememberSaveable(form.productId) { mutableStateOf(false) }

    if (editing && confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.inventory_archive_title)) },
            text = { Text(stringResource(R.string.inventory_archive_hint)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmArchive = false
                    onArchive()
                }) {
                    Text(stringResource(R.string.inventory_archive_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) {
                    Text(stringResource(R.string.inventory_archive_cancel))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            stringResource(
                if (editing) R.string.inventory_edit_hint
                else R.string.inventory_add_hint
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ProductField(
            form.name,
            R.string.inventory_name,
            ProductProblem.Name,
            form,
            KeyboardType.Text
        ) { value ->
            onChange { copy(name = value) }
        }

        RupiahPriceField(
            value = form.price,
            onChange = { value -> onChange { copy(price = value) } },
            enabled = !form.saving,
            isError = form.problem == ProductProblem.Price,
        )

        val stockLabel = if (editing) {
            R.string.inventory_stock_current
        } else {
            R.string.inventory_initial_stock
        }

        ProductField(
            form.stock,
            stockLabel,
            ProductProblem.Stock,
            form,
            KeyboardType.Number
        ) { value ->
            onChange { copy(stock = value) }
        }

        ProductField(
            form.minimumStock,
            R.string.inventory_minimum_stock,
            ProductProblem.MinimumStock,
            form,
            KeyboardType.Number
        ) { value ->
            onChange { copy(minimumStock = value) }
        }

        ProductField(
            form.barcode,
            R.string.inventory_barcode,
            ProductProblem.DuplicateBarcode,
            form,
            KeyboardType.Text
        ) { value ->
            onChange { copy(barcode = value) }
        }

        when (form.problem) {
            ProductProblem.NotFound,
            ProductProblem.Storage -> Text(
                stringResource(form.problem.message),
                color = MaterialTheme.colorScheme.error
            )

            else -> Unit
        }

        Button(
            onClick = onSave,
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(
                    if (form.saving) R.string.inventory_saving
                    else R.string.inventory_save
                )
            )
        }
        if (editing) {
            TextButton(
                onClick = { confirmArchive = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !form.saving,
            ) {
                Text(stringResource(R.string.inventory_archive), color = MaterialTheme.colorScheme.error)
            }
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
    onChange: (String) -> Unit
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
        shape = CardDefaults.outlinedShape,
        modifier = Modifier.fillMaxWidth()
            .bringIntoViewRequester(focusError),
        enabled = !form.saving,
        singleLine = true,
        isError = invalid,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType
        ),
        supportingText = if (invalid) {
            { Text(stringResource(problem.message)) }
        } else {
            null
        }
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
