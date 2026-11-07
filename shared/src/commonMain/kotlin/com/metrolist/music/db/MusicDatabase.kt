/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.DeleteTable
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.metrolist.music.db.daos.SpeedDialDao
import com.metrolist.music.db.entities.AlbumArtistMap
import com.metrolist.music.db.entities.AlbumEntity
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.db.entities.Event
import com.metrolist.music.db.entities.FormatEntity
import com.metrolist.music.db.entities.LyricsEntity
import com.metrolist.music.db.entities.PlayCountEntity
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.PlaylistSongMapPreview
import com.metrolist.music.db.entities.PodcastEntity
import com.metrolist.music.db.entities.RecognitionHistory
import com.metrolist.music.db.entities.RelatedSongMap
import com.metrolist.music.db.entities.SearchHistory
import com.metrolist.music.db.entities.SetVideoIdEntity
import com.metrolist.music.db.entities.SongAlbumMap
import com.metrolist.music.db.entities.SongArtistMap
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.db.entities.SortedSongAlbumMap
import com.metrolist.music.db.entities.SortedSongArtistMap
import com.metrolist.music.db.entities.SpeedDialItem
import com.metrolist.innertube.utils.Logger
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class MusicDatabase(
    internal val delegate: InternalDatabase,
) : DatabaseDao by delegate.dao {
    val speedDialDao: SpeedDialDao
        get() = delegate.speedDialDao

    /** Runs [block] on Room's query context without waiting for it. */
    fun query(block: suspend MusicDatabase.() -> Unit) {
        delegate.getCoroutineScope().launch {
            block(this@MusicDatabase)
        }
    }

    /** Runs [block] in a write transaction on Room's query context without waiting for it. */
    fun transaction(block: suspend MusicDatabase.() -> Unit) {
        delegate.getCoroutineScope().launch {
            withTransaction(block)
        }
    }

    suspend fun withTransaction(block: suspend MusicDatabase.() -> Unit) =
        delegate.inTransaction {
            block(this@MusicDatabase)
        }

    fun close() = delegate.close()
}

internal expect suspend fun InternalDatabase.inTransaction(block: suspend () -> Unit)

@Database(
    entities = [
        SongEntity::class,
        ArtistEntity::class,
        AlbumEntity::class,
        PlaylistEntity::class,
        SongArtistMap::class,
        SongAlbumMap::class,
        AlbumArtistMap::class,
        PlaylistSongMap::class,
        SearchHistory::class,
        FormatEntity::class,
        LyricsEntity::class,
        Event::class,
        RelatedSongMap::class,
        SetVideoIdEntity::class,
        PlayCountEntity::class,
        RecognitionHistory::class,
        SpeedDialItem::class,
        PodcastEntity::class,
    ],
    views = [
        SortedSongArtistMap::class,
        SortedSongAlbumMap::class,
        PlaylistSongMapPreview::class,
    ],
    version = 38,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = Migration5To6::class),
        AutoMigration(from = 6, to = 7, spec = Migration6To7::class),
        AutoMigration(from = 7, to = 8, spec = Migration7To8::class),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10, spec = Migration9To10::class),
        AutoMigration(from = 10, to = 11, spec = Migration10To11::class),
        AutoMigration(from = 11, to = 12, spec = Migration11To12::class),
        AutoMigration(from = 12, to = 13, spec = Migration12To13::class),
        AutoMigration(from = 13, to = 14, spec = Migration13To14::class),
        AutoMigration(from = 14, to = 15),
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17, spec = Migration16To17::class),
        AutoMigration(from = 17, to = 18),
        AutoMigration(from = 18, to = 19, spec = Migration18To19::class),
        AutoMigration(from = 19, to = 20, spec = Migration19To20::class),
        AutoMigration(from = 20, to = 21, spec = Migration20To21::class),
        AutoMigration(from = 21, to = 22, spec = Migration21To22::class),
        AutoMigration(from = 22, to = 23, spec = Migration22To23::class),
        AutoMigration(from = 23, to = 24, spec = Migration23To24::class),
        AutoMigration(from = 24, to = 25),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30, spec = Migration29To30::class),
        AutoMigration(from = 30, to = 31),
        AutoMigration(from = 31, to = 32),
        AutoMigration(from = 32, to = 33),
        AutoMigration(from = 33, to = 34),
        AutoMigration(from = 34, to = 35),
        AutoMigration(from = 35, to = 36, spec = Migration35To36::class),
        AutoMigration(from = 36, to = 37),
        AutoMigration(from = 37, to = 38),
    ],
)
@TypeConverters(Converters::class)
@ConstructedBy(InternalDatabaseConstructor::class)
abstract class InternalDatabase : RoomDatabase() {
    abstract val dao: DatabaseDao
    abstract val speedDialDao: SpeedDialDao

