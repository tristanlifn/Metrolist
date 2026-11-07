/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.google.protobuf.value
import com.metrolist.music.extensions.toEnum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.properties.ReadOnlyProperty
import timber.log.Timber
import java.io.File
import java.io.IOException

@Volatile private var dataStoreInstance: DataStore<Preferences>? = null
private val dataStoreLock = Any()

val Context.dataStore: DataStore<Preferences>
    get() {
        dataStoreInstance?.let { return it }
        synchronized(dataStoreLock) {
            dataStoreInstance?.let { return it }
            // Ensure the datastore parent directory exists before DataStore init.
            // On Samsung One UI 8.5+/Android 16 the directory can be missing after
            // deep sleep, causing IOException in FileStorageConnection.writeScope()
            // that crashes the process and triggers a soft reboot.
            File(filesDir, "datastore").mkdirs()
            val dataStoreDir = File(filesDir, "datastore")
            val dataStore = preferencesDataStore(name = "settings").getValue(this, ::dataStore)
            return object : DataStore<Preferences> by dataStore {
                override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
                    // Same Samsung workaround as above, re-applied before every write.
                    dataStoreDir.mkdirs()
                    return dataStore.updateData(transform)
                }
            }.also { dataStoreInstance = it }
        }
    }

/**
 * Safe DataStore write that ensures the parent directory exists before every edit.
 * Catches and reports IOException instead of crashing the coroutine scope.
 * Returns true if the write succeeded, false if it failed.
 */
suspend fun Context.safeDataStoreEdit(
    transform: suspend (MutablePreferences) -> Unit,
): Boolean = dataStore.safeEdit(transform)

fun <T> preference(
    context: Context,
    key: Preferences.Key<T>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key] ?: defaultValue }

inline fun <reified T : Enum<T>> enumPreference(
    context: Context,
    key: Preferences.Key<String>,
    defaultValue: T,
) = ReadOnlyProperty<Any?, T> { _, _ -> context.dataStore[key].toEnum(defaultValue) }
