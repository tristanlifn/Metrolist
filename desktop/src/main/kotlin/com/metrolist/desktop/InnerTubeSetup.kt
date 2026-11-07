/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.YouTubeLocale
import com.metrolist.music.constants.ContentCountryKey
import com.metrolist.music.constants.ContentLanguageKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.SYSTEM_DEFAULT
import com.metrolist.music.constants.UseLoginForBrowse
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.utils.safeEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

/** Keeps the YouTube client's locale, visitor id and account in step with settings, as the Android App class does. */
fun CoroutineScope.syncInnerTubeSettings(dataStore: DataStore<Preferences>) {
    launch {
        val settings = dataStore.data.first()
        val locale = Locale.getDefault()
        YouTube.locale =
            YouTubeLocale(
                gl = settings[ContentCountryKey]?.takeIf { it != SYSTEM_DEFAULT } ?: locale.country.ifBlank { "US" },
                hl = settings[ContentLanguageKey]?.takeIf { it != SYSTEM_DEFAULT } ?: locale.language.ifBlank { "en" },
            )
        YouTube.useLoginForBrowse = settings[UseLoginForBrowse] ?: true
    }
    launch {
        dataStore.data.map { it[VisitorDataKey] }.distinctUntilChanged().collect { visitorData ->
            YouTube.visitorData =
                visitorData?.takeIf { it != "null" }
                    ?: YouTube.visitorData().getOrNull()?.also { fresh ->
                        dataStore.safeEdit { it[VisitorDataKey] = fresh }
                    }
        }
    }
    launch {
        dataStore.data.map { it[DataSyncIdKey] }.distinctUntilChanged().collect { dataSyncId ->
            YouTube.dataSyncId =
                dataSyncId?.let {
                    it.takeIf { !it.contains("||") }
                        ?: it.takeIf { it.endsWith("||") }?.substringBefore("||")
                        ?: it.substringAfter("||")
                }
        }
    }
    launch {
        dataStore.data.map { it[InnerTubeAuthUserKey] ?: "0" }.distinctUntilChanged().collect { YouTube.authUser = it }
    }
    launch {
        dataStore.data.map { it[InnerTubeCookieKey] }.distinctUntilChanged().collect { cookie ->
            runCatching { YouTube.cookie = cookie }.onFailure { Logger.e(it) { "Could not parse the saved cookie" } }
        }
    }
}
