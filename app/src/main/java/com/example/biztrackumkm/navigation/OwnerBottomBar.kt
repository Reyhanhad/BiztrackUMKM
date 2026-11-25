package com.example.biztrackumkm.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.biztrackumkm.R

internal enum class OwnerTab(@get:StringRes val label: Int, @get:DrawableRes val icon: Int) {
    Home(R.string.nav_home, R.drawable.ic_nav_home),
    Inventory(R.string.dashboard_inventory, R.drawable.ic_nav_inventory),
    Cashier(R.string.cashier_open, R.drawable.ic_nav_cashier),
    History(R.string.receipt_history_action, R.drawable.ic_nav_history),
    Report(R.string.nav_report, R.drawable.ic_nav_report),
}

@Composable
internal fun OwnerBottomBar(selected: OwnerTab, enabled: Boolean, onSelect: (OwnerTab) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            HorizontalDivider(color = colors.outlineVariant)
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                for (tab in OwnerTab.entries) {
                    val active = tab == selected
                    val tint = if (active) colors.primary else colors.onSurfaceVariant
                    Column(
                        modifier = Modifier.weight(1f).heightIn(min = 60.dp)
                            .selectable(selected = active, enabled = enabled, role = Role.Tab, onClick = { onSelect(tab) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(width = 30.dp, height = 3.dp).background(if (active) tint else Color.Transparent))
                        Column(
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(painterResource(tab.icon), contentDescription = null, modifier = Modifier.size(22.dp), tint = tint)
                            Text(
                                stringResource(tab.label),
                                color = tint,
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}
