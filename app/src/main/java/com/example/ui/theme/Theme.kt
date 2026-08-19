package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF111111),
    primaryContainer = Color(0xFF262626),
    onPrimaryContainer = Color(0xFFF5F5F5),
    secondary = Color(0xFFA3A3A3),
    onSecondary = Color(0xFF111111),
    secondaryContainer = Color(0xFF262626),
    onSecondaryContainer = Color(0xFFE5E5E5),
    tertiary = Color(0xFFD4D4D4),
    onTertiary = Color(0xFF111111),
    tertiaryContainer = Color(0xFF262626),
    onTertiaryContainer = Color(0xFFE5E5E5),
    error = CrimsonStrict,
    onError = Color.White,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF171717),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF4F4F5),
    onPrimaryContainer = Color(0xFF171717),
    secondary = Color(0xFF525252),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF171717),
    tertiary = Color(0xFF404040),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4F4F5),
    onTertiaryContainer = Color(0xFF171717),
    error = CrimsonStrict,
    onError = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant
)

@Composable
fun FocusGuardTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
