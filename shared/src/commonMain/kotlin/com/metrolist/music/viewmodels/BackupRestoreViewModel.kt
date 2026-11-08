/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.innertube.utils.sha1
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.resources.Res
import com.metrolist.music.resources.backup_create_failed
import com.metrolist.music.resources.backup_create_success
import com.metrolist.music.resources.restore_database_incompatible
import com.metrolist.music.resources.restore_failed
import com.metrolist.music.ui.utils.PickedFile
import com.metrolist.music.utils.ArtistNameAliases
import com.metrolist.music.utils.BackupPlatform
import com.metrolist.music.utils.PlatformActions
import com.metrolist.music.utils.reportException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupPreviewInfo(
    val hasAuthData: Boolean = false,
    val accountName: String? = null,
    val accountEmail: String? = null,
    val accountImageUrl: String? = null,
    val cookie: String? = null,
)

data class CsvImportState(
    val previewRows: List<List<String>> = emptyList(),
    val artistColumnIndex: Int = 0,
    val titleColumnIndex: Int = 1,
    val urlColumnIndex: Int = -1,
    val hasHeader: Boolean = true,
)

data class ConvertedSongLog(
    val title: String,
    val artists: String,
)

class BackupRestoreViewModel(
    val database: MusicDatabase,
    private val platform: BackupPlatform,
    private val platformActions: PlatformActions,
) : ViewModel() {
    fun backup(openStream: () -> OutputStream) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                openStream().buffered().let(::ZipOutputStream).use { outputStream ->
                    platform.settingsFile.inputStream().buffered().use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(SETTINGS_FILENAME))
                        inputStream.copyTo(outputStream)
                    }
                    outputStream.putNextEntry(ZipEntry(ArtistNameAliases.BACKUP_FILENAME))
                    outputStream.write(ArtistNameAliases.serialize().encodeToByteArray())
                    database.checkpoint()
                    val dbPath = platform.databaseFile.path
                    File(dbPath).inputStream().use { inputStream ->
                        outputStream.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                        inputStream.copyTo(outputStream)
                    }
                    val walFile = File("$dbPath-wal")
                    if (walFile.exists()) {
                        walFile.inputStream().use { inputStream ->
                            outputStream.putNextEntry(ZipEntry("${InternalDatabase.DB_NAME}-wal"))
                            inputStream.copyTo(outputStream)
                        }
                    }
                    val shmFile = File("$dbPath-shm")
                    if (shmFile.exists()) {
                        shmFile.inputStream().use { inputStream ->
                            outputStream.putNextEntry(ZipEntry("${InternalDatabase.DB_NAME}-shm"))
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }.onSuccess {
                platformActions.toast(Res.string.backup_create_success)
            }.onFailure {
                reportException(it)
                platformActions.toast(Res.string.backup_create_failed)
            }
        }
    }

    fun restore(file: PickedFile) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentDbPath = platform.databaseFile.path
            val restoreDbPath = "${platform.databaseFile.parent}/restored_${InternalDatabase.DB_NAME}"
            val tempSettings = File("${platform.settingsFile.path}.restore")
            val logger = Logger.withTag("RESTORE")

            runCatching {
                logger.i { "Starting restore" }

                File(restoreDbPath).delete()
                File("$restoreDbPath-wal").delete()
                File("$restoreDbPath-shm").delete()
                tempSettings.delete()

                var foundDb = false
                var foundSettings = false
                var restoredArtistNameAliases: Map<String, String>? = null

                file.openStream().use { raw ->
                    ZipInputStream(raw).use { inputStream ->
                        var entry = runCatching { inputStream.nextEntry }.getOrNull()
                        while (entry != null) {
                            logger.i { "Found zip entry: ${entry.name}" }
                            when (entry.name) {
                                SETTINGS_FILENAME -> {
                                    foundSettings = true
                                    tempSettings.outputStream().use { it.write(inputStream.readBytes()) }
                                }
                                ArtistNameAliases.BACKUP_FILENAME -> {
                                    restoredArtistNameAliases =
                                        ArtistNameAliases.deserialize(inputStream.readBytes().decodeToString())
                                }
                                InternalDatabase.DB_NAME -> {
                                    foundDb = true
                                    File(restoreDbPath).outputStream().use { inputStream.copyTo(it) }
                                }
                                "${InternalDatabase.DB_NAME}-wal" -> {
                                    // Skip WAL — we'll open cleanly
                                }
                                "${InternalDatabase.DB_NAME}-shm" -> {
                                    // Skip SHM — we'll open cleanly
                                }
                                else -> logger.i { "Skipping unexpected entry: ${entry.name}" }
                            }
                            entry = runCatching { inputStream.nextEntry }.getOrNull()
                        }
                    }
                }

                if (!foundDb && !foundSettings) {
                    logger.w { "No expected entries found in archive" }
                    platformActions.toast(Res.string.restore_failed)
                    return@launch
                }

                if (foundDb) {
                    val backupDbVersion = readDatabaseVersion(File(restoreDbPath))
                    // The live file only shows the current version once the WAL is checkpointed.
                    database.checkpoint()
                    val currentDbVersion = readDatabaseVersion(platform.databaseFile)
                    if (backupDbVersion !in platform.oldestRestorableDatabaseVersion..currentDbVersion) {
                        logger.w { "Backup DB version $backupDbVersion can't be restored on version $currentDbVersion" }
                        platformActions.toast(Res.string.restore_database_incompatible, long = true)
                        return@launch
                    }
                }

                // === Proceed with restore ===

                // 1. Stop playback first — ensures no pending DB writes during swap
                try {
                    withTimeout(5000) { platform.stopPlayback() }
                } catch (e: Exception) {
                    logger.e(e) { "Timeout waiting for playback to stop" }
                }

                // 2. Close the database — all operations should be done by now
                database.close()

                // 3. Swap DB files — staged copy to avoid corrupting the live DB
                var dbSwapSucceeded = true
                if (foundDb) {
                    try {
                        val currentFile = File(currentDbPath)
                        val stagedFile = File("$currentDbPath.restore_staged")
                        val backupFile = File("$currentDbPath.restore_backup")

                        stagedFile.delete()
                        backupFile.delete()

                        File(restoreDbPath).copyTo(stagedFile, overwrite = true)

                        // Preserve current DB, promote staged, remove backup
                        if (!currentFile.renameTo(backupFile)) {
                            error("Failed to preserve current DB before restore")
                        }
                        if (!stagedFile.renameTo(currentFile)) {
                            // Rollback: restore the original
                            backupFile.renameTo(currentFile)
                            error("Failed to promote restored DB")
                        }
                        backupFile.delete()

                        // Delete stale WAL/SHM so the DB opens clean
                        File("$currentDbPath-wal").delete()
                        File("$currentDbPath-shm").delete()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to swap DB files" }
                        dbSwapSucceeded = false
                    }
                }

                if (dbSwapSucceeded) {
                    if (foundSettings) {
                        tempSettings.copyTo(platform.settingsFile, overwrite = true)
                    }

                    platform.playerStateFiles.forEach(File::delete)
                    platform.restoreArtistNameAliases(restoredArtistNameAliases.orEmpty())

                    // 4. Restart — Room will open the swapped DB and run migrations if needed
                    platform.restart()
                } else {
                    platformActions.toast(Res.string.restore_failed)
                }
            }.onFailure {
                reportException(it)
                logger.e(it) { "Restore failed unexpectedly" }
                platformActions.toast(Res.string.restore_failed)
            }

            File(restoreDbPath).delete()
            File("$restoreDbPath-wal").delete()
            File("$restoreDbPath-shm").delete()
            tempSettings.delete()
        }
    }

    fun previewBackup(file: PickedFile): BackupPreviewInfo {
        return runCatching {
            file.openStream().use { raw ->
                ZipInputStream(raw).use { inputStream ->
                    var entry = runCatching { inputStream.nextEntry }.getOrNull()
                    while (entry != null) {
                        if (entry.name == SETTINGS_FILENAME) {
                            val bytes = inputStream.readBytes()
                            val content = bytes.decodeToString(throwOnInvalidSequence = false)

                            // Check for auth data (SAPISID cookie indicates logged in)
                            val hasAuthData = content.contains("SAPISID=")

                            // Extract cookie string from backup
                            val cookie = if (hasAuthData) {
                                extractCookieFromPrefs(content)
                            } else null

                            val accountName = if (hasAuthData) {
                                extractAccountNameFromPrefs(content) ?: "YouTube Account"
                            } else null

                            return BackupPreviewInfo(
                                hasAuthData = hasAuthData,
                                accountName = accountName,
                                accountEmail = null,
                                accountImageUrl = null,
                                cookie = cookie,
                            )
                        }
                        entry = runCatching { inputStream.nextEntry }.getOrNull()
                    }
                }
            }
            BackupPreviewInfo()
        }.getOrElse {
            Logger.withTag("BACKUP_PREVIEW").e(it) { "Failed to preview backup" }
            BackupPreviewInfo()
        }
    }

    private fun extractAccountNameFromPrefs(content: String): String? {
        val sessionPattern = Regex("""(?:SESSION_INDEX|sessionIndex|session)_?\s*["':]*\s*(\d+)""")
        val index = sessionPattern.find(content)?.groupValues?.getOrNull(1)
        return if (index != null) "Account #$index" else null
    }

    private fun extractCookieFromPrefs(content: String): String? {
        // Find innerTubeCookie key and extract the cookie value.
        // The proto format has the key followed by type markers and then the string value.
        val keyMarker = "innerTubeCookie"
        val keyIndex = content.indexOf(keyMarker)
        if (keyIndex == -1) return null

        val afterKey = content.substring(keyIndex + keyMarker.length)

        // Cookie starts after some proto markers and contains semicolon-separated values.
        // Look for the first cookie key pattern like "__Secure-" or "HSID=" etc.
        val cookiePatterns = listOf("__Secure-", "HSID=", "SSID=", "SID=", "SAPISID=")
        var cookieStart = -1
        for (pattern in cookiePatterns) {
            val idx = afterKey.indexOf(pattern)
            if (idx != -1 && (cookieStart == -1 || idx < cookieStart)) {
                cookieStart = idx
            }
        }
        if (cookieStart == -1) return null

        // Find the end of the cookie (next control character or next key).
        val cookieContent = afterKey.substring(cookieStart)
        val cookieEnd = cookieContent.indexOfFirst {
            it.code < 32 && it != '\t' && it != '\n' && it != '\r'
        }

        val rawCookie = if (cookieEnd > 0) {
            cookieContent.substring(0, cookieEnd)
        } else {
            cookieContent.take(5000) // Reasonable max length
        }
        // Remove any control characters (newlines, etc.) that are invalid in HTTP headers.
        return rawCookie.replace(Regex("[\\x00-\\x1F\\x7F]"), "").trim()
    }

    suspend fun fetchAccountInfoFromBackup(cookie: String): BackupPreviewInfo? {
        return runCatching {
            // Parse cookie to get SAPISID for auth header
            val cookieMap = parseCookieString(cookie)
            val sapisid = cookieMap["SAPISID"] ?: return@runCatching null

            // Generate SAPISIDHASH auth header
            val origin = "https://music.youtube.com"
            val currentTime = System.currentTimeMillis() / 1000
            val sapisidHash = sha1("$currentTime $sapisid $origin")
            val authHeader = "SAPISIDHASH ${currentTime}_$sapisidHash"

            val client = OkHttpClient()
            val requestBody = """{"context":{"client":{"clientName":"WEB_REMIX","clientVersion":"1.20240101.01.00"}}}"""
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("https://music.youtube.com/youtubei/v1/account/account_menu?prettyPrint=false")
                .post(requestBody)
                .header("Cookie", cookie)
                .header("Authorization", authHeader)
                .header("Origin", origin)
                .header("Referer", "$origin/")
                .header("X-Origin", origin)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@runCatching null

            // Parse the JSON response
            val json = Json { ignoreUnknownKeys = true }
            val jsonResponse = json.parseToJsonElement(responseBody).jsonObject

            // Navigate to activeAccountHeaderRenderer
            val header = jsonResponse["actions"]
                ?.jsonArray?.getOrNull(0)
                ?.jsonObject?.get("openPopupAction")
                ?.jsonObject?.get("popup")
                ?.jsonObject?.get("multiPageMenuRenderer")
                ?.jsonObject?.get("header")
                ?.jsonObject?.get("activeAccountHeaderRenderer")
                ?.jsonObject

            if (header != null) {
                val name = header["accountName"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.content

                val email = header["email"]
                    ?.jsonObject?.get("runs")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.content

                val thumbnailUrl = header["accountPhoto"]
                    ?.jsonObject?.get("thumbnails")
                    ?.jsonArray?.getOrNull(0)
                    ?.jsonObject?.get("url")
                    ?.jsonPrimitive?.content

                if (name != null) {
                    BackupPreviewInfo(
                        hasAuthData = true,
                        accountName = name,
                        accountEmail = email,
                        accountImageUrl = thumbnailUrl,
                        cookie = cookie,
                    )
                } else null
            } else null
        }.getOrElse {
            Logger.withTag("BACKUP_PREVIEW").e(it) { "Failed to fetch account info from backup" }
            null
        }
    }

    fun previewCsvFile(file: PickedFile): CsvImportState {
        val previewRows = mutableListOf<List<String>>()
        val csvState: CsvImportState
        runCatching {
            file.openStream().use { stream ->
                val lines = stream.bufferedReader().readLines()
                val rowsToPreview = lines.take(6).map { parseCsvLine(it) }
                previewRows.addAll(rowsToPreview)

                val hasHeader = lines.isNotEmpty() && lines[0].contains(",")
                csvState = CsvImportState(
                    previewRows = previewRows,
                    hasHeader = hasHeader,
                )
                return csvState
            }
        }.onFailure {
            reportException(it)
            platformActions.toast("Failed to preview CSV file")
        }
        return CsvImportState()
    }

    suspend fun importPlaylistFromCsv(
        file: PickedFile,
        columnMapping: CsvImportState,
        onProgress: (Int) -> Unit = {},
        onLogUpdate: (List<ConvertedSongLog>) -> Unit = {},
    ): ArrayList<Song> = withContext(Dispatchers.IO) {
        val songs = arrayListOf<Song>()
        val recentLogs = mutableListOf<ConvertedSongLog>()

        runCatching {
            file.openStream().use { stream ->
                val lines = stream.bufferedReader().readLines()
                val startIndex = if (columnMapping.hasHeader) 1 else 0
                val totalLines = lines.size - startIndex

                lines.drop(startIndex).forEachIndexed { index, line ->
                    val parts = parseCsvLine(line)

                    if (parts.isNotEmpty()) {
                        if (columnMapping.artistColumnIndex < parts.size && columnMapping.titleColumnIndex < parts.size) {
                            val title = parts[columnMapping.titleColumnIndex].trim()
                            val artistStr = parts[columnMapping.artistColumnIndex].trim()

                            if (title.isNotEmpty() && artistStr.isNotEmpty()) {
                                val artists = artistStr.split(";", ",").map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .map { ArtistEntity(id = "", name = it) }

                                val mockSong = Song(
                                    song = SongEntity(
                                        id = "",
                                        title = title,
                                    ),
                                    artists = artists,
                                )
                                songs.add(mockSong)

                                val logEntry = ConvertedSongLog(
                                    title = title,
                                    artists = artists.joinToString(", ") { it.name },
                                )
                                recentLogs.add(0, logEntry)
                                if (recentLogs.size > 3) {
                                    recentLogs.removeAt(recentLogs.size - 1)
                                }
                                onLogUpdate(recentLogs.toList())
                            }
                        }
                    }

                    val progress = ((index + 1) * 100) / totalLines
                    onProgress(progress)
                }
            }
        }.onFailure {
            reportException(it)
        }

        songs
    }

    suspend fun importPlaylistFromCsv(file: PickedFile): ArrayList<Song> {
        return importPlaylistFromCsv(file, CsvImportState())
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString())
        return result.map { it.trim().trim('"') }
    }

    fun loadM3UOnline(
        file: PickedFile,
    ): ArrayList<Song> {
        val songs = ArrayList<Song>()

        runCatching {
            file.openStream().use { stream ->
                val lines = stream.bufferedReader().readLines()
                if (lines.isNotEmpty() && lines.first().startsWith("#EXTM3U")) {
                    lines.forEachIndexed { _, rawLine ->
                        if (rawLine.startsWith("#EXTINF:")) {
                            val artists =
                                rawLine.substringAfter("#EXTINF:").substringAfter(',').substringBefore(" - ").split(';')
                            val title = rawLine.substringAfter("#EXTINF:").substringAfter(',').substringAfter(" - ")

                            val mockSong = Song(
                                song = SongEntity(
                                    id = "",
                                    title = title,
                                ),
                                artists = artists.map { ArtistEntity("", it) },
                            )
                            songs.add(mockSong)
                        }
                    }
                }
            }
        }

        if (songs.isEmpty()) {
            platformActions.toast("No songs found. Invalid file, or perhaps no song matches were found.")
        }
        return songs
    }

    companion object {
        const val SETTINGS_FILENAME = "settings.preferences_pb"

        /** SQLite's user_version, which Room uses as the schema version, read from the file header; -1 if unreadable. */
        private fun readDatabaseVersion(file: File): Int =
            runCatching {
                RandomAccessFile(file, "r").use {
                    val magic = ByteArray(16).also(it::readFully)
                    if (!magic.contentEquals("SQLite format 3\u0000".encodeToByteArray())) return -1
                    it.seek(60)
                    it.readInt()
                }
            }.getOrDefault(-1)
    }
}
