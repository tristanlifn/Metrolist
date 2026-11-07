/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.ContentMetadata
import com.metrolist.music.db.entities.Song

class Media3SongCache(
    private val cache: Cache,
) : SongCache {
    override fun contentLength(song: Song): Long? =
        song.format?.contentLength
            ?: ContentMetadata
                .getContentLength(cache.getContentMetadata(song.id))
                .takeIf { it > 0L }

    override fun isCached(
        songId: String,
        contentLength: Long,
    ) = cache.isCached(songId, 0, contentLength)

    override fun remove(songId: String) = cache.removeResource(songId)

    override val size: Long
        get() = runCatching { cache.cacheSpace }.getOrDefault(0L)

    override fun clear() = cache.keys.forEach(cache::removeResource)
}
