/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

/** Same states as Media3's Download, so the shared UI doesn't depend on Media3. */
enum class DownloadState { QUEUED, STOPPED, DOWNLOADING, COMPLETED, FAILED, REMOVING, RESTARTING }

/** Offline song downloads, keyed by song id. */
interface SongDownloads {
    val downloads: StateFlow<Map<String, DownloadState>>

    fun getDownload(songId: String): Flow<DownloadState?> = downloads.map { it[songId] }

    fun download(mediaMetadata: MediaMetadata)

    fun download(songId: String)

    fun remove(songId: String)
}

fun SongDownloads.download(song: Song) = download(song.toMediaMetadata())

fun SongDownloads.download(song: SongItem) = download(song.toMediaMetadata())
