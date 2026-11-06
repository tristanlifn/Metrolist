/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.IOException

operator fun <T> DataStore<Preferences>.get(key: Preferences.Key<T>): T? =
    runBlocking(Dispatchers.IO) {
        data.first()[key]
    }

fun <T> DataStore<Preferences>.get(
    key: Preferences.Key<T>,
    defaultValue: T,
): T =
    runBlocking(Dispatchers.IO) {
        data.first()[key] ?: defaultValue
    }

/**
 * DataStore write that reports an IOException instead of crashing the coroutine scope.
 * Returns true if the write succeeded, false if it failed.
 */
suspend fun DataStore<Preferences>.safeEdit(transform: suspend (MutablePreferences) -> Unit): Boolean =
    try {
        edit(transform)
        true
    } catch (e: IOException) {
        Logger.e(e) { "DataStore edit failed" }
        reportException(e)
        false
    }
