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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.metrolist.music.playback.QueueEntry
import com.metrolist.innertube.YouTube
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.action_download
import com.metrolist.music.resources.add_to_library
import com.metrolist.music.resources.add_to_playlist
import com.metrolist.music.resources.add_to_playlist_desc
import com.metrolist.music.resources.add_to_queue
import com.metrolist.music.resources.add_to_queue_desc
import com.metrolist.music.resources.cancel
import com.metrolist.music.resources.delete
import com.metrolist.music.resources.delete_uploaded_songs
import com.metrolist.music.resources.delete_uploaded_songs_confirm
import com.metrolist.music.resources.deleted_n_songs
import com.metrolist.music.resources.deleting
import com.metrolist.music.resources.dislike_all
import com.metrolist.music.resources.download
import com.metrolist.music.resources.downloading
import com.metrolist.music.resources.favorite
import com.metrolist.music.resources.favorite_border
import com.metrolist.music.resources.library_add
import com.metrolist.music.resources.library_add_check
import com.metrolist.music.resources.like_all
import com.metrolist.music.resources.offline
import com.metrolist.music.resources.play
import com.metrolist.music.resources.play_next
import com.metrolist.music.resources.play_next_desc
import com.metrolist.music.resources.playlist_add
import com.metrolist.music.resources.playlist_play
import com.metrolist.music.resources.queue_music
import com.metrolist.music.resources.remove_download
import com.metrolist.music.resources.remove_download_playlist_confirm
import com.metrolist.music.resources.remove_from_cache
import com.metrolist.music.resources.remove_from_library
import com.metrolist.music.resources.shuffle
import com.metrolist.music.resources.upload_progress
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.metrolist.music.utils.PlatformActions
import org.koin.compose.koinInject
import com.metrolist.music.playback.DownloadState
import com.metrolist.music.playback.download
import com.metrolist.music.ui.utils.isPortraitWindow
@Composable
fun SelectionSongMenu(
    songSelection: List<Song>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
    songPosition: List<PlaylistSongMap>? = emptyList(),
    isUploadedPlaylist: Boolean = false,
    onRemoveFromCache: (() -> Unit)? = null,
) {
    val platformActions = koinInject<PlatformActions>()
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val syncUtils = LocalSyncUtils.current
    val deletedNSongsTemplate = stringResource(Res.string.deleted_n_songs)
    val listenTogetherManager = com.metrolist.music.LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && listenTogetherManager.isHost == false

    val allInLibrary by remember {
        mutableStateOf(
            songSelection.all {
                it.song.inLibrary != null
            },
        )
    }

    val allLiked by remember(songSelection) {
        mutableStateOf(
            songSelection.isNotEmpty() &&
                songSelection.all {
                    it.song.liked
                },
        )
    }

    var downloadState by remember {
        mutableStateOf<DownloadState?>(DownloadState.STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id] == DownloadState.COMPLETED }) {
                    DownloadState.COMPLETED
                } else if (songSelection.all {
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

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = { songSelection.map { it.id } },
        onGetSongIds = { songSelection.map { it.id } },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    var showRemoveDownloadDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteUploadedDialog by remember {
        mutableStateOf(false)
    }
    var isDeleting by remember { mutableStateOf(false) }
    var deleteProgress by remember { mutableIntStateOf(0) }
    var totalToDelete by remember { mutableIntStateOf(0) }

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(Res.string.remove_download_playlist_confirm, "selection"),
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
                        songSelection.forEach { song ->
                            downloadUtil.remove(song.song.id)
                        }
                    },
                ) {
                    Text(text = okLabel())
                }
            },
        )
    }

    if (showDeleteUploadedDialog) {
        DefaultDialog(
            onDismiss = {
                if (!isDeleting) {
                    showDeleteUploadedDialog = false
                }
            },
            icon = {
                Icon(
                    painter = painterResource(Res.drawable.delete),
                    contentDescription = null,
                )
            },
            title = {
                Text(
                    if (isDeleting) {
                        stringResource(Res.string.deleting)
                    } else {
                        stringResource(Res.string.delete_uploaded_songs)
                    },
                )
            },
            buttons = {
                if (!isDeleting) {
                    TextButton(
                        onClick = { showDeleteUploadedDialog = false },
                    ) {
                        Text(text = stringResource(Res.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            totalToDelete = songSelection.size
                            deleteProgress = 0
                            isDeleting = true
                            val songsToDelete = songSelection.toList()
                            coroutineScope.launch(Dispatchers.IO) {
                                var successCount = 0
                                songsToDelete.forEachIndexed { index, song ->
                                    deleteProgress = index + 1
                                    val entityId = song.song.uploadEntityId
                                    if (entityId != null) {
                                        YouTube.deleteUploadedSong(entityId).onSuccess {
                                            database.query {
                                                delete(song.song)
                                            }
                                            successCount++
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    platformActions.toast(String.format(deletedNSongsTemplate, successCount))
                                    isDeleting = false
                                    showDeleteUploadedDialog = false
                                    onDismiss()
                                    clearAction()
                                }
                            }
                        },
                    ) {
                        Text(text = stringResource(Res.string.delete))
                    }
                }
            },
        ) {
            if (isDeleting) {
                Text(
                    text = stringResource(Res.string.upload_progress, deleteProgress, totalToDelete),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { if (totalToDelete > 0) deleteProgress.toFloat() / totalToDelete else 0f },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = stringResource(Res.string.delete_uploaded_songs_confirm, songSelection.size),
                    style = MaterialTheme.typography.bodyLarge,
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
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "Selection",
                                            items = songSelection.map { it.toMediaMetadata() },
                                        ),
                                    )
                                    clearAction()
                                },
                            )
                        } else {
                            null
                        },
                        if (!isGuest) {
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
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = "Selection",
                                            items = songSelection.shuffled().map { it.toMediaMetadata() },
                                        ),
                                    )
                                    clearAction()
                                },
                            )
                        } else {
                            null
                        },
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.playlist_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(Res.string.add_to_playlist),
                            onClick = {
                                showChoosePlaylistDialog = true
                            },
                        ),
                    ),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }
        item {
            Material3MenuGroup(
                items =
                    buildList {
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
                                        onDismiss()
                                        playerConnection.playNext(songSelection.map { it.toMediaMetadata() })
                                        clearAction()
                                    },
                                ),
                            )
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.shuffle)) },
                                    description = { Text(text = stringResource(Res.string.add_to_queue_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.shuffle),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Selection",
                                                items = songSelection.shuffled().map { it.toMediaMetadata() },
                                            ),
                                        )
                                        clearAction()
                                    },
                                ),
                            )
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
                                        playerConnection.addToQueue(songSelection.map { it.toMediaMetadata() })
                                        clearAction()
                                    },
                                ),
                            )
                        }
                        add(
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
                        )
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text =
                                            stringResource(
                                                if (allInLibrary) Res.string.remove_from_library else Res.string.add_to_library,
                                            ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (allInLibrary) Res.drawable.library_add_check else Res.drawable.library_add,
                                            ),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    if (allInLibrary) {
                                        database.query {
                                            songSelection.forEach { song ->
                                                inLibrary(song.id, null)
                                            }
                                        }
                                        coroutineScope.launch {
                                            // Use the new reliable method that fetches fresh tokens
                                            songSelection.forEach { song ->
                                                YouTube.toggleSongLibrary(song.id, false)
                                            }
                                        }
                                    } else {
                                        database.transaction {
                                            songSelection.forEach { song ->
                                                insert(song.toMediaMetadata())
                                                inLibrary(song.id, LocalDateTime.now())
                                            }
                                        }
                                        coroutineScope.launch {
                                            // Use the new reliable method that fetches fresh tokens
                                            songSelection
                                                .filter { it.song.inLibrary == null }
                                                .forEach { song ->
                                                    YouTube.toggleSongLibrary(song.id, true)
                                                }
                                        }
                                    }
                                },
                            ),
                        )
                    },
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    buildList {
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
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.download),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            songSelection.forEach { downloadUtil.download(it) }
                                        },
                                    )
                                }
                            },
                        )
                        onRemoveFromCache?.let { removeFromCache ->
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.remove_from_cache)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        removeFromCache()
                                        clearAction()
                                    },
                                ),
                            )
                        }
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text =
                                            stringResource(
                                                if (allLiked) Res.string.dislike_all else Res.string.like_all,
                                            ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (allLiked) Res.drawable.favorite else Res.drawable.favorite_border,
                                            ),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    val allLiked = songSelection.all { it.song.liked }
                                    onDismiss()
                                    database.query {
                                        songSelection.forEach { song ->
                                            if ((!allLiked && !song.song.liked) || allLiked) {
                                                val s = song.song.toggleLike()
                                                update(s)
                                                syncUtils.likeSong(s)
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                        if (songPosition?.isNotEmpty() == true) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.delete)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        var i = 0
                                        database.query {
                                            songPosition.forEach { cur ->
                                                move(cur.playlistId, cur.position - i, Int.MAX_VALUE)
                                                delete(cur.copy(position = Int.MAX_VALUE))
                                                i++
                                            }
                                        }
                                        clearAction()
                                    },
                                ),
                            )
                        }
                        if (isUploadedPlaylist) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.delete_uploaded_songs)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        showDeleteUploadedDialog = true
                                    },
                                ),
                            )
                        }
                    },
            )
        }
    }
}
@Composable
fun SelectionMediaMetadataMenu(
    songSelection: List<MediaMetadata>,
    currentItems: List<QueueEntry>,
    onDismiss: () -> Unit,
    clearAction: () -> Unit,
) {
    val database = LocalDatabase.current
    val downloadUtil = LocalDownloadUtil.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = com.metrolist.music.LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && listenTogetherManager.isHost == false

    val allLiked by remember(songSelection) {
        mutableStateOf(songSelection.isNotEmpty() && songSelection.all { it.liked })
    }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val notAddedList by remember {
        mutableStateOf(mutableListOf<Song>())
    }

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            // Safe: AddToPlaylistDialog wraps this lambda in withContext(Dispatchers.IO)
            songSelection.map { song ->
                database.insert(song)
                song.id
            }
        },
        onGetSongIds = { songSelection.map { it.id } },
        onDismiss = { showChoosePlaylistDialog = false },
    )

    var downloadState by remember {
        mutableStateOf<DownloadState?>(DownloadState.STOPPED)
    }

    LaunchedEffect(songSelection) {
        if (songSelection.isEmpty()) return@LaunchedEffect
        downloadUtil.downloads.collect { downloads ->
            downloadState =
                if (songSelection.all { downloads[it.id] == DownloadState.COMPLETED }) {
                    DownloadState.COMPLETED
                } else if (songSelection.all {
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

    if (showRemoveDownloadDialog) {
        DefaultDialog(
            onDismiss = { showRemoveDownloadDialog = false },
            content = {
                Text(
                    text = stringResource(Res.string.remove_download_playlist_confirm, "selection"),
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
                        songSelection.forEach { song ->
                            downloadUtil.remove(song.id)
                        }
                    },
                ) {
                    Text(text = okLabel())
                }
            },
        )
    }

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
            Material3MenuGroup(
                items =
                    buildList {
                        if (currentItems.isNotEmpty() && !isGuest) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.delete)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.delete),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        var i = 0
                                        currentItems.forEach { cur ->
                                            playerConnection.removeItem(cur.index - i++)
                                        }
                                        clearAction()
                                    },
                                ),
                            )
                        }
                        if (!isGuest) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.play)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.play),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Selection",
                                                items = songSelection,
                                            ),
                                        )
                                        clearAction()
                                    },
                                ),
                            )
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.shuffle)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.shuffle),
                                            contentDescription = null,
                                        )
                                    },
                                    onClick = {
                                        onDismiss()
                                        playerConnection.playQueue(
                                            ListQueue(
                                                title = "Selection",
                                                items = songSelection.shuffled(),
                                            ),
                                        )
                                        clearAction()
                                    },
                                ),
                            )
                            if (!isGuest) {
                                add(
                                    Material3MenuItemData(
                                        title = { Text(text = stringResource(Res.string.add_to_queue)) },
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.queue_music),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            onDismiss()
                                            playerConnection.addToQueue(songSelection)
                                            clearAction()
                                        },
                                    ),
                                )
                            }
                        }
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.add_to_playlist)) },
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
                        )
                    },
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    buildList {
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text = stringResource(Res.string.like_all),
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (allLiked) Res.drawable.favorite else Res.drawable.favorite_border,
                                            ),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    database.query {
                                        if (allLiked) {
                                            songSelection.forEach { song ->
                                                update(song.toSongEntity().toggleLike())
                                            }
                                        } else {
                                            songSelection.filter { !it.liked }.forEach { song ->
                                                update(song.toSongEntity().toggleLike())
                                            }
                                        }
                                    }
                                },
                            ),
                        )
                        add(
                            when (downloadState) {
                                DownloadState.COMPLETED -> {
                                    Material3MenuItemData(
                                        title = {
                                            Text(
                                                text = stringResource(Res.string.remove_download),
                                                color = MaterialTheme.colorScheme.surface,
                                            )
                                        },
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.offline),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.surface,
                                            )
                                        },
                                        onClick = {
                                            showRemoveDownloadDialog = true
                                        },
                                        cardColors =
                                            CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.onSurface,
                                            ),
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
                                        icon = {
                                            Icon(
                                                painter = painterResource(Res.drawable.download),
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            songSelection.forEach { downloadUtil.download(it) }
                                        },
                                    )
                                }
                            },
                        )
                    },
            )
        }
    }
}
