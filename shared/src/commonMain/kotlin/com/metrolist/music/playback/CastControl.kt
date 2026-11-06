/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import kotlinx.coroutines.flow.StateFlow

/** Playback on a remote device; only some platforms and builds support it. */
interface CastControl {
    val isCasting: StateFlow<Boolean>
    val castDeviceName: StateFlow<String?>
    val castPosition: StateFlow<Long>
    val castDuration: StateFlow<Long>
    val castIsPlaying: StateFlow<Boolean>
    val castVolume: StateFlow<Float>

    fun play()

    fun pause()

    fun seekTo(position: Long)

    fun setVolume(volume: Float)

    /** Returns false when [mediaId] isn't in the remote queue. */
    fun navigateToMediaIfInQueue(mediaId: String): Boolean
}
