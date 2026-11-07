/**
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.desktop.login

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** The user's default browser, as far as reading its cookies goes. */
sealed interface DefaultBrowser {
    val name: String

    data class Firefox(
        override val name: String,
        val profileDir: File,
    ) : DefaultBrowser

    data class Unsupported(
        override val name: String,
    ) : DefaultBrowser
}

object BrowserCookies {
    private val home = File(System.getProperty("user.home"))

    // Firefox-family browsers and where their profiles live, newest location first.
    private val firefoxFamily =
        listOf(
            Triple("firefox", "Firefox", listOf(".config/mozilla/firefox", ".mozilla/firefox")),
            Triple("librewolf", "LibreWolf", listOf(".config/librewolf/librewolf", ".librewolf")),
            Triple("zen", "Zen", listOf(".config/zen", ".zen")),
        )

    fun defaultBrowser(): DefaultBrowser {
        val desktopId =
            runCatching { ProcessBuilder("xdg-settings", "get", "default-web-browser").start().inputStream.bufferedReader().readText().trim() }
                .getOrDefault("")
                .lowercase()
        val match = firefoxFamily.firstOrNull { (id, _, _) -> desktopId.contains(id) }
        if (match != null) {
            return profile(match.third)?.let { DefaultBrowser.Firefox(match.second, it) } ?: DefaultBrowser.Unsupported(match.second)
        }
        val known = listOf("chrom" to "Chromium", "chrome" to "Chrome", "brave" to "Brave", "vivaldi" to "Vivaldi", "opera" to "Opera")
        known.firstOrNull { (id, _) -> desktopId.contains(id) }?.let { return DefaultBrowser.Unsupported(it.second) }
        // Launcher wrappers (e.g. Omarchy's) hide the real browser; use a Firefox-family profile if there is one.
        firefoxFamily.firstNotNullOfOrNull { (_, name, dirs) -> profile(dirs)?.let { DefaultBrowser.Firefox(name, it) } }
            ?.let { return it }
        return DefaultBrowser.Unsupported(desktopId.removeSuffix(".desktop").ifBlank { "your browser" })
    }

    /** The cookies a browser would send to music.youtube.com, as a Cookie header, or null if signed out. */
    fun youTubeMusicCookie(browser: DefaultBrowser.Firefox): String? {
        val source = File(browser.profileDir, "cookies.sqlite").takeIf { it.exists() } ?: return null
        // Firefox keeps the database locked and recent writes in the WAL file, so read a copy of both.
        val copyDir = Files.createTempDirectory("metrolist-cookies").toFile()
        try {
            listOf("", "-wal").forEach { suffix ->
                val file = File(source.path + suffix)
                if (file.exists()) Files.copy(file.toPath(), File(copyDir, "cookies.sqlite$suffix").toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            val nowSeconds = System.currentTimeMillis() / 1000
            val cookies = mutableListOf<Pair<String, String>>()
            BundledSQLiteDriver().open(File(copyDir, "cookies.sqlite").path).use { connection ->
                connection
                    .prepare("SELECT name, value FROM moz_cookies WHERE host IN ('.youtube.com', 'youtube.com', 'music.youtube.com', '.music.youtube.com') AND expiry > ? ORDER BY host")
                    .use { statement ->
                        statement.bindLong(1, nowSeconds)
                        while (statement.step()) cookies += statement.getText(0) to statement.getText(1)
                    }
            }
            val names = cookies.map { it.first }.toSet()
            if ("SAPISID" !in names && "__Secure-3PAPISID" !in names) return null
            return cookies.distinctBy { it.first }.joinToString("; ") { (name, value) -> "$name=$value" }
        } finally {
            copyDir.deleteRecursively()
        }
    }

    // Uses profiles.ini: the install's default profile if set, otherwise the profile marked Default=1.
    private fun profile(candidates: List<String>): File? {
        for (relative in candidates) {
            val root = File(home, relative)
            val ini = File(root, "profiles.ini").takeIf { it.exists() } ?: continue
            val sections = mutableMapOf<String, MutableMap<String, String>>()
            var current = ""
            ini.readLines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("[") -> current = trimmed.removeSurrounding("[", "]").also { sections[it] = mutableMapOf() }
                    "=" in trimmed -> sections[current]?.put(trimmed.substringBefore("="), trimmed.substringAfter("="))
                }
            }
            val installDefault = sections.entries.firstOrNull { it.key.startsWith("Install") }?.value?.get("Default")
            val path = installDefault ?: sections.values.firstOrNull { it["Default"] == "1" && it.containsKey("Path") }?.get("Path") ?: continue
            val dir = if (File(path).isAbsolute) File(path) else File(root, path)
            if (dir.isDirectory) return dir
        }
        return null
    }
}

/** Developer check: reports which browser login would use and whether it's signed in, without printing cookie values. */
fun runLoginCheck() {
    val browser = BrowserCookies.defaultBrowser()
    println("default browser: $browser")
    if (browser is DefaultBrowser.Firefox) {
        val cookie = BrowserCookies.youTubeMusicCookie(browser)
        println(if (cookie == null) "signed in: no" else "signed in: yes (${cookie.split("; ").size} cookies)")
    }
}
