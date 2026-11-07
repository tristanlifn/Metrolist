/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.menu

import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.utils.okLabel
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.res.Configuration
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalDownloadUtil
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.VarispeedKey
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.listentogether.ListenTogetherEvent
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
import com.metrolist.music.listentogether.ListenTogetherManager

@Composable
fun ListenTogetherDialog(
    visible: Boolean,
    mediaMetadata: MediaMetadata?,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val platformActions = koinInject<PlatformActions>()
    val listenTogetherManager = koinInject<ListenTogetherManager>()
    val joiningRoomTemplate = stringResource(Res.string.joining_room)

    // Handle case where manager is not available
    if (listenTogetherManager == null) {
        ListDialog(onDismiss = onDismiss) {
            item {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.group),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(Res.string.listen_together),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.listen_together_not_configured),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                    ) {
                        Text(okLabel())
                    }
                }
            }
        }
        return
    }

    val connectionState by listenTogetherManager.connectionState.collectAsStateWithLifecycle()
    val roomState by listenTogetherManager.roomState.collectAsStateWithLifecycle()
    val userId by listenTogetherManager.userId.collectAsStateWithLifecycle()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsStateWithLifecycle()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsStateWithLifecycle()

    // Load saved username
    var savedUsername by rememberPreference(com.metrolist.music.constants.ListenTogetherUsernameKey, "")
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    // Local UI state for join/create actions
    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    // User action menu state
    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    // Localized helper strings
    val waitingForApprovalText = stringResource(Res.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(Res.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(Res.string.join_request_denied)

    // User action menu dialog
    if (selectedUserForMenu != null && selectedUsername != null) {
        ListDialog(
            onDismiss = {
                selectedUserForMenu = null
                selectedUsername = null
            },
        ) {
            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.group),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = stringResource(Res.string.manage_user),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = selectedUsername ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }

            // Kick button
            item {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clickable {
                                selectedUserForMenu?.let {
                                    listenTogetherManager.kickUser(it, "Removed by host")
                                }
                                selectedUserForMenu = null
                                selectedUsername = null
                            },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.kick_user),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                text = stringResource(Res.string.kick_user_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Permanently kick button
            item {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clickable {
                                selectedUserForMenu?.let { userId ->
                                    selectedUsername?.let { username ->
                                        listenTogetherManager.blockUser(username)
                                        listenTogetherManager.kickUser(userId, Res.string.user_blocked_by_host.toString())
                                    }
                                }
                                selectedUserForMenu = null
                                selectedUsername = null
                            },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.permanently_kick_user),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.permanently_kick_user_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Transfer ownership button
            item {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .clickable {
                                selectedUserForMenu?.let {
                                    listenTogetherManager.transferHost(it)
                                }
                                selectedUserForMenu = null
                                selectedUsername = null
                            },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.crown),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.transfer_ownership),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = stringResource(Res.string.transfer_ownership_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
        return
    }

    // Sync usernameInput when savedUsername changes
    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }

    // Listen to low level events to update UI state (join rejected, approved, room created)
    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage =
                        when {
                            reason.isNullOrBlank() -> joinRequestDeniedText
                            reason.contains("invalid", ignoreCase = true) == true -> invalidRoomCodeText
                            else -> "$joinRequestDeniedText: $reason"
                        }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }

                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }

                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    platformActions.copyToClipboard("ListenTogetherRoom", event.roomCode)
                }

                else -> { /* ignore other events here */ }
            }
        }
    }

    // Check if already in a room
    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId

    ListDialog(onDismiss = onDismiss) {
        // Header - Icon on left, text left-aligned
        item {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.group),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text =
                        if (isInRoom) {
                            if (isHost) stringResource(Res.string.hosting_room) else stringResource(Res.string.in_room)
                        } else {
                            stringResource(Res.string.listen_together)
                        },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Connection status
        item {
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color =
                    when (connectionState) {
                        ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        ConnectionState.ERROR -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.surfaceVariant
                    },
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(10.dp)
                                    .background(
                                        color =
                                            when (connectionState) {
                                                ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                                ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary
                                                ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                                ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.outline
                                            },
                                        shape = RoundedCornerShape(50),
                                    ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text =
                                when (connectionState) {
                                    ConnectionState.CONNECTED -> stringResource(Res.string.listen_together_connected)
                                    ConnectionState.CONNECTING -> stringResource(Res.string.listen_together_connecting)
                                    ConnectionState.RECONNECTING -> stringResource(Res.string.listen_together_reconnecting)
                                    ConnectionState.ERROR -> stringResource(Res.string.listen_together_error)
                                    ConnectionState.DISCONNECTED -> stringResource(Res.string.listen_together_disconnected)
                                },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                when (connectionState) {
                                    ConnectionState.CONNECTED -> MaterialTheme.colorScheme.primary
                                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> MaterialTheme.colorScheme.secondary
                                    ConnectionState.ERROR -> MaterialTheme.colorScheme.error
                                    ConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }

                    if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                            Button(
                                onClick = { listenTogetherManager.connect() },
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Text(stringResource(Res.string.connect), fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Button(
                                onClick = { listenTogetherManager.disconnect() },
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                    ),
                            ) {
                                Text(stringResource(Res.string.disconnect), fontWeight = FontWeight.SemiBold)
                            }
                            FilledTonalButton(
                                onClick = { listenTogetherManager.forceReconnect() },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text("Reconnect", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
            item {
                Text(
                    text = stringResource(Res.string.listen_together_background_disconnect_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (isInRoom) {
            // Room status card
            roomState?.let { room ->
                item {
                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(Res.string.room_code),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = room.roomCode,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 6.sp,
                                )
                            }
                            if (isHost) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val inviteLink =
                                    remember(room.roomCode) {
                                        "https://metrolist.cc/listen?code=${room.roomCode}"
                                    }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            platformActions.copyToClipboard("Listen Together Link", inviteLink)
                                            platformActions.toast(Res.string.copied_to_clipboard)
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.link),
                                            contentDescription = stringResource(Res.string.copy_link),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(Res.string.copy_link))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    FilledTonalButton(
                                        onClick = {
                                            platformActions.copyToClipboard("Room Code", room.roomCode)
                                            platformActions.toast(Res.string.copied_to_clipboard)
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.content_copy),
                                            contentDescription = stringResource(Res.string.copy_code),
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(Res.string.copy_code))
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                // Connected users - horizontal layout
                val connectedUsers = room.usersList.filter { it.isConnected }

                item {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.connected_users, connectedUsers.size),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 12.dp),
                        )

                        // Horizontal scrollable row for users
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            connectedUsers.forEach { user ->
                                // User avatar card
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier =
                                        Modifier
                                            .width(72.dp)
                                            .clickable(
                                                enabled = isHost && user.userId != userId,
                                                onClick = {
                                                    selectedUserForMenu = user.userId
                                                    selectedUsername = user.username
                                                },
                                            ),
                                ) {
                                    // Circular avatar
                                    Box(
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(52.dp),
                                            shape = RoundedCornerShape(50),
                                            color =
                                                if (user.isHost) {
                                                    MaterialTheme.colorScheme.primary
                                                } else if (user.userId == userId) {
                                                    MaterialTheme.colorScheme.secondary
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant
                                                },
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize(),
                                            ) {
                                                Text(
                                                    text = user.username.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color =
                                                        if (user.isHost) {
                                                            MaterialTheme.colorScheme.onPrimary
                                                        } else if (user.userId == userId) {
                                                            MaterialTheme.colorScheme.onSecondary
                                                        } else {
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                )
                                            }
                                        }

                                        // Host/You badge
                                        if (user.isHost || user.userId == userId) {
                                            Surface(
                                                modifier =
                                                    Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .offset(x = 4.dp, y = 4.dp)
                                                        .size(18.dp),
                                                shape = RoundedCornerShape(50),
                                                color = if (user.isHost) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.fillMaxSize(),
                                                ) {
                                                    Icon(
                                                        painter =
                                                            painterResource(
                                                                if (user.isHost) Res.drawable.crown else Res.drawable.person,
                                                            ),
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(12.dp),
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Username
                                    Text(
                                        text = user.username,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (user.userId == userId) FontWeight.Bold else FontWeight.Medium,
                                        color =
                                            if (user.isHost) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                    )

                                    // Role label
                                    if (user.isHost) {
                                        Text(
                                            text = stringResource(Res.string.host_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        )
                                    } else if (user.userId == userId) {
                                        Text(
                                            text = stringResource(Res.string.you_label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending join requests (host only)
                if (isHost && pendingJoinRequests.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.pending_requests),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pendingJoinRequests) { request ->
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondary,
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize(),
                                        ) {
                                            Text(
                                                text = request.username.take(1).uppercase(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondary,
                                            )
                                        }
                                    }
                                    Text(
                                        text = request.username,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { listenTogetherManager.approveJoin(request.userId) },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.check),
                                            contentDescription = stringResource(Res.string.approve),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { listenTogetherManager.rejectJoin(request.userId, "Rejected by host") },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.close),
                                            contentDescription = stringResource(Res.string.reject),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pending suggestions (host only)
                if (isHost && pendingSuggestions.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(Res.string.pending_suggestions),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(pendingSuggestions) { suggestion ->
                        Surface(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(
                                        painter = painterResource(Res.drawable.queue_music),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.trackInfo.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = suggestion.fromUsername,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { listenTogetherManager.approveSuggestion(suggestion.suggestionId) },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.check),
                                            contentDescription = stringResource(Res.string.approve),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                    IconButton(
                                        onClick = { listenTogetherManager.rejectSuggestion(suggestion.suggestionId, "Rejected by host") },
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.close),
                                            contentDescription = stringResource(Res.string.reject),
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Leave room button
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                stringResource(Res.string.cancel),
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Button(
                            onClick = {
                                listenTogetherManager.leaveRoom()
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.logout),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.leave_room), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // Join/Create room section
            item {
                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.listen_together_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )

                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text(stringResource(Res.string.username)) },
                            placeholder = { Text(stringResource(Res.string.enter_username)) },
                            leadingIcon = {
                                Icon(
                                    painterResource(Res.drawable.person),
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            trailingIcon = {
                                if (usernameInput.isNotBlank()) {
                                    IconButton(onClick = { usernameInput = "" }) {
                                        Icon(painterResource(Res.drawable.close), null)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        HorizontalDivider()

                        Text(
                            text = stringResource(Res.string.join_existing_room),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )

                        OutlinedTextField(
                            value = roomCodeInput,
                            onValueChange = { roomCodeInput = it.uppercase().filter { c -> c.isLetterOrDigit() }.take(8) },
                            label = { Text(stringResource(Res.string.room_code)) },
                            placeholder = { Text("ABCD1234") },
                            supportingText = {
                                Text(
                                    text = "${roomCodeInput.length}/8",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    painterResource(Res.drawable.token),
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors =
                                OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Status messages
                        if (isJoiningRoom) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = waitingForApprovalText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        joinErrorMessage?.let { msg ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(12.dp),
                                ) {
                                    Icon(
                                        painterResource(Res.drawable.error),
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons
            item {
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Create Room button (left side)
                        Button(
                            onClick = {
                                val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                val finalUsername = username.trim()
                                if (finalUsername.isNotBlank()) {
                                    savedUsername = finalUsername
                                    platformActions.toast(Res.string.creating_room)
                                    isCreatingRoom = true
                                    isJoiningRoom = false
                                    joinErrorMessage = null
                                    listenTogetherManager.connect()
                                    listenTogetherManager.createRoom(finalUsername)
                                } else {
                                    platformActions.toast(Res.string.error_username_empty)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                ),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.add),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(Res.string.create_room), fontWeight = FontWeight.SemiBold)
                        }

                        // Join Room button (right side - only visible when room code is complete)
                        if (roomCodeInput.length == 8) {
                            Button(
                                onClick = {
                                    val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                                    val finalUsername = username.trim()
                                    if (finalUsername.isNotBlank()) {
                                        savedUsername = finalUsername
                                        platformActions.toast(String.format(joiningRoomTemplate, roomCodeInput))
                                        isJoiningRoom = true
                                        isCreatingRoom = false
                                        joinErrorMessage = null
                                        listenTogetherManager.connect()
                                        listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                                    } else {
                                        platformActions.toast(Res.string.error_username_empty)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = (usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                    ),
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.login),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.join_room), fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(Res.string.cancel),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
