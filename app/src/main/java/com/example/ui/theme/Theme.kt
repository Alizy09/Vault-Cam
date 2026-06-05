package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Helper
private val ColorSlateValue = androidx.compose.ui.graphics.Color(0xFF030712)

private val VaultColorScheme = darkColorScheme(
    primary = PrimaryCyberGreen,
    secondary = SecondaryDarkBlue,
    tertiary = AccentGold,
    background = DarkBackground,
    surface = SurfaceObsidian,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    onPrimary = ColorSlateValue,
    error = WarningRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VaultColorScheme,
        typography = Typography,
        content = content
    )
}
