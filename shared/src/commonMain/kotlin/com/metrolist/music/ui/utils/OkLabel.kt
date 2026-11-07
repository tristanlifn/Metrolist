/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable

/** The platform's own translated "OK"; the app has no string of its own for it. */
@Composable
expect fun okLabel(): String
