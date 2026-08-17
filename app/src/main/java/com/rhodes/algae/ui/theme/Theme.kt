package com.rhodes.algae.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// 浅绿主色调 — 浮游生物主题
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E7D52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EDDA),
    onPrimaryContainer = Color(0xFF0D2B17),
    secondary = Color(0xFFD4A853),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF8E1),
    tertiary = Color(0xFF4A7C96),
    surface = Color(0xFFFFFCF5),
    surfaceVariant = Color(0xFFF0F4E8),
    background = Color(0xFFF5F0E8),
    error = Color(0xFFC62828),
    outline = Color(0xFF6B7B6E),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D2B17),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFD4EDDA),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF3E2A00),
    secondaryContainer = Color(0xFF5D4000),
    tertiary = Color(0xFF90CAF9),
    surface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFF2A2D28),
    background = Color(0xFF121412),
    error = Color(0xFFEF9A9A),
    outline = Color(0xFF8A9386),
)

object ThemeState {
    enum class Mode { Auto, Light, Dark }
    var mode by mutableStateOf(Mode.Auto)
        private set
    private lateinit var prefs: android.content.SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("algae_theme", Context.MODE_PRIVATE)
        mode = when (prefs.getString("theme_mode", "auto")) {
            "light" -> Mode.Light
            "dark"  -> Mode.Dark
            else   -> Mode.Auto
        }
    }

    fun apply(m: Mode) {
        mode = m
        prefs.edit().putString("theme_mode", when (m) {
            Mode.Auto  -> "auto"
            Mode.Light -> "light"
            Mode.Dark  -> "dark"
        }).apply()
    }
}

@Composable
fun AlgaeTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeState.mode) {
        ThemeState.Mode.Auto -> isSystemInDarkTheme()
        ThemeState.Mode.Light -> false
        ThemeState.Mode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content,
    )
}
