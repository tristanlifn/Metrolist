/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable

/** Keeps the display awake while [enabled] and this is in composition. */
@Composable
expect fun KeepScreenOn(enabled: Boolean)
