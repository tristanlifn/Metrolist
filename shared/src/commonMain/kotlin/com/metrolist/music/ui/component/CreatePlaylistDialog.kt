/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.metrolist.music.LocalSyncUtils
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.add
import com.metrolist.music.resources.allows_for_sync_witch_youtube
import com.metrolist.music.resources.create_playlist
import com.metrolist.music.resources.not_logged_in_youtube
import com.metrolist.music.resources.playlist_created_locally
import com.metrolist.music.resources.sync_disabled
import com.metrolist.music.resources.sync_playlist
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.metrolist.music.utils.PlatformActions
import org.koin.compose.koinInject

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    initialTextFieldValue: String? = null,
    allowSyncing: Boolean = true,
    onPlaylistCreated: ((String) -> Unit)? = null,
) {
    val syncUtils = LocalSyncUtils.current
    val coroutineScope = rememberCoroutineScope()
    var syncedPlaylist by remember { mutableStateOf(false) }
    val platformActions = koinInject<PlatformActions>()

    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val isSignedIn = innerTubeCookie.isNotEmpty()

    val notLoggedInYoutubeStr = stringResource(Res.string.not_logged_in_youtube)
    val syncDisabledStr = stringResource(Res.string.sync_disabled)
    val playlistCreatedLocallyStr = stringResource(Res.string.playlist_created_locally)

    TextFieldDialog(
        icon = { Icon(painter = painterResource(Res.drawable.add), contentDescription = null) },
        title = { Text(text = stringResource(Res.string.create_playlist)) },
        initialTextFieldValue = TextFieldValue(initialTextFieldValue ?: ""),
        onDismiss = onDismiss,
        onDone = { playlistName ->
            syncUtils.createPlaylist(
                playlist = PlaylistEntity(
                    name = playlistName,
                    bookmarkedAt = LocalDateTime.now(),
                    isEditable = true,
                ),
                syncWithYouTube = syncedPlaylist,
            ) { playlistId, remoteCreated ->
                if (syncedPlaylist && !remoteCreated) {
                    platformActions.toast(playlistCreatedLocallyStr, long = true)
                }
                onPlaylistCreated?.invoke(playlistId)
            }
        },
        extraContent = {
            if (allowSyncing) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.sync_playlist),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = stringResource(Res.string.allows_for_sync_witch_youtube),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        modifier = Modifier.padding(start = 12.dp),
                        checked = syncedPlaylist,
                        onCheckedChange = {
                            coroutineScope.launch {
                                val isYtmSyncEnabled = withContext(Dispatchers.IO) { syncUtils.isSyncEnabled() }
                                if (!isSignedIn && !syncedPlaylist) {
                                    platformActions.toast(notLoggedInYoutubeStr)
                                } else if (!isYtmSyncEnabled) {
                                    platformActions.toast(syncDisabledStr)
                                } else {
                                    syncedPlaylist = !syncedPlaylist
                                }
                            }
                        },
                    )
                }
            }
        },
    )
}
