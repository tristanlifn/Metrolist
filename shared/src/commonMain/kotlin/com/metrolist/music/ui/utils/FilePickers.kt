/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.InputStream
import java.io.OutputStream

/** A file the user picked; both functions block, so call them off the main thread. */
class PickedFile(
    val nameAndSize: () -> Pair<String, Long>,
    val openStream: () -> InputStream,
)

/** A picked, square-cropped image: [location] can be stored and loaded by Coil, [readBytes] gives its contents. */
class PickedImage(
    val location: String,
    val readBytes: () -> ByteArray?,
)

/** Returns a function that opens the platform's picker for audio files to upload, or null where there is none. */
val LocalAudioFilesPicker =
    staticCompositionLocalOf<@Composable (onPicked: (List<PickedFile>) -> Unit) -> (() -> Unit)?> { { null } }

/** Returns a function that picks and crops a playlist cover, or null where there is none. */
val LocalPlaylistCoverPicker =
    staticCompositionLocalOf<@Composable (title: String, onPicked: (PickedImage) -> Unit) -> (() -> Unit)?> { { _, _ -> null } }

/** Returns a function that opens the platform's picker for one file of the given MIME types, or null where there is none. */
val LocalOpenFilePicker =
    staticCompositionLocalOf<@Composable (onPicked: (PickedFile) -> Unit) -> ((mimeTypes: Array<String>) -> Unit)?> { { null } }

/**
 * Returns a function that asks where to save a new file under a suggested name, or null where there is none.
 * [onPicked] receives a function that opens the chosen file for writing; it blocks, so call it off the main thread.
 */
val LocalSaveFilePicker =
    staticCompositionLocalOf<@Composable (mimeType: String, onPicked: (openStream: () -> OutputStream) -> Unit) -> ((suggestedName: String) -> Unit)?> {
        { _, _ -> null }
    }
