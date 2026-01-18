package com.devhjs.mathgraphstudy.presentation.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGold,
    primaryContainer = PrimaryGoldVariant,
    secondary = BlueAccent,
    tertiary = GreyAccent, // Use Grey for tertiary or keep Cyan if preferred, but user said Grey for secondary accents
    background = BlackCharcoal,
    surface = SurfaceCard,
    surfaceVariant = SurfaceCard, // or slightly lighter? Keep consistent for now
    onPrimary = BlackCharcoal, // Gold is bright, so text on it should be dark
    onSecondary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Red500,
    outline = BorderColor,
    outlineVariant = BorderColor // Also useful for dividers often
)

// Use the same dark scheme for light mode to enforce the unified dark design
private val LightColorScheme = DarkColorScheme

@Composable
fun MathGraphStudyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    // Disable dynamic color to enforce our custom theme
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