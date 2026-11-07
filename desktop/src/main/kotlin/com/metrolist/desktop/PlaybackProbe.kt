/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import com.metrolist.desktop.playback.StreamResolver
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

// Developer check that stream extraction works: finds a song and plays it with the mpv binary.
// Run with `--probe <query>`.
fun runPlaybackProbe(args: List<String>): Unit = runBlocking {
    val query = args.joinToString(" ").ifBlank { "Daft Punk Get Lucky" }
    val song =
        YouTube.search(query, YouTube.SearchFilter.FILTER_SONG).getOrThrow()
            .items.filterIsInstance<SongItem>().firstOrNull()
            ?: run {
                System.err.println("No song found for \"$query\"")
                exitProcess(1)
            }
    println("Found: ${song.title} - ${song.artists.joinToString { it.name }} (${song.id})")

    val stream =
        StreamResolver().resolve(song.id) ?: run {
            System.err.println("No playable stream for ${song.id}")
            exitProcess(1)
        }
    println("Stream: client=${stream.clientName}")

    val command =
        listOf("mpv", "--no-video", "--term-osd-bar") +
            stream.headers.map { (key, value) -> "--http-header-fields-append=$key: $value" } +
            stream.url
    exitProcess(ProcessBuilder(command).inheritIO().start().waitFor())
}
