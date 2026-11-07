/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable
import coil3.request.ImageRequest

@Composable
actual fun PlayerWindowEffects(
    active: Boolean,
    lightStatusBar: Boolean,
    hideStatusBar: Boolean,
    keepScreenOn: Boolean,
    lightStatusBarAfter: Boolean,
) = Unit

actual fun ImageRequest.Builder.softwareBitmap(): ImageRequest.Builder = this
