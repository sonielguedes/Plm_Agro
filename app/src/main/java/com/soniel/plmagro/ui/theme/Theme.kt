package com.soniel.plmagro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = Color.Black,
    secondary = DarkGreen,
    onSecondary = Color.White,
    tertiary = NeonGreen,
    background = BackgroundDark,
    surface = SurfaceDark,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

private val LightColorScheme = lightColorScheme(
    primary = DarkGreen, // No modo dia, o verde escuro fica mais legível como primário
    onPrimary = Color.White,
    secondary = DarkGreen,
    onSecondary = Color.White,
    tertiary = DarkGreen,
    background = BackgroundLight,
    surface = SurfaceLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
)

@Composable
fun PlmAgroTheme(
    isNightMode: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isNightMode) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
