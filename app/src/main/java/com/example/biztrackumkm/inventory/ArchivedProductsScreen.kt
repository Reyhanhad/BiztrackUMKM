package com.example.biztrackumkm.inventory

import android.database.sqlite.SQLiteException
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchivedProductsScreen(viewModel: InventoryViewModel, onBack: () -> Unit) {
    val products by viewModel.archivedProducts.collectAsStateWithLifecycle<List<Product>?>(initialValue = null)
    val archived = products
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val restoreError = stringResource(R.string.inventory_restore_error)
    var restoring by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.inventory_archive_list)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text(stringResource(R.string.inventory_back)) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { insets ->
        LazyColumn(
            modifier = Modifier.padding(insets).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                archived == null -> item { CircularProgressIndicator() }
                archived.isEmpty() -> item { Text(stringResource(R.string.inventory_archive_empty)) }
                else -> items(archived, key = { it.id }) { product ->
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium)
                                product.barcode?.let {
                                    Text(
                                        stringResource(R.string.inventory_barcode_display, it),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                            TextButton(
                                enabled = !restoring,
                                onClick = {
                                    if (!restoring) {
                                        restoring = true
                                        scope.launch {
                                            try {
                                                viewModel.restoreArchivedProduct(product.id)
                                            } catch (error: SQLiteException) {
                                                Log.e("Inventory", "Gagal memulihkan produk", error)
                                                snackbar.showSnackbar(restoreError)
                                            } finally {
                                                restoring = false
                                            }
                                        }
                                    }
                                },
                            ) { Text(stringResource(R.string.inventory_restore)) }
                        }
                    }
                }
            }
        }
    }
}
