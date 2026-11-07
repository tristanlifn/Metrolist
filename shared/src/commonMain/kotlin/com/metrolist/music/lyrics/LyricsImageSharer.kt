/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.lyrics

import com.metrolist.music.ui.component.LyricsBackgroundStyle
import com.metrolist.music.ui.screens.settings.LyricsPosition

/** Renders a lyrics card as an image and opens the share sheet for it. */
interface LyricsImageSharer {
    suspend fun share(
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int,
        backgroundStyle: LyricsBackgroundStyle,
        textColor: Int,
        secondaryTextColor: Int,
        alignment: LyricsPosition,
        chooserTitle: String,
    )
}
