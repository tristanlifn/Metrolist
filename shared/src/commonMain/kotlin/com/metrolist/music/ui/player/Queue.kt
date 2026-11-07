/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.metrolist.music.LocalNavController
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.PlayerBackgroundStyle
import com.metrolist.music.constants.QueueEditLockKey
import com.metrolist.music.constants.UseNewPlayerDesignKey
import com.metrolist.music.extensions.move
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.QueueEntry
import com.metrolist.music.playback.RepeatMode
import com.metrolist.music.ui.component.ActionPromptDialog
import com.metrolist.music.ui.component.BottomSheet
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.models.QueueSession
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.MediaMetadataListItem
import com.metrolist.music.ui.menu.PlayerMenu
import com.metrolist.music.ui.menu.QueueMenu
import com.metrolist.music.ui.menu.SelectionMediaMetadataMenu
import com.metrolist.music.ui.utils.ShowMediaInfo
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import kotlin.math.roundToInt
import com.metrolist.music.constants.SleepTimerDefaultKey
import androidx.compose.runtime.derivedStateOf
import com.metrolist.music.constants.SleepTimerFadeOutKey
import com.metrolist.music.constants.SleepTimerStopAfterCurrentSongKey
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.Button
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.bedtime
import com.metrolist.music.resources.cancel
import com.metrolist.music.resources.close
import com.metrolist.music.resources.delete
import com.metrolist.music.resources.delete_queue
import com.metrolist.music.resources.delete_queue_confirm
import com.metrolist.music.resources.drag_handle
import com.metrolist.music.resources.end_of_song
import com.metrolist.music.resources.expand_less
import com.metrolist.music.resources.expand_more
import com.metrolist.music.resources.lock
import com.metrolist.music.resources.lock_open
import com.metrolist.music.resources.lyrics
import com.metrolist.music.resources.minute
import com.metrolist.music.resources.more_vert
import com.metrolist.music.resources.n_selected
import com.metrolist.music.resources.n_song
import com.metrolist.music.resources.playlist_play
import com.metrolist.music.resources.queue
import com.metrolist.music.resources.queue_music
import com.metrolist.music.resources.removed_song_from_playlist
import com.metrolist.music.resources.repeat
import com.metrolist.music.resources.repeat_one
import com.metrolist.music.resources.set_as_default
import com.metrolist.music.resources.shuffle
import com.metrolist.music.resources.similar_content
import com.metrolist.music.resources.sleep_timer
import com.metrolist.music.resources.sleep_timer_default_set
import com.metrolist.music.resources.switch_queue
import com.metrolist.music.resources.undo
import com.metrolist.music.resources.untitled_queue
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import com.metrolist.music.utils.PlatformActions
import org.koin.compose.koinInject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.utils.safeEdit
import com.metrolist.music.ui.utils.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Queue(
    state: BottomSheetState,
    playerBottomSheetState: BottomSheetState,
    modifier: Modifier = Modifier,
    background: Color,
    onBackgroundColor: Color,
    TextBackgroundColor: Color,
    textButtonColor: Color,
    iconButtonColor: Color,
    pureBlack: Boolean,
    showInlineLyrics: Boolean,
    playerBackground: PlayerBackgroundStyle = PlayerBackgroundStyle.DEFAULT,
    onToggleLyrics: () -> Unit = {},
) {
    val navController = LocalNavController.current
    val dataStore = koinInject<DataStore<Preferences>>()
    val platformActions = koinInject<PlatformActions>()
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboard.current
    val menuState = LocalMenuState.current
    val sleepTimerDefaultSetTemplate = stringResource(Res.string.sleep_timer_default_set)
    val bottomSheetPageState = LocalBottomSheetPageState.current

    // Listen Together state (reactive)
    val listenTogetherManager = LocalListenTogetherManager.current
    val listenTogetherRoleState = listenTogetherManager?.role?.collectAsStateWithLifecycle(initialValue = com.metrolist.music.listentogether.RoomRole.NONE)
    val isListenTogetherGuest = listenTogetherRoleState?.value == RoomRole.GUEST

    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val repeatMode by playerConnection.repeatMode.collectAsStateWithLifecycle()

    val currentWindowIndex by playerConnection.currentQueuePosition.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val currentFormat by playerConnection.currentFormat.collectAsStateWithLifecycle(initialValue = null)

    val selectedSongs = remember { mutableStateListOf<MediaMetadata>() }
    val selectedItems = remember { mutableStateListOf<QueueEntry>() }

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.cast
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<String>, String>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }
    if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    var locked by rememberPreference(QueueEditLockKey, defaultValue = true)

    val (useNewPlayerDesign, onUseNewPlayerDesignChange) =
        rememberPreference(
            UseNewPlayerDesignKey,
            defaultValue = true,
        )

    val snackbarHostState = remember { SnackbarHostState() }
    var dismissJob: Job? by remember { mutableStateOf(null) }

    val coroutineScope = rememberCoroutineScope()
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerDefault by rememberPreference(SleepTimerDefaultKey, 30f)
    var sleepTimerValue by remember { mutableFloatStateOf(sleepTimerDefault) }
    val isAtDefault by remember {
        derivedStateOf { sleepTimerValue.roundToInt() == sleepTimerDefault.roundToInt() }
    }
    LaunchedEffect(sleepTimerDefault) { sleepTimerValue = sleepTimerDefault }
    val sleepTimerStopAfterCurrentSong by rememberPreference(SleepTimerStopAfterCurrentSongKey, false)
    val sleepTimerFadeOut by rememberPreference(SleepTimerFadeOutKey, false)
    val sleepTimerEnabled = remember(
        playerConnection.sleepTimer?.triggerTime,
        playerConnection.sleepTimer?.pauseWhenSongEnd
    ) {
        playerConnection.sleepTimer?.isActive ?: false
    }
    var sleepTimerTimeLeft by remember { mutableLongStateOf(0L) }

    LaunchedEffect(sleepTimerEnabled) {
        if (sleepTimerEnabled) {
            while (isActive) {
                sleepTimerTimeLeft =
                    if (playerConnection.sleepTimer?.pauseWhenSongEnd == true) {
                        playerConnection.duration - playerConnection.currentPosition
                    } else {
                        (playerConnection.sleepTimer?.triggerTime ?: 0L) - System.currentTimeMillis()
                    }
                delay(1000L)
            }
        }
    }

    BottomSheet(
        state = state,
        modifier = modifier,
        background = {
            Box(Modifier.fillMaxSize().background(Color.Unspecified))
        },
        collapsedContent = {
            if (useNewPlayerDesign) {
                // New design
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp, vertical = 12.dp)
                            .windowInsetsPadding(
                                WindowInsets.systemBars.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                                ),
                            ),
                ) {
                    val buttonSize = 42.dp
                    val iconSize = 24.dp
                    val queueShape =
                        RoundedCornerShape(
                            topStart = 50.dp,
                            bottomStart = 50.dp,
                            topEnd = 3.dp,
                            bottomEnd = 3.dp,
                        )
                    val middleShape = RoundedCornerShape(3.dp)
                    val repeatShape =
                        RoundedCornerShape(
                            topStart = 3.dp,
                            bottomStart = 3.dp,
                            topEnd = 50.dp,
                            bottomEnd = 50.dp,
                        )

                    PlayerQueueButton(
                        icon = Res.drawable.queue_music,
                        onClick = { state.expandSoft() },
                        isActive = false,
                        shape = queueShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    PlayerQueueButton(
                        icon = Res.drawable.bedtime,
                        onClick = {
                            if (sleepTimerEnabled) {
                                playerConnection.sleepTimer?.clear()
                            } else {
                                showSleepTimerDialog = true
                            }
                        },
                        isActive = sleepTimerEnabled,
                        enabled = !isListenTogetherGuest,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        text = if (sleepTimerEnabled) makeTimeString(sleepTimerTimeLeft) else null,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()
                    PlayerQueueButton(
                        icon = Res.drawable.shuffle,
                        onClick = {
                            playerConnection.setShuffleModeEnabled(!shuffleModeEnabled)
                        },
                        isActive = shuffleModeEnabled,
                        enabled = !isListenTogetherGuest,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    PlayerQueueButton(
                        icon = Res.drawable.lyrics,
                        onClick = { onToggleLyrics() },
                        isActive = showInlineLyrics,
                        shape = middleShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    PlayerQueueButton(
                        icon =
                            when (repeatMode) {
                                RepeatMode.ALL -> Res.drawable.repeat
                                RepeatMode.ONE -> Res.drawable.repeat_one
                                else -> Res.drawable.repeat
                            },
                        onClick = {
                            playerConnection.toggleRepeatMode()
                        },
                        isActive = repeatMode != RepeatMode.OFF,
                        enabled = !isListenTogetherGuest,
                        shape = repeatShape,
                        modifier = Modifier.size(buttonSize),
                        textButtonColor = textButtonColor,
                        iconButtonColor = iconButtonColor,
                        iconSize = iconSize,
                        textBackgroundColor = TextBackgroundColor,
                        playerBackground = playerBackground,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier =
                            Modifier
                                .size(buttonSize)
                                .clip(CircleShape)
                                .background(textButtonColor)
                                .clickable {
                                    menuState.show {
                                        PlayerMenu(
                                            mediaMetadata = mediaMetadata,
                                            playerBottomSheetState = playerBottomSheetState,
                                            onShowDetailsDialog = {
                                                mediaMetadata?.id?.let {
                                                    bottomSheetPageState.show {
                                                        ShowMediaInfo(it)
                                                    }
                                                }
                                            },
                                            onDismiss = menuState::dismiss,
                                        )
                                    }
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.more_vert),
                            contentDescription = null,
                            modifier = Modifier.size(iconSize),
                            tint = iconButtonColor,
                        )
                    }
                }
            } else {
                // Old design
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp, vertical = 12.dp)
                            .windowInsetsPadding(
                                WindowInsets.systemBars
                                    .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                            ),
                ) {
                    TextButton(
                        onClick = { state.expandSoft() },
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.queue_music),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.queue),
                                color = TextBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }

                    TextButton(
                        enabled = !isListenTogetherGuest,
                        onClick = {
                            if (!isListenTogetherGuest) {
                                if (sleepTimerEnabled) {
                                    playerConnection.sleepTimer?.clear()
                                } else {
                                    showSleepTimerDialog = true
                                }
                            }
                        },
                        modifier = Modifier.weight(1.2f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.bedtime),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            AnimatedContent(
                                label = "sleepTimer",
                                targetState = sleepTimerEnabled,
                            ) { enabled ->
                                if (enabled) {
                                    Text(
                                        text = makeTimeString(sleepTimerTimeLeft),
                                        color = TextBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                } else {
                                    Text(
                                        text = stringResource(Res.string.sleep_timer),
                                        color = TextBackgroundColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.basicMarquee(),
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = {
                            onToggleLyrics()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.lyrics),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = TextBackgroundColor,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(Res.string.lyrics),
                                color = TextBackgroundColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.basicMarquee(),
                            )
                        }
                    }
                }
            }

            if (showSleepTimerDialog) {
                ActionPromptDialog(
                    titleBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.sleep_timer),
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    },
                    onDismiss = { showSleepTimerDialog = false },
                    onConfirm = {
                        showSleepTimerDialog = false
                        playerConnection.sleepTimer?.start(
                            minute = sleepTimerValue.roundToInt(),
                            stopAfterCurrentSong = sleepTimerStopAfterCurrentSong,
                            fadeOut = sleepTimerFadeOut,
                        )
                    },
                    onCancel = {
                        showSleepTimerDialog = false
                    },
                    onReset = {
                        sleepTimerValue = sleepTimerDefault
                    },
                    content = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text =
                                    pluralStringResource(
                                        Res.plurals.minute,
                                        sleepTimerValue.roundToInt(),
                                        sleepTimerValue.roundToInt(),
                                    ),
                                style = MaterialTheme.typography.bodyLarge,
                            )

                            Spacer(Modifier.height(16.dp))

                            Slider(
                                value = sleepTimerValue,
                                onValueChange = { sleepTimerValue = it },
                                valueRange = 5f..120f,
                                steps = (120 - 5) / 5 - 1,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(8.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (isAtDefault) {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                dataStore.safeEdit { settings ->
                                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                                }
                                            }
                                            platformActions.toast(String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()))
                                        },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                    ) {
                                        Text(stringResource(Res.string.set_as_default))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                dataStore.safeEdit { settings ->
                                                    settings[SleepTimerDefaultKey] = sleepTimerValue
                                                }
                                            }
                                            platformActions.toast(String.format(sleepTimerDefaultSetTemplate, sleepTimerValue.roundToInt()))
                                        },
                                    ) {
                                        Text(stringResource(Res.string.set_as_default))
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        showSleepTimerDialog = false
                                        playerConnection.sleepTimer?.start(
                                            minute = -1,
                                        )
                                    },
                                ) {
                                    Text(stringResource(Res.string.end_of_song))
                                }
                            }
                        }
                    },
                )
            }
        },
    ) {
        val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
        val queueWindows by playerConnection.queue.collectAsStateWithLifecycle()
        val queueSessions by playerConnection.queueSessions.collectAsStateWithLifecycle()
        val activeQueueId by playerConnection.activeQueueId.collectAsStateWithLifecycle()
        var queueSwitcherOpen by remember { mutableStateOf(false) }
        var queueToDelete by remember { mutableStateOf<QueueSession?>(null) }

        queueToDelete?.let { session ->
            DefaultDialog(
                onDismiss = { queueToDelete = null },
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.delete),
                        contentDescription = null,
                    )
                },
                title = { Text(stringResource(Res.string.delete_queue)) },
                buttons = {
                    TextButton(onClick = { queueToDelete = null }) {
                        Text(stringResource(Res.string.cancel))
                    }
                    TextButton(
                        onClick = {
                            playerConnection.deleteQueue(session.id)
                            queueToDelete = null
                        },
                    ) {
                        Text(stringResource(Res.string.delete))
                    }
                },
            ) {
                Text(stringResource(Res.string.delete_queue_confirm, session.title ?: stringResource(Res.string.untitled_queue)))
            }
        }
        val automix by playerConnection.automixItems.collectAsStateWithLifecycle()
        val mutableQueueWindows = remember { mutableStateListOf<QueueEntry>() }
        val queueLength =
            remember(queueWindows) {
                queueWindows.sumOf { it.metadata!!.duration }
            }

        val coroutineScope = rememberCoroutineScope()

        val headerItems = 1
        val lazyListState = rememberLazyListState()
        var dragInfo by remember { mutableStateOf<Pair<Int, Int>?>(null) }

        val currentPlayingUid =
            remember(currentWindowIndex, queueWindows) {
                if (currentWindowIndex in queueWindows.indices) {
                    queueWindows[currentWindowIndex].uid
                } else {
                    null
                }
            }

        val reorderableState =
            rememberReorderableLazyListState(
                lazyListState = lazyListState,
                scrollThresholdPadding =
                    WindowInsets.systemBars
                        .add(
                            WindowInsets(
                                top = ListItemHeight,
                                bottom = ListItemHeight,
                            ),
                        ).asPaddingValues(),
            ) { from, to ->
                val currentDragInfo = dragInfo
                dragInfo =
                    if (currentDragInfo == null) {
                        from.index to to.index
                    } else {
                        currentDragInfo.first to to.index
                    }

                val safeFrom = (from.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)
                val safeTo = (to.index - headerItems).coerceIn(0, mutableQueueWindows.lastIndex)

                mutableQueueWindows.move(safeFrom, safeTo)
            }

        LaunchedEffect(reorderableState.isAnyItemDragging) {
            if (!reorderableState.isAnyItemDragging) {
                dragInfo?.let { (from, to) ->
                    val safeFrom = (from - headerItems).coerceIn(0, queueWindows.lastIndex)
                    val safeTo = (to - headerItems).coerceIn(0, queueWindows.lastIndex)

                    playerConnection.moveItem(safeFrom, safeTo)
                    dragInfo = null
                }
            }
        }

        LaunchedEffect(queueWindows) {
            mutableQueueWindows.apply {
                clear()
                addAll(queueWindows)
            }
        }

        LaunchedEffect(mutableQueueWindows, currentWindowIndex) {
            if (currentWindowIndex != -1) {
                lazyListState.scrollToItem(currentWindowIndex)
            }
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(background),
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding =
                    WindowInsets.systemBars
                        .add(
                            WindowInsets(
                                top = ListItemHeight + 8.dp,
                                bottom = ListItemHeight + 8.dp,
                            ),
                        ).asPaddingValues(),
                modifier = Modifier.nestedScroll(state.preUpPostDownNestedScrollConnection),
            ) {
                item(key = "queue_top_spacer") {
                    Spacer(
                        modifier =
                            Modifier
                                .animateContentSize()
                                .height(if (inSelectMode) 48.dp else 0.dp),
                    )
                }

                itemsIndexed(
                    items = mutableQueueWindows,
                    key = { _, item -> item.uid.hashCode() },
                ) { index, window ->
                    ReorderableItem(
                        state = reorderableState,
                        key = window.uid.hashCode(),
                    ) {
                        val currentItem by rememberUpdatedState(window)
                        val isActive = window.uid == currentPlayingUid
                        val dismissBoxState =
                            rememberSwipeToDismissBoxState(
                                positionalThreshold = { totalDistance -> totalDistance },
                            )

                        var processedDismiss by remember { mutableStateOf(false) }
                        val removedSongMsg =
                            stringResource(Res.string.removed_song_from_playlist, currentItem.metadata?.title ?: "")
                        val undoStr = stringResource(Res.string.undo)
                        LaunchedEffect(dismissBoxState.currentValue) {
                            val dv = dismissBoxState.currentValue
                            if (!processedDismiss && !isListenTogetherGuest && (
                                    dv == SwipeToDismissBoxValue.StartToEnd ||
                                        dv == SwipeToDismissBoxValue.EndToStart
                                )
                            ) {
                                processedDismiss = true
                                playerConnection.removeItem(currentItem.index)
                                dismissJob?.cancel()
                                dismissJob =
                                    coroutineScope.launch {
                                        val snackbarResult =
                                            snackbarHostState.showSnackbar(
                                                message = removedSongMsg,
                                                actionLabel = undoStr,
                                                duration = SnackbarDuration.Short,
                                            )
                                        if (snackbarResult == SnackbarResult.ActionPerformed) {
                                            currentItem.metadata?.let { playerConnection.insertItem(currentItem.index, it) }
                                        }
                                    }
                            }
                            if (dv == SwipeToDismissBoxValue.Settled) {
                                processedDismiss = false
                            }
                        }

                        val onCheckedChange: (Boolean) -> Unit = {
                            if (it) {
                                selection.add(window.metadata!!.id)
                            } else {
                                selection.remove(window.metadata!!.id)
                            }
                        }

                        val content: @Composable () -> Unit = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.animateItem(),
                            ) {
                                MediaMetadataListItem(
                                    mediaMetadata = window.metadata!!,
                                    isSelected = false,
                                    isActive = isActive,
                                    isPlaying = isPlaying && isActive,
                                    trailingContent = {
                                        if (inSelectMode) {
                                            Checkbox(
                                                checked = window.metadata!!.id in selection,
                                                onCheckedChange = onCheckedChange,
                                            )
                                        } else {
                                            if (!isListenTogetherGuest) {
                                                IconButton(
                                                    onClick = {
                                                        menuState.show {
                                                            QueueMenu(
                                                                mediaMetadata = window.metadata!!,
                                                                playerBottomSheetState = playerBottomSheetState,
                                                                onShowDetailsDialog = {
                                                                    window.metadata!!.id.let {
                                                                        bottomSheetPageState.show {
                                                                            ShowMediaInfo(it)
                                                                        }
                                                                    }
                                                                },
                                                                onDismiss = menuState::dismiss,
                                                                onRemoveFromQueue = {
                                                                    playerConnection.removeItem(currentItem.index)
                                                                }
                                                            )
                                                        }
                                                    },
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.more_vert),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                            if (!locked && !isListenTogetherGuest) {
                                                IconButton(
                                                    onClick = { },
                                                    modifier = Modifier.draggableHandle(),
                                                ) {
                                                    Icon(
                                                        painter = painterResource(Res.drawable.drag_handle),
                                                        contentDescription = null,
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .background(background)
                                            .combinedClickable(
                                                onClick = {
                                                    if (inSelectMode) {
                                                        onCheckedChange(window.metadata!!.id !in selection)
                                                    } else if (!isListenTogetherGuest) {
                                                        if (index == currentWindowIndex) {
                                                            if (isCasting) {
                                                                if (castIsPlaying) {
                                                                    castHandler?.pause()
                                                                } else {
                                                                    castHandler?.play()
                                                                }
                                                            } else {
                                                                playerConnection.togglePlayPause()
                                                            }
                                                        } else {
                                                            if (isCasting) {
                                                                val mediaId = window.metadata!!.id
                                                                val navigated = castHandler?.navigateToMediaIfInQueue(mediaId) ?: false
                                                                if (!navigated) {
                                                                    playerConnection.seekToItem(window.index)
                                                                }
                                                            } else {
                                                                playerConnection.seekToItem(window.index)
                                                                playerConnection.play()
                                                            }
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    if (!inSelectMode) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        inSelectMode = true
                                                        onCheckedChange(true)
                                                    }
                                                },
                                            ),
                                )
                            }
                        }

                        if (locked) {
                            content()
                        } else {
                            SwipeToDismissBox(
                                state = dismissBoxState,
                                backgroundContent = {},
                            ) {
                                content()
                            }
                        }
                    }
                }

                if (automix.isNotEmpty()) {
                    item(key = "automix_divider") {
                        HorizontalDivider(
                            modifier =
                                Modifier
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                                    .animateItem(),
                        )

                        Text(
                            text = stringResource(Res.string.similar_content),
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }

                    itemsIndexed(
                        items = automix,
                        key = { _, it -> it.id },
                    ) { index, item ->
                        Row(
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            MediaMetadataListItem(
                                mediaMetadata = item,
                                trailingContent = {
                                    if (!isListenTogetherGuest) {
                                        IconButton(
                                            onClick = {
                                                playerConnection.playNextAutomix(index)
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.playlist_play),
                                                contentDescription = null,
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                playerConnection.addToQueueAutomix(index)
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.queue_music),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {},
                                            onLongClick = {
                                                menuState.show {
                                                    QueueMenu(
                                                        mediaMetadata = item,
                                                        playerBottomSheetState = playerBottomSheetState,
                                                        onShowDetailsDialog = {
                                                            item.id.let {
                                                                bottomSheetPageState.show {
                                                                    ShowMediaInfo(it)
                                                                }
                                                            }
                                                        },
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ).animateItem(),
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier =
                Modifier
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                    ) { }
                    .background(
                        if (pureBlack) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme
                                .secondaryContainer
                                .copy(alpha = 0.90f)
                        },
                    ).windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                    ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .height(ListItemHeight)
                        .padding(horizontal = 12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .weight(1f)
                            .clickable(enabled = !inSelectMode && queueSessions.isNotEmpty()) {
                                queueSwitcherOpen = !queueSwitcherOpen
                            },
                ) {
                    Text(
                        text = queueTitle.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (queueSessions.isNotEmpty() && !inSelectMode) {
                        Icon(
                            painter = painterResource(if (queueSwitcherOpen) Res.drawable.expand_less else Res.drawable.expand_more),
                            contentDescription = stringResource(Res.string.switch_queue),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !inSelectMode,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    Row {
                        IconButton(
                            onClick = { locked = !locked },
                            modifier = Modifier.padding(horizontal = 6.dp),
                        ) {
                            Icon(
                                painter = painterResource(if (locked) Res.drawable.lock else Res.drawable.lock_open),
                                contentDescription = null,
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text =
                            pluralStringResource(
                                Res.plurals.n_song,
                                queueWindows.size,
                                queueWindows.size,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        text = makeTimeString(queueLength * 1000L),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            AnimatedVisibility(
                visible = queueSwitcherOpen && !inSelectMode,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() } * 0.7f),
                ) {
                    items(queueSessions, key = { it.id }) { session ->
                        val isActive = session.id == activeQueueId
                        val songCount = if (isActive) queueWindows.size else session.items.size
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight)
                                    .clickable {
                                        playerConnection.switchQueue(session.id)
                                        queueSwitcherOpen = false
                                    }.padding(start = 12.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title ?: stringResource(Res.string.untitled_queue),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = pluralStringResource(Res.plurals.n_song, songCount, songCount),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { queueToDelete = session }) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = stringResource(Res.string.delete_queue),
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = inSelectMode,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                val selectedSongs =
                    remember(selection.toList(), mutableQueueWindows) {
                        mutableQueueWindows
                            .filter { it.metadata?.id in selection }
                            .mapNotNull { it.metadata }
                    }
                val selectedItems =
                    remember(selection.toList(), mutableQueueWindows) {
                        mutableQueueWindows.filter { it.metadata?.id in selection }
                    }
                val count = selection.size
                Row(
                    modifier =
                        Modifier
                            .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = onExitSelectionMode,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.close),
                            contentDescription = null,
                        )
                    }
                    Text(
                        text = pluralStringResource(Res.plurals.n_selected, count, count),
                        modifier = Modifier.weight(1f),
                    )
                    Checkbox(
                        checked = count == mutableQueueWindows.size && count > 0,
                        onCheckedChange = {
                            if (count == mutableQueueWindows.size) {
                                selection.clear()
                            } else {
                                selection.clear()
                                mutableQueueWindows.forEach {
                                    it.metadata?.let { metadata -> selection.add(metadata.id) }
                                }
                            }
                        },
                    )
                    IconButton(
                        enabled = count > 0,
                        onClick = {
                            menuState.show {
                                SelectionMediaMetadataMenu(
                                    songSelection = selectedSongs,
                                    onDismiss = menuState::dismiss,
                                    clearAction = onExitSelectionMode,
                                    currentItems = selectedItems,
                                )
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.more_vert),
                            contentDescription = null,
                            tint = LocalContentColor.current,
                        )
                    }
                }
            }
            if (pureBlack) {
                HorizontalDivider()
            }
        }

        val shuffleModeEnabled by playerConnection.shuffleModeEnabled.collectAsStateWithLifecycle()

        Box(
            modifier =
                Modifier
                    .background(
                        if (pureBlack) {
                            Color.Black
                        } else {
                            MaterialTheme.colorScheme
                                .secondaryContainer
                                .copy(alpha = 0.90f)
                        },
                    ).fillMaxWidth()
                    .height(
                        ListItemHeight +
                            WindowInsets.systemBars
                                .asPaddingValues()
                                .calculateBottomPadding(),
                    ).align(Alignment.BottomCenter)
                    .clickable {
                        state.collapseSoft()
                    }.windowInsetsPadding(
                        WindowInsets.systemBars
                            .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ).padding(12.dp),
        ) {
            IconButton(
                enabled = !isListenTogetherGuest,
                modifier = Modifier.align(Alignment.CenterStart),
                onClick = {
                    coroutineScope
                        .launch {
                            lazyListState.animateScrollToItem(
                                if (shuffleModeEnabled) playerConnection.queue.value.getOrNull(currentWindowIndex)?.index ?: 0 else 0,
                            )
                        }.invokeOnCompletion {
                            playerConnection.setShuffleModeEnabled(!playerConnection.shuffleModeEnabled.value)
                        }
                },
            ) {
                val baseAlpha = if (shuffleModeEnabled) 1f else 0.5f
                val finalAlpha = if (!isListenTogetherGuest) baseAlpha else 0.3f
                Icon(
                    painter = painterResource(Res.drawable.shuffle),
                    contentDescription = null,
                    modifier = Modifier.alpha(finalAlpha),
                )
            }

            Icon(
                painter = painterResource(Res.drawable.expand_more),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
            )

            IconButton(
                enabled = !isListenTogetherGuest,
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = playerConnection::toggleRepeatMode,
            ) {
                val baseAlpha = if (repeatMode == RepeatMode.OFF) 0.5f else 1f
                val finalAlpha = if (!isListenTogetherGuest) baseAlpha else 0.3f
                Icon(
                    painter =
                        painterResource(
                            when (repeatMode) {
                                RepeatMode.OFF, RepeatMode.ALL -> Res.drawable.repeat
                                RepeatMode.ONE -> Res.drawable.repeat_one
                            },
                        ),
                    contentDescription = null,
                    modifier = Modifier.alpha(finalAlpha),
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .padding(
                        bottom =
                            ListItemHeight +
                                WindowInsets.systemBars
                                    .asPaddingValues()
                                    .calculateBottomPadding(),
                    ).align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun PlayerQueueButton(
    icon: DrawableResource,
    onClick: () -> Unit,
    isActive: Boolean,
    enabled: Boolean = true,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    text: String? = null,
    textButtonColor: Color,
    iconButtonColor: Color,
    iconSize: androidx.compose.ui.unit.Dp,
    textBackgroundColor: Color,
    playerBackground: PlayerBackgroundStyle,
) {
    val buttonModifier =
        Modifier
            .clip(shape)
            .clickable(enabled = enabled, onClick = onClick)

    val alphaFactor = if (enabled) 1f else 0.35f

    val appliedModifier =
        if (isActive) {
            modifier.then(buttonModifier.background(textButtonColor)).alpha(alphaFactor)
        } else {
            modifier
                .then(
                    buttonModifier.border(
                        width = 1.dp,
                        color = textButtonColor.copy(alpha = 0.3f),
                        shape = shape,
                    ),
                ).alpha(alphaFactor)
        }

    Box(
        modifier = appliedModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (text != null) {
            Text(
                text = text,
                color = iconButtonColor.copy(alpha = if (enabled) 1f else 0.6f),
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .basicMarquee(),
            )
        } else {
            val baseTint =
                if (isActive) {
                    iconButtonColor
                } else {
                    when (playerBackground) {
                        PlayerBackgroundStyle.BLUR, PlayerBackgroundStyle.GRADIENT -> {
                            Color.White
                        }

                        PlayerBackgroundStyle.DEFAULT -> {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        }
                    }
                }
            val finalTint = if (enabled) baseTint else baseTint.copy(alpha = 0.5f)
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = finalTint,
            )
        }
    }
}
