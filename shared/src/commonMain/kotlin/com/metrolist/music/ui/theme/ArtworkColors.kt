/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.ui.graphics.Color
import coil3.PlatformContext

/**
 * Player background gradient picked from the artwork at [url]: its main colour, a darker shade, then black.
 * Null when the image can't be loaded.
 */
expect suspend fun artworkGradientColors(
    context: PlatformContext,
    url: String,
    fallbackColor: Int,
    memoryCacheKey: String? = null,
): List<Color>?

/** The artwork's saturated colours, most common first. Empty when the image can't be loaded. */
suspend fun artworkSwatchColors(
    context: PlatformContext,
    url: String,
): List<Color> = artworkColorsByPopulation(context, url).filter { it.saturation() > 0.2f }

internal expect suspend fun artworkColorsByPopulation(
    context: PlatformContext,
    url: String,
): List<Color>

/** A vibrant colour and a darker vibrant one from the artwork, each falling back to [fallbackColor]. */
expect suspend fun artworkVibrantColors(
    context: PlatformContext,
    url: String,
    fallbackColor: Int,
): Pair<Color, Color>?

// HSV saturation, as android.graphics.Color.colorToHSV computes it.
private fun Color.saturation(): Float {
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    return if (max == 0f) 0f else (max - min) / max
}

/** The app theme seed picked from the artwork at [url], or null when the image can't be loaded. */
expect suspend fun artworkThemeColor(
    context: PlatformContext,
    url: String,
    fallbackColor: Int,
): Color?

internal fun gradientFrom(primaryColor: Color): List<Color> =
    listOf(
        primaryColor,
        primaryColor.copy(
            red = primaryColor.red * 0.6f,
            green = primaryColor.green * 0.6f,
            blue = primaryColor.blue * 0.6f,
        ),
        Color.Black,
    )
