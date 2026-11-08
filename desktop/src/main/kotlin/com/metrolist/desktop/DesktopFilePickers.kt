/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.metrolist.music.ui.utils.PickedFile
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.OutputStream

@Composable
fun rememberDesktopOpenFilePicker(onPicked: (PickedFile) -> Unit): (Array<String>) -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    return {
        showFileDialog(FileDialog.LOAD, null)?.let { file ->
            currentOnPicked(PickedFile(nameAndSize = { file.name to file.length() }, openStream = file::inputStream))
        }
    }
}

@Composable
fun rememberDesktopSaveFilePicker(onPicked: (openStream: () -> OutputStream) -> Unit): (String) -> Unit {
    val currentOnPicked by rememberUpdatedState(onPicked)
    return { suggestedName -> showFileDialog(FileDialog.SAVE, suggestedName)?.let { file -> currentOnPicked(file::outputStream) } }
}

// Modal, so the UI waits while the dialog is open, like it does for the Android pickers.
private fun showFileDialog(
    mode: Int,
    suggestedName: String?,
): File? {
    val dialog = FileDialog(null as Frame?, null, mode).apply { file = suggestedName }
    dialog.isVisible = true
    return dialog.file?.let { File(dialog.directory, it) }
}
