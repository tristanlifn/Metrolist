/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import com.metrolist.desktop.playback.DesktopMusicPlayer
import com.metrolist.desktop.playback.LibC
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.BackupPlatform
import com.metrolist.music.viewmodels.BackupRestoreViewModel
import java.io.File

class DesktopBackupPlatform(
    private val player: DesktopMusicPlayer,
) : BackupPlatform {
    override val settingsFile = File(DesktopPaths.dataDir, BackupRestoreViewModel.SETTINGS_FILENAME)

    override val databaseFile = File(DesktopPaths.dataDir, InternalDatabase.DB_NAME)

    override val playerStateFiles = listOf(DesktopPaths.dataDir.resolve(QUEUE_SESSIONS_FILE))

    // Migrations below 25 are Android-only SQL; Room's auto-migrations cover everything after.
    override val oldestRestorableDatabaseVersion = 25

    override suspend fun stopPlayback() = player.release()

    // Desktop keeps artist aliases in memory only.
    override fun restoreArtistNameAliases(aliases: Map<String, String>) = ArtistNameAliases.replaceAll(aliases)

    override fun restart() {
        val info = ProcessHandle.current().info()
        ProcessBuilder(listOf(info.command().get()) + info.arguments().orElse(emptyArray()))
            .inheritIO()
            .start()
        // Skips the shutdown hook, which would save the old queue again over the restored data.
        System.out.flush()
        System.err.flush()
        LibC.INSTANCE._exit(0)
    }

    companion object {
        const val QUEUE_SESSIONS_FILE = "queue_sessions"
    }
}
