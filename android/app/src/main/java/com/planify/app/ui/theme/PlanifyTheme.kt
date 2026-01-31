package com.planify.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalThemeColors = staticCompositionLocalOf { ThemeColors.default() }

@Composable
fun PlanifyTheme(themeState: ThemeState, content: @Composable () -> Unit) {
    val colors = themeState.theme
    val scheme = lightColorScheme(
        primary = colors.primary,
        secondary = colors.secondary,
        tertiary = colors.accent,
        background = colors.background,
        surface = colors.background,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = colors.text,
        onSurface = colors.text
    )

    CompositionLocalProvider(LocalThemeColors provides colors) {
        MaterialTheme(
            colorScheme = scheme,
            content = content
        )
    }
}
