package com.example.unitv

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

private val UnitvDarkColors = darkColorScheme(
    primary = Color(0xFFE6B85C),
    onPrimary = Color(0xFF241A08),
    secondary = Color(0xFFE6A0A8),
    onSecondary = Color(0xFF351018),
    background = Color(0xFF16060A),
    onBackground = Color(0xFFF7EEF0),
    surface = Color(0xFF2A0B12),
    onSurface = Color(0xFFF7EEF0),
    surfaceVariant = Color(0xFF4A1822),
    onSurfaceVariant = Color(0xFFD9C2C6),
    error = Color(0xFFFFB4AB)
)

private val UnitvTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

@Composable
fun UnitvTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = UnitvDarkColors,
        typography = UnitvTypography,
        content = content
    )
}
