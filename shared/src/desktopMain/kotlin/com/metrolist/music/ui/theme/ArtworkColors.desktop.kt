/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.toBitmap
import com.materialkolor.quantize.QuantizerCelebi
import com.materialkolor.score.Score
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Android uses androidx.palette; this is the same quantize-then-score idea with Material's own quantizer.
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
            .apply { memoryCacheKey?.let { memoryCacheKey(it) } }
            .build()
    val bitmap =
        runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()?.image?.toBitmap()
            ?: return null
    val primary =
        withContext(Dispatchers.Default) {
            val pixels = bitmap.asComposeImageBitmap().toPixelMap().buffer
            Score.score(QuantizerCelebi.quantize(pixels, 8), 1, fallbackColor, true).first()
        }
    return gradientFrom(Color(primary))
}

private suspend fun artworkColorCounts(
    context: PlatformContext,
    url: String,
): Map<Int, Int>? {
    val request = ImageRequest.Builder(context).data(url).build()
    val bitmap =
        runCatching { SingletonImageLoader.get(context).execute(request) }.getOrNull()?.image?.toBitmap()
            ?: return null
    return withContext(Dispatchers.Default) {
        QuantizerCelebi.quantize(bitmap.asComposeImageBitmap().toPixelMap().buffer, 16)
    }
}

internal actual suspend fun artworkColorsByPopulation(
    context: PlatformContext,
    url: String,
): List<Color> =
    artworkColorCounts(context, url)
        ?.entries
        ?.sortedByDescending { it.value }
        ?.map { Color(it.key) }
        .orEmpty()

actual suspend fun artworkVibrantColors(
    context: PlatformContext,
    url: String,
    fallbackColor: Int,
): Pair<Color, Color>? {
    val counts = artworkColorCounts(context, url) ?: return null
    val ranked = Score.score(counts, 2, fallbackColor, true)
    val vibrant = Color(ranked.first())
    return vibrant to (ranked.getOrNull(1)?.let(::Color) ?: gradientFrom(vibrant)[1])
}
