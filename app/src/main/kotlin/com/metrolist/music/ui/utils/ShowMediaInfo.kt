/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.MediaInfo
import com.metrolist.music.LocalArtistNameAliases
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.LoudnessLevel
import com.metrolist.music.constants.LoudnessLevelKey
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.rememberEnumPreference
import androidx.compose.ui.platform.LocalLocale
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.media3_icon_feed
import com.metrolist.music.resources.media3_icon_thumb_down_unfilled
import com.metrolist.music.resources.media3_icon_thumb_up_unfilled
import com.metrolist.music.resources.media3_icon_bookmark_filled
import com.metrolist.music.resources.bitrate
import com.metrolist.music.resources.codecs
import com.metrolist.music.resources.content_copy
import com.metrolist.music.resources.contrast
import com.metrolist.music.resources.description
import com.metrolist.music.resources.dislikes
import com.metrolist.music.resources.file_size
import com.metrolist.music.resources.general
import com.metrolist.music.resources.gradient
import com.metrolist.music.resources.info
import com.metrolist.music.resources.information
import com.metrolist.music.resources.key
import com.metrolist.music.resources.likes
import com.metrolist.music.resources.loudness
import com.metrolist.music.resources.loudness_level
import com.metrolist.music.resources.loudness_level_aggressive
import com.metrolist.music.resources.loudness_level_balanced
import com.metrolist.music.resources.loudness_level_loud
import com.metrolist.music.resources.loudness_level_quiet
import com.metrolist.music.resources.media_id
import com.metrolist.music.resources.mime_type
import com.metrolist.music.resources.music_note
import com.metrolist.music.resources.person
import com.metrolist.music.resources.play
import com.metrolist.music.resources.radio
import com.metrolist.music.resources.sample_rate
import com.metrolist.music.resources.song_artists
import com.metrolist.music.resources.song_title
import com.metrolist.music.resources.stream_client
import com.metrolist.music.resources.unknown
import com.metrolist.music.resources.views
import com.metrolist.music.resources.volume
import com.metrolist.music.resources.volume_mute
import com.metrolist.music.resources.volume_up
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun getLoudnessLevelLabel(loudnessLevel: LoudnessLevel): String {
    return when (loudnessLevel) {
        LoudnessLevel.AGGRESSIVE -> stringResource(Res.string.loudness_level_aggressive)
        LoudnessLevel.LOUD -> stringResource(Res.string.loudness_level_loud)
        LoudnessLevel.BALANCED -> stringResource(Res.string.loudness_level_balanced)
        LoudnessLevel.QUIET -> stringResource(Res.string.loudness_level_quiet)
    }
}

