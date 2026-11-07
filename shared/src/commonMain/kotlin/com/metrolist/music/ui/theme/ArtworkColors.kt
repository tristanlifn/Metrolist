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
