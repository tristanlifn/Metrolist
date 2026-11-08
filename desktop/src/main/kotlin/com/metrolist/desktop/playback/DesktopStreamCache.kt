/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.playback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import co.touchlab.kermit.Logger
import com.metrolist.music.constants.EnableSongCacheKey
import com.metrolist.music.constants.MaxSongCacheSizeKey
import com.metrolist.music.db.entities.Song
import com.metrolist.music.playback.SongCache
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.BitSet
import java.util.concurrent.Executors

/**
 * Cache for streamed songs, like Media3's player cache on Android. mpv plays the current song through a
 * loopback proxy that fetches missing ranges from YouTube and stores them, so the song is downloaded once;
 * a song whose every byte has been fetched is kept as a file and played from disk next time.
 */
class DesktopStreamCache(
    private val dir: File,
    private val dataStore: DataStore<Preferences>,
) : SongCache {
    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build()

    @Volatile private var current: Entry? = null

    private val executor = Executors.newCachedThreadPool { Thread(it, "StreamCache").apply { isDaemon = true } }
    private val server by lazy {
        HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0).apply {
            createContext("/") { exchange -> exchange.use { serve(it) } }
            executor = this@DesktopStreamCache.executor
            start()
        }
    }

    init {
        dir.mkdirs()
        dir.listFiles().orEmpty().filter { it.name.endsWith(PART_SUFFIX) }.forEach { it.delete() }
    }

    /** The fully cached file for [songId] if the cache is enabled, marked as recently used. */
    suspend fun file(songId: String): File? {
        if (!isEnabled()) return null
        return File(dir, songId).takeIf { it.isFile }?.apply { setLastModified(System.currentTimeMillis()) }
    }

    /** What mpv should load to play [stream]: the proxy URL that caches it, or the stream itself when the cache is off. */
    suspend fun playbackUrl(
        songId: String,
        stream: ResolvedStream,
    ): String {
        if (!isEnabled()) return stream.url
        val entry = Entry(songId, stream, File(dir, songId + PART_SUFFIX))
        // Closing waits for a chunk that's being fetched, so it doesn't hold up loading the next song.
        current.also { current = entry }?.let { old -> executor.execute(old::close) }
        return "http://127.0.0.1:${server.address.port}/$songId"
    }

    override fun contentLength(song: Song): Long? = File(dir, song.id).takeIf { it.isFile }?.length()

    override fun isCached(
        songId: String,
        contentLength: Long,
    ) = File(dir, songId).isFile

    override fun remove(songId: String) {
        File(dir, songId).delete()
    }

    override val size: Long
        get() = dir.listFiles().orEmpty().filter { !it.name.endsWith(PART_SUFFIX) }.sumOf { it.length() }

    override fun clear() {
        dir.listFiles().orEmpty().filter { !it.name.endsWith(PART_SUFFIX) }.forEach { it.delete() }
    }

    private suspend fun isEnabled(): Boolean {
        val preferences = dataStore.data.first()
        return preferences[EnableSongCacheKey] != false && maxBytes(preferences) != 0L
    }

    private fun maxBytes(preferences: Preferences): Long =
        when (val megabytes = preferences[MaxSongCacheSizeKey] ?: DEFAULT_MAX_MB) {
            -1 -> Long.MAX_VALUE
            else -> megabytes * 1024L * 1024L
        }

    private fun serve(exchange: HttpExchange) {
        val entry = current?.takeIf { it.songId == exchange.requestURI.path.removePrefix("/") }
        if (entry == null) return exchange.sendResponseHeaders(404, -1)
        val range = exchange.requestHeaders.getFirst("Range")?.removePrefix("bytes=")?.split('-')
        val start = range?.getOrNull(0)?.toLongOrNull() ?: 0L
        try {
            val total = entry.ensureChunk(start / CHUNK_BYTES)
            if (start >= total) {
                exchange.responseHeaders.set("Content-Range", "bytes */$total")
                return exchange.sendResponseHeaders(416, -1)
            }
            val end = minOf(range?.getOrNull(1)?.toLongOrNull() ?: (total - 1), total - 1)
            exchange.responseHeaders.set("Accept-Ranges", "bytes")
            if (range != null) exchange.responseHeaders.set("Content-Range", "bytes $start-$end/$total")
            exchange.sendResponseHeaders(if (range != null) 206 else 200, end - start + 1)
            val buffer = ByteArray(64 * 1024)
            var position = start
            while (position <= end) {
                entry.ensureChunk(position / CHUNK_BYTES)
                val count = entry.read(position, buffer, minOf(buffer.size.toLong(), end - position + 1).toInt())
                exchange.responseBody.write(buffer, 0, count)
                position += count
            }
        } catch (e: IOException) {
            // mpv drops the connection when it seeks or the song changes.
            Logger.withTag("StreamCache").d { "Stopped serving ${entry.songId}: ${e.message}" }
        }
    }

    private inner class Entry(
        val songId: String,
        private val stream: ResolvedStream,
        private val part: File,
    ) {
        private val raf = RandomAccessFile(part, "rw")
        private val chunks = BitSet()
        private var total = -1L
        @Volatile private var closed = false

        /** Fetches chunk [index] unless it's already stored, and returns the song's total size. */
        @Synchronized
        fun ensureChunk(index: Long): Long {
            if (closed) throw IOException("Song changed")
            if (total >= 0 && (index * CHUNK_BYTES >= total || chunks[index.toInt()])) return total
            var offset = index * CHUNK_BYTES
            val chunkEnd = offset + CHUNK_BYTES
            while (offset < chunkEnd && (total < 0 || offset < total)) {
                val request =
                    HttpRequest
                        .newBuilder(URI(stream.url))
                        .apply { stream.headers.forEach { (key, value) -> runCatching { header(key, value) } } }
                        .header("Range", "bytes=$offset-${chunkEnd - 1}")
                        .build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofInputStream())
                if (response.statusCode() != 206) {
                    response.body().close()
                    throw IOException("HTTP ${response.statusCode()} for ${stream.clientName}")
                }
                total = response.headers().firstValue("Content-Range").map { it.substringAfter('/').toLongOrNull() }.orElse(null)
                    ?: throw IOException("Missing Content-Range")
                val buffer = ByteArray(64 * 1024)
                val before = offset
                response.body().use { body ->
                    while (!closed) {
                        val count = body.read(buffer)
                        if (count < 0) break
                        raf.seek(offset)
                        raf.write(buffer, 0, count)
                        offset += count
                    }
                }
                if (closed) throw IOException("Song changed")
                if (offset == before) throw IOException("Empty response at $offset of $total")
            }
            chunks.set(index.toInt())
            if (chunks.cardinality().toLong() == (total + CHUNK_BYTES - 1) / CHUNK_BYTES) complete()
            return total
        }

        @Synchronized
        fun read(
            position: Long,
            buffer: ByteArray,
            length: Int,
        ): Int {
            if (closed) throw IOException("Song changed")
            raf.seek(position)
            return raf.read(buffer, 0, length)
        }

        fun close() {
            closed = true
            synchronized(this) {
                raf.close()
                part.delete()
            }
        }

        // The open file handle keeps working after the rename, so playback continues uninterrupted.
        private fun complete() {
            val target = File(dir, songId)
            if (!part.renameTo(target)) return
            val limit = runCatching { runBlocking { maxBytes(dataStore.data.first()) } }.getOrDefault(Long.MAX_VALUE)
            var used = size
            dir
                .listFiles()
                .orEmpty()
                .filter { !it.name.endsWith(PART_SUFFIX) && it != target }
                .sortedBy { it.lastModified() }
                .forEach { file ->
                    if (used <= limit) return
                    used -= file.length()
                    file.delete()
                }
        }
    }

    private companion object {
        const val PART_SUFFIX = ".part"
        const val CHUNK_BYTES = 1024L * 1024
        const val DEFAULT_MAX_MB = 1024
    }
}
