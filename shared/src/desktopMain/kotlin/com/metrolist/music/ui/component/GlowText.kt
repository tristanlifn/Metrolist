/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.Paint

private val boldTypeface = FontMgr.default.matchFamilyStyle(null, FontStyle.BOLD)
private val glowPaint = Paint().apply { isAntiAlias = true }

actual fun DrawScope.drawGlowText(
    text: String,
    baseline: Float,
    color: Color,
    fontSizePx: Float,
    blurRadiusPx: Float,
) {
    drawIntoCanvas { canvas ->
        // Android's BlurMaskFilter takes a radius; Skia wants the sigma it converts that radius to.
        glowPaint.maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, blurRadiusPx * 0.57735f + 0.5f)
        glowPaint.color = color.toArgb()
        canvas.nativeCanvas.drawString(text, 0f, baseline, Font(boldTypeface, fontSizePx), glowPaint)
    }
}
