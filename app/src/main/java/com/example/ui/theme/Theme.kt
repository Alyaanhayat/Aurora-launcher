package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.ColorPaletteSeed
import com.example.model.ThemeMode

@Composable
fun isLauncherDarkTheme(themeMode: ThemeMode): Boolean {
    val systemInDark = isSystemInDarkTheme()
    return when (themeMode) {
        ThemeMode.SYSTEM -> systemInDark
        ThemeMode.MINIMAL_DARK -> true
        ThemeMode.AMOLED_BLACK -> true
        ThemeMode.CLEAN_LIGHT -> false
    }
}

fun buildCustomColorScheme(
    seed: ColorPaletteSeed,
    isDark: Boolean,
    isAmoled: Boolean
): ColorScheme {
    return if (isDark) {
        val (primary, secondary, tertiary) = when (seed) {
            ColorPaletteSeed.LAVENDER -> Triple(LavenderDarkPrimary, LavenderDarkSecondary, LavenderDarkTertiary)
            ColorPaletteSeed.OCEAN_BLUE -> Triple(OceanDarkPrimary, OceanDarkSecondary, OceanDarkTertiary)
            ColorPaletteSeed.BOTANICAL_SAGE -> Triple(SageDarkPrimary, SageDarkSecondary, SageDarkTertiary)
            ColorPaletteSeed.SUNSET_AMBER -> Triple(SunsetDarkPrimary, SunsetDarkSecondary, SunsetDarkTertiary)
            ColorPaletteSeed.ROSE_BERRY -> Triple(RoseDarkPrimary, RoseDarkSecondary, RoseDarkTertiary)
            ColorPaletteSeed.MINIMAL_MONO -> Triple(MonoDarkPrimary, MonoDarkSecondary, MonoDarkTertiary)
        }
        val onPrimary = if (seed == ColorPaletteSeed.MINIMAL_MONO) Color(0xFF121417) else Color.White
        val bg = if (isAmoled) AmoledBg else DarkBg
        val surface = if (isAmoled) AmoledSurface else DarkSurface
        val surfaceVariant = if (isAmoled) AmoledSurfaceVariant else DarkSurfaceVariant

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = lerp(surface, primary, 0.26f),
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color(0xFF101216),
            secondaryContainer = lerp(surface, secondary, 0.24f),
            onSecondaryContainer = Color.White,
            tertiary = tertiary,
            onTertiary = Color(0xFF101216),
            tertiaryContainer = lerp(surface, tertiary, 0.24f),
            onTertiaryContainer = Color.White,
            background = bg,
            onBackground = Color(0xFFF0F1F5),
            surface = surface,
            onSurface = Color(0xFFECEFF4),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFB0B3C0),
            outline = Color(0xFF383B47),
            outlineVariant = Color(0xFF2B2E38),
            error = Color(0xFFFF6B6B),
            onError = Color(0xFF1F0000),
            errorContainer = Color(0xFF5C1414),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        val (primary, secondary, tertiary) = when (seed) {
            ColorPaletteSeed.LAVENDER -> Triple(LavenderPrimary, LavenderSecondary, LavenderTertiary)
            ColorPaletteSeed.OCEAN_BLUE -> Triple(OceanPrimary, OceanSecondary, OceanTertiary)
            ColorPaletteSeed.BOTANICAL_SAGE -> Triple(SagePrimary, SageSecondary, SageTertiary)
            ColorPaletteSeed.SUNSET_AMBER -> Triple(SunsetPrimary, SunsetSecondary, SunsetTertiary)
            ColorPaletteSeed.ROSE_BERRY -> Triple(RosePrimary, RoseSecondary, RoseTertiary)
            ColorPaletteSeed.MINIMAL_MONO -> Triple(MonoLightPrimary, MonoLightSecondary, MonoLightTertiary)
        }

        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lerp(LightSurface, primary, 0.16f),
            onPrimaryContainer = lerp(primary, Color.Black, 0.35f),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = lerp(LightSurface, secondary, 0.16f),
            onSecondaryContainer = lerp(secondary, Color.Black, 0.35f),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = lerp(LightSurface, tertiary, 0.16f),
            onTertiaryContainer = lerp(tertiary, Color.Black, 0.35f),
            background = LightBg,
            onBackground = Color(0xFF1A1C22),
            surface = LightSurface,
            onSurface = Color(0xFF1A1C22),
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = Color(0xFF4B5262),
            outline = Color(0xFFC8CEDB),
            outlineVariant = Color(0xFFE0E4EE),
            error = Color(0xFFD32F2F),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        )
    }
}

@Composable
fun AuraLauncherTheme(
    themeMode: ThemeMode = ThemeMode.MINIMAL_DARK,
    paletteSeed: ColorPaletteSeed = ColorPaletteSeed.LAVENDER,
    content: @Composable () -> Unit
) {
    val isDark = isLauncherDarkTheme(themeMode)
    val isAmoled = themeMode == ThemeMode.AMOLED_BLACK

    val context = LocalContext.current
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

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
