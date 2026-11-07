/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db


import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.core.content.contentValuesOf
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.SongArtistMap
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

val MusicDatabase.openHelper: SupportSQLiteOpenHelper
    get() = delegate.openHelper

internal actual suspend fun InternalDatabase.inTransaction(block: suspend () -> Unit) =
    withTransaction { block() }

/**
 * Reads the SQLite user_version pragma from a database file without
 * involving Room. Returns -1 if the file cannot be read.
 */
fun InternalDatabase.Companion.readDatabaseVersion(dbPath: String): Int {
    return try {
        val file = File(dbPath)
        if (!file.exists()) return -1
        android.database.sqlite.SQLiteDatabase.openDatabase(
            dbPath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY,
        ).use { rawDb ->
            rawDb.version
        }
    } catch (e: Exception) {
        Timber.tag("MusicDatabase").e(e, "Failed to read database version from $dbPath")
        -1
    }
}

fun InternalDatabase.Companion.build(
    context: Context,
    dbName: String = DB_NAME,
    withPragmaCallback: Boolean = true,
): InternalDatabase {
    val builder = Room
        .databaseBuilder(context, InternalDatabase::class.java, dbName)
        .openHelperFactory(BackupBeforeMigrationFactory(context, dbName))
        .addMigrations(
            MIGRATION_1_2,
            MIGRATION_21_24,
            MIGRATION_22_24,
            MIGRATION_24_25,
        ).fallbackToDestructiveMigration(false)
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setTransactionExecutor(
            java.util.concurrent.Executors
                .newFixedThreadPool(4),
        ).setQueryExecutor(
            java.util.concurrent.Executors
                .newFixedThreadPool(4),
        )

    if (withPragmaCallback) {
        builder.addCallback(
            object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    applyPragmaSettings(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    applyPragmaSettings(db)
                }

                override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                    super.onDestructiveMigration(db)
                    backupDatabase(context, dbName)
                }
            },
        )
    }

    return builder.build()
}

fun InternalDatabase.Companion.newInstance(context: Context): MusicDatabase =
    MusicDatabase(delegate = build(context))

fun InternalDatabase.Companion.newInternalDatabaseInstance(context: Context, dbName: String = DB_NAME): InternalDatabase =
    build(context, dbName)

private fun applyPragmaSettings(db: SupportSQLiteDatabase) {
    try {
        db.query("PRAGMA busy_timeout = 60000").close()
        db.query("PRAGMA cache_size = -16000").close()
        db.query("PRAGMA wal_autocheckpoint = 1000").close()
        db.query("PRAGMA synchronous = NORMAL").close()
    } catch (e: Exception) {
        Timber.tag("MusicDatabase").e(e, "Failed to set PRAGMA settings")
    }
}

private fun backupDatabase(
    context: Context,
    dbName: String,
): File? {
    val dbFile = context.getDatabasePath(dbName)
    if (!dbFile.exists()) return null

    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
    val backupDir = File(context.filesDir, "database_backups").apply { mkdirs() }
    val backupBase = File(backupDir, "${dbName}_backup_$timestamp")

    fun copyFile(
        src: File,
        dst: File,
    ): Boolean =
        try {
            src.inputStream().use { input ->
                dst.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Timber.tag("DatabaseBackup").e(e, "Failed to copy ${src.name}")
            false
        }

    val success = copyFile(dbFile, File("$backupBase.db"))
    if (success) {
        File("${dbFile.absolutePath}-wal").takeIf { it.exists() }?.let {
            copyFile(it, File("$backupBase.db-wal"))
        }
        File("${dbFile.absolutePath}-shm").takeIf { it.exists() }?.let {
            copyFile(it, File("$backupBase.db-shm"))
        }
        Timber.tag("DatabaseBackup").i("Backed up database to $backupBase.db")
    }
    return if (success) File("$backupBase.db") else null
}

private class BackupBeforeMigrationFactory(
    private val context: Context,
    private val dbName: String,
    private val delegate: SupportSQLiteOpenHelper.Factory =
        androidx.sqlite.db.framework
            .FrameworkSQLiteOpenHelperFactory(),
) : SupportSQLiteOpenHelper.Factory {
    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val wrappedCallback = BackupCallback(context, configuration.callback, dbName)
        val wrappedConfig = SupportSQLiteOpenHelper.Configuration.builder(configuration.context)
            .name(configuration.name)
            .callback(wrappedCallback)
            .noBackupDirectory(configuration.useNoBackupDirectory)
            .allowDataLossOnRecovery(configuration.allowDataLossOnRecovery)
            .build()
        return delegate.create(wrappedConfig)
    }
}

