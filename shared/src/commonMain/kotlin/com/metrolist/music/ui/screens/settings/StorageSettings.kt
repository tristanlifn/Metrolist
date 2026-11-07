/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import org.koin.compose.koinInject
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.annotation.ExperimentalCoilApi
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.constants.EnableSongCacheKey
import com.metrolist.music.constants.MaxImageCacheSizeKey
import com.metrolist.music.constants.MaxSongCacheSizeKey
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.Material3SettingsGroup
import com.metrolist.music.ui.component.Material3SettingsItem
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.ByteString.Companion.encodeUtf8
import java.io.File
import kotlin.math.roundToInt
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.arrow_back
import com.metrolist.music.resources.cache_size_warning_confirm
import com.metrolist.music.resources.cache_size_warning_message
import com.metrolist.music.resources.cache_size_warning_title
import com.metrolist.music.resources.cached
import com.metrolist.music.resources.cancel
import com.metrolist.music.resources.check
import com.metrolist.music.resources.clear_all
import com.metrolist.music.resources.clear_all_downloads
import com.metrolist.music.resources.clear_downloads_dialog
import com.metrolist.music.resources.clear_image_cache
import com.metrolist.music.resources.clear_image_cache_dialog
import com.metrolist.music.resources.clear_song_cache
import com.metrolist.music.resources.clear_song_cache_dialog
import com.metrolist.music.resources.close
import com.metrolist.music.resources.disable
import com.metrolist.music.resources.downloaded_songs
import com.metrolist.music.resources.enable_song_cache
import com.metrolist.music.resources.enable_song_cache_desc
import com.metrolist.music.resources.image_cache
import com.metrolist.music.resources.manage_search
import com.metrolist.music.resources.max_image_cache_size
import com.metrolist.music.resources.max_song_cache_size
import com.metrolist.music.resources.song_cache
import com.metrolist.music.resources.storage
import com.metrolist.music.resources.unlimited
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import coil3.compose.LocalPlatformContext
import com.metrolist.music.playback.DownloadSongCache
import com.metrolist.music.playback.SongCache
import com.metrolist.music.ui.utils.formatFileSize

