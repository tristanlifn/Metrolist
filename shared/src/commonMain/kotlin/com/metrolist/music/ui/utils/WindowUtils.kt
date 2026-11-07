/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalWindowInfo

/** True when the window is taller than it is wide, matching Android's portrait orientation. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun isPortraitWindow(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return size.height >= size.width
}
