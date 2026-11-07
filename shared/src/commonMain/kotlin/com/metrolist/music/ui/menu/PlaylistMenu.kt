/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import com.metrolist.music.ui.utils.okLabel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.playlistSongSortDescendingKey
import com.metrolist.music.constants.playlistSongSortTypeKey
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistSong
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.TextFieldDialog
import com.metrolist.music.ui.menu.ExportDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.action_download
import com.metrolist.music.resources.add
import com.metrolist.music.resources.add_to_queue
import com.metrolist.music.resources.add_to_queue_desc
import com.metrolist.music.resources.cancel
import com.metrolist.music.resources.delete
import com.metrolist.music.resources.delete_desc
import com.metrolist.music.resources.delete_playlist_confirm
import com.metrolist.music.resources.download
import com.metrolist.music.resources.download_desc
import com.metrolist.music.resources.downloading
import com.metrolist.music.resources.edit
import com.metrolist.music.resources.edit_desc
import com.metrolist.music.resources.edit_playlist
import com.metrolist.music.resources.export_playlist
import com.metrolist.music.resources.favorite
import com.metrolist.music.resources.favorite_border
import com.metrolist.music.resources.offline
import com.metrolist.music.resources.pin_to_speed_dial
import com.metrolist.music.resources.play
import com.metrolist.music.resources.play_next
import com.metrolist.music.resources.play_next_desc
import com.metrolist.music.resources.playlist_play
import com.metrolist.music.resources.queue_music
import com.metrolist.music.resources.radio
import com.metrolist.music.resources.remove
import com.metrolist.music.resources.remove_download
import com.metrolist.music.resources.remove_download_playlist_confirm
import com.metrolist.music.resources.share
import com.metrolist.music.resources.share_desc
import com.metrolist.music.resources.shuffle
import com.metrolist.music.resources.start_radio
import com.metrolist.music.resources.start_radio_desc
import com.metrolist.music.resources.unpin_from_speed_dial
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.metrolist.music.resources.export_failed
import com.metrolist.music.resources.export_success
import com.metrolist.music.utils.PlatformActions
import org.koin.compose.koinInject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.utils.safeEdit
import com.metrolist.music.playback.DownloadState
import com.metrolist.music.playback.download
import com.metrolist.music.utils.PlaylistExporter
import com.metrolist.music.utils.exportPlaylist
import com.metrolist.music.ui.utils.isPortraitWindow

