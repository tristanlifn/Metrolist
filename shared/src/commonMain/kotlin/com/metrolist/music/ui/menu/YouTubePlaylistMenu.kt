/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import com.metrolist.music.ui.utils.okLabel
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.utils.completed
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalArtistNameAliases
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.action_download
import com.metrolist.music.resources.add
import com.metrolist.music.resources.add_to_playlist
import com.metrolist.music.resources.add_to_playlist_desc
import com.metrolist.music.resources.add_to_queue
import com.metrolist.music.resources.add_to_queue_desc
import com.metrolist.music.resources.already_in_playlist
import com.metrolist.music.resources.cancel
import com.metrolist.music.resources.close
import com.metrolist.music.resources.download
import com.metrolist.music.resources.download_desc
import com.metrolist.music.resources.downloading
import com.metrolist.music.resources.error_podcast_subscribe
import com.metrolist.music.resources.error_podcast_unsubscribe
import com.metrolist.music.resources.export_playlist
import com.metrolist.music.resources.favorite
import com.metrolist.music.resources.favorite_border
import com.metrolist.music.resources.offline
import com.metrolist.music.resources.pin_to_speed_dial
import com.metrolist.music.resources.play
import com.metrolist.music.resources.play_next
import com.metrolist.music.resources.play_next_desc
import com.metrolist.music.resources.playlist_add
import com.metrolist.music.resources.playlist_play
import com.metrolist.music.resources.queue_music
import com.metrolist.music.resources.radio
import com.metrolist.music.resources.remove
import com.metrolist.music.resources.remove_download
import com.metrolist.music.resources.remove_download_playlist_confirm
import com.metrolist.music.resources.select
import com.metrolist.music.resources.select_all
import com.metrolist.music.resources.share
import com.metrolist.music.resources.share_desc
import com.metrolist.music.resources.shuffle
import com.metrolist.music.resources.start_radio
import com.metrolist.music.resources.unpin_from_speed_dial
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.metrolist.music.resources.export_failed
import com.metrolist.music.resources.export_success
import com.metrolist.music.utils.PlatformActions
import org.koin.compose.koinInject
import com.metrolist.music.playback.DownloadState
import com.metrolist.music.playback.download
import com.metrolist.music.utils.PlaylistExporter
import com.metrolist.music.utils.exportPlaylist
import co.touchlab.kermit.Logger
import com.metrolist.music.ui.utils.isPortraitWindow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTubePlaylistMenu(
    playlist: PlaylistItem,
    songs: List<SongItem> = emptyList(),
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    selectAction: () -> Unit = {},
    canSelect: Boolean = false,
) {
    val platformActions = koinInject<PlatformActions>()
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val dbPlaylist by database.playlistByBrowseId(playlist.id).collectAsStateWithLifecycle(initialValue = null)
    val isPinned by database.speedDialDao.isPinned(playlist.id).collectAsStateWithLifecycle(initialValue = false)
    val artistNameAliases = LocalArtistNameAliases.current

    var showChoosePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showImportPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var showErrorPlaylistAddDialog by rememberSaveable { mutableStateOf(false) }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<MediaMetadata>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            val allSongs =
                songs
                    .ifEmpty {
                        YouTube
                            .playlist(playlist.id)
                            .completed()
                            .getOrNull()
                            ?.songs
                            .orEmpty()
                    }.map {
                        it.toMediaMetadata()
                    }
            database.withTransaction {
                allSongs.forEach { insert(it) }
            }
            allSongs.map { it.id }
        },
        onGetSongIds = {
            songs.map { it.id }
        },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    YouTubeListItem(
        item = playlist,
        trailingContent = {
            if (playlist.id != "LM" && !playlist.isEditable) {
                IconButton(
                    onClick = {
                        val isCurrentlySaved = dbPlaylist?.playlist?.bookmarkedAt != null
                        if (dbPlaylist?.playlist == null) {
                            database.transaction {
                                val playlistEntity =
                                    PlaylistEntity(
                                        name = playlist.title,
                                        browseId = playlist.id,
                                        thumbnailUrl = playlist.thumbnail,
                                        isEditable = playlist.isEditable,
                                        remoteSongCount =
                                            playlist.songCountText?.let {
                                                Regex("""\d+""").find(it)?.value?.toIntOrNull()
                                            },
                                        playEndpointParams = playlist.playEndpoint?.params,
                                        shuffleEndpointParams = playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = playlist.radioEndpoint?.params,
                                    ).toggleLike()
                                insert(playlistEntity)
                            }
                        } else {
                            database.transaction {
                                val currentPlaylist = dbPlaylist!!.playlist
                                update(currentPlaylist, playlist)
                                update(currentPlaylist.toggleLike())
                            }
                        }
                        coroutineScope.launch(Dispatchers.IO) {
                            if (!isCurrentlySaved) {
                                val playlistFull = database.playlistByBrowseId(playlist.id).first()
                                if (playlistFull != null) {
                                    val songIds = songs
                                        .ifEmpty {
                                            YouTube
                                                .playlist(playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }.map { it.toMediaMetadata() }
                                        .onEach { database.transaction { insert(it) } }
                                        .map { it.id to it.setVideoId }
                                    database.addSongsToPlaylist(playlistFull, songIds)
                                }
                            }
                            if (playlist.isPodcast) {
                                YouTube
                                    .savePodcast(playlist.id, !isCurrentlySaved)
                                    .onSuccess {
                                        Logger.d { "[PODCAST_SAVE] savePodcast API success for ${playlist.id}" }
                                    }.onFailure { e ->
                                        Logger.e(e) { "[PODCAST_SAVE] savePodcast API failed for ${playlist.id}" }
                                        withContext(Dispatchers.Main) {
                                            platformActions.toast(if (isCurrentlySaved) Res.string.error_podcast_unsubscribe else Res.string.error_podcast_subscribe)
                                        }
                                    }
                            }
                        }
                    },
                ) {
                    Icon(
                        painter =
                            painterResource(
                                if (dbPlaylist?.playlist?.bookmarkedAt !=
                                    null
                                ) {
                                    Res.drawable.favorite
                                } else {
                                    Res.drawable.favorite_border
                                },
                            ),
                        tint =
                            if (dbPlaylist?.playlist?.bookmarkedAt !=
                                null
                            ) {
                                MaterialTheme.colorScheme.error
                            } else {
                                LocalContentColor.current
                            },
                        contentDescription = null,
                    )
                }
            }
        },
    )
    HorizontalDivider()

    var downloadState by remember {
        mutableStateOf<DownloadState?>(DownloadState.STOPPED)
    }
    LaunchedEffect(songs) {
        if (songs.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songs.all { downloads[it.id] == DownloadState.COMPLETED }) {
                    DownloadState.COMPLETED
                } else if (songs.all {
                        downloads[it.id] == DownloadState.QUEUED ||
                            downloads[it.id] == DownloadState.DOWNLOADING ||
                            downloads[it.id] == DownloadState.COMPLETED
                    }
                ) {
                    DownloadState.DOWNLOADING
                } else {
                    DownloadState.STOPPED
                }
        }
    }
    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }
    var showExportDialog by remember { mutableStateOf(false) }
    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text =
                        stringResource(
                            Res.string.remove_download_playlist_confirm,
                            playlist.title,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = { showRemoveDownloadDialog = false },
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                        songs.forEach { song ->
                            downloadUtil.remove(song.id)
                        }
                    },
                ) {
                    Text(text = okLabel())
                }
            },
        )
    }

    ImportPlaylistDialog(
        isVisible = showImportPlaylistDialog,
        onGetSong = {
            val allSongs =
                songs
                    .ifEmpty {
                        YouTube
                            .playlist(playlist.id)
                            .completed()
                            .getOrNull()
                            ?.songs
                            .orEmpty()
                    }.map {
                        it.toMediaMetadata()
                    }
            database.withTransaction {
                allSongs.forEach { insert(it) }
            }
            allSongs.map { it.id }
        },
        playlistTitle = playlist.title,
        onDismiss = { showImportPlaylistDialog = false },
    )

    if (showErrorPlaylistAddDialog) {
        ListDialog(
            onDismiss = {
                showErrorPlaylistAddDialog = false
                onDismiss()
            },
        ) {
            item {
                ListItem(
                    content = { Text(text = stringResource(Res.string.already_in_playlist)) },
                    leadingContent = {
                        Image(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier.size(ListThumbnailSize),
                        )
                    },
                    modifier = Modifier.clickable { showErrorPlaylistAddDialog = false },
                )
            }

            items(notAddedList) { song ->
                ListItem(
                    content = { Text(text = song.title) },
                    leadingContent = {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(ListThumbnailSize),
                        ) {
                            AsyncImage(
                                model = song.thumbnailUrl?.resize(200, 200),
                                contentDescription = null,
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(ThumbnailCornerRadius)),
                            )
                        }
                    },
                    supportingContent = {
                        Text(
                            text =
                                joinByBullet(
                                    song.artists.joinToString {
                                        ArtistNameAliases.resolve(artistNameAliases, it.id, it.name)
                                    },
                                    makeTimeString(song.duration * 1000L),
                                ),
                        )
                    },
                )
            }
        }
    }

    val isPortrait = isPortraitWindow()

    LazyColumn(
        contentPadding =
            PaddingValues(
                start = 0.dp,
                top = 0.dp,
                end = 0.dp,
                bottom = 8.dp + WindowInsets.systemBars.asPaddingValues().calculateBottomPadding(),
            ),
    ) {
        item {
            NewActionGrid(
                actions =
                    buildList {
                        if (!isGuest) {
                            playlist.playEndpoint?.let { playEndpoint ->
                                add(
                                    NewAction(
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.play),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        text = stringResource(Res.string.play),
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(playEndpoint))
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            playlist.shuffleEndpoint?.let { shuffleEndpoint ->
                                add(
                                    NewAction(
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.shuffle),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        text = stringResource(Res.string.shuffle),
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                            playlist.radioEndpoint?.let { radioEndpoint ->
                                add(
                                    NewAction(
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.radio),
                                                contentDescription = null,
                                                modifier = Modifier.size(28.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        },
                                        text = stringResource(Res.string.start_radio),
                                        onClick = {
                                            playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                        }
                    },
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }

        item {
            Material3MenuGroup(
                items =
                    listOfNotNull(
                        if (!isGuest) {
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.play_next)) },
                                description = { Text(text = stringResource(Res.string.play_next_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.playlist_play),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        songs
                                            .ifEmpty {
                                                withContext(Dispatchers.IO) {
                                                    YouTube
                                                        .playlist(playlist.id)
                                                        .completed()
                                                        .getOrNull()
                                                        ?.songs
                                                        .orEmpty()
                                                }
                                            }.let { songs ->
                                                playerConnection.playNext(
                                                    songs.map {
                                                        it
                                                            .copy(thumbnail = it.thumbnail.resize(544, 544))
                                                            .toMediaMetadata()
                                                    },
                                                )
                                            }
                                    }
                                    onDismiss()
                                },
                            )
                        } else {
                            null
                        },
                        if (!isGuest) {
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.add_to_queue)) },
                                description = { Text(text = stringResource(Res.string.add_to_queue_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.queue_music),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch {
                                        songs
                                            .ifEmpty {
                                                withContext(Dispatchers.IO) {
                                                    YouTube
                                                        .playlist(playlist.id)
                                                        .completed()
                                                        .getOrNull()
                                                        ?.songs
                                                        .orEmpty()
                                                }
                                            }.let { songs ->
                                                playerConnection.addToQueue(songs.map { it.toMediaMetadata() })
                                            }
                                    }
                                    onDismiss()
                                },
                            )
                        } else {
                            null
                        },
                        Material3MenuItemData(
                            title = { Text(text = stringResource(Res.string.add_to_playlist)) },
                            description = { Text(text = stringResource(Res.string.add_to_playlist_desc)) },
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.playlist_add),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                showChoosePlaylistDialog = true
                            },
                        ),
                        Material3MenuItemData(
                            title = {
                                Text(
                                    text = if (isPinned) stringResource(Res.string.unpin_from_speed_dial) else stringResource(Res.string.pin_to_speed_dial),
                                )
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(if (isPinned) Res.drawable.remove else Res.drawable.add),
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                coroutineScope.launch(Dispatchers.IO) {
                                    if (isPinned) {
                                        database.speedDialDao.delete(playlist.id)
                                    } else {
                                        database.speedDialDao.insert(SpeedDialItem.fromYTItem(playlist))
                                    }
                                }
                                onDismiss()
                            },
                        ),
                    ),
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    buildList {
                        if (songs.isNotEmpty()) {
                            add(
                                when (downloadState) {
                                    DownloadState.COMPLETED -> {
                                        Material3MenuItemData(
                                            title = {
                                                Text(
                                                    text = stringResource(Res.string.remove_download),
                                                )
                                            },
                                            icon = {
                                                Icon(
                                                    painter = painterResource(Res.drawable.offline),
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                showRemoveDownloadDialog = true
                                            },
                                        )
                                    }

                                    DownloadState.QUEUED, DownloadState.DOWNLOADING -> {
                                        Material3MenuItemData(
                                            title = { Text(text = stringResource(Res.string.downloading)) },
                                            icon = {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp,
                                                )
                                            },
                                            onClick = {
                                                showRemoveDownloadDialog = true
                                            },
                                        )
                                    }

                                    else -> {
                                        Material3MenuItemData(
                                            title = { Text(text = stringResource(Res.string.action_download)) },
                                            description = { Text(text = stringResource(Res.string.download_desc)) },
                                            icon = {
                                                Icon(
                                                    painter = painterResource(Res.drawable.download),
                                                    contentDescription = null,
                                                )
                                            },
                                            onClick = {
                                                songs.forEach { downloadUtil.download(it) }
                                            },
                                        )
                                    }
                                },
                            )
                        }
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.export_playlist)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.share),
                                        contentDescription = null,
                                    )
                                },
                                onClick = { showExportDialog = true },
                            ),
                        )
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.share)) },
                                description = { Text(text = stringResource(Res.string.share_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.share),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    platformActions.shareText(playlist.shareLink)
                                    onDismiss()
                                },
                            ),
                        )
                        if (canSelect) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.select)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.select_all),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        selectAction()
                                    },
                                ),
                            )
                        }
                    },
            )
        }
    }

    val exportPlaylistStr = stringResource(Res.string.export_playlist)

    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            onShare = { format ->
                coroutineScope.launch {
                    val ytSongs =
                        if (songs.isEmpty()) {
                            withContext(Dispatchers.IO) {
                                YouTube
                                    .playlist(playlist.id)
                                    .completed()
                                    .getOrNull()
                                    ?.songs
                                    .orEmpty()
                            }
                        } else {
                            songs
                        }

                    platformActions.exportPlaylist(
                        playlist.title,
                        format,
                        PlaylistExporter.exportYouTube(format, ytSongs),
                        save = false,
                        chooserTitle = exportPlaylistStr,
                    )
                }
                onDismiss()
            },
            onSave = { format ->
                coroutineScope.launch {
                    val ytSongs =
                        if (songs.isEmpty()) {
                            withContext(Dispatchers.IO) {
                                YouTube
                                    .playlist(playlist.id)
                                    .completed()
                                    .getOrNull()
                                    ?.songs
                                    .orEmpty()
                            }
                        } else {
                            songs
                        }

                    platformActions.exportPlaylist(
                        playlist.title,
                        format,
                        PlaylistExporter.exportYouTube(format, ytSongs),
                        save = true,
                        chooserTitle = exportPlaylistStr,
                    )
                }
                onDismiss()
            },
        )
    }
}

