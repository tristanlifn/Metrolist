/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import com.metrolist.innertube.models.SongItem
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.export_failed
import com.metrolist.music.resources.export_success

object PlaylistExporter {
    /** [format] is "csv" or "m3u", as picked in the export dialog. */
    fun export(format: String, songs: List<PlaylistSong>): String =
        if (format == "csv") {
            buildString {
                // Add CSV header
                append("Title,Artist,Album,YouTube Video ID\n")

                // Add each song as a CSV row
                songs.forEach { playlistSong ->
                    val song = playlistSong.song.song
                    val artists = playlistSong.song.artists
                    val album = playlistSong.song.album
                    append("\"${song.title.replace("\"", "\"\"")}\"")
                    append(",")
                    append("\"${artists.joinToString("; ") { it.name.replace("\"", "\"\"") }}\"")
                    append(",")
                    append("\"${album?.title?.replace("\"", "\"\"") ?: ""}\"")
                    append(",")
                    append("${song.id}")
                    append("\n")
                }
            }
        } else {
            buildString {
                // Add M3U header
                append("#EXTM3U\n")

                // Add each song as M3U entry
                songs.forEach { playlistSong ->
                    val song = playlistSong.song.song
                    val artists = playlistSong.song.artists
                    append("#EXTINF:${song.duration},")
                    append("${artists.joinToString(";") { it.name }} - ${song.title}")
                    append("\n")
                    append("https://youtube.com/watch?v=${song.id}\n")
                }
            }
        }

    fun exportYouTube(format: String, songs: List<SongItem>): String =
        if (format == "csv") {
            buildString {
                // Add CSV header
                append("Title,Artist,Album,YouTube Video ID\n")

                // Add each song as a CSV row
                songs.forEach { songItem ->
                    append("\"${songItem.title.replace("\"", "\"\"")}\"")
                    append(",")
                    append("\"${songItem.artists.joinToString("; ") { it.name.replace("\"", "\"\"") }}\"")
                    append(",")
                    append("\"${songItem.album?.name?.replace("\"", "\"\"") ?: ""}\"")
                    append(",")
                    append("${songItem.id}")
                    append("\n")
                }
            }
        } else {
            buildString {
                // Add M3U header
                append("#EXTM3U\n")

                // Add each song as M3U entry
                songs.forEach { songItem ->
                    append("#EXTINF:${songItem.duration},")
                    append("${songItem.artists.joinToString(" - ") { it.name }} - ${songItem.title}")
                    append("\n")
                    // For M3U, we would typically include a URL, but since we don't have direct URLs,
                    // we'll use a placeholder that indicates this is a YouTube Music track
                    append("#YTM:${songItem.id}\n")
                }
            }
        }
}

/** Shares or saves an exported playlist, reporting the outcome with a toast. */
fun PlatformActions.exportPlaylist(
    playlistName: String,
    format: String,
    content: String,
    save: Boolean,
    chooserTitle: String,
) {
    val fileName = "$playlistName.$format"
    val mimeType = if (format == "csv") "text/csv" else "audio/x-mpegurl"
    if (save) {
        saveToDocuments(fileName, mimeType, content)
            .onSuccess { toast(Res.string.export_success) }
            .onFailure { toast(Res.string.export_failed) }
    } else {
        shareFile(fileName, mimeType, content, chooserTitle)
            .onFailure { toast(Res.string.export_failed) }
    }
}
