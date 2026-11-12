package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.ColorPaletteSeed
import com.example.model.ThemeMode

fun buildCustomColorScheme(
    seed: ColorPaletteSeed,
    isDark: Boolean,
    isAmoled: Boolean
): ColorScheme {
    val (primary, secondary, tertiary) = when (seed) {
        ColorPaletteSeed.LAVENDER -> Triple(LavenderPrimary, LavenderSecondary, LavenderTertiary)
        ColorPaletteSeed.OCEAN_BLUE -> Triple(OceanPrimary, OceanSecondary, OceanTertiary)
        ColorPaletteSeed.BOTANICAL_SAGE -> Triple(SagePrimary, SageSecondary, SageTertiary)
        ColorPaletteSeed.SUNSET_AMBER -> Triple(SunsetPrimary, SunsetSecondary, SunsetTertiary)
        ColorPaletteSeed.ROSE_BERRY -> Triple(RosePrimary, RoseSecondary, RoseTertiary)
        ColorPaletteSeed.MINIMAL_MONO -> Triple(MonoPrimary, MonoSecondary, MonoTertiary)
    }

    return if (isDark) {
        val bg = if (isAmoled) AmoledBg else DarkBg
        val surface = if (isAmoled) AmoledSurface else DarkSurface
        val surfaceVariant = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant

        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.25f),
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = secondary.copy(alpha = 0.2f),
            onSecondaryContainer = Color.White,
            tertiary = tertiary,
            onTertiary = Color.Black,
            background = bg,
            onBackground = Color(0xFFF0F1F5),
            surface = surface,
            onSurface = Color(0xFFECEFF4),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFB0B3C0),
            outline = Color(0xFF383B47)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = secondary.copy(alpha = 0.15f),
            onSecondaryContainer = secondary,
            tertiary = tertiary,
            background = LightBg,
            onBackground = Color(0xFF1E2024),
            surface = LightSurface,
            onSurface = Color(0xFF1A1C20),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF5A5E6B),
            outline = Color(0xFFD0D3DE)
        )
    }
}

@Composable
fun AuraLauncherTheme(
    themeMode: ThemeMode = ThemeMode.MINIMAL_DARK,
    paletteSeed: ColorPaletteSeed = ColorPaletteSeed.LAVENDER,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.MINIMAL_DARK -> true
        ThemeMode.AMOLED_BLACK -> true
        ThemeMode.CLEAN_LIGHT -> false
    }
    val isAmoled = themeMode == ThemeMode.AMOLED_BLACK

    val context = LocalContext.current
    val colorScheme = if (themeMode == ThemeMode.SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        buildCustomColorScheme(paletteSeed, isDark = isDark, isAmoled = isAmoled)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
