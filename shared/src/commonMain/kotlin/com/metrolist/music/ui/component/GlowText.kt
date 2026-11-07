/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope

/** Draws [text] in bold as a soft blurred glow, used behind sung lyric words. */
expect fun DrawScope.drawGlowText(
    text: String,
    baseline: Float,
    color: Color,
    fontSizePx: Float,
    blurRadiusPx: Float,
)
