package com.example.biztrackumkm

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.biztrackumkm.auth.AuthViewModel
import com.example.biztrackumkm.cashier.CheckoutViewModel
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.navigation.AppNavigation
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme

class MainActivity : ComponentActivity() {

    private val inventory: InventoryViewModel by viewModels {
        viewModelFactory {
            initializer {
                InventoryViewModel(
                    ProductDatabase.get(application).products()
                )
            }
        }
    }

    private val checkout: CheckoutViewModel by viewModels {
        viewModelFactory { initializer { CheckoutViewModel(ProductDatabase.get(application).sales()) } }
    }

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        setContent {
            BiztrackUMKMTheme {
                AppNavigation(
                    authViewModel = authViewModel,
                    inventoryViewModel = inventory,
                    checkoutViewModel = checkout,
                )
            }
        }
    }
}
