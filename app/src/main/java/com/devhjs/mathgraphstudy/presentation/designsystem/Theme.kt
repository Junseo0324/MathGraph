package com.devhjs.mathgraphstudy.presentation.designsystem

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private fun AppColorScheme.toMaterialScheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primaryGold,
        primaryContainer = primaryGoldVariant,
        onPrimary = onPrimary,
        secondary = blueAccent,
        onSecondary = textPrimary,
        background = background,
        onBackground = textPrimary,
        surface = surfaceCard,
        onSurface = textPrimary,
        surfaceVariant = surfaceCard,
        onSurfaceVariant = textSecondary,
        error = red500,
        outline = borderColor,
        outlineVariant = borderColor
    )
}

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

        darkTheme -> DarkAppColors.toMaterialScheme(dark = true)
        else -> LightAppColors.toMaterialScheme(dark = false)
    }

    CompositionLocalProvider(LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}