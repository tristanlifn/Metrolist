/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.AppSession
import com.metrolist.music.utils.safeEdit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AccountSettingsViewModel(
    private val syncUtils: SyncUtils,
    private val dataStore: DataStore<Preferences>,
    private val appSession: AppSession,
) : ViewModel() {

    /**
     * Clear all library data including songs, albums, artists, playlists, podcasts.
     */
    suspend fun clearAllLibraryData() {
        Logger.d { "[LOGOUT_CLEAR] ViewModel: clearAllLibraryData called" }
        syncUtils.clearAllLibraryData()
        Logger.d { "[LOGOUT_CLEAR] ViewModel: clearAllLibraryData completed" }
    }

    /**
     * Forget the account FIRST (clearing auth so all background syncs skip),
     * THEN clear all library data. This prevents sync operations that are
     * triggered by the database becoming empty from re-adding songs.
     */
    suspend fun logoutAndClearLibraryData() {
        Logger.d { "[LOGOUT_CLEAR] ViewModel: logoutAndClearLibraryData called" }
        withContext(Dispatchers.IO) {
            // Forget account first — clears cookie/auth from DataStore.
            // Once isLoggedIn() returns false, ALL sync operations will skip.
            appSession.forgetAccount()

            // Now clear the local database. Any sync coroutines that observe
            // the empty state will check isLoggedIn() and skip silently.
            syncUtils.clearAllLibraryData()
        }
        Logger.d { "[LOGOUT_CLEAR] ViewModel: logoutAndClearLibraryData completed" }
    }

    /**
     * Just logout without clearing library data
     */
    suspend fun logoutKeepData(onCookieChange: (String) -> Unit) {
        Logger.d { "[LOGOUT_KEEP] ViewModel: logoutKeepData called" }
        withContext(Dispatchers.IO) {
            appSession.forgetAccount()
        }
        Logger.d { "[LOGOUT_KEEP] ViewModel: Account forgotten, clearing cookie in UI" }
        onCookieChange("")
    }

    /**
     * Save token credentials atomically to DataStore, then restart the app.
     * This ensures all writes complete before the process is killed,
     * preventing the race condition where Runtime.exit(0) kills the process
     * before async DataStore coroutines finish writing.
     */
    fun saveTokenAndRestart(
        cookie: String,
        visitorData: String,
        dataSyncId: String,
        authUser: String,
        accountName: String,
        accountEmail: String,
        accountChannelHandle: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = dataStore.safeEdit { settings ->
                settings[InnerTubeCookieKey] = cookie
                settings[VisitorDataKey] = visitorData
                settings[DataSyncIdKey] = dataSyncId
                settings[InnerTubeAuthUserKey] = authUser.filter(Char::isDigit).ifBlank { "0" }
                settings[AccountNameKey] = accountName
                settings[AccountEmailKey] = accountEmail
                settings[AccountChannelHandleKey] = accountChannelHandle
            }
            if (!saved) {
                Logger.e { "saveTokenAndRestart: DataStore write failed — skipping restart to avoid losing credentials" }
                return@launch
            }
            withContext(Dispatchers.Main) {
                appSession.restart()
            }
        }
    }
}
