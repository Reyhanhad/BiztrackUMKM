package com.example.biztrackumkm.cashier

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import com.example.biztrackumkm.inventory.Product
import java.math.BigInteger
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.flow.Flow

private val CartQuantitiesSaver = Saver<SnapshotStateMap<Long, Int>, LongArray>(
    save = { quantities ->
        val values = LongArray(quantities.size * 2)
        var index = 0
        for ((id, quantity) in quantities) {
            values[index++] = id
            values[index++] = quantity.toLong()
        }
        values
    },
    restore = { values ->
        mutableStateMapOf<Long, Int>().apply {
            for (index in values.indices step 2) put(values[index], values[index + 1].toInt())
        }
    },
)

private data class CartLine(val product: Product, val quantity: Int) {
    val subtotal: Number = if (product.priceRupiah <= Long.MAX_VALUE / quantity) {
        product.priceRupiah * quantity
    } else {
        BigInteger.valueOf(product.priceRupiah) * BigInteger.valueOf(quantity.toLong())
    }
}

private fun cartTotal(lines: List<CartLine>): Number {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CashierScreen(products: Flow<List<Product>>, onBack: () -> Unit) {
    val catalog by products.collectAsStateWithLifecycle<List<Product>?>(initialValue = null)
    val quantities = rememberSaveable(saver = CartQuantitiesSaver) { mutableStateMapOf<Long, Int>() }
    val selected = quantities.toMap()
    val lines = remember(catalog, selected) {
        catalog.orEmpty().mapNotNull { product ->
            selected[product.id]?.let { CartLine(product, it) }
        }
    }
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    val rupiah = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")) }
    val total = remember(lines) { rupiah.format(cartTotal(lines)) }
    val unitCount = remember(lines) { lines.sumOf { it.quantity.toLong() } }
    fun increment(id: Long) {
        val quantity = quantities[id] ?: 0
        if (quantity < Int.MAX_VALUE) quantities[id] = quantity + 1
    }
    fun decrement(id: Long) {
        val quantity = quantities[id] ?: return
        if (quantity > 1) quantities[id] = quantity - 1
    }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cashier_title)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.cashier_back)) }
                },
            )
        },
        bottomBar = { CartTotal(total) },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.padding(insets).consumeWindowInsets(insets).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedButton(
                    onClick = { pickerOpen = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = catalog != null,
                ) {
                    Text(stringResource(R.string.cashier_search))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.cashier_cart),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        stringResource(R.string.cashier_count, lines.size, unitCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            when {
                catalog == null -> item { CircularProgressIndicator() }
                lines.isEmpty() -> item {
                    Text(
                        stringResource(R.string.cashier_cart_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> items(lines, key = { it.product.id }) { line ->
                    CartLineCard(
                        line = line,
                        rupiah = rupiah,
                        onDecrease = { decrement(line.product.id) },
                        onIncrease = { increment(line.product.id) },
                        onRemove = { quantities.remove(line.product.id) },
                    )
                }
            }
        }
    }

    if (pickerOpen) {
        ProductPicker(
            catalog = catalog.orEmpty(),
            rupiah = rupiah,
            onDismiss = { pickerOpen = false },
            onPick = {
                increment(it.id)
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun CartTotal(total: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.cashier_total), Modifier.weight(1f))
            Text(
                stringResource(R.string.cashier_amount, total),
                modifier = Modifier.weight(2f),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun CartLineCard(
    line: CartLine,
    rupiah: NumberFormat,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onRemove: () -> Unit,
) {
    val product = line.product
    val colors = MaterialTheme.colorScheme
    val remove = stringResource(R.string.cashier_remove_product, product.name)
    val decrease = stringResource(R.string.cashier_decrease, product.name)
    val increase = stringResource(R.string.cashier_increase, product.name)
    val subtotal = remember(line.subtotal, rupiah) { rupiah.format(line.subtotal) }
    OutlinedCard(colors = CardDefaults.outlinedCardColors(containerColor = colors.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { ProductSummary(product, rupiah) }
                TextButton(onClick = onRemove, modifier = Modifier.semantics { contentDescription = remove }) {
                    Text(stringResource(R.string.cashier_remove), color = colors.error)
                }
            }
            HorizontalDivider(color = colors.outlineVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedIconButton(
                    onClick = onDecrease,
                    enabled = line.quantity > 1,
                    modifier = Modifier.semantics { contentDescription = decrease },
                ) { Text("−") }
                Text(line.quantity.toString())
                OutlinedIconButton(
                    onClick = onIncrease,
                    enabled = line.quantity < Int.MAX_VALUE,
                    modifier = Modifier.semantics { contentDescription = increase },
                ) { Text("+") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.cashier_subtotal), style = MaterialTheme.typography.labelMedium)
                    Text(
                        stringResource(R.string.cashier_amount, subtotal),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductPicker(
    catalog: List<Product>,
    rupiah: NumberFormat,
    onDismiss: () -> Unit,
    onPick: (Product) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val matching = remember(catalog, query) {
        val term = query.trim()
        if (term.isEmpty()) catalog else catalog.filter { it.name.contains(term, ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.cashier_search)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (catalog.isEmpty()) {
                    Text(stringResource(R.string.cashier_no_products))
                } else {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.cashier_search_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (matching.isEmpty()) {
                        Text(stringResource(R.string.cashier_no_matches))
                    } else {
                        LazyColumn(Modifier.heightIn(max = 256.dp)) {
                            items(matching, key = { it.id }) { product ->
                                val add = stringResource(R.string.cashier_add_product, product.name)
                                Row(
                                    Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) { ProductSummary(product, rupiah) }
                                    TextButton(
                                        onClick = { onPick(product) },
                                        modifier = Modifier.semantics { contentDescription = add },
                                    ) { Text(stringResource(R.string.cashier_add)) }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cashier_close_picker)) }
        },
    )
}

@Composable
private fun ProductSummary(product: Product, rupiah: NumberFormat) {
    val price = remember(product.priceRupiah, rupiah) { rupiah.format(product.priceRupiah) }
    Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Text(
        stringResource(R.string.cashier_unit_price, price),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        stringResource(R.string.cashier_stock, product.stock),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
}
