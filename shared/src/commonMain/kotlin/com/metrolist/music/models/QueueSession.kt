/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.models

import com.metrolist.music.playback.queues.Queue
import java.io.Serializable

data class QueueSession(
    val id: Long,
    val title: String?,
    val items: List<MediaMetadata>,
    val mediaItemIndex: Int,
    val position: Long,
    val shuffleEnabled: Boolean,
    val shuffleOrder: List<Int>?,
    // Keeps pagination working when switching back within the same process; dropped on disk.
    @Transient val liveQueue: Queue? = null,
) : Serializable

data class PersistQueueSessions(
    val activeId: Long?,
    val sessions: List<QueueSession>,
) : Serializable
