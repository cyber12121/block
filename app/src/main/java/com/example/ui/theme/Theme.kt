package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2E3568),
    onPrimaryContainer = Color(0xFFDDE1FF),
    secondary = CyanAccent,
    onSecondary = Color(0xFF052E26),
    secondaryContainer = Color(0xFF0E3B33),
    onSecondaryContainer = Color(0xFFCCFBF1),
    tertiary = AmberFocus,
    onTertiary = Color(0xFF3A2A0B),
    tertiaryContainer = Color(0xFF3C2E12),
    onTertiaryContainer = Color(0xFFFCEBC7),
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
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E3FF),
    onPrimaryContainer = Color(0xFF1B2250),
    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5F5EE),
    onSecondaryContainer = Color(0xFF042F2A),
    tertiary = Color(0xFF8A5A12),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCEBC7),
    onTertiaryContainer = Color(0xFF3A2A0B),
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
    // FocusGuard is designed as a dark "midnight" app; always use the dark
    // scheme so the UI is consistent regardless of system setting.
    darkTheme: Boolean = true,
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
