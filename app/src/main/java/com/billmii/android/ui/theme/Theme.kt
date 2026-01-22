package com.billmii.android.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * BillMii Theme - 应用主题
 * Material Design 3 theme for BillMii application
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00695C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9DFBCF),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF4A635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E3),
    onSecondaryContainer = Color(0xFF05201C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFAFDFB),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFAFDFB),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFDAE5E1),
    onSurfaceVariant = Color(0xFF3F4946),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C5),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E3130),
    inverseOnSurface = Color(0xFFF0F1EF),
    inversePrimary = Color(0xFF81D5B4)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF65D899),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005147),
    onPrimaryContainer = Color(0xFF9DFBCF),
    secondary = Color(0xFFB0CCC7),
    onSecondary = Color(0xFF1B3531),
    secondaryContainer = Color(0xFF324B47),
    onSecondaryContainer = Color(0xFFCCE8E3),
    tertiary = Color(0xFFB0C9E2),
    onTertiary = Color(0xFF19324D),
    tertiaryContainer = Color(0xFF2D4865),
    onTertiaryContainer = Color(0xFFCCE5FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF191C1B),
    onBackground = Color(0xFFE1E3E1),
    surface = Color(0xFF191C1B),
    onSurface = Color(0xFFE1E3E1),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4946),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE1E3E1),
    inverseOnSurface = Color(0xFF191C1B),
    inversePrimary = Color(0xFF00695C)
)

@Composable
fun BillMiiTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}