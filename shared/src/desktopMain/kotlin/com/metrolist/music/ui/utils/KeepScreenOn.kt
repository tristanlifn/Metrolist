/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable

// Desktop screen savers are handled by the media session (MPRIS) inhibit, not per window.
@Composable
actual fun KeepScreenOn(enabled: Boolean) = Unit
