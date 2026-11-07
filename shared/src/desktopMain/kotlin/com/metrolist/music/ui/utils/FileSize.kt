/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable

// Same 1000-based units Android's Formatter uses.
@Composable
actual fun formatFileSize(bytes: Long): String {
    if (bytes < 1000) return "$bytes B"
    var value = bytes.toDouble()
    var unit = 0
    val units = listOf("kB", "MB", "GB", "TB")
    while (value >= 1000 * 1000 && unit < units.lastIndex) {
        value /= 1000
        unit++
    }
    return "%.1f %s".format(value / 1000, units[unit])
}
