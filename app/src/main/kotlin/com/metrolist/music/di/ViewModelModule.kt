/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.di

import com.metrolist.music.ui.screens.equalizer.EQViewModel
import com.metrolist.music.ui.screens.equalizer.wizard.WizardViewModel
import com.metrolist.music.viewmodels.AccountSettingsViewModel
import com.metrolist.music.viewmodels.AccountViewModel
import com.metrolist.music.viewmodels.AlbumViewModel
import com.metrolist.music.viewmodels.ArtistAlbumsViewModel
import com.metrolist.music.viewmodels.ArtistItemsViewModel
import com.metrolist.music.viewmodels.ArtistSongsViewModel
import com.metrolist.music.viewmodels.ArtistViewModel
import com.metrolist.music.viewmodels.AutoPlaylistViewModel
import com.metrolist.music.viewmodels.BackupRestoreViewModel
import com.metrolist.music.viewmodels.BrowseViewModel
import com.metrolist.music.viewmodels.CachePlaylistViewModel
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.viewmodels.HistoryViewModel
import com.metrolist.music.viewmodels.HomeViewModel
import com.metrolist.music.viewmodels.LibraryAlbumsViewModel
import com.metrolist.music.viewmodels.LibraryArtistsViewModel
import com.metrolist.music.viewmodels.LibraryMixViewModel
import com.metrolist.music.viewmodels.LibraryPlaylistsViewModel
import com.metrolist.music.viewmodels.LibraryPodcastsViewModel
import com.metrolist.music.viewmodels.LibrarySongsViewModel
import com.metrolist.music.viewmodels.LocalPlaylistViewModel
import com.metrolist.music.viewmodels.LocalSearchViewModel
import com.metrolist.music.viewmodels.LyricsMenuViewModel
import com.metrolist.music.viewmodels.LyricsViewModel
import com.metrolist.music.viewmodels.MoodAndGenresViewModel
import com.metrolist.music.viewmodels.NewReleaseViewModel
import com.metrolist.music.viewmodels.OnlinePlaylistViewModel
import com.metrolist.music.viewmodels.OnlinePodcastViewModel
import com.metrolist.music.viewmodels.OnlineSearchSuggestionViewModel
import com.metrolist.music.viewmodels.OnlineSearchViewModel
import com.metrolist.music.viewmodels.PlaylistsViewModel
import com.metrolist.music.viewmodels.StatsViewModel
import com.metrolist.music.viewmodels.TopPlaylistViewModel
import com.metrolist.music.viewmodels.YouTubeBrowseViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val viewModelModule = module {
    viewModelOf(::AccountSettingsViewModel)
    viewModelOf(::AccountViewModel)
    viewModelOf(::AlbumViewModel)
    viewModelOf(::ArtistAlbumsViewModel)
    viewModelOf(::ArtistItemsViewModel)
    viewModelOf(::ArtistSongsViewModel)
    viewModelOf(::ArtistViewModel)
    viewModelOf(::AutoPlaylistViewModel)
    viewModelOf(::BackupRestoreViewModel)
    viewModelOf(::BrowseViewModel)
    viewModelOf(::CachePlaylistViewModel)
    viewModelOf(::ChartsViewModel)
    viewModelOf(::EQViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::LibraryAlbumsViewModel)
    viewModelOf(::LibraryArtistsViewModel)
    viewModelOf(::LibraryMixViewModel)
    viewModelOf(::LibraryPlaylistsViewModel)
    viewModelOf(::LibraryPodcastsViewModel)
    viewModelOf(::LibrarySongsViewModel)
    viewModelOf(::LocalPlaylistViewModel)
    viewModelOf(::LocalSearchViewModel)
    viewModelOf(::LyricsMenuViewModel)
    viewModelOf(::LyricsViewModel)
    viewModelOf(::MoodAndGenresViewModel)
    viewModelOf(::NewReleaseViewModel)
    viewModelOf(::OnlinePlaylistViewModel)
    viewModelOf(::OnlinePodcastViewModel)
    viewModelOf(::OnlineSearchSuggestionViewModel)
    viewModelOf(::OnlineSearchViewModel)
    viewModelOf(::PlaylistsViewModel)
    viewModelOf(::StatsViewModel)
    viewModelOf(::TopPlaylistViewModel)
    viewModelOf(::WizardViewModel)
    viewModelOf(::YouTubeBrowseViewModel)
}
