/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.content.Intent
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.extensions.div
import com.metrolist.music.playback.MusicService
import com.metrolist.music.playback.MusicService.Companion.PERSISTENT_AUTOMIX_FILE
import com.metrolist.music.playback.MusicService.Companion.PERSISTENT_PLAYER_STATE_FILE
import com.metrolist.music.playback.MusicService.Companion.PERSISTENT_QUEUE_FILE
import com.metrolist.music.viewmodels.BackupRestoreViewModel
import java.io.File

class AndroidBackupPlatform(
    private val context: Context,
) : BackupPlatform {
    override val settingsFile: File
        get() = context.filesDir / "datastore" / BackupRestoreViewModel.SETTINGS_FILENAME

    override val databaseFile: File
        get() = context.getDatabasePath(InternalDatabase.DB_NAME)

    override val playerStateFiles: List<File>
        get() = listOf(PERSISTENT_QUEUE_FILE, PERSISTENT_AUTOMIX_FILE, PERSISTENT_PLAYER_STATE_FILE).map(context.filesDir::resolve)

    override val oldestRestorableDatabaseVersion = 1

    override suspend fun stopPlayback() {
        context.stopService(Intent(context, MusicService::class.java))
        MusicService.shutdownDeferred.await()
    }

    override fun restoreArtistNameAliases(aliases: Map<String, String>) = ArtistNameAliases.restore(context, aliases)

    override fun restart() {
        val intent =
            context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        context.startActivity(intent)
        Runtime.getRuntime().exit(0)
    }
}
