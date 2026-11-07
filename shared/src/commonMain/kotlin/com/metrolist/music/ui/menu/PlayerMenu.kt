/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import com.metrolist.music.ui.utils.okLabel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.VarispeedKey
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.ListDialog
import com.metrolist.music.ui.component.Material3MenuGroup
import com.metrolist.music.ui.component.Material3MenuItemData
import com.metrolist.music.ui.component.NewAction
import com.metrolist.music.ui.component.NewActionGrid
import com.metrolist.music.ui.component.VolumeSlider
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.action_download
import com.metrolist.music.resources.add
import com.metrolist.music.resources.add_to_library
import com.metrolist.music.resources.add_to_playlist
import com.metrolist.music.resources.advanced
import com.metrolist.music.resources.advanced_desc
import com.metrolist.music.resources.album
import com.metrolist.music.resources.approve
import com.metrolist.music.resources.artist
import com.metrolist.music.resources.cancel
import com.metrolist.music.resources.cast
import com.metrolist.music.resources.casting_to
import com.metrolist.music.resources.check
import com.metrolist.music.resources.close
import com.metrolist.music.resources.connect
import com.metrolist.music.resources.connected_users
import com.metrolist.music.resources.content_copy
import com.metrolist.music.resources.copy_code
import com.metrolist.music.resources.copy_link
import com.metrolist.music.resources.create_room
import com.metrolist.music.resources.crown
import com.metrolist.music.resources.details
import com.metrolist.music.resources.details_desc
import com.metrolist.music.resources.disconnect
import com.metrolist.music.resources.discover_tune
import com.metrolist.music.resources.download
import com.metrolist.music.resources.downloading
import com.metrolist.music.resources.enter_username
import com.metrolist.music.resources.equalizer
import com.metrolist.music.resources.equalizer_desc
import com.metrolist.music.resources.error
import com.metrolist.music.resources.graphic_eq
import com.metrolist.music.resources.group
import com.metrolist.music.resources.host_label
import com.metrolist.music.resources.hosting_room
import com.metrolist.music.resources.in_room
import com.metrolist.music.resources.info
import com.metrolist.music.resources.invalid_room_code
import com.metrolist.music.resources.join_existing_room
import com.metrolist.music.resources.join_request_denied
import com.metrolist.music.resources.join_room
import com.metrolist.music.resources.joining_room
import com.metrolist.music.resources.kick_user
import com.metrolist.music.resources.kick_user_desc
import com.metrolist.music.resources.leave_room
import com.metrolist.music.resources.library_add
import com.metrolist.music.resources.library_add_check
import com.metrolist.music.resources.link
import com.metrolist.music.resources.link_copied
import com.metrolist.music.resources.listen_together
import com.metrolist.music.resources.listen_together_background_disconnect_note
import com.metrolist.music.resources.listen_together_connected
import com.metrolist.music.resources.listen_together_connecting
import com.metrolist.music.resources.listen_together_description
import com.metrolist.music.resources.listen_together_disconnected
import com.metrolist.music.resources.listen_together_error
import com.metrolist.music.resources.listen_together_not_configured
import com.metrolist.music.resources.listen_together_reconnecting
import com.metrolist.music.resources.login
import com.metrolist.music.resources.logout
import com.metrolist.music.resources.manage_user
import com.metrolist.music.resources.mic
import com.metrolist.music.resources.offline
import com.metrolist.music.resources.pending_requests
import com.metrolist.music.resources.pending_suggestions
import com.metrolist.music.resources.permanently_kick_user
import com.metrolist.music.resources.permanently_kick_user_desc
import com.metrolist.music.resources.person
import com.metrolist.music.resources.pin_to_speed_dial
import com.metrolist.music.resources.playlist_add
import com.metrolist.music.resources.queue_music
import com.metrolist.music.resources.radio
import com.metrolist.music.resources.reject
import com.metrolist.music.resources.remove
import com.metrolist.music.resources.remove_download
import com.metrolist.music.resources.remove_from_library
import com.metrolist.music.resources.replay
import com.metrolist.music.resources.reset
import com.metrolist.music.resources.resync
import com.metrolist.music.resources.room_code
import com.metrolist.music.resources.speed
import com.metrolist.music.resources.start_radio
import com.metrolist.music.resources.starting_radio
import com.metrolist.music.resources.system_equalizer
import com.metrolist.music.resources.system_equalizer_desc
import com.metrolist.music.resources.tempo_and_pitch
import com.metrolist.music.resources.token
import com.metrolist.music.resources.transfer_ownership
import com.metrolist.music.resources.transfer_ownership_desc
import com.metrolist.music.resources.tune
import com.metrolist.music.resources.unpin_from_speed_dial
import com.metrolist.music.resources.user_blocked_by_host
import com.metrolist.music.resources.username
import com.metrolist.music.resources.view_album
import com.metrolist.music.resources.view_artist
import com.metrolist.music.resources.view_podcast
import com.metrolist.music.resources.waiting_for_approval
import com.metrolist.music.resources.you_label
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.DrawableResource
import com.metrolist.music.resources.copied_to_clipboard
import com.metrolist.music.resources.creating_room
import com.metrolist.music.resources.error_username_empty
import com.metrolist.music.utils.PlatformActions
import org.koin.compose.koinInject
import com.metrolist.music.playback.DownloadState
import com.metrolist.music.playback.download
import com.metrolist.music.LocalListenTogetherDialog
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalSystemEqualizerLauncher
import com.metrolist.music.ui.utils.isPortraitWindow

