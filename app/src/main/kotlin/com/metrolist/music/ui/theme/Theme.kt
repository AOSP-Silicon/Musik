/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import com.materialkolor.score.Score

val DefaultThemeColor = Color(0xFFED5564)

@Composable
fun MetrolistTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    musikRedTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // Determine if system dynamic colors should be used (Android S+ and default theme color)
    val useSystemDynamicColor = (!musikRedTheme && themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    // Select the appropriate color scheme generation method
    val baseColorScheme = if (useSystemDynamicColor) {
        // Use standard Material 3 dynamic color functions for system wallpaper colors
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        // Use materialKolor only when a specific seed color is provided
        rememberDynamicColorScheme(
            seedColor = if (musikRedTheme) Color(0xFFFF2D55) else themeColor,
            isDark = darkTheme,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
            style = PaletteStyle.TonalSpot
        )
    }

    val musikRedScheme = remember(baseColorScheme, darkTheme, musikRedTheme, pureBlack) {
        if (musikRedTheme) {
            baseColorScheme.copy(
                // Primary - Red
                primary = Color(0xFFFF2D55),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFF2D55),
                onPrimaryContainer = Color.White,

                // Secondary - Blue
                secondary = Color(0xFF007AFF),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFF007AFF),
                onSecondaryContainer = Color.White,

                // Tertiary - Green
                tertiary = Color(0xFF34C759),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFF34C759),
                onTertiaryContainer = Color.White,

                // Error
                error = Color(0xFFFF3B30),
                onError = Color.White,
                errorContainer = if (darkTheme) Color(0xFF630000) else Color(0xFFFFDAD5),
                onErrorContainer = if (darkTheme) Color(0xFFFFDAD5) else Color(0xFF410001),

                // Surface colors: Pure white background, subtle gray containers
                surface = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF121212)
                    else -> Color.White
                },
                onSurface = if (darkTheme) Color.White else Color.Black,

                surfaceTint = Color.Transparent,

                surfaceVariant = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF2C2C2E)
                    else -> Color(0xFFE8E8ED)
                },
                onSurfaceVariant = if (darkTheme) Color(0xFFC7C7CC) else Color(0xFF6C6C70),

                surfaceContainer = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF1D1D1D)
                    else -> Color(0xFFF2F2F7)
                },
                surfaceContainerHigh = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF252525)
                    else -> Color(0xFFEFEFF4)
                },
                surfaceContainerHighest = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF303030)
                    else -> Color(0xFFE5E5EA)
                },
                surfaceContainerLow = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF191919)
                    else -> Color(0xFFF8F8FA)
                },
                surfaceContainerLowest = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF111112)
                    else -> Color(0xFFFFFFFF)
                },
                surfaceBright = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF3A3A3C)
                    else -> Color(0xFFF9F9FB)
                },
                surfaceDim = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF0A0A0A)
                    else -> Color(0xFFE8E8ED)
                },

                background = when {
                    pureBlack && darkTheme -> Color.Black
                    darkTheme -> Color(0xFF121212)
                    else -> Color.White
                },
                onBackground = if (darkTheme) Color.White else Color.Black,

                // System Grays
                outline = if (darkTheme) Color(0xFF38383A).copy(alpha = 0.6f) else Color(0xFF3C3C43).copy(alpha = 0.36f),
                outlineVariant = if (darkTheme) Color(0xFF38383A).copy(alpha = 0.36f) else Color(0xFF3C3C43).copy(alpha = 0.18f),

                inverseSurface = if (darkTheme) Color(0xFFE5E5EA) else Color(0xFF3A3A3C),
                inverseOnSurface = if (darkTheme) Color.Black else Color.White,
                inversePrimary = if (darkTheme) Color(0xFF5AC8FA) else Color(0xFFFF2D55),

                scrim = Color(0x80000000)
            )
        } else {
            baseColorScheme
        }
    }

    // Apply pureBlack modification if needed, similar to original logic
    val colorScheme = remember(musikRedScheme, pureBlack, darkTheme, musikRedTheme) {
        if (musikRedTheme) {
            musikRedScheme
        } else {
            if (darkTheme && pureBlack) baseColorScheme.pureBlack(true) else baseColorScheme
        }
    }

    // Use standard MaterialTheme instead of MaterialExpressiveTheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // Use the defined AppTypography
        content = content
    )
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}
