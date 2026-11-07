package com.metrolist.desktop.playback

import co.touchlab.kermit.Logger
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.DownloadState
import com.metrolist.music.playback.SongCache
import com.metrolist.music.playback.SongDownloads
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap

/** Offline downloads kept as one file per song; also serves as the download [SongCache] shown in storage settings. */
class DesktopSongDownloads(
    private val database: MusicDatabase,
    private val streamResolver: StreamResolver,
    private val dir: File,
) : SongDownloads, SongCache {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parallelDownloads = Semaphore(MAX_PARALLEL_DOWNLOADS)
    private val jobs = ConcurrentHashMap<String, Job>()
    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build()

    private val _downloads =
        MutableStateFlow(
            dir.apply { mkdirs() }.listFiles().orEmpty().filter { it.isFile && !it.name.endsWith(PART_SUFFIX) }.associate { it.name to DownloadState.COMPLETED },
        )
    override val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    init {
        dir.listFiles().orEmpty().filter { it.name.endsWith(PART_SUFFIX) }.forEach { it.delete() }
    }

    /** The downloaded file for [songId], or null when it isn't fully downloaded. */
    fun file(songId: String): File? = File(dir, songId).takeIf { _downloads.value[songId] == DownloadState.COMPLETED && it.exists() }

    override fun download(mediaMetadata: MediaMetadata) {
        val id = mediaMetadata.id
        if (_downloads.value[id] in setOf(DownloadState.COMPLETED, DownloadState.QUEUED, DownloadState.DOWNLOADING)) return
        setState(id, DownloadState.QUEUED)
        jobs[id] =
            scope.launch {
                registerSong(mediaMetadata)
                parallelDownloads.withPermit {
                    setState(id, DownloadState.DOWNLOADING)
                    val part = File(dir, id + PART_SUFFIX)
                    runCatching {
                        val stream = streamResolver.resolve(id) ?: error("No playable stream")
                        fetch(stream, part)
                        ensureActive()
                        check(part.renameTo(File(dir, id))) { "Could not move $part into place" }
                    }.onSuccess {
                        setState(id, DownloadState.COMPLETED)
                        database.updateDownloadedInfo(id, true, LocalDateTime.now())
                    }.onFailure {
                        part.delete()
                        if (it is CancellationException) return@withPermit
                        Logger.withTag("DesktopDownloads").e(it) { "Download failed for $id" }
                        setState(id, DownloadState.FAILED)
                    }
                }
                jobs.remove(id)
            }
    }

    override fun download(songId: String) {
        scope.launch { database.getSongByIdBlocking(songId)?.let { download(it.toMediaMetadata()) } }
    }

    override fun remove(songId: String) {
        jobs.remove(songId)?.cancel()
        File(dir, songId).delete()
        File(dir, songId + PART_SUFFIX).delete()
        _downloads.update { it - songId }
        scope.launch { database.updateDownloadedInfo(songId, false, null) }
    }

    override fun contentLength(song: Song): Long? = file(song.id)?.length()

    override fun isCached(
        songId: String,
        contentLength: Long,
    ) = file(songId) != null

    override val size: Long
        get() = dir.listFiles().orEmpty().sumOf { it.length() }

    override fun clear() {
        (_downloads.value.keys + jobs.keys).forEach(::remove)
    }

    private suspend fun registerSong(mediaMetadata: MediaMetadata) {
        mediaMetadata.album?.let { album ->
            if (database.albumEntity(album.id) == null) {
                database.insert(AlbumEntity(id = album.id, title = album.title, thumbnailUrl = mediaMetadata.thumbnailUrl, songCount = 0, duration = 0))
            }
        }
        val existing = database.getSongByIdBlocking(mediaMetadata.id)
        if (existing == null) {
            database.insert(mediaMetadata)
        } else {
            database.update(existing, mediaMetadata, overwriteTitle = false, overwriteArtists = false)
        }
    }

    // Fetched in ranges like a player would; YouTube throttles single whole-file requests.
    private suspend fun fetch(
        stream: ResolvedStream,
        target: File,
    ) {
        target.outputStream().use { out ->
            var offset = 0L
            var total = Long.MAX_VALUE
            while (offset < total) {
                currentCoroutineContext().ensureActive()
                val request =
                    HttpRequest
                        .newBuilder(URI(stream.url))
                        .apply { stream.headers.forEach { (key, value) -> runCatching { header(key, value) } } }
                        .header("Range", "bytes=$offset-${offset + CHUNK_BYTES - 1}")
                        .build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
                check(response.statusCode() == 206 || response.statusCode() == 200) { "HTTP ${response.statusCode()}" }
                total = response.headers().firstValue("Content-Range").map { it.substringAfter('/').toLongOrNull() }.orElse(null)
                    ?: response.headers().firstValueAsLong("Content-Length").orElse(-1).takeIf { response.statusCode() == 200 && it >= 0 }
                    ?: error("Unknown content length")
                val read = response.body().use { it.copyTo(out) }
                check(read > 0) { "Empty response at $offset of $total" }
                offset += read
            }
        }
    }

    private fun setState(
        id: String,
        state: DownloadState,
    ) = _downloads.update { it + (id to state) }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val MAX_PARALLEL_DOWNLOADS = 3
        const val CHUNK_BYTES = 4L * 1024 * 1024
    }
}
