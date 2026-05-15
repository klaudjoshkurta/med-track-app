package com.shkurta.medtrack.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = SurfaceDark,
    onPrimaryContainer = White,
    secondary = OnSurfaceDark,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = SurfaceDark,
    onSurface = White,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceDark,
    outline = OutlineDark,
    error = White,
    onError = Black
)

private val LightColorScheme = lightColorScheme(
    primary = Black,
    onPrimary = White,
    primaryContainer = SurfaceLight,
    onPrimaryContainer = Black,
    secondary = OnSurfaceLight,
    onSecondary = White,
    background = White,
    onBackground = Black,
    surface = SurfaceLight,
    onSurface = Black,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceLight,
    outline = OutlineLight,
    error = Black,
    onError = White
)

@Composable
fun MedTrackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
