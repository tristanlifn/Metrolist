/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db

import androidx.room.Room
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import java.io.File

internal actual suspend fun InternalDatabase.inTransaction(block: suspend () -> Unit) =
    useWriterConnection { connection -> connection.immediateTransaction { block() } }

// Desktop databases are created at the current version, so the Android-only manual migrations aren't needed.
fun InternalDatabase.Companion.build(file: File): InternalDatabase =
    Room
        .databaseBuilder<InternalDatabase>(name = file.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
