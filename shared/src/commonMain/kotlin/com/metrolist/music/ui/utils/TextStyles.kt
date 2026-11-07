/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.ui.text.PlatformTextStyle

/** Drops Android's extra font padding so line heights match the design; nothing to drop elsewhere. */
expect val NoFontPaddingTextStyle: PlatformTextStyle?
