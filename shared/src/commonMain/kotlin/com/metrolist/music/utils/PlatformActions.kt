/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString

private val resourceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/** Small OS integrations the shared UI needs: transient messages, the share sheet, the clipboard and exported files. */
interface PlatformActions {
    fun toast(message: String, long: Boolean = false)

    fun toast(message: StringResource, vararg formatArgs: Any, long: Boolean = false) {
        resourceScope.launch { toast(getString(message, *formatArgs), long) }
    }

    fun shareText(text: String, subject: String? = null)

    fun copyToClipboard(label: String, text: String)

    /** Writes [content] to a file named [fileName] and opens the share sheet for it. */
    fun shareFile(fileName: String, mimeType: String, content: String, chooserTitle: String?): Result<Unit>

    /** Saves [content] as [fileName] in the user's documents folder. */
    fun saveToDocuments(fileName: String, mimeType: String, content: String): Result<Unit>
}
