/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.metrolist.innertube.YouTube
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeAuthUserKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.VisitorDataKey
import com.metrolist.music.lyrics.LyricsImageSharer
import com.metrolist.music.ui.component.LyricsBackgroundStyle
import com.metrolist.music.ui.screens.settings.LyricsPosition
import com.metrolist.music.utils.AppSession
import com.metrolist.music.utils.NetworkConnectivity
import com.metrolist.music.utils.WrappedPreloader
import com.metrolist.music.utils.safeEdit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.NetworkInterface
import kotlin.system.exitProcess

// Placeholders until the desktop player, downloads and Wrapped land; they keep the shared screens working.

class DesktopNetworkConnectivity : NetworkConnectivity {
    override val networkStatus: StateFlow<Boolean> = MutableStateFlow(isCurrentlyConnected()).asStateFlow()

    override fun isCurrentlyConnected(): Boolean =
        runCatching { NetworkInterface.networkInterfaces().anyMatch { it.isUp && !it.isLoopback } }.getOrDefault(true)
}

class NoWrappedPreloader : WrappedPreloader {
    override val isDataReady: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()

    override suspend fun preload() = Unit

    override fun dispose() = Unit
}

class NoLyricsImageSharer : LyricsImageSharer {
    override suspend fun share(
        coverArtUrl: String?,
        songTitle: String,
        artistName: String,
        lyrics: String,
        width: Int,
        height: Int,
        backgroundColor: Int,
        backgroundStyle: LyricsBackgroundStyle,
        textColor: Int,
        secondaryTextColor: Int,
        alignment: LyricsPosition,
        chooserTitle: String,
    ) = Unit
}

class DesktopAppSession(
    private val dataStore: DataStore<Preferences>,
) : AppSession {
    override suspend fun forgetAccount() {
        dataStore.safeEdit { settings ->
            settings.remove(InnerTubeCookieKey)
            settings.remove(VisitorDataKey)
            settings.remove(DataSyncIdKey)
            settings.remove(InnerTubeAuthUserKey)
            settings.remove(AccountNameKey)
            settings.remove(AccountEmailKey)
            settings.remove(AccountChannelHandleKey)
        }
        YouTube.cookie = null
        YouTube.visitorData = null
        YouTube.dataSyncId = null
        YouTube.authUser = "0"
    }

    // There is no launcher to bring the app back, so restarting means closing; the user reopens it.
    override fun restart(): Unit = exitProcess(0)
}