@Composable
fun PlayerMenu(
    mediaMetadata: MediaMetadata?,
    playerBottomSheetState: BottomSheetState,
    isQueueTrigger: Boolean? = false,
    onShowDetailsDialog: () -> Unit,
    onDismiss: () -> Unit,
) {
    mediaMetadata ?: return
    val navController = LocalNavController.current
    val platformActions = koinInject<PlatformActions>()
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val playerVolume = playerConnection.volume.collectAsStateWithLifecycle()

    // Cast state for volume control - safely access castConnectionHandler to prevent crashes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.cast
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castVolume by castHandler?.castVolume?.collectAsStateWithLifecycle() ?: remember { mutableFloatStateOf(1f) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsStateWithLifecycle() ?: remember { mutableStateOf<String?>(null) }

    val varispeedMode by rememberPreference(VarispeedKey, defaultValue = false)

    val librarySong by database.song(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = null)
    val coroutineScope = rememberCoroutineScope()
    val downloadUtil = LocalDownloadUtil.current

    val download by downloadUtil
        .getDownload(mediaMetadata.id)
        .collectAsStateWithLifecycle(initialValue = null)

    val isPinned by database.speedDialDao.isPinned(mediaMetadata.id).collectAsStateWithLifecycle(initialValue = false)

    val artists =
        remember(mediaMetadata.artists) {
            mediaMetadata.artists.filter { it.id != null }
        }

    var showChoosePlaylistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showListenTogetherDialog by rememberSaveable {
        mutableStateOf(false)
    }

    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = com.metrolist.music.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == com.metrolist.music.listentogether.RoomRole.GUEST
    val pendingSuggestionCount by listenTogetherManager?.pendingSuggestionCount?.collectAsStateWithLifecycle(initialValue = 0)
        ?: remember { mutableStateOf(0) }

    val openSystemEqualizer = LocalSystemEqualizerLauncher.current()

    AddToPlaylistDialog(
        isVisible = showChoosePlaylistDialog,
        onGetSong = {
            database.withTransaction {
                insert(mediaMetadata)
            }
            listOf(mediaMetadata.id)
        },
        onGetSongIds = { listOf(mediaMetadata.id) },
        onDismiss = {
            showChoosePlaylistDialog = false
        },
    )

    LocalListenTogetherDialog.current(showListenTogetherDialog, mediaMetadata) { showListenTogetherDialog = false }

    var showSelectArtistDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSelectArtistDialog) {
        ListDialog(
            onDismiss = { showSelectArtistDialog = false },
        ) {
            items(artists) { artist ->
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier =
                        Modifier
                            .fillParentMaxWidth()
                            .height(ListItemHeight)
                            .clickable {
                                navController.navigate("artist/${artist.id}")
                                showSelectArtistDialog = false
                                playerBottomSheetState.collapseSoft()
                                onDismiss()
                            }.padding(horizontal = 24.dp),
                ) {
                    Text(
                        text = artist.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    var showPitchTempoDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showPitchTempoDialog) {
        TempoPitchDialog(
            onDismiss = { showPitchTempoDialog = false },
        )
    }

    var showSpeedDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if (showSpeedDialog) {
        SpeedDialog(
            onDismiss = { showSpeedDialog = false },
        )
    }

    if (isQueueTrigger != true) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 6.dp),
        ) {
            // Show Cast indicator when casting
            if (isCasting && castDeviceName != null) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.cast),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(Res.string.casting_to, castDeviceName ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                VolumeSlider(
                    value = if (isCasting) castVolume else playerVolume.value,
                    onValueChange = { volume ->
                        if (isCasting) {
                            castHandler?.setVolume(volume)
                        } else {
                            playerConnection.setVolume(volume)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

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
            val startingRadioText = stringResource(Res.string.starting_radio)
            NewActionGrid(
                actions =
                    listOfNotNull(
                        if (!isListenTogetherGuest) {
                            NewAction(
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.radio),
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                                text = stringResource(Res.string.start_radio),
                                onClick = {
                                    platformActions.toast(startingRadioText)
                                    playerConnection.startRadioSeamlessly()
                                    onDismiss()
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
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(Res.string.add_to_playlist),
                            onClick = { showChoosePlaylistDialog = true },
                        ),
                        NewAction(
                            icon = {
                                Icon(
                                    painter = painterResource(Res.drawable.link),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            text = stringResource(Res.string.copy_link),
                            onClick = {
                                platformActions.copyToClipboard("Song Link", "https://music.youtube.com/watch?v=${mediaMetadata.id}")
                                platformActions.toast(Res.string.link_copied)
                                onDismiss()
                            },
                        ),
                    ),
                columns = if (isListenTogetherGuest) 2 else 3,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp),
            )
        }

        item {
            // Check if this is a podcast episode (album ID doesn't start with MPREb_)
            val isPodcast = mediaMetadata.album?.let { !it.id.startsWith("MPREb_") } ?: false

            Material3MenuGroup(
                items =
                    buildList {
                        // Don't show "View Artist" for podcasts - only show "View Podcast"
                        if (artists.isNotEmpty() && !isPodcast) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.view_artist)) },
                                    description = {
                                        Text(
                                            text = mediaMetadata.artists.joinToString { it.name },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.artist),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        if (mediaMetadata.artists.size == 1) {
                                            navController.navigate("artist/${mediaMetadata.artists[0].id}")
                                            playerBottomSheetState.collapseSoft()
                                            onDismiss()
                                        } else {
                                            showSelectArtistDialog = true
                                        }
                                    },
                                ),
                            )
                        }
                        val currentAlbum = mediaMetadata.album
                        if (currentAlbum != null) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(if (isPodcast) Res.string.view_podcast else Res.string.view_album)) },
                                    description = {
                                        Text(
                                            text = currentAlbum.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    icon = {
                                        Icon(
                                            painter = painterResource(if (isPodcast) Res.drawable.mic else Res.drawable.album),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        if (isPodcast) {
                                            navController.navigate("online_podcast/${currentAlbum.id}")
                                        } else {
                                            navController.navigate("album/${currentAlbum.id}")
                                        }
                                        playerBottomSheetState.collapseSoft()
                                        onDismiss()
                                    },
                                ),
                            )
                        }
                        // Add to Library option
                        val isInLibrary = librarySong?.song?.inLibrary != null
                        add(
                            Material3MenuItemData(
                                title = {
                                    Text(
                                        text =
                                            stringResource(
                                                if (isInLibrary) {
                                                    Res.string.remove_from_library
                                                } else {
                                                    Res.string.add_to_library
                                                },
                                            ),
                                    )
                                },
                                icon = {
                                    Icon(
                                        painter =
                                            painterResource(
                                                if (isInLibrary) {
                                                    Res.drawable.library_add_check
                                                } else {
                                                    Res.drawable.library_add
                                                },
                                            ),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    playerConnection.toggleLibrary()
                                    onDismiss()
                                },
                            ),
                        )
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
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        if (isPinned) {
                                            database.speedDialDao.delete(mediaMetadata.id)
                                        } else {
                                            database.speedDialDao.insert(SpeedDialItem.fromYTItem(mediaMetadata.toYTItem()))
                                        }
                                    }
                                    onDismiss()
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
                    listOf(
                        when (download) {
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
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        downloadUtil.remove(mediaMetadata.id)
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
                                        downloadUtil.remove(mediaMetadata.id)
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
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        downloadUtil.download(mediaMetadata)
                                    },
                                )
                            }
                        },
                    ),
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            Material3MenuGroup(
                items =
                    buildList {
                        if (listenTogetherManager != null) add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.listen_together)) },
                                icon = {
                                    // Show a small badge when there are pending suggestions
                                    Box {
                                        Icon(
                                            painter = painterResource(Res.drawable.group),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        if (pendingSuggestionCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier =
                                                    Modifier
                                                        .offset(x = 8.dp, y = (-6).dp)
                                                        .align(Alignment.TopEnd),
                                            ) {
                                                Text(
                                                    text = pendingSuggestionCount.toString(),
                                                    color = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = { showListenTogetherDialog = true },
                            ),
                        )
                        if (isListenTogetherGuest) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.resync)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.replay),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        listenTogetherManager?.requestSync()
                                        onDismiss()
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
                        add(
                            Material3MenuItemData(
                                title = { Text(text = stringResource(Res.string.details)) },
                                description = { Text(text = stringResource(Res.string.details_desc)) },
                                icon = {
                                    Icon(
                                        painter = painterResource(Res.drawable.info),
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                    )
                                },
                                onClick = {
                                    onShowDetailsDialog()
                                    onDismiss()
                                },
                            ),
                        )

                        if (isQueueTrigger != true) {
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.equalizer)) },
                                    description = { Text(text = stringResource(Res.string.equalizer_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.equalizer),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        navController.navigate("equalizer")
                                        onDismiss()
                                    },
                                ),
                            )
                            if (openSystemEqualizer != null) add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.system_equalizer)) },
                                    description = { Text(text = stringResource(Res.string.system_equalizer_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.graphic_eq),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        openSystemEqualizer()
                                        onDismiss()
                                    },
                                ),
                            )
                            add(
                                Material3MenuItemData(
                                    title = { Text(text = stringResource(Res.string.advanced)) },
                                    description = { Text(text = stringResource(Res.string.advanced_desc)) },
                                    icon = {
                                        Icon(
                                            painter = painterResource(Res.drawable.tune),
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    },
                                    onClick = {
                                        if (!varispeedMode) showPitchTempoDialog = true
                                        else showSpeedDialog = true
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
fun TempoPitchDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var tempo by remember {
        mutableFloatStateOf(playerConnection.playbackSpeed)
    }
    var transposeValue by remember {
        mutableIntStateOf(round(12 * log2(playerConnection.pitch)).toInt())
    }
    val updatePlaybackParameters = {
        playerConnection.playbackSpeed = tempo
        playerConnection.pitch = 2f.pow(transposeValue.toFloat() / 12)
    }
    val listenTogetherManager = LocalListenTogetherManager.current
    val isInRoom = listenTogetherManager?.isInRoom ?: false

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.tempo_and_pitch))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    tempo = 1f
                    transposeValue = 0
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(Res.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(okLabel())
            }
        },
        text = {
            Column {
                if (!isInRoom) {
                    ValueAdjuster(
                        icon = Res.drawable.speed,
                        currentValue = tempo,
                        values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                        onValueUpdate = {
                            tempo = it
                            updatePlaybackParameters()
                        },
                        valueText = { "x$it" },
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                ValueAdjuster(
                    icon = Res.drawable.discover_tune,
                    currentValue = transposeValue,
                    values = (-12..12).toList(),
                    onValueUpdate = {
                        transposeValue = it
                        updatePlaybackParameters()
                    },
                    valueText = { "${if (it > 0) "+" else ""}$it" },
                )
            }
        },
    )
}

@Composable
fun SpeedDialog(onDismiss: () -> Unit) {
    val playerConnection = LocalPlayerConnection.current ?: return
    var speed by remember {
        mutableFloatStateOf(playerConnection.playbackSpeed)
    }
    val updatePlaybackParameters = {
        playerConnection.playbackSpeed = speed
        playerConnection.pitch = speed
    }
    val listenTogetherManager = LocalListenTogetherManager.current

    AlertDialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(Res.string.speed))
        },
        dismissButton = {
            TextButton(
                onClick = {
                    speed = 1f
                    updatePlaybackParameters()
                },
            ) {
                Text(stringResource(Res.string.reset))
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(okLabel())
            }
        },
        text = {
            Column {
                ValueAdjuster(
                    icon = Res.drawable.speed,
                    currentValue = speed,
                    values = (0..35).map { round((0.25f + it * 0.05f) * 100) / 100 },
                    onValueUpdate = {
                        speed = it
                        updatePlaybackParameters()
                    },
                    valueText = { "x$it" },
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        },
    )
}
@Composable
fun <T> ValueAdjuster(
    icon: DrawableResource,
    currentValue: T,
    values: List<T>,
    onValueUpdate: (T) -> Unit,
    valueText: (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
        )

        IconButton(
            enabled = currentValue != values.first(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) - 1])
            },
        ) {
            Icon(
                painter = painterResource(Res.drawable.remove),
                contentDescription = null,
            )
        }

        Text(
            text = valueText(currentValue),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(80.dp),
        )

        IconButton(
            enabled = currentValue != values.last(),
            onClick = {
                onValueUpdate(values[values.indexOf(currentValue) + 1])
            },
        ) {
            Icon(
                painter = painterResource(Res.drawable.add),
                contentDescription = null,
            )
        }
    }
}
