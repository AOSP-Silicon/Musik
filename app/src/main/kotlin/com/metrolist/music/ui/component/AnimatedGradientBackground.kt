package com.metrolist.music.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private fun getProcessedColors(colors: List<Color>): List<Color> {
    return when {
        colors.size >= 5 -> colors.take(5) // Use up to 5 provided colors
        colors.size == 2 -> colors
        colors.size == 1 -> {
            val base = colors[0]
            val lighter = base.copy( // Generate a lighter shade
                red = (base.red * 1.2f).coerceIn(0f, 1f),
                green = (base.green * 1.2f).coerceIn(0f, 1f),
                blue = (base.blue * 1.2f).coerceIn(0f, 1f)
            )
            listOf(base, lighter)
        }
        else -> defaultGradientColors.take(2) // Fallback to first two defaults if 0 or invalid
    }
}

private val SmoothEasing = CubicBezierEasing(0.2f, 0.9f, 0.4f, 1.0f)

private val defaultGradientColors = listOf(
    Color(0xFF5E5CE0),
    Color(0xFFD4145A),
    Color(0xFFAF52DE),
    Color(0xFFFF2D55),
    Color(0xFF11998E),
    Color(0xFF5E5CE0),
)

@Composable
fun AnimatedGradientBackground(
    colors: List<Color> = defaultGradientColors,
    isActive: Boolean = true,
    intensity: Float = 0.6f,
    animationDuration: Int = 500,
    enableBreathing: Boolean = true,
    modifier: Modifier = Modifier
) {
    val processedColors = remember(colors) { getProcessedColors(colors) }

    val (primaryColor, secondaryColor) = remember(processedColors) {
        // Use the first two processed colors for animation
        Pair(processedColors.firstOrNull() ?: defaultGradientColors[0],
             processedColors.getOrNull(1) ?: defaultGradientColors[1])
    }

    val animatedPrimary by animateColorAsState(
        targetValue = primaryColor,
        animationSpec = tween(
            durationMillis = animationDuration,
            easing = SmoothEasing
        ),
        label = "DynamicGradient_Primary"
    )

    val animatedSecondary by animateColorAsState(
        targetValue = secondaryColor,
        animationSpec = tween(
            durationMillis = animationDuration + 200,
            easing = SmoothEasing
        ),
        label = "DynamicGradient_Secondary"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "DynamicGradient_Breathing")
    val breathingIntensity by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = SmoothEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DynamicGradient_BreathingValue"
    )

    val currentFinalIntensity = if (!isActive && enableBreathing) {
        intensity * breathingIntensity
    } else {
        intensity
    }

    val colorStops = remember(
        animatedPrimary,
        animatedSecondary,
        processedColors,
        currentFinalIntensity
    ) {
        val numProcessedColors = processedColors.size
        // If only one color, it's a solid background. Use two stops for simplicity.
        if (numProcessedColors == 0) {
            arrayOf(0f to defaultGradientColors[0], 1f to defaultGradientColors[1])
        } else if (numProcessedColors == 1) {
             arrayOf(0f to processedColors[0], 1f to processedColors[0].copy(alpha = 0.8f * currentFinalIntensity))
        } else {
            val stopInterval = 1.0f / (numProcessedColors - 1)
            val stops = mutableListOf<Pair<Float, Color>>()

            for (i in 0 until numProcessedColors) {
                val position = i * stopInterval
                val colorToUse: Color

                when (i) {
                    0 -> colorToUse = animatedPrimary // Use animated primary for the first stop
                    1 -> colorToUse = animatedSecondary // Use animated secondary for the second stop
                    else -> colorToUse = processedColors[i] // Use static colors for subsequent stops
                }

                // Apply a progressive fade to colors.
                // Alpha multipliers are heuristic, aiming for a pleasing fade.
                val adjustedAlpha = when (i) {
                    0 -> 0.85f // Animated primary color, let it be full
                    1 -> 0.80f // Animated secondary color, slight reduction
                    2 -> 0.75f
                    3 -> 0.80f
                    4 -> 0.45f
                    else -> 0.35f // Fallback
                }

                stops.add(position to colorToUse.copy(alpha = adjustedAlpha))
            }
            stops.toTypedArray()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Apply the overall intensity and breathing effect here
            .alpha(if (isActive) currentFinalIntensity else 0.0f)
            .background(
                Brush.verticalGradient(
                    colorStops = colorStops,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}
