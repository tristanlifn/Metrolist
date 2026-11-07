/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.HideVideoSongsKey
import com.metrolist.music.constants.HistoryDuration
import com.metrolist.music.constants.PauseListenHistoryKey
import com.metrolist.music.constants.PersistentQueueKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.Event
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.PersistQueueSessions
import com.metrolist.music.models.QueueSession
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.CastControl
import com.metrolist.music.playback.MusicPlayer
import com.metrolist.music.playback.PlaybackState
import com.metrolist.music.playback.PlayerError
import com.metrolist.music.playback.QueueEntry
import com.metrolist.music.playback.RepeatMode
import com.metrolist.music.playback.SleepTimerControl
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.playback.queues.filterExplicit
import com.metrolist.music.playback.queues.filterVideoSongs
import com.metrolist.music.utils.SyncUtils
import com.metrolist.music.utils.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDateTime

/**
 * [MusicPlayer] backed by libmpv. All queue and playback state is changed on [scope], a single-threaded
 * context, so mpv's event thread only posts work to it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DesktopMusicPlayer(
    private val database: MusicDatabase,
    private val dataStore: DataStore<Preferences>,
    private val syncUtils: SyncUtils,
    private val streamResolver: StreamResolver,
    private val downloads: DesktopSongDownloads,
    // Where queue sessions are kept between runs; null keeps them in memory only.
    private val sessionsFile: File?,
) : MusicPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default.limitedParallelism(1))
    private val mpv = Mpv { event -> scope.launch { onMpvEvent(event) } }

    private data class Entry(val uid: Long, val metadata: MediaMetadata)

    // items is the unshuffled queue; order maps playback positions to items indices.
    private val items = mutableListOf<Entry>()
    private var order = listOf<Int>()
    private var position = -1
    private var nextUid = 0L
    private var source: Queue? = null
    private var pagingJob: Job? = null
    private var loadJob: Job? = null
    private var playedMs = 0L
    private var playingSinceMs: Long? = null

    // mpv reports initial property values before anything is loaded; those must not mark the player ready.
    private var fileLoaded = false

    override val mediaMetadata = MutableStateFlow<MediaMetadata?>(null)
    override val isReady: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()
    private val _newQueueStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val newQueueStarted: Flow<Unit> = _newQueueStarted
    private val playWhenReady = MutableStateFlow(false)
    override val playbackState = MutableStateFlow(PlaybackState.IDLE)
    // Updated in the same step as its inputs, so observers never see a stale combination.
    override val isPlaying = MutableStateFlow(false)
    override val isEffectivelyPlaying: StateFlow<Boolean> get() = isPlaying
    override val currentSong: StateFlow<Song?> =
        mediaMetadata.flatMapLatest { database.song(it?.id) }.stateIn(scope, SharingStarted.Eagerly, null)
    override val currentLyrics: StateFlow<LyricsEntity?> =
        mediaMetadata.flatMapLatest { database.lyrics(it?.id) }.stateIn(scope, SharingStarted.Eagerly, null)
    override val currentFormat: StateFlow<FormatEntity?> =
        mediaMetadata.flatMapLatest { database.format(it?.id) }.stateIn(scope, SharingStarted.Eagerly, null)
    override val queueTitle = MutableStateFlow<String?>(null)
    override val queue = MutableStateFlow<List<QueueEntry>>(emptyList())
    override val currentQueuePosition = MutableStateFlow(-1)
    override val queueSessions = MutableStateFlow<List<QueueSession>>(emptyList())
    override val activeQueueId = MutableStateFlow<Long?>(null)
    override val shuffleModeEnabled = MutableStateFlow(false)
    override val repeatMode = MutableStateFlow(RepeatMode.OFF)
    override val canSkipPrevious = MutableStateFlow(false)
    override val canSkipNext = MutableStateFlow(false)
    override val error = MutableStateFlow<PlayerError?>(null)
    override val isMuted = MutableStateFlow(false)
    override val currentStreamClient = MutableStateFlow<String?>(null)
    override val volume = MutableStateFlow(1f)
    override val automixItems: StateFlow<List<MediaMetadata>> = MutableStateFlow(emptyList<MediaMetadata>()).asStateFlow()
    override val sleepTimer: SleepTimerControl? = null
    override val cast: CastControl? = null

    override val currentPosition: Long
        get() = mpv.getDouble("time-pos")?.let { (it * 1000).toLong() } ?: 0L

    override val duration: Long
        get() =
            mpv.getDouble("duration")?.let { (it * 1000).toLong() }
                ?: mediaMetadata.value?.duration?.takeIf { it > 0 }?.let { it * 1000L }
                ?: MusicPlayer.TIME_UNSET

    override var playbackSpeed: Float = 1f
        set(value) {
            field = value
            mpv.setProperty("speed", value.toString())
        }

    // mpv keeps pitch while changing speed; independent pitch shifting isn't wired up yet.
    override var pitch: Float = 1f

    override val hasPreviousItem: Boolean get() = position > 0
    override val hasNextItem: Boolean get() = position in 0 until order.size - 1

    init {
        scope.launch {
            restoreSessions()
            // Saved while playing and on pause too, so a hard kill or a lost session keeps the queue.
            isPlaying.drop(1).collectLatest { playing ->
                if (!playing) return@collectLatest persistActiveSession()
                while (true) {
                    delay(SAVE_INTERVAL_MS)
                    persistActiveSession()
                }
            }
        }
    }

    override fun playQueue(queue: Queue) {
        scope.launch {
            recordHistory()
            pagingJob?.cancel()
            snapshotActiveSession()
            startQueueSession(queue)
            val sessionId = activeQueueId.value
            source = queue
            queue.preloadItem?.let { preload ->
                setItems(listOf(preload), 0)
                startCurrent(0L, playWhenReady = true)
            }
            val status =
                queue
                    .getInitialStatus()
                    .filterExplicit(dataStore.get(HideExplicitKey, false))
                    .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
            queueTitle.value = status.title
            status.title?.let { onSessionTitleResolved(sessionId, it) }
            if (status.items.isEmpty()) return@launch
            val alreadyPlaying = queue.preloadItem != null && status.items.getOrNull(status.mediaItemIndex)?.id == mediaMetadata.value?.id
            setItems(status.items, status.mediaItemIndex)
            _newQueueStarted.tryEmit(Unit)
            if (!alreadyPlaying) startCurrent(status.position, playWhenReady = true)
        }
    }

    override fun switchQueue(id: Long) {
        scope.launch {
            if (id == activeQueueId.value) return@launch
            val target = queueSessions.value.firstOrNull { it.id == id }?.takeIf { it.items.isNotEmpty() } ?: return@launch
            recordHistory()
            pagingJob?.cancel()
            snapshotActiveSession()
            loadSession(target, playWhenReady.value)
            queueSessions.value = listOf(target) + queueSessions.value.filter { it.id != id }
            activeQueueId.value = id
            saveSessions()
        }
    }

    override fun deleteQueue(id: Long) {
        scope.launch {
            queueSessions.value = queueSessions.value.filter { it.id != id }
            if (activeQueueId.value == id) {
                activeQueueId.value = null
                clearNow()
            }
            saveSessions()
        }
    }

    override fun playNext(items: List<MediaMetadata>) {
        scope.launch {
            if (position < 0) return@launch setAndPlay(items)
            val insertAt = order[position] + 1
            insertEntries(insertAt, items, playbackPosition = position + 1)
        }
    }

    override fun addToQueue(items: List<MediaMetadata>) {
        scope.launch {
            if (position < 0) return@launch setAndPlay(items)
            insertEntries(this@DesktopMusicPlayer.items.size, items, playbackPosition = order.size)
        }
    }

    override fun play() {
        scope.launch {
            if (playbackState.value == PlaybackState.ENDED) {
                position = 0
                startCurrent(0L, playWhenReady = true)
            } else {
                mpv.setProperty("pause", "no")
            }
        }
    }

    override fun pause() {
        mpv.setProperty("pause", "yes")
    }

    override fun togglePlayPause() {
        if (playWhenReady.value) pause() else play()
    }

    override fun seekTo(position: Long) {
        mpv.command("seek", (position / 1000.0).toString(), "absolute")
    }

    override fun seekToItem(index: Int) {
        scope.launch {
            val target = order.indexOf(index).takeIf { it >= 0 } ?: return@launch
            moveTo(target)
        }
    }

    override fun seekToNext() = skipToNextItem()

    // Like Media3: past the first few seconds, "previous" restarts the song.
    override fun seekToPrevious() {
        if (currentPosition > RESTART_THRESHOLD_MS || !hasPreviousItem) seekTo(0) else skipToPreviousItem()
    }

    override fun skipToNextItem() {
        scope.launch {
            when {
                position < order.size - 1 -> moveTo(position + 1)
                repeatMode.value == RepeatMode.ALL && order.isNotEmpty() -> moveTo(0)
            }
        }
    }

    override fun skipToPreviousItem() {
        scope.launch { if (position > 0) moveTo(position - 1) }
    }

    override fun retry() {
        scope.launch { startCurrent(currentPosition, playWhenReady = true) }
    }

    override fun moveItem(
        from: Int,
        to: Int,
    ) {
        scope.launch {
            if (from !in order.indices || to !in order.indices) return@launch
            val current = order.getOrNull(position)
            if (shuffleModeEnabled.value) {
                order = order.toMutableList().apply { add(to, removeAt(from)) }
            } else {
                items.add(to, items.removeAt(from))
                order = items.indices.toList()
            }
            position = current?.let { c -> if (shuffleModeEnabled.value) order.indexOf(c) else positionOfMovedItem(c, from, to) } ?: position
            publishQueue()
        }
    }

    override fun insertItem(
        index: Int,
        item: MediaMetadata,
    ) {
        scope.launch { insertEntries(index.coerceIn(0, items.size), listOf(item), playbackPosition = null) }
    }

    override fun removeItem(index: Int) {
        scope.launch {
            if (index !in items.indices) return@launch
            val removedPosition = order.indexOf(index)
            val wasCurrent = removedPosition == position
            items.removeAt(index)
            order = order.filter { it != index }.map { if (it > index) it - 1 else it }
            if (removedPosition < position) position--
            if (items.isEmpty()) return@launch clearNow()
            position = position.coerceIn(0, order.size - 1)
            publishQueue()
            if (wasCurrent) startCurrent(0L, playWhenReady.value)
        }
    }

    override fun clearQueue() {
        scope.launch { clearNow() }
    }

    override fun setShuffleModeEnabled(enabled: Boolean) {
        scope.launch {
            val current = order.getOrNull(position)
            shuffleModeEnabled.value = enabled
            order =
                if (enabled && current != null) {
                    // Media3 keeps the playing song first and shuffles the rest.
                    listOf(current) + items.indices.filter { it != current }.shuffled()
                } else {
                    items.indices.toList()
                }
            position = current?.let(order::indexOf) ?: -1
            publishQueue()
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        repeatMode.value = mode
        scope.launch { updateSkipState() }
    }

    override fun toggleRepeatMode() {
        setRepeatMode(
            when (repeatMode.value) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            },
        )
    }

    override fun toggleLike() {
        scope.launch {
            val song = currentSong.value?.song?.toggleLike(syncToYouTube = false) ?: return@launch
            database.update(song)
            syncUtils.likeSong(song)
        }
    }

    override fun toggleLibrary() {
        scope.launch {
            val song = currentSong.value?.song ?: return@launch
            val token = if (song.inLibrary != null) song.libraryRemoveToken else song.libraryAddToken
            token?.let { YouTube.feedback(listOf(it)) }
            database.update(song.toggleLibrary())
        }
    }

    override fun toggleMute() {
        isMuted.value = !isMuted.value
        mpv.setProperty("mute", if (isMuted.value) "yes" else "no")
    }

    override fun setVolume(volume: Float) {
        this.volume.value = volume.coerceIn(0f, 1f)
        mpv.setProperty("volume", (this.volume.value * 100).toString())
    }

    // Replaces everything after the playing song with a radio based on it, like the Android service.
    override fun startRadioSeamlessly() {
        scope.launch {
            val current = mediaMetadata.value ?: return@launch
            val status =
                YouTubeQueue(WatchEndpoint(videoId = current.id))
                    .also { source = it }
                    .getInitialStatus()
                    .filterExplicit(dataStore.get(HideExplicitKey, false))
                    .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
            status.title?.let { queueTitle.value = it }
            val radio = status.items.filter { it.id != current.id }
            if (radio.isEmpty() || position < 0) return@launch
            val keep = order.take(position + 1).map { items[it] }
            items.clear()
            items.addAll(keep)
            radio.forEach { items.add(Entry(nextUid++, it)) }
            order = items.indices.toList()
            position = keep.size - 1
            shuffleModeEnabled.value = false
            publishQueue()
        }
    }

    override fun refreshSongMetadata(song: Song) {
        scope.launch {
            val updated = song.toMediaMetadata()
            items.replaceAll { if (it.metadata.id == song.id) it.copy(metadata = updated) else it }
            if (mediaMetadata.value?.id == song.id) mediaMetadata.value = updated
            publishQueue()
        }
    }

    override fun loadAutomix(playlistId: String) = Unit

    override fun playNextAutomix(index: Int) = Unit

    override fun addToQueueAutomix(index: Int) = Unit

    override fun clearAutomix() = Unit

    fun release() {
        runBlocking {
            withTimeoutOrNull(RELEASE_TIMEOUT_MS) {
                scope.launch { persistActiveSession() }.join()
            }
        }
        mpv.destroy()
    }

    private suspend fun restoreSessions() {
        val file = sessionsFile?.takeIf { it.exists() } ?: return
        if (!dataStore.get(PersistentQueueKey, true)) return
        val saved =
            runCatching { ObjectInputStream(file.inputStream()).use { it.readObject() as PersistQueueSessions } }
                .onFailure {
                    Logger.withTag("DesktopPlayer").w(it) { "Failed to read queue sessions, clearing data" }
                    file.delete()
                }.getOrNull() ?: return
        queueSessions.value = saved.sessions
        val active = saved.sessions.firstOrNull { it.id == saved.activeId && it.items.isNotEmpty() } ?: return
        activeQueueId.value = active.id
        loadSession(active, playWhenReady = false)
    }

    private suspend fun loadSession(
        session: QueueSession,
        playWhenReady: Boolean,
    ) {
        val index = session.mediaItemIndex.coerceIn(0, session.items.lastIndex)
        // Sessions read from disk lose their source queue, so they can't page further.
        source = session.liveQueue
        queueTitle.value = session.title
        setItems(session.items, index)
        if (session.shuffleEnabled) {
            shuffleModeEnabled.value = true
            order = session.shuffleOrder?.takeIf { it.sorted() == items.indices.toList() }
                ?: (listOf(index) + items.indices.filter { it != index }.shuffled())
            position = order.indexOf(index)
            publishQueue()
        }
        _newQueueStarted.tryEmit(Unit)
        startCurrent(session.position, playWhenReady)
    }

    private suspend fun persistActiveSession() {
        snapshotActiveSession()
        saveSessions()
    }

    private fun snapshotActiveSession() {
        val id = activeQueueId.value ?: return
        val playing = order.getOrNull(position) ?: return
        queueSessions.value =
            queueSessions.value.map { session ->
                if (session.id != id) {
                    session
                } else {
                    session.copy(
                        title = queueTitle.value,
                        items = items.map { it.metadata },
                        mediaItemIndex = playing,
                        position = currentPosition,
                        shuffleEnabled = shuffleModeEnabled.value,
                        shuffleOrder = order.takeIf { shuffleModeEnabled.value },
                        liveQueue = source,
                    )
                }
            }
    }

    private suspend fun startQueueSession(queue: Queue) {
        val session =
            QueueSession(
                id = (queueSessions.value.maxOfOrNull { it.id } ?: 0L) + 1,
                title = null,
                items = emptyList(),
                mediaItemIndex = 0,
                position = 0L,
                shuffleEnabled = false,
                shuffleOrder = null,
                liveQueue = queue,
            )
        queueSessions.value = (listOf(session) + queueSessions.value).take(MAX_QUEUE_SESSIONS)
        activeQueueId.value = session.id
        saveSessions()
    }

    private suspend fun onSessionTitleResolved(
        sessionId: Long?,
        title: String,
    ) {
        if (sessionId == null || activeQueueId.value != sessionId) return
        // Playing a source again replaces its previous queue instead of duplicating it
        queueSessions.value =
            queueSessions.value
                .filter { it.id == sessionId || it.title != title }
                .map { if (it.id == sessionId) it.copy(title = title) else it }
        saveSessions()
    }

    private suspend fun saveSessions() {
        val file = sessionsFile ?: return
        if (!dataStore.get(PersistentQueueKey, true)) return
        val persisted = PersistQueueSessions(activeQueueId.value, queueSessions.value)
        runCatching {
            val tmp = File(file.path + ".tmp")
            ObjectOutputStream(tmp.outputStream()).use { it.writeObject(persisted) }
            tmp.renameTo(file)
        }.onFailure { Logger.withTag("DesktopPlayer").e(it) { "Failed to save queue sessions" } }
    }

    private suspend fun setAndPlay(newItems: List<MediaMetadata>) {
        setItems(newItems, 0)
        _newQueueStarted.tryEmit(Unit)
        startCurrent(0L, playWhenReady = true)
    }

    private fun setItems(
        newItems: List<MediaMetadata>,
        startIndex: Int,
    ) {
        items.clear()
        newItems.forEach { items.add(Entry(nextUid++, it)) }
        shuffleModeEnabled.value = false
        order = items.indices.toList()
        position = startIndex.coerceIn(0, items.size - 1)
        publishQueue()
    }

    private fun insertEntries(
        itemsIndex: Int,
        newItems: List<MediaMetadata>,
        playbackPosition: Int?,
    ) {
        if (newItems.isEmpty()) return
        items.addAll(itemsIndex, newItems.map { Entry(nextUid++, it) })
        val shifted = order.map { if (it >= itemsIndex) it + newItems.size else it }
        val added = List(newItems.size) { itemsIndex + it }
        val at = (playbackPosition ?: shifted.indexOfFirst { it > itemsIndex - 1 }.takeIf { it >= 0 } ?: shifted.size).coerceIn(0, shifted.size)
        order = shifted.take(at) + added + shifted.drop(at)
        if (at <= position) position += newItems.size
        publishQueue()
    }

    private fun positionOfMovedItem(
        current: Int,
        from: Int,
        to: Int,
    ): Int =
        when {
            current == from -> to
            from < current && to >= current -> current - 1
            from > current && to <= current -> current + 1
            else -> current
        }

    private suspend fun moveTo(target: Int) {
        recordHistory()
        position = target
        publishQueue()
        startCurrent(0L, playWhenReady = true)
    }

    private suspend fun startCurrent(
        startMs: Long,
        playWhenReady: Boolean,
    ) {
        val entry = order.getOrNull(position)?.let(items::getOrNull) ?: return
        val metadata = entry.metadata
        mediaMetadata.value = metadata
        error.value = null
        fileLoaded = false
        setPlaybackState(PlaybackState.BUFFERING)
        playedMs = 0L
        loadJob?.cancel()
        loadJob =
            scope.launch {
                if (database.song(metadata.id).first() == null) {
                    database.insert(metadata)
                }
                // Downloaded songs play from disk, so they work offline.
                val localFile = downloads.file(metadata.id)
                // Shown as the stream title in volume mixers instead of the stream URL.
                mpv.setProperty("force-media-title", "${metadata.title} – ${metadata.artists.joinToString { it.name }}")
                mpv.setProperty("start", if (startMs > 0) "+${startMs / 1000.0}" else "none")
                mpv.setProperty("pause", if (playWhenReady) "no" else "yes")
                setPlayWhenReady(playWhenReady)
                if (localFile != null) {
                    currentStreamClient.value = null
                    mpv.setProperty("http-header-fields", "")
                    mpv.command("loadfile", localFile.path, "replace")
                    return@launch
                }
                val stream =
                    runCatching { streamResolver.resolve(metadata.id) }
                        .onFailure { Logger.withTag("DesktopPlayer").e(it) { "Stream extraction failed for ${metadata.id}" } }
                        .getOrNull()
                if (stream == null) {
                    error.value = PlayerError(ERROR_CODE_NO_STREAM, "ERROR_CODE_IO_UNSPECIFIED", IllegalStateException("No playable stream"))
                    setPlaybackState(PlaybackState.IDLE)
                    return@launch
                }
                currentStreamClient.value = stream.clientName
                mpv.setProperty("http-header-fields", "")
                stream.headers.forEach { (key, value) ->
                    // change-list append adds one entry as-is, so header values may contain commas.
                    mpv.command("change-list", "http-header-fields", "append", "$key: $value")
                }
                mpv.command("loadfile", stream.url, "replace")
            }
        maybeLoadMore()
    }

    private suspend fun onMpvEvent(event: Mpv.Event) {
        when (event) {
            Mpv.Event.FileLoaded -> {
                fileLoaded = true
                setPlaybackState(PlaybackState.READY)
                if (playWhenReady.value) playingSinceMs = System.currentTimeMillis()
            }
            is Mpv.Event.EndFile -> onEndFile(event)
            is Mpv.Event.PropertyChanged ->
                when (event.name) {
                    "pause" -> {
                        val playing = !mpv.getFlag("pause")
                        setPlayWhenReady(playing)
                        if (playing) playingSinceMs = System.currentTimeMillis() else accumulatePlayTime()
                    }
                    "paused-for-cache" ->
                        if (fileLoaded && playbackState.value != PlaybackState.IDLE && playbackState.value != PlaybackState.ENDED) {
                            setPlaybackState(if (mpv.getFlag("paused-for-cache")) PlaybackState.BUFFERING else PlaybackState.READY)
                        }
                }
        }
    }

    private suspend fun onEndFile(event: Mpv.Event.EndFile) {
        when (event.reason) {
            LibMpv.END_FILE_REASON_EOF -> {
                recordHistory()
                when {
                    repeatMode.value == RepeatMode.ONE -> startCurrent(0L, playWhenReady = true)
                    position < order.size - 1 -> moveTo(position + 1)
                    repeatMode.value == RepeatMode.ALL && order.isNotEmpty() -> moveTo(0)
                    else -> {
                        setPlaybackState(PlaybackState.ENDED)
                        mpv.setProperty("pause", "yes")
                    }
                }
            }
            LibMpv.END_FILE_REASON_ERROR -> {
                error.value = PlayerError(ERROR_CODE_PLAYBACK, "ERROR_CODE_IO_UNSPECIFIED", RuntimeException(event.error ?: "Playback failed"))
                setPlaybackState(PlaybackState.IDLE)
            }
        }
    }

    private fun setPlaybackState(state: PlaybackState) {
        playbackState.value = state
        isPlaying.value = playWhenReady.value && state == PlaybackState.READY
    }

    private fun setPlayWhenReady(play: Boolean) {
        playWhenReady.value = play
        isPlaying.value = play && playbackState.value == PlaybackState.READY
    }

    private fun accumulatePlayTime() {
        playingSinceMs?.let { playedMs += System.currentTimeMillis() - it }
        playingSinceMs = null
    }

    // Same rule as the Android service: count a play once it passes the history duration setting.
    private suspend fun recordHistory() {
        accumulatePlayTime()
        val songId = mediaMetadata.value?.id ?: return
        val played = playedMs
        playedMs = 0L
        val minimumMs = (dataStore[HistoryDuration] ?: 30f) * 1000f
        if (played < minimumMs || dataStore.get(PauseListenHistoryKey, false)) return
        database.incrementTotalPlayTime(songId, played)
        runCatching { database.insert(Event(songId = songId, timestamp = LocalDateTime.now(), playTime = played)) }
    }

    private fun maybeLoadMore() {
        val queueSource = source ?: return
        if (pagingJob?.isActive == true || !queueSource.hasNextPage() || position < order.size - PAGE_AHEAD) return
        pagingJob =
            scope.launch {
                val more =
                    queueSource
                        .nextPage()
                        .filterExplicit(dataStore.get(HideExplicitKey, false))
                        .filterVideoSongs(dataStore.get(HideVideoSongsKey, false))
                insertEntries(items.size, more, playbackPosition = order.size)
            }
    }

    private suspend fun clearNow() {
        recordHistory()
        loadJob?.cancel()
        pagingJob?.cancel()
        mpv.command("stop")
        items.clear()
        order = emptyList()
        position = -1
        source = null
        mediaMetadata.value = null
        setPlaybackState(PlaybackState.IDLE)
        publishQueue()
    }

    private fun publishQueue() {
        queue.value = order.map { index -> QueueEntry(items[index].uid, index, items[index].metadata) }
        currentQueuePosition.value = position
        updateSkipState()
    }

    private fun updateSkipState() {
        canSkipPrevious.value = position >= 0
        canSkipNext.value = position in 0 until order.size - 1 || (repeatMode.value == RepeatMode.ALL && order.isNotEmpty())
    }

    private companion object {
        const val RESTART_THRESHOLD_MS = 3_000L
        const val PAGE_AHEAD = 3
        const val MAX_QUEUE_SESSIONS = 20
        const val RELEASE_TIMEOUT_MS = 2_000L
        const val SAVE_INTERVAL_MS = 30_000L
        const val ERROR_CODE_NO_STREAM = 2004
        const val ERROR_CODE_PLAYBACK = 2001
    }
}
