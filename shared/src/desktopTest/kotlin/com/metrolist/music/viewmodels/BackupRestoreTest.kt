package com.metrolist.music.viewmodels

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.db.build
import com.metrolist.music.db.entities.SongEntity
import com.metrolist.music.ui.utils.PickedFile
import com.metrolist.music.utils.BackupPlatform
import com.metrolist.music.utils.PlatformActions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupRestoreTest {
    private val dir = Files.createTempDirectory("metrolist-backup").toFile()
    private val platform = FakeBackupPlatform(dir)
    private val actions = FakePlatformActions()

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    @Test
    fun `restore brings back the backed-up library and settings, then restarts`() =
        runBlocking<Unit> {
            val backupFile = File(dir, "metrolist.backup")
            platform.settingsFile.writeText("before")
            val database = MusicDatabase(InternalDatabase.build(platform.databaseFile))
            database.insert(SongEntity(id = "kept", title = "Kept"))

            val viewModel = BackupRestoreViewModel(database, platform, actions)
            viewModel.backup(backupFile::outputStream)
            assertEquals("backup_create_success", actions.nextMessage())

            database.insert(SongEntity(id = "added", title = "Added later"))
            platform.settingsFile.writeText("after")
            platform.playerStateFiles.single().writeText("old queue")

            viewModel.restore(PickedFile(nameAndSize = { backupFile.name to backupFile.length() }, openStream = backupFile::inputStream))
            withTimeout(10_000) { platform.restarted.await() }

            assertEquals("before", platform.settingsFile.readText())
            assertFalse(platform.playerStateFiles.single().exists())
            val restored = MusicDatabase(InternalDatabase.build(platform.databaseFile))
            try {
                assertNotNull(restored.getSongByIdBlocking("kept"))
                assertNull(restored.getSongByIdBlocking("added"))
            } finally {
                restored.close()
            }
        }

    @Test
    fun `restore refuses a database older than the platform can migrate`() =
        runBlocking<Unit> {
            val oldDb = File(dir, "old.db")
            BundledSQLiteDriver().open(oldDb.path).use { it.execSQL("PRAGMA user_version = 5") }
            val backupFile = File(dir, "old.backup")
            ZipOutputStream(backupFile.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry(InternalDatabase.DB_NAME))
                oldDb.inputStream().use { it.copyTo(zip) }
            }
            val database = MusicDatabase(InternalDatabase.build(platform.databaseFile))
            database.insert(SongEntity(id = "current", title = "Current"))

            BackupRestoreViewModel(database, platform, actions)
                .restore(PickedFile(nameAndSize = { backupFile.name to backupFile.length() }, openStream = backupFile::inputStream))

            assertEquals("restore_database_incompatible", actions.nextMessage())
            assertFalse(platform.restarted.isCompleted)
            assertNotNull(database.getSongByIdBlocking("current"))
            database.close()
        }
}

private class FakeBackupPlatform(
    dir: File,
) : BackupPlatform {
    val restarted = CompletableDeferred<Unit>()
    override val settingsFile = File(dir, BackupRestoreViewModel.SETTINGS_FILENAME)
    override val databaseFile = File(dir, InternalDatabase.DB_NAME)
    override val playerStateFiles = listOf(File(dir, "queue"))
    override val oldestRestorableDatabaseVersion = 25

    override suspend fun stopPlayback() = Unit

    override fun restoreArtistNameAliases(aliases: Map<String, String>) = Unit

    override fun restart() {
        restarted.complete(Unit)
    }
}

// Records toasts by resource key, so tests don't depend on the English text.
private class FakePlatformActions : PlatformActions {
    private val messages = kotlinx.coroutines.channels.Channel<String>(capacity = 10)

    suspend fun nextMessage(): String = withTimeout(10_000) { messages.receive() }

    override fun toast(
        message: String,
        long: Boolean,
    ) {
        messages.trySend(message)
    }

    override fun toast(
        message: org.jetbrains.compose.resources.StringResource,
        vararg formatArgs: Any,
        long: Boolean,
    ) {
        messages.trySend(message.key)
    }

    override fun shareText(
        text: String,
        subject: String?,
        chooserTitle: String?,
    ) = Unit

    override fun searchWeb(query: String) = Unit

    override fun copyToClipboard(
        label: String,
        text: String,
    ) = Unit

    override fun shareFile(
        fileName: String,
        mimeType: String,
        content: String,
        chooserTitle: String?,
    ) = Result.success(Unit)

    override fun saveToDocuments(
        fileName: String,
        mimeType: String,
        content: String,
    ) = Result.success(Unit)

    override val supportsBlur = false
    override val supportsDynamicIcon = false

    override fun setDynamicIcon(enabled: Boolean) = Unit

    override val hasSystemAppLanguageSettings = false

    override fun openSystemAppLanguageSettings() = Unit

    override val hasDefaultLinkSettings = false

    override fun openDefaultLinkSettings() = false

    override val isAndroidAutoInstalled = false
}
