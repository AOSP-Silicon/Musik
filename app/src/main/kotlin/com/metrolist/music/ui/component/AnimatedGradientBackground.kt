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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import android.os.Build
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.metrolist.music.LocalPlayerConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val SmoothEasing = CubicBezierEasing(0.2f, 0.9f, 0.4f, 1.0f)

private val defaultGradientColors = listOf(
    Color(0xFFAF52DE),
    Color(0xFF5AC8FA),
    Color(0xFFFFFFFF),
    Color(0xFFA29BFE),
)

private fun getProcessedColors(colors: List<Color>): List<Color> {
    val targetSize = 4
    val result = mutableListOf<Color>()

    val available = colors.take(targetSize)
    result.addAll(available)

    while (result.size < targetSize) {
        val last = if (result.isNotEmpty()) result.last() else defaultGradientColors[0]
        val next = when (result.size) {
            0 -> defaultGradientColors[0]
            1 -> last.copy(
                red = (last.red * 1.3f).coerceIn(0f, 1f),
                green = (last.green * 1.3f).coerceIn(0f, 1f),
                blue = (last.blue * 1.3f).coerceIn(0f, 1f)
            )
            2 -> {
                val first = result[0]
                Color(
                    red = (first.red * 0.5f + last.red * 0.5f).coerceIn(0f, 1f),
                    green = (first.green * 0.5f + last.green * 0.5f).coerceIn(0f, 1f),
                    blue = (first.blue * 0.5f + last.blue * 0.5f).coerceIn(0f, 1f)
                )
            }
            3 -> Color.White
            else -> last.copy(alpha = 0.6f)
        }
        result.add(next)
    }

    if (result.size >= 3) {
        result[2] = Color.White
    }

    return result
}

private suspend fun extractColorsFromAlbumArt(
    context: android.content.Context,
    imageUrl: String
): List<Color> {
    return withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .size(100, 100)
                .allowHardware(false)
                .build()

            val result = context.imageLoader.execute(request)
            val bitmap = result.image?.toBitmap()

            if (bitmap != null) {
                val palette = Palette.from(bitmap)
                    .maximumColorCount(8)
                    .resizeBitmapArea(100 * 100)
                    .generate()

                val colors = mutableListOf<Color>()

                palette.vibrantSwatch?.let { colors.add(Color(it.rgb)) }
                palette.mutedSwatch?.let { colors.add(Color(it.rgb)) }
                palette.darkVibrantSwatch?.let { colors.add(Color(it.rgb)) }
                palette.lightVibrantSwatch?.let { colors.add(Color(it.rgb)) }

                if (colors.isNotEmpty()) {
                    return@withContext colors
                }
            }

            defaultGradientColors

        } catch (e: Exception) {
            defaultGradientColors
        }
    }
}

@Composable
fun AnimatedGradientBackground(
    colors: List<Color> = defaultGradientColors,
    isActive: Boolean = true,
    intensity: Float = 0.6f,
    animationDuration: Int = 500,
    enableBreathing: Boolean = true,
    blurRadius: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState() ?: remember { mutableStateOf(null) }
    val albumArtUrl = mediaMetadata?.thumbnailUrl
    var extractedColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(albumArtUrl) {
        if (albumArtUrl != null && albumArtUrl.isNotBlank()) {
            val extracted = extractColorsFromAlbumArt(context, albumArtUrl)
            extractedColors = extracted
        } else {
            extractedColors = emptyList()
        }
    }

    val colorsToUse = if (extractedColors.isNotEmpty()) extractedColors else colors

    val finalColors = colorsToUse
    val processedColors = remember(finalColors) { getProcessedColors(finalColors) }

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
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(blurRadius)
                } else {
                    Modifier
                }
            )
            .background(
                Brush.verticalGradient(
                    colorStops = colorStops,
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}
