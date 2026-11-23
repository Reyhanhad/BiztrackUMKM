package com.example.biztrackumkm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BrandColors = darkColorScheme(
    primary = Orange,
    onPrimary = Ink,
    background = Navy,
    onBackground = Ice,
    surface = Navy,
    onSurface = Ice,
    surfaceContainer = NavyCard,
    onSurfaceVariant = Slate,
    outline = Steel,
    outlineVariant = SteelMuted,
    error = Coral,
)

@Composable
fun BiztrackUMKMTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = BrandColors, typography = Typography, content = content)
}