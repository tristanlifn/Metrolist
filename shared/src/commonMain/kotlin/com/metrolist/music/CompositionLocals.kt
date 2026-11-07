/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.listentogether.ListenTogetherSession
import com.metrolist.music.playback.MusicPlayer
import com.metrolist.music.playback.SongDownloads
import com.metrolist.music.utils.SyncUtils
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.metrolist.music.models.MediaMetadata

val LocalDatabase = staticCompositionLocalOf<MusicDatabase> { error("No database provided") }
val LocalNavController = staticCompositionLocalOf<NavController> { error("No NavController provided") }
val LocalPlayerConnection = staticCompositionLocalOf<MusicPlayer?> { error("No PlayerConnection provided") }
val LocalPlayerAwareWindowInsets = compositionLocalOf<WindowInsets> { error("No WindowInsets provided") }
val LocalDownloadUtil = staticCompositionLocalOf<SongDownloads> { error("No DownloadUtil provided") }
val LocalListenTogetherManager = staticCompositionLocalOf<ListenTogetherSession?> { null }
val LocalSyncUtils = staticCompositionLocalOf<SyncUtils> { error("No SyncUtils provided") }
val LocalChangelogState = staticCompositionLocalOf<MutableState<Boolean>> { error("No LocalChangelogState provided") }
val LocalArtistNameAliases = staticCompositionLocalOf<Map<String, String>> { emptyMap() }
// Flavour-specific cast button; draws nothing where casting isn't available.
val LocalCastButton = staticCompositionLocalOf<@Composable (modifier: Modifier, tintColor: Color) -> Unit> { { _, _ -> } }
// Platform-only player menu features; the defaults hide them.
val LocalListenTogetherDialog =
    staticCompositionLocalOf<@Composable (visible: Boolean, mediaMetadata: MediaMetadata?, onDismiss: () -> Unit) -> Unit> {
        { _, _, _ -> }
    }
val LocalSystemEqualizerLauncher = staticCompositionLocalOf<@Composable () -> (() -> Unit)?> { { null } }
val LocalIsPlayerExpanded = compositionLocalOf { false }
