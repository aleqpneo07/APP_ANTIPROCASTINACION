package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NocturnalDarkColorScheme = darkColorScheme(
    primary = ElectricIndigo,
    onPrimary = Color(0xFF090B10),
    primaryContainer = ElectricIndigoContainer,
    onPrimaryContainer = ElectricIndigoBright,
    secondary = NocturnalAmber,
    onSecondary = Color(0xFF141008),
    secondaryContainer = NocturnalAmberContainer,
    onSecondaryContainer = NocturnalAmberSoft,
    tertiary = MoonlightCyan,
    onTertiary = Color(0xFF07141E),
    tertiaryContainer = MoonlightCyanContainer,
    onTertiaryContainer = Color(0xFFBAE6FD),
    background = NightBgDeep,
    onBackground = TextMoonlight,
    surface = NightBgSurface,
    onSurface = TextMoonlight,
    surfaceVariant = NightCardSurface,
    onSurfaceVariant = TextStarlight,
    surfaceContainerHigh = NightCardElevated,
    outline = NightBorderSubtle,
    outlineVariant = Color(0xFF222836),
    error = ConflictWarningRed,
    onError = Color(0xFF240A0F)
)

val NightShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NocturnalDarkColorScheme,
        typography = Typography,
        shapes = NightShapes,
        content = content
    )
}