private class BackupCallback(
    private val context: Context,
    private val delegate: SupportSQLiteOpenHelper.Callback,
    private val dbName: String,
) : SupportSQLiteOpenHelper.Callback(delegate.version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
        applyPragmaSettings(db)
        delegate.onCreate(db)
    }

    override fun onUpgrade(
        db: SupportSQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        Timber.tag("DatabaseBackup").i("Database upgrade $oldVersion -> $newVersion, backing up first")
        backupDatabase(context, dbName)
        delegate.onUpgrade(db, oldVersion, newVersion)
    }

    override fun onDowngrade(
        db: SupportSQLiteDatabase,
        oldVersion: Int,
        newVersion: Int,
    ) {
        Timber.tag("DatabaseBackup").i("Database downgrade $oldVersion -> $newVersion, backing up first")
        backupDatabase(context, dbName)
        delegate.onDowngrade(db, oldVersion, newVersion)
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        applyPragmaSettings(db)
        delegate.onOpen(db)
    }
}

// ===== Migrations =====

private fun addColumnIfMissing(
    db: SupportSQLiteDatabase,
    tableName: String,
    columnName: String,
    columnDefinition: String,
) {
    var columnExists = false
    db.query("PRAGMA table_info('$tableName')").use { cursor ->
        val nameIndex = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                columnExists = true
                break
            }
        }
    }

    if (!columnExists) {
        db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
    }
}

private fun addVersion24ColumnsIfMissing(db: SupportSQLiteDatabase) {
    addColumnIfMissing(db, "song", "libraryAddToken", "TEXT")
    addColumnIfMissing(db, "song", "libraryRemoveToken", "TEXT")
    addColumnIfMissing(db, "song", "romanizeLyrics", "INTEGER NOT NULL DEFAULT true")
    addColumnIfMissing(db, "song", "isDownloaded", "INTEGER NOT NULL DEFAULT 0")
    addColumnIfMissing(db, "song", "isUploaded", "INTEGER NOT NULL DEFAULT false")
    addColumnIfMissing(db, "album", "isUploaded", "INTEGER NOT NULL DEFAULT false")
    addColumnIfMissing(db, "playlist", "thumbnailUrl", "TEXT")
}

