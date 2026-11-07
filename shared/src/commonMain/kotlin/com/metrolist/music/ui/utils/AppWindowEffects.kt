/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable

/** Window-level settings for the whole app: system bar icon colours and the display refresh rate. */
@Composable
expect fun AppWindowEffects(
    darkTheme: Boolean,
    highRefreshRate: Boolean,
)
