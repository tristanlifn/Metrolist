/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import com.metrolist.music.models.toMediaMetadata
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.db.entities.AlbumWithSongs
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

class LocalAlbumRadio(
    private val albumWithSongs: AlbumWithSongs,
    private val startIndex: Int = 0,
) : Queue {
    override val preloadItem: MediaMetadata? = null

    private lateinit var playlistId: String
    private val endpoint: WatchEndpoint
        get() = WatchEndpoint(
            playlistId = playlistId
        )

    private var continuation: String? = null
    private var firstTimeLoaded: Boolean = false

    override suspend fun getInitialStatus(): Queue.Status = withContext(IO) {
        Queue.Status(
            title = albumWithSongs.album.title,
            items = albumWithSongs.songs.map { it.toMediaMetadata() },
            mediaItemIndex = startIndex
        )
    }

    override fun hasNextPage(): Boolean = !firstTimeLoaded || continuation != null

    override suspend fun nextPage(): List<MediaMetadata> = withContext(IO) {
        if (!firstTimeLoaded) {
            playlistId = YouTube.album(albumWithSongs.album.id).getOrThrow().album.playlistId
            val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
            continuation = nextResult.continuation
            firstTimeLoaded = true
            return@withContext nextResult.items.subList(
                albumWithSongs.songs.size,
                nextResult.items.size
            ).map { it.toMediaMetadata() }
        }
        val nextResult = YouTube.next(endpoint, continuation).getOrThrow()
        continuation = nextResult.continuation
        nextResult.items.map { it.toMediaMetadata() }
    }
}