val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            data class OldSongEntity(
                val id: String,
                val title: String,
                val duration: Int = -1,
                val thumbnailUrl: String? = null,
                val albumId: String? = null,
                val albumName: String? = null,
                val liked: Boolean = false,
                val totalPlayTime: Long = 0,
                val downloadState: Int = 0,
                val createDate: LocalDateTime = LocalDateTime.now(),
                val modifyDate: LocalDateTime = LocalDateTime.now(),
            )

            val converters = Converters()
            val artistMap = mutableMapOf<Int, String>()
            val artists = mutableListOf<ArtistEntity>()
            db.query("SELECT * FROM artist").use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = ArtistEntity.generateArtistId()
                    artistMap[oldId] = newId
                    artists.add(
                        ArtistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }

            val playlistMap = mutableMapOf<Int, String>()
            val playlists = mutableListOf<PlaylistEntity>()
            db.query("SELECT * FROM playlist").use { cursor ->
                while (cursor.moveToNext()) {
                    val oldId = cursor.getInt(0)
                    val newId = PlaylistEntity.generatePlaylistId()
                    playlistMap[oldId] = newId
                    playlists.add(
                        PlaylistEntity(
                            id = newId,
                            name = cursor.getString(1),
                        ),
                    )
                }
            }
            val playlistSongMaps = mutableListOf<PlaylistSongMap>()
            db.query("SELECT * FROM playlist_song").use { cursor ->
                while (cursor.moveToNext()) {
                    playlistSongMaps.add(
                        PlaylistSongMap(
                            playlistId = playlistMap[cursor.getInt(1)]!!,
                            songId = cursor.getString(2),
                            position = cursor.getInt(3),
                        ),
                    )
                }
            }
            playlistSongMaps.sortBy { it.position }
            val playlistSongCount = mutableMapOf<String, Int>()
            playlistSongMaps.map { map ->
                if (map.playlistId !in playlistSongCount) playlistSongCount[map.playlistId] = 0
                map.copy(position = playlistSongCount[map.playlistId]!!).also {
                    playlistSongCount[map.playlistId] = playlistSongCount[map.playlistId]!! + 1
                }
            }
            val songs = mutableListOf<OldSongEntity>()
            val songArtistMaps = mutableListOf<SongArtistMap>()
            db.query("SELECT * FROM song").use { cursor ->
                while (cursor.moveToNext()) {
                    val songId = cursor.getString(0)
                    songs.add(
                        OldSongEntity(
                            id = songId,
                            title = cursor.getString(1),
                            duration = cursor.getInt(3),
                            liked = cursor.getInt(4) == 1,
                            createDate =
                                Instant
                                    .ofEpochMilli(Date(cursor.getLong(8)).time)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDateTime(),
                            modifyDate =
                                Instant
                                    .ofEpochMilli(Date(cursor.getLong(9)).time)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDateTime(),
                        ),
                    )
                    songArtistMaps.add(
                        SongArtistMap(
                            songId = songId,
                            artistId = artistMap[cursor.getInt(2)]!!,
                            position = 0,
                        ),
                    )
                }
            }
            db.execSQL("DROP TABLE IF EXISTS song")
            db.execSQL("DROP TABLE IF EXISTS artist")
            db.execSQL("DROP TABLE IF EXISTS playlist")
            db.execSQL("DROP TABLE IF EXISTS playlist_song")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `duration` INTEGER NOT NULL, `thumbnailUrl` TEXT, `albumId` TEXT, `albumName` TEXT, `liked` INTEGER NOT NULL, `totalPlayTime` INTEGER NOT NULL, `isTrash` INTEGER NOT NULL, `download_state` INTEGER NOT NULL, `create_date` INTEGER NOT NULL, `modify_date` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `artist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `thumbnailUrl` TEXT, `bannerUrl` TEXT, `description` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `year` INTEGER, `thumbnailUrl` TEXT, `songCount` INTEGER NOT NULL, `duration` INTEGER NOT NULL, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `author` TEXT, `authorId` TEXT, `year` INTEGER, `thumbnailUrl` TEXT, `createDate` INTEGER NOT NULL, `lastUpdateTime` INTEGER NOT NULL, PRIMARY KEY(`id`))",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_artist_map` (`songId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`songId`, `artistId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_songId` ON `song_artist_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_artist_map_artistId` ON `song_artist_map` (`artistId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `song_album_map` (`songId` TEXT NOT NULL, `albumId` TEXT NOT NULL, `index` INTEGER, PRIMARY KEY(`songId`, `albumId`), FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_songId` ON `song_album_map` (`songId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_song_album_map_albumId` ON `song_album_map` (`albumId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `album_artist_map` (`albumId` TEXT NOT NULL, `artistId` TEXT NOT NULL, `order` INTEGER NOT NULL, PRIMARY KEY(`albumId`, `artistId`), FOREIGN KEY(`albumId`) REFERENCES `album`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`artistId`) REFERENCES `artist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_albumId` ON `album_artist_map` (`albumId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_album_artist_map_artistId` ON `album_artist_map` (`artistId`)")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `playlist_song_map` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` TEXT NOT NULL, `songId` TEXT NOT NULL, `position` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlist`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`songId`) REFERENCES `song`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_playlistId` ON `playlist_song_map` (`playlistId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_song_map_songId` ON `playlist_song_map` (`songId`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `download` (`id` INTEGER NOT NULL, `songId` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `search_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `query` TEXT NOT NULL)",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_search_history_query` ON `search_history` (`query`)")
            db.execSQL("CREATE VIEW `sorted_song_artist_map` AS SELECT * FROM song_artist_map ORDER BY position")
            db.execSQL(
                "CREATE VIEW `playlist_song_map_preview` AS SELECT * FROM playlist_song_map WHERE position <= 3 ORDER BY position",
            )
            artists.forEach { artist ->
                db.insert(
                    "artist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to artist.id,
                        "name" to artist.name,
                        "createDate" to converters.dateToTimestamp(artist.lastUpdateTime),
                        "lastUpdateTime" to converters.dateToTimestamp(artist.lastUpdateTime),
                    ),
                )
            }
            songs.forEach { song ->
                db.insert(
                    "song",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to song.id,
                        "title" to song.title,
                        "duration" to song.duration,
                        "liked" to song.liked,
                        "totalPlayTime" to song.totalPlayTime,
                        "isTrash" to false,
                        "download_state" to song.downloadState,
                        "create_date" to converters.dateToTimestamp(song.createDate),
                        "modify_date" to converters.dateToTimestamp(song.modifyDate),
                    ),
                )
            }
            songArtistMaps.forEach { songArtistMap ->
                db.insert(
                    "song_artist_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "songId" to songArtistMap.songId,
                        "artistId" to songArtistMap.artistId,
                        "position" to songArtistMap.position,
                    ),
                )
            }
            playlists.forEach { playlist ->
                db.insert(
                    "playlist",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "id" to playlist.id,
                        "name" to playlist.name,
                        "createDate" to converters.dateToTimestamp(LocalDateTime.now()),
                        "lastUpdateTime" to converters.dateToTimestamp(LocalDateTime.now()),
                    ),
                )
            }
            playlistSongMaps.forEach { playlistSongMap ->
                db.insert(
                    "playlist_song_map",
                    SQLiteDatabase.CONFLICT_ABORT,
                    contentValuesOf(
                        "playlistId" to playlistSongMap.playlistId,
                        "songId" to playlistSongMap.songId,
                        "position" to playlistSongMap.position,
                    ),
                )
            }
        }
    }

val MIGRATION_21_24 =
    object : Migration(21, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addVersion24ColumnsIfMissing(db)
        }
    }

val MIGRATION_22_24 =
    object : Migration(22, 24) {
        override fun migrate(db: SupportSQLiteDatabase) {
            addVersion24ColumnsIfMissing(db)
        }
    }

val MIGRATION_24_25 =
    object : Migration(24, 25) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add perceptualLoudnessDb column to format table for improved audio normalization
            var columnExists = false
            db.query("PRAGMA table_info(format)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (cursor.getString(nameIndex) == "perceptualLoudnessDb") {
                        columnExists = true
                        break
                    }
                }
            }

            if (!columnExists) {
                // Add the column allowing NULL values (since existing rows won't have this data)
                db.execSQL("ALTER TABLE format ADD COLUMN perceptualLoudnessDb REAL DEFAULT NULL")
            }
        }
    }