@OptIn(ExperimentalCoilApi::class, ExperimentalMaterial3Api::class, DelicateCoilApi::class)
@Composable
fun StorageSettings(
    navController: NavController
) {
    val database = LocalDatabase.current
    val imageDiskCache = SingletonImageLoader.get(LocalPlatformContext.current).diskCache ?: return
    val playerCache = koinInject<SongCache>()
    val downloadCache = koinInject<SongCache>(DownloadSongCache)

    val coroutineScope = rememberCoroutineScope()
    val songCacheString = stringResource(Res.string.song_cache).lowercase()
    val imageCacheString = stringResource(Res.string.image_cache).lowercase()
    val (maxImageCacheSize, onMaxImageCacheSizeChange) = rememberPreference(
        key = MaxImageCacheSizeKey,
        defaultValue = 512
    )
    val (maxSongCacheSize, onMaxSongCacheSizeChange) = rememberPreference(
        key = MaxSongCacheSizeKey,
        defaultValue = 1024
    )
    val (enableSongCache, onEnableSongCacheChange) = rememberPreference(
        key = EnableSongCacheKey,
        defaultValue = true
    )

    var clearDownloads by remember { mutableStateOf(false) }
    var clearCacheDialog by remember { mutableStateOf(false) }
    var clearImageCacheDialog by remember { mutableStateOf(false) }

    // State for the confirmation dialog
    var showCacheWarningDialog by remember { mutableStateOf(false) }
    var cacheType by remember { mutableStateOf("") }
    var cacheUsage by remember { androidx.compose.runtime.mutableLongStateOf(0L) }
    var onConfirmAction by remember { mutableStateOf<() -> Unit>({}) }

    var imageCacheSize by remember {
        androidx.compose.runtime.mutableLongStateOf(imageDiskCache.size)
    }
    var playerCacheSize by remember {
        androidx.compose.runtime.mutableLongStateOf(playerCache.size)
    }
    var downloadCacheSize by remember {
        mutableLongStateOf(downloadCache.size)
    }
    val imageCacheProgress by animateFloatAsState(
        targetValue =
            (imageCacheSize.toFloat() / (maxImageCacheSize * 1024 * 1024L)).coerceIn(
                0f,
                1f,
            ),
        label = "imageCacheProgress",
    )
    val playerCacheProgress by animateFloatAsState(
        targetValue =
            (playerCacheSize.toFloat() / (maxSongCacheSize * 1024 * 1024L)).coerceIn(
                0f,
                1f,
            ),
        label = "playerCacheProgress",
    )

    LaunchedEffect(maxImageCacheSize) {
        SingletonImageLoader.reset()
        if (maxImageCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                imageDiskCache.clear()
            }
        }
    }
    LaunchedEffect(maxSongCacheSize) {
        if (maxSongCacheSize == 0) {
            coroutineScope.launch(Dispatchers.IO) {
                playerCache.clear()
            }
        }
    }

    LaunchedEffect(imageDiskCache) {
        while (isActive) {
            delay(500)
            imageCacheSize = imageDiskCache.size
        }
    }
    LaunchedEffect(playerCache) {
        while (isActive) {
            delay(500)
            playerCacheSize = playerCache.size
        }
    }
    LaunchedEffect(downloadCache) {
        while (isActive) {
            delay(500)
            downloadCacheSize = downloadCache.size
        }
    }

    if (clearDownloads) {
        ActionPromptDialog(
            title = stringResource(Res.string.clear_all_downloads),
            onDismiss = { clearDownloads = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    downloadCache.clear()
                }
                clearDownloads = false
            },
            onCancel = { clearDownloads = false },
            content = {
                Text(text = stringResource(Res.string.clear_downloads_dialog))
            },
        )
    }
    if (clearCacheDialog) {
        ActionPromptDialog(
            title = stringResource(Res.string.clear_song_cache),
            onDismiss = { clearCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    playerCache.clear()
                }
                clearCacheDialog = false
            },
            onCancel = { clearCacheDialog = false },
            content = {
                Text(text = stringResource(Res.string.clear_song_cache_dialog))
            },
        )
    }
    if (clearImageCacheDialog) {
        ActionPromptDialog(
            title = stringResource(Res.string.clear_image_cache),
            onDismiss = { clearImageCacheDialog = false },
            onConfirm = {
                coroutineScope.launch(Dispatchers.IO) {
                    val urlsToPreserve = mutableSetOf<String>()
                    val downloadedSongs =
                        try {
                            database.downloadedSongsByNameAsc().first()
                        } catch (e: Exception) {
                            emptyList()
                        }
                    downloadedSongs.forEach { song ->
                        song.song.thumbnailUrl?.let { urlsToPreserve.add(it.encodeUtf8().sha256().hex()) }
                        song.album?.thumbnailUrl?.let { urlsToPreserve.add(it.encodeUtf8().sha256().hex()) }
                    }
                    val directory = imageDiskCache.directory.toFile()
                    if (directory.exists() && directory.isDirectory) {
                        directory.listFiles()?.forEach { file ->
                            if (file.isFile && !file.name.startsWith("journal")) {
                                val isPreserved = urlsToPreserve.any { hash -> file.name.startsWith(hash) }
                                if (!isPreserved) {
                                    file.delete()
                                }
                            }
                        }
                    }
                    imageDiskCache.clear()
                }
                clearImageCacheDialog = false
            },
            onCancel = { clearImageCacheDialog = false },
            content = {
                Text(text = stringResource(Res.string.clear_image_cache_dialog))
            },
        )
    }

    // Confirmation Dialog
    if (showCacheWarningDialog) {
        AlertDialog(
            onDismissRequest = { showCacheWarningDialog = false },
            title = { Text(stringResource(Res.string.cache_size_warning_title)) },
            text = {
                Text(
                    stringResource(
                        Res.string.cache_size_warning_message,
                        formatFileSize(cacheUsage),
                        cacheType,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmAction()
                        showCacheWarningDialog = false
                    },
                ) {
                    Text(
                        stringResource(Res.string.cache_size_warning_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCacheWarningDialog = false }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    Column(
        Modifier
            .windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                ),
            ).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(
                    WindowInsetsSides.Top,
                ),
            ),
        )
        Material3SettingsGroup(
            title = stringResource(Res.string.storage),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(Res.drawable.storage),
                        title = { Text(stringResource(Res.string.downloaded_songs)) },
                        description = {
                            Text(text = formatFileSize(downloadCacheSize))
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(Res.drawable.clear_all),
                        title = { Text(stringResource(Res.string.clear_all_downloads)) },
                        onClick = {
                            clearDownloads = true
                        },
                    ),
                ),
        )

        Material3SettingsGroup(
            title = stringResource(Res.string.song_cache),
            items = listOf(
                Material3SettingsItem(
                    icon = painterResource(Res.drawable.cached),
                    title = { Text(stringResource(Res.string.enable_song_cache)) },
                    description = { Text(stringResource(Res.string.enable_song_cache_desc)) },
                    trailingContent = {
                        Switch(
                            checked = enableSongCache,
                            onCheckedChange = onEnableSongCacheChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (enableSongCache) Res.drawable.check else Res.drawable.close
                                    ),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    },
                    onClick = { onEnableSongCacheChange(!enableSongCache) }
                ),
                Material3SettingsItem(
                    icon = painterResource(Res.drawable.cached),
                    title = { Text(stringResource(Res.string.max_song_cache_size)) },
                    enabled = enableSongCache,
                    description = {
                        val songCacheValues =
                            remember { listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192, -1) }
                        Column {
                            Text(
                                text = when (maxSongCacheSize) {
                                    0 -> stringResource(Res.string.disable)
                                    -1 -> stringResource(Res.string.unlimited)
                                    else -> formatFileSize(maxSongCacheSize * 1024 * 1024L)
                                }
                            )
                            Slider(
                                value = songCacheValues.indexOf(maxSongCacheSize).toFloat(),
                                enabled = enableSongCache,
                                onValueChange = {
                                    val newValue = songCacheValues[it.roundToInt()]
                                    val newLimitInBytes = if (newValue == -1) {
                                        Long.MAX_VALUE
                                    } else {
                                        newValue * 1024 * 1024L
                                    }

                                        if (newLimitInBytes < playerCacheSize) {
                                            cacheUsage = playerCacheSize
                                            cacheType = songCacheString
                                            onConfirmAction = { onMaxSongCacheSizeChange(newValue) }
                                            showCacheWarningDialog = true
                                        } else {
                                            onMaxSongCacheSizeChange(newValue)
                                        }
                                    },
                                    steps = songCacheValues.size - 2,
                                    valueRange = 0f..(songCacheValues.size - 1).toFloat(),
                                )
                                LinearProgressIndicator(
                                    progress = { playerCacheProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    strokeCap = StrokeCap.Round,
                                )
                                Spacer(modifier = Modifier.padding(2.dp))
                                Text(
                                    text =
                                        if (maxSongCacheSize == -1) {
                                            formatFileSize(playerCacheSize)
                                        } else {
                                            "${formatFileSize(playerCacheSize)} / ${
                                                formatFileSize(maxSongCacheSize * 1024 * 1024L)
                                            }"
                                        },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(Res.drawable.clear_all),
                        title = { Text(stringResource(Res.string.clear_song_cache)) },
                        onClick = {
                            clearCacheDialog = true
                        },
                    ),
                ),
        )

        Material3SettingsGroup(
            title = stringResource(Res.string.image_cache),
            items =
                listOf(
                    Material3SettingsItem(
                        icon = painterResource(Res.drawable.manage_search),
                        title = { Text(stringResource(Res.string.max_image_cache_size)) },
                        description = {
                            val imageCacheValues =
                                remember { listOf(0, 128, 256, 512, 1024, 2048, 4096, 8192) }
                            Column {
                                Text(
                                    text =
                                        when (maxImageCacheSize) {
                                            0 -> stringResource(Res.string.disable)
                                            else -> formatFileSize(maxImageCacheSize * 1024 * 1024L)
                                        },
                                )
                                Slider(
                                    value = imageCacheValues.indexOf(maxImageCacheSize).toFloat(),
                                    onValueChange = {
                                        val newValue = imageCacheValues[it.roundToInt()]
                                        val newLimitInBytes = newValue * 1024 * 1024L

                                        if (newLimitInBytes < imageCacheSize) {
                                            cacheUsage = imageCacheSize
                                            cacheType = imageCacheString
                                            onConfirmAction = { onMaxImageCacheSizeChange(newValue) }
                                            showCacheWarningDialog = true
                                        } else {
                                            onMaxImageCacheSizeChange(newValue)
                                        }
                                    },
                                    steps = imageCacheValues.size - 2,
                                    valueRange = 0f..(imageCacheValues.size - 1).toFloat(),
                                )
                                LinearProgressIndicator(
                                    progress = { imageCacheProgress },
                                    modifier = Modifier.fillMaxWidth(),
                                    strokeCap = StrokeCap.Round,
                                )
                                Spacer(modifier = Modifier.padding(2.dp))
                                Text(
                                    text = "${formatFileSize(imageCacheSize)} / ${
                                        formatFileSize(maxImageCacheSize * 1024 * 1024L)
                                    }",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        },
                    ),
                    Material3SettingsItem(
                        icon = painterResource(Res.drawable.clear_all),
                        title = { Text(stringResource(Res.string.clear_image_cache)) },
                        onClick = {
                            clearImageCacheDialog = true
                        },
                    ),
                ),
        )
    }

    TopAppBar(
        title = { Text(stringResource(Res.string.storage)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(Res.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
    )
}
