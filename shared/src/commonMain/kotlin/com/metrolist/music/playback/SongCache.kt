/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.music.db.entities.Song

/** The cache that holds streamed songs, separate from downloads. */
interface SongCache {
    /** Size of [song]'s full file, or null when it isn't known. */
    fun contentLength(song: Song): Long?

    fun isCached(
        songId: String,
        contentLength: Long,
    ): Boolean

    fun remove(songId: String)
}
