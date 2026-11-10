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
    onPrimary = Color(0xFF080C16),
    primaryContainer = ElectricIndigoContainer,
    onPrimaryContainer = ElectricIndigoBright,
    secondary = NocturnalAmber,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = NocturnalAmberContainer,
    onSecondaryContainer = NocturnalAmberSoft,
    tertiary = MoonlightCyan,
    onTertiary = Color(0xFF041E30),
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
    outlineVariant = Color(0xFF1E293B),
    error = ConflictWarningRed,
    onError = Color(0xFF2D0610)
)

val NightShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // NightPlanner is purposefully crafted with a native nocturnal dark aesthetic
    // optimized for low eye strain during night planning sessions.
    MaterialTheme(
        colorScheme = NocturnalDarkColorScheme,
        typography = Typography,
        shapes = NightShapes,
        content = content
    )
}