    companion object {
        const val DB_NAME = "song.db"
    }
}

@Suppress("KotlinNoActualForExpect")
expect object InternalDatabaseConstructor : RoomDatabaseConstructor<InternalDatabase> {
    override fun initialize(): InternalDatabase
}

private fun SQLiteConnection.columnNames(tableName: String): Set<String> =
    prepare("PRAGMA table_info('$tableName')").use { statement ->
        val nameIndex = statement.getColumnNames().indexOf("name")
        buildSet {
            while (statement.step()) {
                if (nameIndex >= 0) add(statement.getText(nameIndex))
            }
        }
    }

/** Reads every row of [sql] before returning, so callers can safely write to the same table afterwards. */
private fun <T> SQLiteConnection.readRows(
    sql: String,
    row: (androidx.sqlite.SQLiteStatement) -> T,
): List<T> =
    prepare(sql).use { statement ->
        buildList {
            while (statement.step()) add(row(statement))
        }
    }

private fun addColumnIfMissing(
    connection: SQLiteConnection,
    tableName: String,
    columnName: String,
    columnDefinition: String,
) {
    if (columnName !in connection.columnNames(tableName)) {
        connection.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
    }
}

private fun addVersion24ColumnsIfMissing(connection: SQLiteConnection) {
    addColumnIfMissing(connection, "song", "libraryAddToken", "TEXT")
    addColumnIfMissing(connection, "song", "libraryRemoveToken", "TEXT")
    addColumnIfMissing(connection, "song", "romanizeLyrics", "INTEGER NOT NULL DEFAULT true")
    addColumnIfMissing(connection, "song", "isDownloaded", "INTEGER NOT NULL DEFAULT 0")
    addColumnIfMissing(connection, "song", "isUploaded", "INTEGER NOT NULL DEFAULT false")
    addColumnIfMissing(connection, "album", "isUploaded", "INTEGER NOT NULL DEFAULT false")
    addColumnIfMissing(connection, "playlist", "thumbnailUrl", "TEXT")
}

// ===== AutoMigration Specs =====

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isTrash"),
    DeleteColumn(tableName = "playlist", columnName = "author"),
    DeleteColumn(tableName = "playlist", columnName = "authorId"),
    DeleteColumn(tableName = "playlist", columnName = "year"),
    DeleteColumn(tableName = "playlist", columnName = "thumbnailUrl"),
    DeleteColumn(tableName = "playlist", columnName = "createDate"),
    DeleteColumn(tableName = "playlist", columnName = "lastUpdateTime"),
)
@RenameColumn.Entries(
    RenameColumn(
        tableName = "song",
        fromColumnName = "download_state",
        toColumnName = "downloadState",
    ),
    RenameColumn(tableName = "song", fromColumnName = "create_date", toColumnName = "createDate"),
    RenameColumn(tableName = "song", fromColumnName = "modify_date", toColumnName = "modifyDate"),
)
class Migration5To6 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.readRows("SELECT id FROM playlist WHERE id NOT LIKE 'LP%'") { it.getText(0) }.forEach { id ->
            connection.execSQL("UPDATE playlist SET browseId = '$id' WHERE id = '$id'")
        }
    }
}

class Migration6To7 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.readRows("SELECT id, createDate FROM song") { it.getText(0) to it.getLong(1) }.forEach { (id, createDate) ->
            connection.execSQL("UPDATE song SET inLibrary = $createDate WHERE id = '$id'")
        }
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "createDate"),
    DeleteColumn(tableName = "song", columnName = "modifyDate"),
)
class Migration7To8 : AutoMigrationSpec

