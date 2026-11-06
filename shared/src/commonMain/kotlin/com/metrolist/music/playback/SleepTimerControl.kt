/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

interface SleepTimerControl {
    /** Wall-clock time in ms when playback pauses, or -1 when not timed. */
    val triggerTime: Long
    val pauseWhenSongEnd: Boolean
    val isActive: Boolean

    /** A [minute] of -1 pauses at the end of the current song. */
    fun start(minute: Int)

    fun start(
        minute: Int,
        stopAfterCurrentSong: Boolean,
        fadeOut: Boolean,
    )

    fun clear()
}
