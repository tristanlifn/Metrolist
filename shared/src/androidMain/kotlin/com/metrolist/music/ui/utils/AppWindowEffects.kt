/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.app.Activity
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import kotlin.math.abs

@Suppress("DEPRECATION")
@Composable
actual fun AppWindowEffects(
    darkTheme: Boolean,
    highRefreshRate: Boolean,
) {
    val window = (LocalContext.current as? Activity)?.window ?: return

    LaunchedEffect(highRefreshRate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val layoutParams = window.attributes
            if (highRefreshRate) {
                layoutParams.preferredDisplayModeId = 0
            } else {
                val modes = window.windowManager.defaultDisplay.supportedModes
                val mode60 =
                    modes.firstOrNull { abs(it.refreshRate - 60f) < 1f }
                        ?: modes.minByOrNull { abs(it.refreshRate - 60f) }
                if (mode60 != null) {
                    layoutParams.preferredDisplayModeId = mode60.modeId
                }
            }
            window.attributes = layoutParams
        } else {
            val params = window.attributes
            params.preferredRefreshRate = if (highRefreshRate) 0f else 60f
            window.attributes = params
        }
    }

    LaunchedEffect(darkTheme) {
        WindowCompat.getInsetsController(window, window.decorView.rootView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = (if (darkTheme) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            window.navigationBarColor = (if (darkTheme) Color.Transparent else Color.Black.copy(alpha = 0.2f)).toArgb()
        }
    }
}