@Composable
fun PlaylistMenu(
    playlist: Playlist,
    coroutineScope: CoroutineScope,
    onDismiss: () -> Unit,
    autoPlaylist: Boolean? = false,
    downloadPlaylist: Boolean? = false,
    songList: List<Song>? = emptyList(),
) {
    val dataStore = koinInject<DataStore<Preferences>>()
    val platformActions = koinInject<PlatformActions>()
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val dbPlaylist by database.playlist(playlist.id).collectAsStateWithLifecycle(initialValue = playlist)
    var songs by remember {
        mutableStateOf(emptyList<Song>())
    }

    LaunchedEffect(Unit) {
        if (autoPlaylist == false) {
            database.playlistSongs(playlist.id).collect {
                songs = it.map(PlaylistSong::song)
            }
        } else {
            if (songList != null) {
                songs = songList
            }
        }
    }

    var downloadState by remember {
        mutableStateOf<DownloadState?>(DownloadState.STOPPED)
    }

    val editable: Boolean = playlist.playlist.isEditable == true

    val isPinned by database.speedDialDao.isPinned(playlist.id).collectAsStateWithLifecycle(initialValue = false)

    var showExportDialog by remember { mutableStateOf(false) }

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

    var showEditDialog by remember {
        mutableStateOf(false)
    }

    if (showEditDialog) {
        TextFieldDialog(
            icon = { Icon(painter = painterResource(Res.drawable.edit), contentDescription = null) },
            title = { Text(text = stringResource(Res.string.edit_playlist)) },
            onDismiss = { showEditDialog = false },
            initialTextFieldValue =
                TextFieldValue(
                    playlist.playlist.name,
                    TextRange(playlist.playlist.name.length),
                ),
            onDone = { name ->
                onDismiss()
                database.query {
                    update(
                        playlist.playlist.copy(
                            name = name,
                            lastUpdateTime = LocalDateTime.now(),
                        ),
                    )
                }
                coroutineScope.launch(Dispatchers.IO) {
                    playlist.playlist.browseId?.let { YouTube.renamePlaylist(it, name) }
                }
            },
        )
    }

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text =
                        stringResource(
                            Res.string.remove_download_playlist_confirm,
                            playlist.playlist.name,
                        ),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showRemoveDownloadDialog = false
                    },
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

    var showDeletePlaylistDialog by remember {
        mutableStateOf(false)
    }

    if (showDeletePlaylistDialog) {
        DefaultDialog(
            onDismiss = { showDeletePlaylistDialog = false },
            content = {
                Text(
                    text = stringResource(Res.string.delete_playlist_confirm, playlist.playlist.name),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 18.dp),
                )
            },
            buttons = {
                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                    },
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }

                TextButton(
                    onClick = {
                        showDeletePlaylistDialog = false
                        onDismiss()
                        database.transaction {
                            // First toggle the like using the same logic as the like button
                            if (playlist.playlist.bookmarkedAt != null) {
                                // Using the same toggleLike() method that's used in the like button
                                update(playlist.playlist.toggleLike())
                            }
                            // Then delete the playlist
                            delete(playlist.playlist)
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            playlist.playlist.browseId?.let { YouTube.deletePlaylist(it) }

                            dataStore.safeEdit {
                                it.remove(playlistSongSortTypeKey(playlistId = playlist.playlist.id))
                                it.remove(playlistSongSortDescendingKey(playlistId = playlist.playlist.id))
                            }
                        }
                    },
                ) {
                    Text(text = okLabel())
                }
            },
        )
    }

    PlaylistListItem(
        playlist = playlist,
        trailingContent = {
            if (playlist.playlist.isEditable != true) {
                IconButton(
                    onClick = {
                        database.query {
                            dbPlaylist?.playlist?.toggleLike()?.let { update(it) }
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

    Spacer(modifier = Modifier.height(12.dp))

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
                    listOfNotNull(
                        if (!isGuest) {
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
                                    onDismiss()
                                    if (songs.isNotEmpty()) {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = playlist.playlist.name,
                                                items = songs.map(Song::toMediaMetadata),
                                            ),
                                        )
                                    }
                                },
                            )
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
                                    onDismiss()
                                    if (songs.isNotEmpty()) {
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = playlist.playlist.name,
                                                items = songs.shuffled().map(Song::toMediaMetadata),
                                            ),
                                        )
                                    }
                                },
                            )
                        } else {
                            null
                        },
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.share),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(Res.string.share),
                            onClick = {
                                onDismiss()
                                platformActions.shareText("https://music.youtube.com/playlist?list=${dbPlaylist?.playlist?.browseId}")
                            },
                        ),
                    ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
                columns = if (isGuest) 2 else 3,
            )
        }

        item {
            Material3MenuGroup(
                items =
                    buildList {
                        if (!isGuest) {
                            playlist.playlist.browseId?.let { browseId ->
                                add(
                                    Material3MenuItemData(
                                        title = { Text(text = stringResource(Res.string.start_radio)) },
                                        description = { Text(text = stringResource(Res.string.start_radio_desc)) },
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.radio),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                YouTube.playlist(browseId).getOrNull()?.playlist?.let { playlistItem ->
                                                    playlistItem.radioEndpoint?.let { radioEndpoint ->
                                                        withContext(Dispatchers.Main) {
                                                            playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                        }
                                                    }
                                                }
                                            }
                                            onDismiss()
                                        },
                                    ),
                                )
                            }
                        }
                        if (!isGuest) {
                            add(
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
                                            playerConnection.playNext(songs.map { it.toMediaMetadata() })
                                        }
                                        onDismiss()
                                    },
                                ),
                            )
                        }
                        if (!isGuest) {
                            add(
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
                                        onDismiss()
                                        playerConnection.addToQueue(songs.map { it.toMediaMetadata() })
                                    },
                                ),
                            )
                        }
                    },
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    buildList {
                        if (editable && autoPlaylist != true && !isGuest) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.edit)) },
                                    description = { Text(text = stringResource(Res.string.edit_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.edit),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showEditDialog = true
                                    },
                                ),
                            )
                        }
                        add(
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
                                            database.speedDialDao.insert(
                                                SpeedDialItem(
                                                    id = playlist.id,
                                                    title = playlist.playlist.name,
                                                    subtitle = null,
                                                    subtitleIds = null,
                                                    thumbnailUrl = playlist.thumbnails.firstOrNull(),
                                                    type = "LOCAL_PLAYLIST",
                                                ),
                                            )
                                        }
                                    }
                                    onDismiss()
                                },
                            ),
                        )
                        if (downloadPlaylist != true) {
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
                        // Export playlist
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
                        if (autoPlaylist != true && !isGuest) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.delete)) },
                                    description = { Text(text = stringResource(Res.string.delete_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showDeletePlaylistDialog = true
                                    },
                                ),
                            )
                        }
                        playlist.playlist.shareLink?.let { shareLink ->
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
                                        platformActions.shareText(shareLink)
                                        onDismiss()
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
                val playlistSongs =
                    songs.map { s ->
                        com.metrolist.music.db.entities.PlaylistSong(
                            map =
                                com.metrolist.music.db.entities.PlaylistSongMap(
                                    songId = s.id,
                                    playlistId = playlist.id,
                                    position = 0,
                                ),
                            song = s,
                        )
                    }
                platformActions.exportPlaylist(
                    playlist.playlist.name,
                    format,
                    PlaylistExporter.export(format, playlistSongs),
                    save = false,
                    chooserTitle = exportPlaylistStr,
                )
                showExportDialog = false
            },
            onSave = { format ->
                val playlistSongs =
                    songs.map { s ->
                        com.metrolist.music.db.entities.PlaylistSong(
                            map =
                                com.metrolist.music.db.entities.PlaylistSongMap(
                                    songId = s.id,
                                    playlistId = playlist.id,
                                    position = 0,
                                ),
                            song = s,
                        )
                    }
                platformActions.exportPlaylist(
                    playlist.playlist.name,
                    format,
                    PlaylistExporter.export(format, playlistSongs),
                    save = true,
                    chooserTitle = exportPlaylistStr,
                )
                showExportDialog = false
            },
        )
    }
}
