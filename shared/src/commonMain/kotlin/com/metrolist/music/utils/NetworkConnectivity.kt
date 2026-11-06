/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import kotlinx.coroutines.flow.StateFlow

interface NetworkConnectivity {
    val networkStatus: StateFlow<Boolean>

    fun isCurrentlyConnected(): Boolean
}
