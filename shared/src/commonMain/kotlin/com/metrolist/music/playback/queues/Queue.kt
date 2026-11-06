/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback.queues

import com.metrolist.music.models.MediaMetadata

interface Queue {
    val preloadItem: MediaMetadata?

    suspend fun getInitialStatus(): Status

    fun hasNextPage(): Boolean

    suspend fun nextPage(): List<MediaMetadata>

    data class Status(
        val title: String?,
        val items: List<MediaMetadata>,
        val mediaItemIndex: Int,
        val position: Long = 0L,
    ) {
        fun filterExplicit(enabled: Boolean = true) =
            if (enabled) filterItems { !it.explicit } else this

        fun filterVideoSongs(disableVideos: Boolean = false) =
            if (disableVideos) filterItems { !it.isVideoSong } else this

        // Keeps mediaItemIndex on the same song; if that song is removed, starts at the next kept one.
        private fun filterItems(keep: (MediaMetadata) -> Boolean): Status {
            val filtered = items.filter(keep)
            if (filtered.size == items.size) return this
            val start = items.getOrNull(mediaItemIndex)
            val keptBefore = items.take(mediaItemIndex.coerceAtLeast(0)).count(keep)
            return copy(
                items = filtered,
                mediaItemIndex = keptBefore.coerceAtMost((filtered.size - 1).coerceAtLeast(0)),
                position = if (start != null && keep(start)) position else 0L,
            )
        }
    }
}

fun List<MediaMetadata>.filterExplicit(enabled: Boolean = true) =
    if (enabled) {
        filterNot {
            it.explicit
        }
    } else {
        this
    }

fun List<MediaMetadata>.filterVideoSongs(disableVideos: Boolean = false) =
    if (disableVideos) {
        filterNot { it.isVideoSong }
    } else {
        this
    }
