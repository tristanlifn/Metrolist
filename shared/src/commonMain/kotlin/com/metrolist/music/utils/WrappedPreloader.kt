/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import kotlinx.coroutines.flow.StateFlow

/** Loads the yearly Wrapped data ahead of time so its screens open without waiting. */
interface WrappedPreloader {
    val isDataReady: StateFlow<Boolean>

    suspend fun preload()

    fun dispose()
}
