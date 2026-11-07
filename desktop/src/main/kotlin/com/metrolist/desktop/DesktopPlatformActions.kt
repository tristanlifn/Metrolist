/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import com.metrolist.music.resources.Res
import com.metrolist.music.resources.copied_to_clipboard
import com.metrolist.music.utils.PlatformActions
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import java.net.URLEncoder

class DesktopPlatformActions : PlatformActions {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)

    /** Toast texts, shown by the window as snackbars. */
    val messages: SharedFlow<String> = _messages

    override fun toast(message: String, long: Boolean) {
        _messages.tryEmit(message)
    }

    // Linux has no system share sheet, so sharing text copies it instead.
    override fun shareText(text: String, subject: String?, chooserTitle: String?) {
        copyToClipboard(subject ?: "text", text)
        toast(Res.string.copied_to_clipboard)
    }

    override fun searchWeb(query: String) {
        openInBrowser("https://www.google.com/search?q=${URLEncoder.encode(query, Charsets.UTF_8)}")
    }

    override fun copyToClipboard(label: String, text: String) {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    }

    override fun shareFile(fileName: String, mimeType: String, content: String, chooserTitle: String?): Result<Unit> =
        saveToDocuments(fileName, mimeType, content)

    override fun saveToDocuments(fileName: String, mimeType: String, content: String): Result<Unit> =
        runCatching {
            val dir = File(DesktopPaths.documentsDir, "MetrolistExports").apply { mkdirs() }
            val base = fileName.substringBeforeLast('.')
            val extension = fileName.substringAfterLast('.', "")
            var file = File(dir, fileName)
            var counter = 1
            while (file.exists()) {
                file = File(dir, if (extension.isEmpty()) "${base}_$counter" else "${base}_$counter.$extension")
                counter++
            }
            file.writeText(content)
        }

    override val supportsBlur = true
    override val supportsDynamicIcon = false

    override fun setDynamicIcon(enabled: Boolean) = Unit

    override val hasSystemAppLanguageSettings = false

    override fun openSystemAppLanguageSettings() = Unit

    override val hasDefaultLinkSettings = false

    override fun openDefaultLinkSettings() = false

    override val isAndroidAutoInstalled = false

    private fun openInBrowser(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            } else {
                ProcessBuilder("xdg-open", url).start()
            }
        }
    }
}
