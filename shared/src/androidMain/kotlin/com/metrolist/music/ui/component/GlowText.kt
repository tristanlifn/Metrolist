/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

// Drawing happens on the main thread only, so one reusable paint avoids allocating per glyph.
private val glowPaint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

actual fun DrawScope.drawGlowText(
    text: String,
    baseline: Float,
    color: Color,
    fontSizePx: Float,
    blurRadiusPx: Float,
) {
    drawIntoCanvas { canvas ->
        glowPaint.maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.NORMAL)
        glowPaint.color = color.toArgb()
        glowPaint.textSize = fontSizePx
        canvas.nativeCanvas.drawText(text, 0f, baseline, glowPaint)
    }
}
