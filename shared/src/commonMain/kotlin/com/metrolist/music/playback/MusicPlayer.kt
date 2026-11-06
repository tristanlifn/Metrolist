/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.QueueSession
import com.metrolist.music.playback.queues.Queue
import kotlinx.coroutines.flow.StateFlow

enum class PlaybackState { IDLE, BUFFERING, READY, ENDED }

enum class RepeatMode { OFF, ONE, ALL }

/**
 * One song in the play queue, listed in playback order (shuffle applied).
 * [uid] is stable while the entry stays in the queue; [index] is its position in the unshuffled queue.
 */
data class QueueEntry(
    val uid: Any,
    val index: Int,
    val metadata: MediaMetadata?,
)

data class PlayerError(
    val code: Int,
    val codeName: String,
    val cause: Throwable,
)

/** Playback as the UI sees it; Android implements it with Media3, desktop with its own player. */
interface MusicPlayer {
    companion object {
        /** [duration] when it isn't known yet; equal to Media3's C.TIME_UNSET. */
        const val TIME_UNSET = Long.MIN_VALUE + 1
    }

    val mediaMetadata: StateFlow<MediaMetadata?>
    val isPlaying: StateFlow<Boolean>

    /** Like [isPlaying], but follows the remote device while casting. */
    val isEffectivelyPlaying: StateFlow<Boolean>
    val playbackState: StateFlow<PlaybackState>
    val currentSong: StateFlow<Song?>
    val currentLyrics: StateFlow<LyricsEntity?>
    val currentFormat: StateFlow<FormatEntity?>

    val queueTitle: StateFlow<String?>
    val queue: StateFlow<List<QueueEntry>>

    /** Position of the current song in [queue], or -1 when nothing is queued. */
    val currentQueuePosition: StateFlow<Int>
    val queueSessions: StateFlow<List<QueueSession>>
    val activeQueueId: StateFlow<Long?>

    val shuffleModeEnabled: StateFlow<Boolean>
    val repeatMode: StateFlow<RepeatMode>
    val canSkipPrevious: StateFlow<Boolean>
    val canSkipNext: StateFlow<Boolean>
    val error: StateFlow<PlayerError?>
    val isMuted: StateFlow<Boolean>
    val currentStreamClient: StateFlow<String?>

    // Read on demand (the UI polls these), not observed.
    val currentPosition: Long
    val duration: Long
    val volume: Float
    var playbackSpeed: Float
    var pitch: Float

    fun playQueue(queue: Queue)

    fun switchQueue(id: Long)

    fun deleteQueue(id: Long)

    fun playNext(items: List<MediaMetadata>)

    fun playNext(item: MediaMetadata) = playNext(listOf(item))

    fun addToQueue(items: List<MediaMetadata>)

    fun addToQueue(item: MediaMetadata) = addToQueue(listOf(item))

    fun play()

    fun pause()

    fun togglePlayPause()

    fun seekTo(position: Long)

    /** Starts the song at [index] in the unshuffled queue from its beginning. */
    fun seekToItem(index: Int)

    fun seekToNext()

    fun seekToPrevious()

    fun moveItem(
        from: Int,
        to: Int,
    )

    fun removeItem(index: Int)

    fun clearQueue()

    fun setShuffleModeEnabled(enabled: Boolean)

    fun setRepeatMode(mode: RepeatMode)

    fun toggleRepeatMode()

    fun toggleLike()

    fun toggleLibrary()

    fun toggleMute()

    fun startRadioSeamlessly()
}
