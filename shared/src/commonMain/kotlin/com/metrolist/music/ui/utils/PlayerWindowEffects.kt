/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable
import coil3.request.ImageRequest

/**
 * Window tweaks while the full player is [active]: status bar icon colour, hiding the status bar
 * and keeping the screen on. [lightStatusBarAfter] is restored when the player closes.
 */
@Composable
expect fun PlayerWindowEffects(
    active: Boolean,
    lightStatusBar: Boolean,
    hideStatusBar: Boolean,
    keepScreenOn: Boolean,
    lightStatusBarAfter: Boolean,
)

/** Decodes into a software bitmap where the platform defaults to hardware ones. */
expect fun ImageRequest.Builder.softwareBitmap(): ImageRequest.Builder
