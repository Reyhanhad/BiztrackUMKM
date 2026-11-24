package com.example.biztrackumkm.inventory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.biztrackumkm.R
import java.text.NumberFormat
import java.util.Locale

private val keypadRows = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("00", "0", "000"),
)

@Composable
internal fun RupiahPriceField(value: String, onChange: (String) -> Unit, enabled: Boolean, isError: Boolean) {
    var keypadOpen by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val bringIntoView = remember { BringIntoViewRequester() }
    val colors = MaterialTheme.colorScheme
    val formatter = remember { NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")) }
    val amount = remember(value) { value.toLongOrNull()?.let(formatter::format) }

    LaunchedEffect(isError) {
        if (isError) bringIntoView.bringIntoView()
    }
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(bringIntoView)) {
        OutlinedCard(
            onClick = {
                focus.clearFocus()
                keyboard?.hide()
                keypadOpen = true
            },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, if (isError) colors.error else colors.outline),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    stringResource(R.string.inventory_price),
                    color = if (isError) colors.error else colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    if (amount == null) stringResource(R.string.inventory_price_enter)
                    else stringResource(R.string.inventory_price_display, amount),
                    color = if (amount == null) colors.onSurfaceVariant else colors.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        if (isError) {
            Text(
                stringResource(R.string.inventory_invalid_price),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = colors.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
    if (keypadOpen) {
        RupiahKeypad(
            value = value,
            formatter = formatter,
            onDismiss = { keypadOpen = false },
            onConfirm = { price ->
                if (price != value) onChange(price)
                keypadOpen = false
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RupiahKeypad(
    value: String,
    formatter: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(value) }
    var tooLarge by rememberSaveable { mutableStateOf(false) }
    val amount = remember(draft) { formatter.format(draft.toLongOrNull() ?: 0L) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.inventory_price), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.inventory_price_display, amount), style = MaterialTheme.typography.headlineMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(
                    onClick = {
                        draft = ""
                        tooLarge = false
                    },
                    enabled = draft.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.inventory_price_clear))
                }
                TextButton(
                    onClick = {
                        draft = draft.dropLast(1)
                        tooLarge = false
                    },
                    enabled = draft.isNotEmpty(),
                ) {
                    Text(stringResource(R.string.inventory_price_backspace))
                }
            }
            for (row in keypadRows) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (digits in row) {
                        OutlinedButton(
                            onClick = {
                                val next = (draft + digits).toLongOrNull()
                                tooLarge = next == null
                                if (next != null) draft = next.toString()
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            Text(digits, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
            if (tooLarge) {
                Text(stringResource(R.string.inventory_price_too_large), color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = { onConfirm(draft) }, enabled = !tooLarge, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.inventory_price_done))
            }
        }
    }
}
