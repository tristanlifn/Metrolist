/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import com.metrolist.desktop.mpris.MprisService
import com.metrolist.desktop.playback.DesktopMusicPlayer
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

// Developer check of the real player at volume 0: plays a search result, then skips once.
// Run with `--player-check <query>`.
fun runPlayerCheck(
    player: DesktopMusicPlayer,
    args: List<String>,
): Unit = runBlocking {
    val query = args.filter { it.toLongOrNull() == null }.joinToString(" ").ifBlank { "Daft Punk Get Lucky" }
    val songs = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow().items.filterIsInstance<SongItem>().take(3)
    player.setVolume(0f)
    player.playQueue(ListQueue(title = query, items = songs.map { it.toMediaMetadata() }))
    repeat(4) {
        delay(4_000)
        println("t=${(it + 1) * 4}s ${player.mediaMetadata.value?.title} state=${player.playbackState.value} playing=${player.isPlaying.value} pos=${player.currentPosition}ms dur=${player.duration}ms client=${player.currentStreamClient.value} error=${player.error.value?.cause?.message}")
    }
    player.skipToNextItem()
    delay(6_000)
    println("after skip: ${player.mediaMetadata.value?.title} state=${player.playbackState.value} pos=${player.currentPosition}ms queue=${player.queue.value.size} at ${player.currentQueuePosition.value}")
    // Stays up with MPRIS published so it can be driven from playerctl; pass a number of seconds to wait.
    val mpris = MprisService.start(player) {}
    delay((args.lastOrNull()?.toLongOrNull() ?: 0L) * 1000)
    println("end: ${player.mediaMetadata.value?.title} state=${player.playbackState.value} playing=${player.isPlaying.value} repeat=${player.repeatMode.value} shuffle=${player.shuffleModeEnabled.value} volume=${player.volume.value}")
    mpris?.close()
    player.release()
    exitProcess(0)
}
