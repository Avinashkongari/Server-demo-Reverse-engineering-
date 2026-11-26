package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val AxionDarkColorScheme = darkColorScheme(
    primary = AxionRed,
    onPrimary = AxionTextPrimary,
    primaryContainer = AxionRedContainer,
    onPrimaryContainer = AxionOnRedContainer,
    secondary = AxionCyan,
    onSecondary = AxionBlack,
    secondaryContainer = AxionCyanContainer,
    onSecondaryContainer = AxionTextPrimary,
    tertiary = AxionAmber,
    onTertiary = AxionBlack,
    tertiaryContainer = AxionAmberContainer,
    onTertiaryContainer = AxionTextPrimary,
    background = AxionBlack,
    onBackground = AxionTextPrimary,
    surface = AxionSurface,
    onSurface = AxionTextPrimary,
    surfaceVariant = AxionSurfaceElevated,
    onSurfaceVariant = AxionTextSecondary,
    surfaceContainerLowest = AxionBlack,
    surfaceContainerLow = AxionObsidian,
    surfaceContainer = AxionSurface,
    surfaceContainerHigh = AxionSurfaceElevated,
    surfaceContainerHighest = AxionSurfaceHigh,
    outline = AxionBorder,
    outlineVariant = AxionBorderStrong,
    error = AxionError,
    onError = AxionTextPrimary,
    errorContainer = AxionErrorContainer,
    onErrorContainer = AxionOnRedContainer
)

val AxionShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun AxionStreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AxionDarkColorScheme,
        typography = AxionTypography,
        shapes = AxionShapes,
        content = content
    )
}
