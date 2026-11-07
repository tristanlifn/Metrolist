package com.metrolist.music.resources

import androidx.compose.ui.unit.Density
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageVector
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** Checks that strings copied from app/src/main/res read the same way Android reads them. */
class AppResourcesTest {
    private val originalLocale = Locale.getDefault()

    @After
    fun restoreLocale() = Locale.setDefault(originalLocale)

    @Test
    fun `android escapes and format arguments are handled`() =
        runBlocking {
            Locale.setDefault(Locale.ENGLISH)
            assertEquals("Couldn't open the app settings", getString(Res.string.open_app_settings_error))
            assertEquals(
                "Tap a section to enable or disable it.\nLong-press the drag handle to reorder. The first section will be shown by default",
                getString(Res.string.android_auto_reorder_hint),
            )
            assertEquals(
                "Do you really want to delete the playlist \"Mix\"?",
                getString(Res.string.delete_playlist_confirm, "Mix"),
            )
            assertEquals("3 songs are already in your playlist", getString(Res.string.duplicates_description_multiple, 3))
            assertEquals("42%", getString(Res.string.percentage_format, 42))
            assertEquals("1 song", getPluralString(Res.plurals.n_song, 1, 1))
            assertEquals("5 songs", getPluralString(Res.plurals.n_song, 5, 5))
        }

    @Test
    fun `remapped locale folders are selected`() =
        runBlocking {
            Locale.setDefault(Locale.forLanguageTag("he"))
            assertEquals(
                "האם אתה באמת רוצה למחוק את רשימת ההשמעה \"Mix\"?",
                getString(Res.string.delete_playlist_confirm, "Mix"),
            )
            Locale.setDefault(Locale.forLanguageTag("sr-Latn"))
            assertEquals(
                "Da li zaista želite da obrišete plejlistu \"Mix\"?",
                getString(Res.string.delete_playlist_confirm, "Mix"),
            )
        }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun `every vector icon parses`() =
        runBlocking {
            val failures =
                Res.allDrawableResources.keys.mapNotNull { name ->
                    val bytes = runCatching { Res.readBytes("drawable/$name.xml") }.getOrNull() ?: return@mapNotNull null
                    runCatching { bytes.decodeToImageVector(Density(1f)) }.exceptionOrNull()?.let { "$name: ${it.message}" }
                }
            assertEquals(emptyList<String>(), failures)
        }
}
