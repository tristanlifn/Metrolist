/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.io.InputStream

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
