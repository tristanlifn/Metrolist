/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.music.constants.LoudnessLevel

/** Gain in millibels that brings a song measured by YouTube to [targetLufs], or null without loudness data. */
fun normalizationGainMb(
    loudnessDb: Double?,
    perceptualLoudnessDb: Double?,
    targetLufs: Float,
): Int? {
    val measuredLufs = perceptualLoudnessDb ?: loudnessDb?.let { it + LoudnessLevel.AGGRESSIVE.targetLufs }
    return measuredLufs
        ?.let { (-(it - targetLufs) * 100.0).toInt() }
        ?.coerceIn(-1500, 300)
}
