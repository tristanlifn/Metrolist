package com.metrolist.desktop.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.utils.safeEdit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Google rotates session cookies in the browser while YouTube is open there, which leaves an imported copy
 * signed out. While the app is signed in, this re-imports the browser's cookies whenever its cookie database changes.
 */
fun CoroutineScope.keepBrowserSessionFresh(dataStore: DataStore<Preferences>) =
    launch(Dispatchers.IO) {
        var browser: DefaultBrowser.Firefox? = null
        var lastModified: List<Long>? = null
        while (isActive) {
            val saved = dataStore.data.first()[InnerTubeCookieKey]
            if (saved != null) {
                val profile = browser ?: (BrowserCookies.defaultBrowser() as? DefaultBrowser.Firefox)?.also { browser = it }
                if (profile != null) {
                    val modified = listOf("", "-wal").map { File(profile.profileDir, "cookies.sqlite$it").lastModified() }
                    if (modified != lastModified) {
                        lastModified = modified
                        BrowserCookies
                            .youTubeMusicCookie(profile)
                            ?.takeIf { it != saved }
                            ?.let { fresh -> dataStore.safeEdit { it[InnerTubeCookieKey] = fresh } }
                    }
                }
            }
            delay(CHECK_INTERVAL_MS)
        }
    }

private const val CHECK_INTERVAL_MS = 60_000L
