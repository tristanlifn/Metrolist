/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.CoroutineScope

class SleepTimer(
    scope: CoroutineScope,
    var player: Player,
    onVolumeMultiplierChanged: (Float) -> Unit = {},
) : PlayerSleepTimer(scope, onVolumeMultiplierChanged),
    Player.Listener {
    override fun pausePlayback() = player.pause()

    override val positionMs: Long get() = player.currentPosition

    override val durationMs: Long get() = player.duration

    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int,
    ) = notifySongTransition()

    override fun onPlaybackStateChanged(
        @Player.State playbackState: Int,
    ) {
        if (playbackState == Player.STATE_ENDED) notifySongTransition()
    }
}