@Composable
fun ShowMediaInfo(videoId: String) {
    if (videoId.isBlank() || videoId.isEmpty()) return

    val windowInsets = WindowInsets.systemBars

    var info by remember {
        mutableStateOf<MediaInfo?>(null)
    }

    val database = LocalDatabase.current
    var song by remember { mutableStateOf<Song?>(null) }

    var currentFormat by remember { mutableStateOf<FormatEntity?>(null) }

    val playerConnection = LocalPlayerConnection.current
    val currentStreamClient by playerConnection?.currentStreamClient?.collectAsState() ?: remember { mutableStateOf(null) }
    val context = LocalContext.current
    val artistNameAliases = LocalArtistNameAliases.current

    val loudnessLevel by rememberEnumPreference(
        LoudnessLevelKey,
        defaultValue = LoudnessLevel.BALANCED
    )

    val targetLufs: Float = loudnessLevel.targetLufs

    LaunchedEffect(Unit, videoId) {
        info = YouTube.getMediaInfo(videoId).getOrNull()
    }

    LaunchedEffect(Unit, videoId) {
        database.song(videoId).collect {
            song = it
        }
    }

    LaunchedEffect(Unit, videoId) {
        database.format(videoId).collect {
            currentFormat = it
        }
    }

    LazyColumn(
        state = rememberLazyListState(),
        modifier = Modifier
            .padding(
                windowInsets
                    .asPaddingValues()
            )
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (info != null) {
            item(contentType = "MediaDetails") {
                Column {
                    val baseList = listOf(
                        stringResource(Res.string.song_title) to (info?.title ?: song?.title),
                        stringResource(Res.string.song_artists) to (
                            song?.artists?.joinToString {
                                ArtistNameAliases.resolve(artistNameAliases, it.id, it.name)
                            } ?: info?.author?.let {
                                ArtistNameAliases.resolve(artistNameAliases, null, it)
                            }
                        ),
                        stringResource(Res.string.media_id) to (song?.id ?: info?.videoId)
                    )

                    val baseIconsList = listOf(
                        Res.drawable.music_note,
                        Res.drawable.person,
                        Res.drawable.media3_icon_bookmark_filled,
                    )

                    val iconsList = listOf(
                        Res.drawable.media3_icon_feed,
                        Res.drawable.media3_icon_thumb_up_unfilled,
                        Res.drawable.media3_icon_thumb_down_unfilled,
                        Res.drawable.key,
                        Res.drawable.play,
                        Res.drawable.info,
                        Res.drawable.radio,
                        Res.drawable.gradient,
                        Res.drawable.contrast,
                        Res.drawable.volume_up,
                        Res.drawable.volume_up,
                        Res.drawable.volume_mute,
                        Res.drawable.content_copy
                    )

                    val measuredLufs: Double? = currentFormat?.perceptualLoudnessDb ?: currentFormat?.loudnessDb?.let { it + LoudnessLevel.AGGRESSIVE.targetLufs }

                    val extendedList = if (currentFormat != null) {
                        listOf(
                            stringResource(Res.string.views) to info?.viewCount?.let(::numberFormatter).orEmpty(),
                            stringResource(Res.string.likes) to info?.like?.let(::numberFormatter).orEmpty(),
                            stringResource(Res.string.dislikes) to info?.dislike?.let(::numberFormatter).orEmpty(),
                            "Itag" to currentFormat?.itag?.toString(),
                            stringResource(Res.string.stream_client) to currentStreamClient,
                            stringResource(Res.string.mime_type) to currentFormat?.mimeType,
                            stringResource(Res.string.codecs) to currentFormat?.codecs,
                            stringResource(Res.string.bitrate) to currentFormat?.bitrate?.let { "${it / 1000} Kbps" },
                            stringResource(Res.string.sample_rate) to currentFormat?.sampleRate?.let { "$it Hz" },
                            stringResource(Res.string.loudness) to measuredLufs?.let {
                                String.format(LocalLocale.current.platformLocale, "%.2f dB", it - targetLufs)
                            },
                            stringResource(Res.string.loudness_level) to getLoudnessLevelLabel(loudnessLevel),
                            stringResource(Res.string.volume) to if (playerConnection != null) "${(playerConnection.volume.value * 100).toInt()}%" else null,
                            stringResource(Res.string.file_size) to
                                    currentFormat?.contentLength?.let {
                                        Formatter.formatShortFileSize(
                                            context,
                                            it
                                        )
                                    },
                        )
                    } else {
                        emptyList()
                    }

                    val cardsBaseList = mutableListOf<Material3SettingsItem>()
                    val cardsExtendedList = mutableListOf<Material3SettingsItem>()
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

                    baseList.forEachIndexed { index, (label, text) ->
                        val displayText = text ?: stringResource(Res.string.unknown)
                        cardsBaseList += Material3SettingsItem(
                            title = { Text(label) },
                            description = { Text(displayText) },
                            icon = painterResource(baseIconsList[index]),
                            onClick = {
                                cm.setPrimaryClip(ClipData.newPlainText("text", displayText))
                                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }

                    extendedList.forEachIndexed { index, (label, text) ->
                        val displayText = text ?: stringResource(Res.string.unknown)
                        cardsExtendedList += Material3SettingsItem(
                            title = { Text(label) },
                            description = { Text(displayText) },
                            icon = painterResource(iconsList[index]),
                            onClick = {
                                cm.setPrimaryClip(ClipData.newPlainText("text", displayText))
                                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                            },
                        )
                    }

                    Material3SettingsGroup(
                        title = stringResource(Res.string.general),
                        items = cardsBaseList
                    )

                    Spacer(Modifier.height(8.dp))

                    Material3SettingsGroup(
                        title = stringResource(Res.string.information),
                        items = cardsExtendedList
                    )

                    Spacer(Modifier.height(8.dp))

                    val descriptionText = info?.description ?: stringResource(Res.string.unknown)

                    Material3SettingsGroup(
                        title = stringResource(Res.string.description),
                        items = listOf(
                            Material3SettingsItem(
                                title = { Text(stringResource(Res.string.description)) },
                                description = { Text(descriptionText) },
                                onClick = {
                                    cm.setPrimaryClip(ClipData.newPlainText("text", descriptionText))
                                    Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()
                                }
                            )
                        )
                    )
                }
            }
        } else {
            item(contentType = "MediaInfoLoader") {
                ShimmerHost {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(all = 16.dp)
                    ) {
                        TextPlaceholder()
                    }
                }
            }
        }
    }
}
