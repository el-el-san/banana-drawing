package com.example.bdrowclient.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 2025 Modern Dark Theme - Soft & Cute
private val DarkColorScheme = darkColorScheme(
    primary = BubblegumPink,
    onPrimary = CloudWhite,
    primaryContainer = HotPink,
    onPrimaryContainer = CloudWhite,
    secondary = DeepLavender,
    onSecondary = CloudWhite,
    secondaryContainer = ElectricPurple,
    onSecondaryContainer = CloudWhite,
    tertiary = GoldenYellow,
    onTertiary = AlmostBlack,
    background = AlmostBlack,
    onBackground = CloudWhite,
    surface = DarkGray,
    onSurface = CloudWhite,
    surfaceVariant = DarkGray,
    onSurfaceVariant = SoftGray,
    error = ErrorRed,
    onError = CloudWhite
)

// 2025 Modern Light Theme - Vibrant & Playful
private val LightColorScheme = lightColorScheme(
    primary = BananaYellow,
    onPrimary = AlmostBlack,
    primaryContainer = PastelYellow,
    onPrimaryContainer = AlmostBlack,
    secondary = CottonCandy,
    onSecondary = AlmostBlack,
    secondaryContainer = PastelPink,
    onSecondaryContainer = AlmostBlack,
    tertiary = Lavender,
    onTertiary = AlmostBlack,
    tertiaryContainer = LavenderLight,
    onTertiaryContainer = AlmostBlack,
    background = CloudWhite,
    onBackground = AlmostBlack,
    surface = MilkWhite,
    onSurface = AlmostBlack,
    surfaceVariant = PastelYellow.copy(alpha = 0.3f),
    onSurfaceVariant = DarkGray,
    error = ErrorRed,
    onError = CloudWhite,
    outline = SoftGray
)

@Composable
fun BdrowClientTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
        shapes = ModernShapes,
        content = content
    )
}
