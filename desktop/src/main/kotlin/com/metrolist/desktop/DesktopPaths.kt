/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop

import java.io.File

/** Where the desktop app keeps its data, following the XDG base directory spec. */
object DesktopPaths {
    val dataDir: File by lazy {
        val base = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() } ?: "${System.getProperty("user.home")}/.local/share"
        File(base, "metrolist").apply { mkdirs() }
    }

    val documentsDir: File
        get() = File(System.getProperty("user.home"), "Documents")
}
