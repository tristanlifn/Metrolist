/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped

import com.metrolist.music.utils.WrappedPreloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class AndroidWrappedPreloader(
    private val wrappedManager: WrappedManager,
    private val wrappedAudioService: WrappedAudioService,
    scope: CoroutineScope,
) : WrappedPreloader {
    override val isDataReady =
        wrappedManager.state
            .map { it.isDataReady }
            .stateIn(scope, SharingStarted.Eagerly, wrappedManager.state.value.isDataReady)

    override suspend fun preload() {
        if (wrappedManager.state.value.isDataReady) return
        wrappedManager.prepare()
        val state = wrappedManager.state.first { it.isDataReady }
        val trackMap = state.trackMap
        if (trackMap.isNotEmpty()) {
            val firstTrackId = trackMap.entries.first().value
            wrappedAudioService.prepareTrack(firstTrackId)
        }
    }

    override fun dispose() = wrappedManager.dispose()
}
