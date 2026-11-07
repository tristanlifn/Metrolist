/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.metrolist.music.constants.MaxSongCacheSizeKey
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.newInternalDatabaseInstance
import com.metrolist.music.eq.EqualizerService
import com.metrolist.music.eq.data.EQProfileRepository
import com.metrolist.music.listentogether.ListenTogetherClient
import com.metrolist.music.listentogether.ListenTogetherManager
import com.metrolist.music.lyrics.LyricsHelper
import com.metrolist.music.playback.DownloadUtil
import com.metrolist.music.playback.MediaLibrarySessionCallback
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.AndroidPlatformActions
import com.metrolist.music.utils.PlatformActions
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.get
import com.metrolist.music.widget.MetrolistWidgetManager
import com.metrolist.music.widget.PlaylistWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import com.metrolist.music.playback.Media3SongCache
import com.metrolist.music.playback.SongCache
import com.metrolist.music.utils.AndroidAppSession
import com.metrolist.music.utils.AppSession
import org.koin.dsl.module

val appModule = module {
    single<CoroutineScope>(ApplicationScope) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single<DataStore<Preferences>> { get<Context>().dataStore }

    single { InternalDatabase.newInternalDatabaseInstance(get()) }
    single { get<InternalDatabase>().dao }
    single { MusicDatabase(get()) }

    single<DatabaseProvider> { StandaloneDatabaseProvider(get()) }
    single<Cache>(PlayerCache) {
        val context = get<Context>()
        val cacheSize = context.dataStore[MaxSongCacheSizeKey] ?: 1024
        val evictor = when (cacheSize) {
            -1 -> NoOpCacheEvictor()
            else -> LeastRecentlyUsedCacheEvictor(cacheSize * 1024 * 1024L)
        }
        SimpleCache(
            context.filesDir.resolve("exoplayer"),
            evictor,
            get(),
        )
    }
    single<Cache>(DownloadCache) {
        SimpleCache(
            get<Context>().filesDir.resolve("download"),
            NoOpCacheEvictor(),
            get(),
        )
    }
    single<SongCache> { Media3SongCache(get(PlayerCache)) }
    single<PlatformActions> { AndroidPlatformActions(get()) }
    single { DownloadUtil(get(), get(), get(), get(DownloadCache), get(PlayerCache)) }

    singleOf(::ListenTogetherClient)
    singleOf(::ListenTogetherManager)
    single { SyncUtils(get<Context>().filesDir, get(), get(), get()) }
    singleOf(::EqualizerService)
    singleOf(::EQProfileRepository)
    single { PlaylistWidgetManager(get(), get(), get(ApplicationScope)) }
    singleOf(::MetrolistWidgetManager)
    singleOf(::AndroidAppSession) bind AppSession::class

    // Unscoped in Hilt, so a new instance per injection.
    factoryOf(::LyricsHelper)
    factoryOf(::MediaLibrarySessionCallback)
}
