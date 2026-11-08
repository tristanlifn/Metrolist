/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import java.io.File

/** What backup and restore need from the platform: where the live data lives and how to restart around a restore. */
interface BackupPlatform {
    /** The DataStore preferences file. */
    val settingsFile: File

    /** The Room database file. */
    val databaseFile: File

    /** Saved playback state that belongs to the old library and is dropped on restore. */
    val playerStateFiles: List<File>

    /** Oldest database version this platform can migrate from. */
    val oldestRestorableDatabaseVersion: Int

    /** Stops playback and waits until nothing writes to the database. */
    suspend fun stopPlayback()

    fun restoreArtistNameAliases(aliases: Map<String, String>)

    /** Starts the app again so everything reopens the restored files; doesn't return. */
    fun restart()
}
