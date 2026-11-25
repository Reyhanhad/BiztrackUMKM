package com.example.biztrackumkm.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.biztrackumkm.R
import com.example.biztrackumkm.cashier.SaleDao
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale

private val dateInput = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT)
    .withResolverStyle(ResolverStyle.STRICT)

private fun parseDate(text: String): LocalDate? = try {
    LocalDate.parse(text, dateInput).takeIf { it.year in 1..9999 }
} catch (_: DateTimeParseException) {
    null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SalesReportScreen(dao: SaleDao, onBack: () -> Unit) {
    val today = LocalDate.now()
    var firstDay by rememberSaveable { mutableLongStateOf(today.withDayOfMonth(1).toEpochDay()) }
    var lastDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var editing by rememberSaveable { mutableStateOf(false) }
    val first = LocalDate.ofEpochDay(firstDay)
    val last = LocalDate.ofEpochDay(lastDay)
    val zone = ZoneId.systemDefault()
    val locale = remember { Locale.forLanguageTag("id-ID") }
    val dateLabel = remember { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
    val number = remember { NumberFormat.getIntegerInstance(locale) }

    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(R.string.report_title)) },
            navigationIcon = {
                TextButton(onClick = onBack) { Text(stringResource(R.string.inventory_back)) }
            },
        )
    }) { padding ->
        // Reset both the list position and collected data when a new period is applied.
        key(first, last, zone) {
            val source = remember(dao) { observeSalesReport(dao, first, last, zone) }
            val report by source.collectAsStateWithLifecycle(initialValue = null)
            val result = report
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    OutlinedCard(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.report_change_period), color = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.report_period_label, first.format(dateLabel), last.format(dateLabel)),
                                style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (result == null) {
                    item { CircularProgressIndicator() }
                } else {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(stringResource(R.string.report_revenue), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(R.string.cashier_amount, number.format(result.revenue)),
                                    style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                                HorizontalDivider()
                                Text(stringResource(R.string.report_transactions, number.format(result.transactionCount)))
                            }
                        }
                    }
                    item { Text(stringResource(R.string.report_ranking), style = MaterialTheme.typography.titleLarge) }
                    if (result.ranking.isEmpty()) {
                        item { Text(stringResource(if (result.transactionCount == 0) R.string.report_empty else R.string.report_no_items)) }
                    }
                    itemsIndexed(result.ranking, key = { _, product -> product.id }) { index, product ->
                        ListItem(
                            leadingContent = { Text((index + 1).toString(), color = MaterialTheme.colorScheme.primary) },
                            headlineContent = { Text(product.name) },
                            trailingContent = { Text(stringResource(R.string.report_units, number.format(product.units))) },
                        )
                    }
                }
            }
        }
    }
    if (editing) {
        PeriodDialog(first, last, onDismiss = { editing = false }) { start, end ->
            firstDay = start.toEpochDay()
            lastDay = end.toEpochDay()
            editing = false
        }
    }
}

@Composable
private fun PeriodDialog(first: LocalDate, last: LocalDate, onDismiss: () -> Unit, onApply: (LocalDate, LocalDate) -> Unit) {
    var fromText by rememberSaveable { mutableStateOf(first.format(dateInput)) }
    var toText by rememberSaveable { mutableStateOf(last.format(dateInput)) }
    val start = parseDate(fromText)
    val end = parseDate(toText)
    val reversed = start != null && end != null && end < start
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_change_period)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.report_date_hint))
                OutlinedTextField(value = fromText, onValueChange = { fromText = it },
                    label = { Text(stringResource(R.string.report_start)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    isError = fromText.length >= 10 && start == null)
                OutlinedTextField(value = toText, onValueChange = { toText = it },
                    label = { Text(stringResource(R.string.report_end)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    isError = reversed || (toText.length >= 10 && end == null))
                if (reversed) Text(stringResource(R.string.report_reversed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = start != null && end != null && !reversed,
                onClick = { if (start != null && end != null) onApply(start, end) }) {
                Text(stringResource(R.string.report_apply))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}