@DeleteTable.Entries(
    DeleteTable(tableName = "download"),
)
class Migration9To10 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "downloadState"),
    DeleteColumn(tableName = "artist", columnName = "bannerUrl"),
    DeleteColumn(tableName = "artist", columnName = "description"),
    DeleteColumn(tableName = "artist", columnName = "createDate"),
)
class Migration10To11 : AutoMigrationSpec

@DeleteColumn.Entries(
    DeleteColumn(tableName = "album", columnName = "createDate"),
)
class Migration11To12 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE album SET bookmarkedAt = lastUpdateTime")
        val albums =
            connection.readRows("SELECT DISTINCT albumId, albumName FROM song") {
                (if (it.isNull(0)) null else it.getText(0)) to (if (it.isNull(1)) null else it.getText(1))
            }
        // OR IGNORE also skips rows whose albumId/albumName is NULL, matching the old CONFLICT_IGNORE insert.
        connection.prepare(
            "INSERT OR IGNORE INTO album (id, title, songCount, duration, lastUpdateTime) VALUES (?, ?, 0, 0, 0)",
        ).use { insert ->
            albums.forEach { (albumId, albumName) ->
                if (albumId == null) insert.bindNull(1) else insert.bindText(1, albumId)
                if (albumName == null) insert.bindNull(2) else insert.bindText(2, albumName)
                insert.step()
                insert.reset()
            }
        }
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_song_albumId` ON `song` (`albumId`)")
    }
}

class Migration12To13 : AutoMigrationSpec

class Migration13To14 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE playlist SET createdAt = '${Converters().dateToTimestamp(LocalDateTime.now())}'")
        connection.execSQL(
            "UPDATE playlist SET lastUpdateTime = '${Converters().dateToTimestamp(LocalDateTime.now())}'",
        )
    }
}

@DeleteColumn.Entries(
    DeleteColumn(tableName = "song", columnName = "isLocal"),
    DeleteColumn(tableName = "song", columnName = "localPath"),
    DeleteColumn(tableName = "artist", columnName = "isLocal"),
    DeleteColumn(tableName = "playlist", columnName = "isLocal"),
)
class Migration16To17 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE playlist SET bookmarkedAt = lastUpdateTime")
        connection.execSQL("UPDATE playlist SET isEditable = 1 WHERE browseId IS NOT NULL")
    }
}

class Migration18To19 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

class Migration19To20 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL("UPDATE song SET explicit = 0 WHERE explicit IS NULL")
    }
}

@DeleteColumn.Entries(
    DeleteColumn(
        tableName = "song",
        columnName = "artistName",
    ),
)
class Migration20To21 : AutoMigrationSpec

class Migration21To22 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        listOf(
            "ALTER TABLE song ADD COLUMN libraryAddToken TEXT DEFAULT ''",
            "ALTER TABLE song ADD COLUMN libraryRemoveToken TEXT DEFAULT ''",
            "ALTER TABLE song ADD COLUMN romanizeLyrics INTEGER NOT NULL DEFAULT 1",
            "ALTER TABLE song ADD COLUMN isDownloaded INTEGER NOT NULL DEFAULT 0",
        ).forEach { sql ->
            try {
                connection.execSQL(sql)
            } catch (e: Exception) {
                Logger.w("Migration21To22: column may already exist (${e.message})")
            }
        }
    }
}

class Migration22To23 : AutoMigrationSpec

class Migration23To24 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        addVersion24ColumnsIfMissing(connection)
    }
}

class Migration29To30 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        // Safeguards for columns that some older builds failed to add.
        addColumnIfMissing(connection, "song", "isVideo", "INTEGER NOT NULL DEFAULT 0")
        addColumnIfMissing(connection, "lyrics", "provider", "TEXT NOT NULL DEFAULT 'Unknown'")
    }
}

class Migration35To36 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        addColumnIfMissing(connection, "song", "playbackPosition", "INTEGER DEFAULT NULL")
        addColumnIfMissing(connection, "song", "uploadEntityId", "TEXT DEFAULT NULL")
        addColumnIfMissing(connection, "song", "isCached", "INTEGER NOT NULL DEFAULT 0")
    }
}
