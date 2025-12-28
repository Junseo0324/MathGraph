package com.devhjs.mathgraphstudy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue500,
    secondary = Cyan400,
    tertiary = Cyan400,
    background = Slate950,
    surface = Slate900,
    surfaceVariant = Slate800,
    onPrimary = Gray200,
    onSecondary = Slate950,
    onBackground = Gray200,
    onSurface = Gray200,
    error = Red500
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