package com.rhodes.algae.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF3A7C6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F5F0),
    secondary = Color(0xFFD4A853),
    secondaryContainer = Color(0xFFFFF8E1),
    surface = Color(0xFFFFFCF5),
    background = Color(0xFFF5F0E8),
    onSurface = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFF666666),
)

@Composable
fun AlgaeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
