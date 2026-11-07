/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.listentogether

import kotlinx.coroutines.flow.StateFlow

enum class RoomRole {
    HOST,
    GUEST,
    NONE,
}

/** The parts of a Listen Together session that general screens and menus react to. */
interface ListenTogetherSession {
    val isInRoom: Boolean
    val isHost: Boolean
    val role: StateFlow<RoomRole>

    fun suggestTrack(
        id: String,
        title: String,
        artist: String,
        album: String?,
        duration: Long,
        thumbnail: String?,
    )
}
