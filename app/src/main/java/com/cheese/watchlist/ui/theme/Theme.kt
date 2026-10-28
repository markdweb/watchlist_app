package com.cheese.watchlist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PinkAccent = Color(0xFFEC4899)
private val OrangeAccent = Color(0xFFFF8A3D)
private val PurpleAccent = Color(0xFF8B5CF6)

private val LightColors = lightColorScheme(
    primary = PrimaryAccent,
    onPrimary = SurfaceWhite,

    secondary = PinkAccent,
    onSecondary = SurfaceWhite,

    tertiary = OrangeAccent,
    onTertiary = SurfaceWhite,

    background = BackgroundLight,
    onBackground = TextPrimary,

    surface = SurfaceWhite,
    onSurface = TextPrimary,

    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,

    outline = BorderSubtle,
    error = StatusDropped
)

private val DarkColors = darkColorScheme(
    primary = OrangeAccent,
    onPrimary = Color.White,

    secondary = PinkAccent,
    onSecondary = Color.White,

    tertiary = PurpleAccent,
    onTertiary = Color.White,

    background = BackgroundDark,
    onBackground = TextPrimaryDark,

    surface = SurfaceDark,
    onSurface = TextPrimaryDark,

    surfaceVariant = SurfaceDark,
    onSurfaceVariant = TextSecondaryDark,

    outline = BorderSubtleDark,
    error = StatusDropped
)

@Composable
fun WatchlistTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = WatchlistTypography,
        content = content
    )
}