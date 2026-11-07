/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.filterExplicit
import com.metrolist.music.extensions.filterVideoSongs
import com.metrolist.music.playback.SongCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

class CachePlaylistViewModel(
        private val dataStore: DataStore<Preferences>,
        private val database: MusicDatabase,
        private val songCache: SongCache,
    ) : ViewModel() {
        private data class Inputs(
            val flagged: List<Song>,
            val hideExplicit: Boolean,
            val hideVideoSongs: Boolean,
        )

        @OptIn(ExperimentalCoroutinesApi::class)
        val cachedSongs: StateFlow<List<Song>> =
            combine(
                database.cachePlaylistSongs(),
                dataStore.data.map { it[HideExplicitKey] ?: false }.distinctUntilChanged(),
                dataStore.data.map { it[HideVideoSongsKey] ?: false }.distinctUntilChanged(),
                ::Inputs,
            ).mapLatest { (flagged, hideExplicit, hideVideoSongs) ->
                val partition =
                    partitionCachedSongs(flagged, songCache::contentLength, songCache::isCached)

                // Clearing the flag removes these songs from cachePlaylistSongs(), so this
                // re-emits once and then settles rather than looping.
                if (partition.stale.isNotEmpty()) {
                    database.withTransaction {
                        partition.stale.forEach { song ->
                            update(song.song.copy(dateDownload = null))
                        }
                    }
                }

                partition.stillCached
                    .sortedByDescending { it.song.dateDownload }
                    .filterExplicit(hideExplicit)
                    .filterVideoSongs(hideVideoSongs)
            }.flowOn(Dispatchers.IO)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        fun removeSongFromCache(songId: String) = removeSongsFromCache(listOf(songId))

        fun removeSongsFromCache(songIds: Collection<String>) {
            songIds.forEach(songCache::remove)

            // Dropping the bytes does not touch the database, so clear the flags explicitly.
            database.query {
                songIds.forEach { songId ->
                    getSongByIdBlocking(songId)?.let { update(it.song.copy(dateDownload = null)) }
                }
            }
        }
    }

/**
 * Result of checking which Cache Playlist entries still have their bytes on disk.
 */
internal data class CachedSongPartition(
    val stillCached: List<Song>,
    val stale: List<Song>,
)

/**
 * Splits songs flagged as belonging to the Cache Playlist into those whose data is still
 * present and those whose flag must be cleared.
 *
 * Downloaded songs are omitted without being marked stale because [Song.song.dateDownload] also
 * stores their download date. A song whose content length is unknown cannot be checked, so it is
 * treated as stale rather than assumed present.
 *
 * [isCached] receives the song id and its content length, and reports whether the complete file
 * is available in the player cache.
 */
internal fun partitionCachedSongs(
    flagged: List<Song>,
    resolveContentLength: (Song) -> Long? = { it.format?.contentLength },
    isCached: (songId: String, contentLength: Long) -> Boolean,
): CachedSongPartition {
    val stillCached = mutableListOf<Song>()
    val stale = mutableListOf<Song>()

    for (song in flagged) {
        if (song.song.isDownloaded) continue
        val contentLength = resolveContentLength(song)
        val present = contentLength != null && isCached(song.song.id, contentLength)
        if (present) stillCached += song else stale += song
    }

    return CachedSongPartition(stillCached, stale)
}
