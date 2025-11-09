package com.example.trektimer.ui.theme

import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = TrekBluePrimary,
    secondary = TrekBlueSecondary,
    tertiary = TrekOrange,
    background = TrekBackground,
    surface = TrekSurface,
    onPrimary = TrekOnPrimary,
    onSecondary = Color.White,
    onBackground = TrekOnBackground,
    onSurface = TrekOnBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = TrekBluePrimaryDark,
    secondary = TrekBlueSecondaryDark,
    tertiary = TrekOrange,
    background = TrekBackgroundDark,
    surface = TrekSurfaceDark,
    onPrimary = TrekOnPrimaryDark,
    onSecondary = TrekOnPrimaryDark,
    onBackground = TrekOnBackgroundDark,
    onSurface = TrekOnBackgroundDark
)

@Composable
fun TrekTimerTheme(
    darkTheme: Boolean = true, // ✅ default dark mode
    dynamicColor: Boolean = false, // ✅ consistent look, no wallpaper colors
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
