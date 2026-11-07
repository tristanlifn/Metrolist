/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.materialkolor.score.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun artworkGradientColors(
    context: PlatformContext,
    url: String,
    fallbackColor: Int,
    memoryCacheKey: String?,
): List<Color>? {
    val request =
        ImageRequest
            .Builder(context)
            .data(url)
            .size(100, 100)
            .allowHardware(false)
            .apply { memoryCacheKey?.let { memoryCacheKey(it) } }
            .build()
    val bitmap =
        runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()?.image?.toBitmap()
            ?: return null
    val palette =
        withContext(Dispatchers.Default) {
            Palette
                .from(bitmap)
                .maximumColorCount(8)
                .resizeBitmapArea(100 * 100)
                .generate()
        }
    return gradientFrom(Color(palette.rankedColors(1, fallbackColor).first()))
}

fun Palette.rankedColors(
    desiredColorCount: Int,
    fallbackColor: Int,
): List<Int> =
    Score.score(
        swatches.associate { it.rgb to it.population },
        desiredColorCount,
        fallbackColor,
        true,
    )
