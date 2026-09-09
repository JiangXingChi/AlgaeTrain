package com.rhodes.algae.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

// 现代简约配色 — 白底 + 墨绿主色 + 中性灰
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E7D52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD4EDDA),
    onPrimaryContainer = Color(0xFF0D2B17),
    secondary = Color(0xFF6B7B6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EDE9),
    onSecondaryContainer = Color(0xFF1F2A22),
    tertiary = Color(0xFF4A7C96),
    surface = Color.White,
    surfaceVariant = Color(0xFFF4F6F4),
    background = Color(0xFFFAFBF9),
    error = Color(0xFFC62828),
    outline = Color(0xFF9AA39C),
)

// 现代简约配色（暗色）
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D2B17),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFD4EDDA),
    secondary = Color(0xFFB0BDB4),
    onSecondary = Color(0xFF1F2A22),
    secondaryContainer = Color(0xFF2E3A32),
    onSecondaryContainer = Color(0xFFD4EDDA),
    tertiary = Color(0xFF90CAF9),
    surface = Color(0xFF171A17),
    surfaceVariant = Color(0xFF222622),
    background = Color(0xFF12140F),
    error = Color(0xFFEF9A9A),
    outline = Color(0xFF8A9386),
)

object ThemeState {
    enum class Mode { Auto, Light, Dark }
    var mode by mutableStateOf(Mode.Auto)
        private set
    // 全局字体缩放（1.0 = 标准；作用于所有 sp 字号）
    var fontScale by mutableStateOf(1f)
        private set
    private lateinit var prefs: android.content.SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("algae_theme", Context.MODE_PRIVATE)
        mode = when (prefs.getString("theme_mode", "auto")) {
            "light" -> Mode.Light
            "dark"  -> Mode.Dark
            else   -> Mode.Auto
        }
        fontScale = prefs.getFloat("font_scale", 1f)
    }

    fun apply(m: Mode) {
        mode = m
        prefs.edit().putString("theme_mode", when (m) {
            Mode.Auto  -> "auto"
            Mode.Light -> "light"
            Mode.Dark  -> "dark"
        }).apply()
    }

    fun applyFontScale(s: Float) {
        fontScale = s
        prefs.edit().putFloat("font_scale", s).apply()
    }
}

@Composable
fun AlgaeTheme(content: @Composable () -> Unit) {
    val dark = when (ThemeState.mode) {
        ThemeState.Mode.Auto -> isSystemInDarkTheme()
        ThemeState.Mode.Light -> false
        ThemeState.Mode.Dark -> true
    }
    // 全局字体缩放：重写 LocalDensity.fontScale（与系统字体缩放相乘，不覆盖用户的系统设置）
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * ThemeState.fontScale)
    ) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            content = content,
        )
    }
}