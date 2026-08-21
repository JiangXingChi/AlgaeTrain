package com.rhodes.algae.ui.theme

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rhodes.algae.R

// 像素字体（Zpix 12px，中文点阵字体，OFL 免费开源，作者 SolidZORO）
val PixelFont = FontFamily(Font(R.font.zpix))

// 像素风形状：全部小直角，还原 8-bit 硬边感
private val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

// 像素风排版：全局 Zpix 像素字体
private val PixelTypography = Typography(
    displayLarge = TextStyle(fontFamily = PixelFont, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = PixelFont, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = PixelFont, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = PixelFont, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = PixelFont, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontFamily = PixelFont, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontFamily = PixelFont, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = PixelFont, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
    titleSmall = TextStyle(fontFamily = PixelFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    bodyLarge = TextStyle(fontFamily = PixelFont, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = PixelFont, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = PixelFont, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = PixelFont, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontFamily = PixelFont, fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
    labelSmall = TextStyle(fontFamily = PixelFont, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold),
)

// 像素风配色 — Game Boy 复古色板（浅色：纸白底 + 墨绿 + 金黄）
private val LightScheme = lightColorScheme(
    primary = Color(0xFF2E7D52),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF0D2B17),
    secondary = Color(0xFFD4A853),
    onSecondary = Color(0xFF3E2A00),
    secondaryContainer = Color(0xFFFFF3CD),
    tertiary = Color(0xFF4A7C96),
    surface = Color(0xFFFFFBF2),
    surfaceVariant = Color(0xFFEFF4E6),
    background = Color(0xFFF2ECDD),
    error = Color(0xFFC62828),
    outline = Color(0xFF4A5549),
)

// 像素风配色（暗色：Game Boy Pocket 深色底）
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF0D2B17),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFD4EDDA),
    secondary = Color(0xFFFFD54F),
    onSecondary = Color(0xFF3E2A00),
    secondaryContainer = Color(0xFF5D4000),
    tertiary = Color(0xFF90CAF9),
    surface = Color(0xFF1B1E1A),
    surfaceVariant = Color(0xFF2A2E28),
    background = Color(0xFF12140F),
    error = Color(0xFFEF9A9A),
    outline = Color(0xFF9AA393),
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
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content,
    )
}
