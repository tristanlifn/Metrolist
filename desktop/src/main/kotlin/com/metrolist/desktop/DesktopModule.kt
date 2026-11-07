/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.metrolist.desktop.playback.DesktopMusicPlayer
import com.metrolist.desktop.playback.DesktopSongDownloads
import com.metrolist.desktop.playback.StreamResolver
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.build
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.lyrics.LyricsImageSharer
import com.metrolist.music.playback.DownloadSongCache
import com.metrolist.music.playback.MusicPlayer
import com.metrolist.music.playback.SongCache
import com.metrolist.music.playback.SongDownloads
import com.metrolist.music.utils.AppSession
import com.metrolist.music.utils.NetworkConnectivity
import com.metrolist.music.utils.PlatformActions
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.WrappedPreloader
import okio.Path.Companion.toOkioPath
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module
import java.io.File

val desktopModule = module {
    single<DataStore<Preferences>> {
        PreferenceDataStoreFactory.createWithPath { File(DesktopPaths.dataDir, "settings.preferences_pb").toOkioPath() }
    }
    single { InternalDatabase.build(File(DesktopPaths.dataDir, "song.db")) }
    single { MusicDatabase(get()) }
    single { DesktopPlatformActions() } bind PlatformActions::class
    single<NetworkConnectivity> { DesktopNetworkConnectivity() }
    single { SyncUtils(DesktopPaths.dataDir, get(), get(), get()) }
    single<SongCache> { NoSongCache() }
    single { StreamResolver() }
    single { DesktopSongDownloads(get(), get(), DesktopPaths.dataDir.resolve("downloads")) } bind SongDownloads::class
    single<SongCache>(DownloadSongCache) { get<DesktopSongDownloads>() }
    single<WrappedPreloader> { NoWrappedPreloader() }
    single<LyricsImageSharer> { NoLyricsImageSharer() }
    single<AppSession> { DesktopAppSession(get()) }
    factoryOf(::LyricsHelper)
    single { DesktopMusicPlayer(get(), get(), get(), get(), get(), DesktopPaths.dataDir.resolve("queue_sessions")) } bind MusicPlayer::class
}
