package com.metrolist.music.db

import androidx.room.useReaderConnection
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.metrolist.music.db.entities.SongEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File

class DesktopDatabaseTest {
    private val dbFile = File.createTempFile("metrolist", ".db").apply { delete() }

    @After
    fun tearDown() {
        dbFile.parentFile.listFiles { file -> file.name.startsWith(dbFile.name) }?.forEach(File::delete)
    }

    @Test
    fun `auto migrations from version 5 keep data and reach the current schema`() =
        runBlocking<Unit> {
            BundledSQLiteDriver().open(dbFile.path).use { connection ->
                connection.createSchema(version = 5)
                connection.execSQL(
                    "INSERT INTO playlist (id, name, createDate, lastUpdateTime) VALUES " +
                        "('PLremote', 'Remote', 1, 1), ('LPlocal', 'Local', 1, 1)",
                )
                connection.execSQL(
                    "INSERT INTO song (id, title, duration, albumId, albumName, liked, totalPlayTime, isTrash, " +
                        "download_state, create_date, modify_date) VALUES " +
                        "('song1', 'Song', 100, 'MPREb_album', 'Album X', 0, 0, 0, 0, 1700000000000, 1700000000000), " +
                        "('song2', 'Loose', 100, NULL, NULL, 0, 0, 0, 0, 5, 5)",
                )
            }

            val database = InternalDatabase.build(dbFile)
            try {
                assertNotNull(database.dao.getSongByIdBlocking("song1"))
                database.useReaderConnection { transactor ->
                    transactor.usePrepared("SELECT browseId FROM playlist WHERE id = 'PLremote'") {
                        it.step()
                        assertEquals("PLremote", it.getText(0))
                    }
                    transactor.usePrepared("SELECT browseId FROM playlist WHERE id = 'LPlocal'") {
                        it.step()
                        assertNull(if (it.isNull(0)) null else it.getText(0))
                    }
                    transactor.usePrepared("SELECT inLibrary FROM song WHERE id = 'song1'") {
                        it.step()
                        assertEquals(1700000000000, it.getLong(0))
                    }
                    transactor.usePrepared("SELECT title FROM album WHERE id = 'MPREb_album'") {
                        it.step()
                        assertEquals("Album X", it.getText(0))
                    }
                    transactor.usePrepared("SELECT COUNT(*) FROM album") {
                        it.step()
                        assertEquals(1, it.getLong(0))
                    }
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun `withTransaction commits on success and rolls back on failure`() =
        runBlocking<Unit> {
            val database = MusicDatabase(InternalDatabase.build(dbFile))
            try {
                database.withTransaction { insert(SongEntity(id = "kept", title = "Kept")) }
                runCatching {
                    database.withTransaction {
                        insert(SongEntity(id = "dropped", title = "Dropped"))
                        error("abort")
                    }
                }
                assertNotNull(database.getSongByIdBlocking("kept"))
                assertNull(database.getSongByIdBlocking("dropped"))
            } finally {
                database.close()
            }
        }

    private fun SQLiteConnection.createSchema(version: Int) {
        val schema =
            Json.parseToJsonElement(
                File("schemas/com.metrolist.music.db.InternalDatabase/$version.json").readText(),
            ).jsonObject["database"]!!.jsonObject

        fun JsonArrayOf(key: String) = schema[key]?.jsonArray.orEmpty()
        JsonArrayOf("entities").forEach { entity ->
            val table = entity.jsonObject["tableName"]!!.jsonPrimitive.content
            execSQL(entity.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            entity.jsonObject["indices"]?.jsonArray.orEmpty().forEach { index ->
                execSQL(index.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${TABLE_NAME}", table))
            }
        }
        JsonArrayOf("views").forEach { view ->
            val name = view.jsonObject["viewName"]!!.jsonPrimitive.content
            execSQL(view.jsonObject["createSql"]!!.jsonPrimitive.content.replace("\${VIEW_NAME}", name))
        }
        JsonArrayOf("setupQueries").forEach { execSQL(it.jsonPrimitive.content) }
        execSQL("PRAGMA user_version = $version")
    }
}
