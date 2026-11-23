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
import com.example.biztrackumkm.inventory.InventoryScreen
import com.example.biztrackumkm.inventory.InventoryViewModel
import com.example.biztrackumkm.inventory.ProductDatabase
import com.example.biztrackumkm.ui.theme.BiztrackUMKMTheme

class MainActivity : ComponentActivity() {
    private val inventory: InventoryViewModel by viewModels {
        viewModelFactory { initializer { InventoryViewModel(ProductDatabase.get(application).products()) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent { BiztrackUMKMTheme { InventoryScreen(inventory) } }
    }
}