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
    secondary = Color(0xFF9CC7FF),
    onSecondary = Color(0xFF0B1C30),
    background = Color(0xFF0A0C12),
    onBackground = Color(0xFFF3F4F8),
    surface = Color(0xFF141821),
    onSurface = Color(0xFFF3F4F8),
    surfaceVariant = Color(0xFF252B38),
    onSurfaceVariant = Color(0xFFBEC5D2),
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
