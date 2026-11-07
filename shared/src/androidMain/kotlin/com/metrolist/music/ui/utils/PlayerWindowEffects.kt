/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil3.request.ImageRequest
import coil3.request.allowHardware

@Composable
actual fun PlayerWindowEffects(
    active: Boolean,
    lightStatusBar: Boolean,
    hideStatusBar: Boolean,
    keepScreenOn: Boolean,
    lightStatusBarAfter: Boolean,
) {
    val window = (LocalContext.current as? Activity)?.window
    DisposableEffect(window, active, lightStatusBar, hideStatusBar, keepScreenOn, lightStatusBarAfter) {
        if (window != null && active) {
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = lightStatusBar
            if (hideStatusBar) {
                insetsController.hide(WindowInsetsCompat.Type.statusBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.statusBars())
            }
            if (keepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        onDispose {
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, window.decorView)
                insetsController.isAppearanceLightStatusBars = lightStatusBarAfter
                insetsController.show(WindowInsetsCompat.Type.statusBars())
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

actual fun ImageRequest.Builder.softwareBitmap(): ImageRequest.Builder = allowHardware(false)
