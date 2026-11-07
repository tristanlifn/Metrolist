/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes

/** Sleep timer logic shared by the platforms; they supply the player calls and report song transitions. */
abstract class PlayerSleepTimer(
    private val scope: CoroutineScope,
    private val onVolumeMultiplierChanged: (Float) -> Unit = {},
) : SleepTimerControl {
    private companion object {
        private const val TIMER_TICK_MS = 1000L
        private const val FADE_OUT_WINDOW_MS = 60_000L
    }

    protected abstract fun pausePlayback()

    protected abstract val positionMs: Long

    /** Length of the current song in ms, or a value <= 0 when unknown. */
    protected abstract val durationMs: Long

    private var sleepTimerJob: Job? = null
    final override var triggerTime by mutableLongStateOf(-1L)
        private set
    final override var pauseWhenSongEnd by mutableStateOf(false)
        private set
    var stopAfterCurrentSongOnTimeout by mutableStateOf(false)
        private set
    var fadeOutEnabled by mutableStateOf(false)
        private set
    override val isActive: Boolean
        get() = triggerTime != -1L || pauseWhenSongEnd

    override fun start(minute: Int) {
        start(
            minute = minute,
            stopAfterCurrentSong = false,
            fadeOut = false,
        )
    }

    override fun start(
        minute: Int,
        stopAfterCurrentSong: Boolean,
        fadeOut: Boolean,
    ) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        updateVolumeMultiplier(1f)
        fadeOutEnabled = fadeOut

        if (minute == -1) {
            pauseWhenSongEnd = true
            stopAfterCurrentSongOnTimeout = false
            triggerTime = -1L
            if (fadeOutEnabled) {
                sleepTimerJob =
                    scope.launch {
                        while (this@PlayerSleepTimer.isActive) {
                            updateVolumeMultiplierForCurrentSong()
                            delay(TIMER_TICK_MS)
                        }
                    }
            }
        } else {
            pauseWhenSongEnd = false
            stopAfterCurrentSongOnTimeout = stopAfterCurrentSong
            triggerTime = System.currentTimeMillis() + minute.minutes.inWholeMilliseconds
            sleepTimerJob =
                scope.launch {
                    while (this@PlayerSleepTimer.isActive) {
                        if (triggerTime != -1L) {
                            val remainingMs = triggerTime - System.currentTimeMillis()
                            if (remainingMs <= 0L) {
                                triggerTime = -1L
                                if (stopAfterCurrentSongOnTimeout) {
                                    pauseWhenSongEnd = true
                                    stopAfterCurrentSongOnTimeout = false
                                    if (!fadeOutEnabled) {
                                        break
                                    }
                                } else {
                                    completeTimerAndPause()
                                    break
                                }
                            } else if (fadeOutEnabled && !stopAfterCurrentSongOnTimeout) {
                                updateVolumeMultiplierForRemainingTime(remainingMs)
                            }
                        } else if (pauseWhenSongEnd && fadeOutEnabled) {
                            updateVolumeMultiplierForCurrentSong()
                        }

                        delay(TIMER_TICK_MS)
                    }
                }
        }
    }

    /**
     * Call when the player moves to another song or the queue ends.
     * If "end of song" mode is active, this pauses the player and deactivates the timer.
     */
    fun notifySongTransition() {
        if (pauseWhenSongEnd) {
            completeTimerAndPause()
        }
    }

    override fun clear() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        stopAfterCurrentSongOnTimeout = false
        fadeOutEnabled = false
        triggerTime = -1L
        updateVolumeMultiplier(1f)
    }

    private fun completeTimerAndPause() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        pauseWhenSongEnd = false
        stopAfterCurrentSongOnTimeout = false
        fadeOutEnabled = false
        triggerTime = -1L
        updateVolumeMultiplier(1f)
        pausePlayback()
    }

    private fun updateVolumeMultiplierForRemainingTime(remainingMs: Long) {
        updateVolumeMultiplier(volumeMultiplierForRemainingTime(remainingMs))
    }

    private fun updateVolumeMultiplierForCurrentSong() {
        val duration = durationMs
        if (duration <= 0) {
            updateVolumeMultiplier(1f)
            return
        }

        val remainingMs = (duration - positionMs).coerceAtLeast(0L)
        updateVolumeMultiplierForRemainingTime(remainingMs)
    }

    private fun volumeMultiplierForRemainingTime(remainingMs: Long): Float {
        if (remainingMs >= FADE_OUT_WINDOW_MS) return 1f
        return (remainingMs.toFloat() / FADE_OUT_WINDOW_MS).coerceIn(0f, 1f)
    }

    private fun updateVolumeMultiplier(multiplier: Float) {
        onVolumeMultiplierChanged(multiplier)
    }
}
