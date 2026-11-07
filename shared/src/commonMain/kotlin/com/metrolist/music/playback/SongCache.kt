/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.music.db.entities.Song
import org.koin.core.qualifier.named

/** The cache that holds streamed songs, separate from downloads. */
interface SongCache {
    /** Size of [song]'s full file, or null when it isn't known. */
    fun contentLength(song: Song): Long?

    fun isCached(
        songId: String,
        contentLength: Long,
    ): Boolean

    fun remove(songId: String)

    /** Bytes currently stored. */
    val size: Long

    fun clear()
}

/** Koin qualifier for the [SongCache] that backs downloads rather than streaming. */
val DownloadSongCache = named("DownloadSongCache")
