package com.metrolist.music.playback

import android.content.Context
import com.metrolist.music.models.MediaMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("UNUSED_PARAMETER")
class CastConnectionHandler(
    context: Context,
    scope: CoroutineScope,
    musicService: MusicService,
) : CastControl {
    override val isCasting: StateFlow<Boolean> = MutableStateFlow(false)
    override val castDeviceName: StateFlow<String?> = MutableStateFlow(null)
    override val castPosition: StateFlow<Long> = MutableStateFlow(0L)
    override val castDuration: StateFlow<Long> = MutableStateFlow(0L)
    override val castIsPlaying: StateFlow<Boolean> = MutableStateFlow(false)
    val castIsBuffering: StateFlow<Boolean> = MutableStateFlow(false)
    override val castVolume: StateFlow<Float> = MutableStateFlow(1f)
    val isSyncingFromCast = false

    fun initialize() = false

    fun disconnect() = Unit

    fun loadCurrentMedia() = Unit

    fun loadMedia(metadata: MediaMetadata) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(position: Long) = Unit

    override fun setVolume(volume: Float) = Unit

    fun skipToNext() = Unit

    fun skipToPrevious() = Unit

    override fun navigateToMediaIfInQueue(mediaId: String) = false

    fun release() = Unit
}
