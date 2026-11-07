/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.metrolist.music.extensions.toEnum
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun <T> rememberPreference(
    key: Preferences.Key<T>,
    defaultValue: T,
    removeIfDefault: Boolean = false
): MutableState<T> {
    val dataStore = koinInject<DataStore<Preferences>>()
    val coroutineScope = rememberCoroutineScope()

    val state =
        remember {
            dataStore.data
                .map { it[key] ?: defaultValue }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(defaultValue)

    return remember(key) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        dataStore.safeEdit {
                            if (removeIfDefault  && value == defaultValue)
                                it.remove(key)
                            else
                                it[key] = value
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}

@Composable
inline fun <reified T : Enum<T>> rememberEnumPreference(
    key: Preferences.Key<String>,
    defaultValue: T,
    removeIfDefault: Boolean = false
): MutableState<T> {
    val dataStore = koinInject<DataStore<Preferences>>()
    val coroutineScope = rememberCoroutineScope()

    val state =
        remember {
            dataStore.data
                .map { it[key].toEnum(defaultValue = defaultValue) }
                .distinctUntilChanged()
        }.collectAsStateWithLifecycle(defaultValue)

    return remember(key) {
        object : MutableState<T> {
            override var value: T
                get() = state.value
                set(value) {
                    coroutineScope.launch {
                        dataStore.safeEdit {
                            if (removeIfDefault  && value == defaultValue)
                                it.remove(key)
                            else
                                it[key] = value.name
                        }
                    }
                }

            override fun component1() = value

            override fun component2(): (T) -> Unit = { value = it }
        }
    }
}
