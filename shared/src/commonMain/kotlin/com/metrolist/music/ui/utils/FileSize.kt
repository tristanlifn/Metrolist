/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable

/** A short, localized size such as "4.2 MB". */
@Composable
expect fun formatFileSize(bytes: Long): String
