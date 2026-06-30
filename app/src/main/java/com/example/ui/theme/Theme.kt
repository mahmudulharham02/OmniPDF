package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val PitchDarkColorScheme = darkColorScheme(
    primary = PitchDarkPrimary,
    onPrimary = PitchDarkBackground,
    primaryContainer = PitchDarkSurface,
    onPrimaryContainer = PitchDarkTextPrimary,
    secondary = PitchDarkTextSecondary,
    onSecondary = PitchDarkBackground,
    background = PitchDarkBackground,
    onBackground = PitchDarkTextPrimary,
    surface = PitchDarkSurface,
    onSurface = PitchDarkTextPrimary,
    surfaceVariant = PitchDarkSurface,
    onSurfaceVariant = PitchDarkTextSecondary,
    outline = PitchDarkTextSecondary
)

private val ClassicLightColorScheme = lightColorScheme(
    primary = ClassicLightPrimary,
    onPrimary = ClassicLightSurface,
    primaryContainer = ClassicLightSurface,
    onPrimaryContainer = ClassicLightTextPrimary,
    secondary = ClassicLightTextSecondary,
    onSecondary = ClassicLightSurface,
    background = ClassicLightBackground,
    onBackground = ClassicLightTextPrimary,
    surface = ClassicLightSurface,
    onSurface = ClassicLightTextPrimary,
    surfaceVariant = ClassicLightSurface,
    onSurfaceVariant = ClassicLightTextSecondary,
    outline = ClassicLightTextSecondary
)

@Composable
fun MyApplicationTheme(
    isPitchDark: Boolean = false,
    useMonospace: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isPitchDark) PitchDarkColorScheme else ClassicLightColorScheme
    val typography = if (useMonospace) MonospacedTypography else SansSerifTypography

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
