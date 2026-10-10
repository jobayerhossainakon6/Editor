package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = StudioCyan,
    onPrimary = Color(0xFF003549),
    primaryContainer = Color(0xFF004D69),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = StudioAmber,
    onSecondary = Color(0xFF452B00),
    secondaryContainer = Color(0xFF623F00),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = StudioPurple,
    background = StudioDarkBg,
    onBackground = DarkOnSurface,
    surface = StudioDarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = StudioDarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

private val LightColorScheme = DarkColorScheme // Video editing is best in dark studio mode

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
